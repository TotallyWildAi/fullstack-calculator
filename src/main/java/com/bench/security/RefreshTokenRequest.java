package com.bench.security;

/**
 * RefreshTokenRequest record for JSON deserialization of refresh endpoint request body.
 * Contains the refresh token to be exchanged for a new access/refresh token pair.
 */
public record RefreshTokenRequest(
    String refresh_token
) {
}
