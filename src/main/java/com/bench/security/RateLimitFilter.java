package com.bench.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Per-user rate limiting filter for the /api/calculate endpoint.
 * Uses a token-bucket algorithm: 10 requests per minute, refill 1 token every 6 seconds.
 * Registered after JwtAuthFilter so the username is available via SecurityContextHolder.
 * Returns HTTP 429 with {"error": "rate_limit_exceeded", "retry_after_seconds": <int>}
 * when the bucket is empty.
 * Anonymous/unauthenticated requests are not rate-limited by this filter (Spring Security rejects them earlier).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    // Constants for rate limiting
    private static final int MAX_TOKENS = 10;           // 10 requests per minute
    private static final long REFILL_INTERVAL_MS = 6000; // 1 token every 6 seconds
    private static final long TOTAL_REFILL_MS = 60000;   // 60 seconds for full bucket
    private static final String RATE_LIMIT_ENDPOINT = "/api/calculate";

    // Bucket storage: username -> [tokens (as AtomicLong), lastRefillTime]
    private final Map<String, BucketState> buckets = new ConcurrentHashMap<>();

    /**
     * Represents the state of a token bucket for a user.
     */
    private static class BucketState {
        final AtomicLong tokens;
        final AtomicLong lastRefillTime;

        BucketState(long initialTokens, long refillTime) {
            this.tokens = new AtomicLong(initialTokens);
            this.lastRefillTime = new AtomicLong(refillTime);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Only apply rate limiting to /api/calculate endpoint
        if (!RATE_LIMIT_ENDPOINT.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get authenticated user from SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            // Unauthenticated requests are not rate-limited here (Spring Security rejects them)
            filterChain.doFilter(request, response);
            return;
        }

        String username = auth.getName();
        if (username == null || username.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check rate limit for this user
        if (!allowRequest(username)) {
            long retryAfter = calculateRetryAfter(username);
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(retryAfter));

            Map<String, Object> errorResponse = Map.of(
                "error", "rate_limit_exceeded",
                "retry_after_seconds", retryAfter
            );
            ObjectMapper mapper = new ObjectMapper();
            response.getWriter().write(mapper.writeValueAsString(errorResponse));
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Check if a request from the user is allowed based on token bucket.
     * Refills tokens as needed and consumes one token if available.
     *
     * @param username the authenticated username
     * @return true if request is allowed (token available), false otherwise
     */
    private boolean allowRequest(String username) {
        BucketState bucket = buckets.computeIfAbsent(username, u -> 
            new BucketState(MAX_TOKENS, System.currentTimeMillis())
        );

        long now = System.currentTimeMillis();
        long lastRefill = bucket.lastRefillTime.get();
        long elapsed = now - lastRefill;

        // Refill tokens based on elapsed time
        if (elapsed >= REFILL_INTERVAL_MS) {
            long tokensToAdd = (elapsed / REFILL_INTERVAL_MS);
            long newTokens = Math.min(MAX_TOKENS, bucket.tokens.get() + tokensToAdd);
            bucket.tokens.set(newTokens);
            bucket.lastRefillTime.set(lastRefill + (tokensToAdd * REFILL_INTERVAL_MS));
        }

        // Try to consume a token
        while (true) {
            long current = bucket.tokens.get();
            if (current <= 0) {
                return false; // No tokens available
            }
            // Atomically decrement if still positive
            if (bucket.tokens.compareAndSet(current, current - 1)) {
                return true;
            }
            // Retry if CAS failed due to concurrent modification
        }
    }

    /**
     * Calculate retry-after seconds for a user whose bucket is empty.
     *
     * @param username the authenticated username
     * @return seconds until at least one token is available
     */
    private long calculateRetryAfter(String username) {
        BucketState bucket = buckets.get(username);
        if (bucket == null) {
            return 6; // Default if bucket doesn't exist
        }

        long now = System.currentTimeMillis();
        long lastRefill = bucket.lastRefillTime.get();
        long elapsed = now - lastRefill;

        // Time until next refill (next token)
        long nextRefillTime = REFILL_INTERVAL_MS - (elapsed % REFILL_INTERVAL_MS);
        return Math.max(1, (nextRefillTime + 999) / 1000); // Round up to nearest second, min 1
    }
}
