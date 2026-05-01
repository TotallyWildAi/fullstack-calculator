package com.bench.security;

/**
 * RefreshTokenRequest record for JSON deserialization of refresh token endpoint request body.
 * Contains the refresh token string.
 */
public record RefreshTokenRequest(
    String refresh_token
) {
}
