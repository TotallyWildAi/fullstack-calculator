package com.bench.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiting filter that enforces per-user request quotas on /api/calculate.
 * Uses a token-bucket algorithm with 10 tokens per minute (1 token every 6 seconds).
 * Registered after JwtAuthFilter so the username is available via SecurityContextHolder.
 * Unauthenticated requests are skipped (already rejected by Spring Security).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Configuration
    private static final int TOKENS_PER_MINUTE = 10;
    private static final long REFILL_INTERVAL_MS = 6000; // 6 seconds = 1 token every 6 seconds
    private static final long TOKENS_PER_REFILL = 1;

    // In-memory storage: username -> [tokens, lastRefillTime]
    private final ConcurrentHashMap<String, long[]> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Only apply rate limiting to /api/calculate endpoint
        String path = request.getRequestURI();
        if (!"/api/calculate".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get the authenticated username from SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            // Unauthenticated requests - skip rate limiting (they'll be rejected by Spring Security anyway)
            filterChain.doFilter(request, response);
            return;
        }

        String username = auth.getName();

        // Check rate limit
        if (!checkRateLimit(username)) {
            // Rate limit exceeded - return 429
            long retryAfterSeconds = calculateRetryAfter(username);
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "rate_limit_exceeded");
            errorResponse.put("retry_after_seconds", retryAfterSeconds);
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        // Rate limit check passed - continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Check if the user has tokens available in their bucket.
     * Refills tokens based on elapsed time, then consumes one token if available.
     *
     * @param username the authenticated username
     * @return true if a token was available and consumed, false otherwise
     */
    private boolean checkRateLimit(String username) {
        long now = System.currentTimeMillis();

        // Get or create bucket for this user
        long[] bucket = buckets.computeIfAbsent(username, k -> new long[]{TOKENS_PER_MINUTE, now});

        synchronized (bucket) {
            long tokens = bucket[0];
            long lastRefillTime = bucket[1];

            // Refill tokens based on elapsed time
            long elapsedMs = now - lastRefillTime;
            long tokensToAdd = (elapsedMs / REFILL_INTERVAL_MS) * TOKENS_PER_REFILL;

            if (tokensToAdd > 0) {
                tokens = Math.min(tokens + tokensToAdd, TOKENS_PER_MINUTE);
                bucket[1] = now - (elapsedMs % REFILL_INTERVAL_MS); // Adjust last refill time
            }

            // Try to consume a token
            if (tokens > 0) {
                bucket[0] = tokens - 1;
                return true;
            }

            return false;
        }
    }

    /**
     * Calculate how many seconds until the next token is available.
     *
     * @param username the authenticated username
     * @return seconds to wait (rounded up)
     */
    private long calculateRetryAfter(String username) {
        long now = System.currentTimeMillis();
        long[] bucket = buckets.get(username);

        if (bucket == null) {
            return 1; // Shouldn't happen, but default to 1 second
        }

        synchronized (bucket) {
            long lastRefillTime = bucket[1];
            long elapsedMs = now - lastRefillTime;
            long msUntilNextToken = REFILL_INTERVAL_MS - (elapsedMs % REFILL_INTERVAL_MS);

            // Round up to nearest second
            return (msUntilNextToken + 999) / 1000;
        }
    }
}
