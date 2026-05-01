package com.bench.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * JWT utility for generating and validating JSON Web Tokens.
 * Uses HS256 signing with a 256-bit secret key.
 * Supports both access tokens (1h TTL) and refresh tokens (30d TTL).
 * Maintains an in-memory revocation list for refresh tokens.
 */
@Component
public class JwtUtil {

    private static final String SECRET_KEY = "MyVerySecureSecretKeyForJWTSigningWith256Bits!";
    private static final long ACCESS_TOKEN_EXPIRATION = 3600000; // 1 hour
    private static final long REFRESH_TOKEN_EXPIRATION = 2592000000L; // 30 days
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final SecretKey key;
    private final Set<String> revokedTokenJtis = new HashSet<>();

    public JwtUtil() {
        this.key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    /**
     * Generate an access token for the given username.
     * Access tokens have a 1-hour TTL and typ claim set to "access".
     *
     * @param username the username to encode in the token
     * @return JWT token string signed with HS256
     */
    public String generateAccessToken(String username) {
        return generateToken(username, ACCESS_TOKEN_EXPIRATION, ACCESS_TOKEN_TYPE);
    }

    /**
     * Generate a refresh token for the given username.
     * Refresh tokens have a 30-day TTL and typ claim set to "refresh".
     *
     * @param username the username to encode in the token
     * @return JWT token string signed with HS256
     */
    public String generateRefreshToken(String username) {
        return generateToken(username, REFRESH_TOKEN_EXPIRATION, REFRESH_TOKEN_TYPE);
    }

    /**
     * Generate a JWT token with the specified type and expiration.
     *
     * @param username the username to encode in the token
     * @param expirationTime the token expiration time in milliseconds
     * @param tokenType the token type ("access" or "refresh")
     * @return JWT token string signed with HS256
     */
    private String generateToken(String username, long expirationTime, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
            .subject(username)
            .claim("typ", tokenType)
            .claim("jti", jti)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * Extract the username (subject) from a JWT token.
     *
     * @param token the JWT token string
     * @return the username encoded in the token
     */
    public String extractUsername(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claims.getSubject();
    }

    /**
     * Extract the token type from a JWT token.
     *
     * @param token the JWT token string
     * @return the token type ("access" or "refresh"), or null if not present
     */
    public String extractTokenType(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return (String) claims.get("typ");
    }

    /**
     * Extract the JTI (JWT ID) from a JWT token.
     *
     * @param token the JWT token string
     * @return the JTI, or null if not present
     */
    public String extractJti(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return (String) claims.get("jti");
    }

    /**
     * Validate a JWT token: check signature, expiration, and username match.
     *
     * @param token the JWT token string
     * @param username the expected username
     * @return true if token is valid and username matches, false otherwise
     */
    public boolean isTokenValid(String token, String username) {
        try {
            String extractedUsername = extractUsername(token);
            return extractedUsername.equals(username);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validate that a token is of the expected type.
     *
     * @param token the JWT token string
     * @param expectedType the expected token type ("access" or "refresh")
     * @return true if token type matches, false otherwise
     */
    public boolean isTokenTypeValid(String token, String expectedType) {
        try {
            String tokenType = extractTokenType(token);
            return expectedType.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Revoke a refresh token by adding its JTI to the revocation list.
     * This is used for refresh token rotation: when a new refresh token is issued,
     * the old one is revoked.
     *
     * @param token the refresh token to revoke
     */
    public void revokeRefreshToken(String token) {
        try {
            String jti = extractJti(token);
            if (jti != null) {
                revokedTokenJtis.add(jti);
            }
        } catch (Exception e) {
            // Ignore exceptions during revocation
        }
    }

    /**
     * Check if a refresh token has been revoked.
     *
     * @param token the refresh token to check
     * @return true if the token has been revoked, false otherwise
     */
    public boolean isRefreshTokenRevoked(String token) {
        try {
            String jti = extractJti(token);
            return jti != null && revokedTokenJtis.contains(jti);
        } catch (Exception e) {
            return false;
        }
    }
}
