package com.bench.security;

/**
 * TokenResponse record for JSON serialization of authentication endpoint responses.
 * Contains both access token and refresh token.
 */
public record TokenResponse(
    String access_token,
    String refresh_token
) {
}
