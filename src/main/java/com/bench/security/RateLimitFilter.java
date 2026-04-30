package com.bench.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiting filter using a token-bucket algorithm per authenticated user.
 * Bucket: 10 requests per minute (1 token every 6 seconds).
 * Only applies to /api/calculate; other endpoints pass through.
 * Unauthenticated requests are already rejected by Spring Security.
 * Returns HTTP 429 with {"error": "rate_limit_exceeded", "retry_after_seconds": <int>}
 * and sets "Retry-After" response header when limit is exceeded.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Token bucket per username: [tokensRemaining, lastRefillTime]
    private static final ConcurrentHashMap<String, AtomicLong[]> buckets = new ConcurrentHashMap<>();
    
    private static final int MAX_TOKENS = 10;
    private static final long REFILL_INTERVAL_MILLIS = 6000; // 6 seconds per token
    private static final long MINUTE_MILLIS = 60000; // 60 seconds total
    private static final int HTTP_429_TOO_MANY_REQUESTS = 429;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Only apply rate limiting to /api/calculate endpoint
        String path = request.getRequestURI();
        if (!path.equals("/api/calculate")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get authenticated user from SecurityContextHolder
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            // Unauthenticated requests should not reach here (Spring Security rejects them earlier)
            filterChain.doFilter(request, response);
            return;
        }

        String username = auth.getName();
        if (!isRateLimited(username)) {
            // Request allowed, proceed
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            long retryAfterSeconds = calculateRetryAfter(username);
            response.setStatus(HTTP_429_TOO_MANY_REQUESTS);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "rate_limit_exceeded");
            errorResponse.put("retry_after_seconds", retryAfterSeconds);

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
    }

    /**
     * Check if the user has exceeded the rate limit.
     * Uses token-bucket algorithm: each user gets 10 tokens per minute.
     * Tokens refill at a rate of 1 every 6 seconds.
     *
     * @param username the authenticated username
     * @return true if rate limited (no tokens left), false otherwise
     */
    private boolean isRateLimited(String username) {
        long currentTime = System.currentTimeMillis();
        
        // Get or create bucket for this user
        AtomicLong[] bucket = buckets.computeIfAbsent(username, k -> {
            // Initialize: [tokensRemaining, lastRefillTime]
            return new AtomicLong[]{new AtomicLong(MAX_TOKENS), new AtomicLong(currentTime)};
        });

        AtomicLong tokensRemaining = bucket[0];
        AtomicLong lastRefillTime = bucket[1];

        // Refill tokens based on elapsed time
        long timeSinceLastRefill = currentTime - lastRefillTime.get();
        long tokensToAdd = timeSinceLastRefill / REFILL_INTERVAL_MILLIS;

        if (tokensToAdd > 0) {
            // Add tokens, but cap at MAX_TOKENS
            long newTokens = Math.min(tokensRemaining.get() + tokensToAdd, MAX_TOKENS);
            tokensRemaining.set(newTokens);
            lastRefillTime.set(currentTime);
        }

        // Try to consume a token
        long currentTokens = tokensRemaining.get();
        if (currentTokens > 0) {
            tokensRemaining.decrementAndGet();
            return false; // Not rate limited
        }

        return true; // Rate limited
    }

    /**
     * Calculate how many seconds until the next token is available.
     *
     * @param username the authenticated username
     * @return seconds to wait before retry
     */
    private long calculateRetryAfter(String username) {
        long currentTime = System.currentTimeMillis();
        AtomicLong[] bucket = buckets.get(username);

        if (bucket == null) {
            return 1; // Fallback
        }

        AtomicLong lastRefillTime = bucket[1];
        long timeSinceLastRefill = currentTime - lastRefillTime.get();
        long timeUntilNextToken = REFILL_INTERVAL_MILLIS - (timeSinceLastRefill % REFILL_INTERVAL_MILLIS);

        return (timeUntilNextToken + 999) / 1000; // Round up to nearest second
    }
}
