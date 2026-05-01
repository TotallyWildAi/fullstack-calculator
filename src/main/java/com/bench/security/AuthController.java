package com.bench.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for authentication endpoints.
 * Handles user login, JWT token generation, and refresh token rotation.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and JWT token management")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * POST /api/auth/login endpoint.
     * Authenticates user with username and password, returns both access and refresh tokens on success.
     *
     * @param loginRequest JSON body with username and password
     * @return ResponseEntity with access_token and refresh_token on success (HTTP 200),
     *         or error message on failure (HTTP 401)
     */
    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates credentials and returns access and refresh tokens for use in subsequent API calls")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful, tokens returned",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"access_token\": \"eyJ...\", \"refresh_token\": \"eyJ...\"}"))
        ),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Authenticate using AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.username(),
                    loginRequest.password()
                )
            );

            // Generate both access and refresh tokens
            String accessToken = jwtUtil.generateAccessToken(loginRequest.username());
            String refreshToken = jwtUtil.generateRefreshToken(loginRequest.username());
            
            TokenResponse response = new TokenResponse(accessToken, refreshToken);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            // Return 401 Unauthorized on authentication failure
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * POST /api/auth/refresh endpoint.
     * Accepts a refresh token and issues a new pair of access and refresh tokens.
     * The old refresh token is revoked (refresh token rotation).
     *
     * @param refreshRequest JSON body with refresh_token
     * @return ResponseEntity with new access_token and refresh_token on success (HTTP 200),
     *         or error message on failure (HTTP 401)
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh tokens", description = "Exchanges a valid refresh token for a new pair of access and refresh tokens")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refresh successful, new tokens returned",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"access_token\": \"eyJ...\", \"refresh_token\": \"eyJ...\"}"))
        ),
        @ApiResponse(responseCode = "401", description = "Invalid or revoked refresh token")
    })
    public ResponseEntity<TokenResponse> refresh(@RequestBody RefreshTokenRequest refreshRequest) {
        try {
            String refreshToken = refreshRequest.refresh_token();
            
            // Validate refresh token exists
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Extract username from refresh token
            String username = jwtUtil.extractUsername(refreshToken);
            
            // Validate token type is "refresh"
            if (!jwtUtil.isTokenTypeValid(refreshToken, "refresh")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Validate token signature and expiration
            if (!jwtUtil.isTokenValid(refreshToken, username)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Check if token has been revoked
            if (jwtUtil.isRefreshTokenRevoked(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Revoke the old refresh token (rotation)
            jwtUtil.revokeRefreshToken(refreshToken);
            
            // Generate new tokens
            String newAccessToken = jwtUtil.generateAccessToken(username);
            String newRefreshToken = jwtUtil.generateRefreshToken(username);
            
            TokenResponse response = new TokenResponse(newAccessToken, newRefreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Return 401 Unauthorized on any error
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
