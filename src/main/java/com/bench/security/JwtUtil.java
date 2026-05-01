package com.bench.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * JWT utility for generating and validating JSON Web Tokens.
 * Uses HS256 signing with a 256-bit secret key.
 * Supports both access tokens (1h TTL) and refresh tokens (30 days TTL).
 */
@Component
public class JwtUtil {

    private static final String SECRET_KEY = "MyVerySecureSecretKeyForJWTSigningWith256Bits!";
    private static final long ACCESS_TOKEN_EXPIRATION_TIME = 3600000; // 1 hour
    private static final long REFRESH_TOKEN_EXPIRATION_TIME = 2592000000L; // 30 days

    private final SecretKey key;

    public JwtUtil() {
        this.key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    /**
     * Generate an access JWT token for the given username.
     *
     * @param username the username to encode in the token
     * @return JWT token string signed with HS256
     */
    public String generateAccessToken(String username) {
        return generateToken(username, ACCESS_TOKEN_EXPIRATION_TIME, "access");
    }

    /**
     * Generate a refresh JWT token for the given username.
     * Refresh tokens include a unique JTI (JWT ID) claim for revocation tracking.
     *
     * @param username the username to encode in the token
     * @return JWT token string signed with HS256
     */
    public String generateRefreshToken(String username) {
        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + REFRESH_TOKEN_EXPIRATION_TIME);

        return Jwts.builder()
            .subject(username)
            .claim("typ", "refresh")
            .claim("jti", jti)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * Internal method to generate a JWT token.
     *
     * @param username the username to encode
     * @param expirationTime the expiration time in milliseconds from now
     * @param type the token type claim (e.g., "access", "refresh")
     * @return JWT token string
     */
    private String generateToken(String username, long expirationTime, String type) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
            .subject(username)
            .claim("typ", type)
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
     * Extract the JTI (JWT ID) claim from a token (for revocation tracking).
     *
     * @param token the JWT token string
     * @return the JTI claim, or null if not present
     */
    public String extractJti(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claims.get("jti", String.class);
    }

    /**
     * Extract the "typ" claim from a token to determine token type.
     *
     * @param token the JWT token string
     * @return the typ claim value (e.g., "access", "refresh"), or null if not present
     */
    public String extractType(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claims.get("typ", String.class);
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
     * Validate a JWT token: check signature and expiration only.
     *
     * @param token the JWT token string
     * @return true if token is valid, false otherwise
     */
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the refresh token expiration time in milliseconds.
     * Used by TokenRevocationStore to clean up revoked tokens.
     *
     * @return refresh token expiration time in milliseconds
     */
    public long getRefreshTokenExpirationTime() {
        return REFRESH_TOKEN_EXPIRATION_TIME;
    }
}
