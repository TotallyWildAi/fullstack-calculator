package com.bench.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of revoked refresh token JTIs.
 * Stores JTI (JWT ID) with its expiration time for cleanup.
 * 
 * Production systems should use a persistent store (Redis, database, etc.)
 * to ensure revocation across multiple server instances and after restarts.
 */
@Component
public class TokenRevocationStore {

    @Autowired
    private JwtUtil jwtUtil;

    // Map of JTI -> expiration time
    private final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();

    /**
     * Revoke a token by storing its JTI.
     *
     * @param jti the JWT ID claim from the refresh token
     */
    public void revokeToken(String jti) {
        // Store expiration time so we can clean it up later
        long expirationTime = System.currentTimeMillis() + jwtUtil.getRefreshTokenExpirationTime();
        revokedTokens.put(jti, expirationTime);
    }

    /**
     * Check if a token JTI has been revoked.
     *
     * @param jti the JWT ID claim from the refresh token
     * @return true if the token has been revoked, false otherwise
     */
    public boolean isTokenRevoked(String jti) {
        return revokedTokens.containsKey(jti);
    }

    /**
     * Clean up expired revoked tokens.
     * Runs every 10 minutes to remove stale entries.
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        revokedTokens.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}
