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
 * Handles user login, JWT token generation, and token refresh.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and JWT token management")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenRevocationStore tokenRevocationStore;

    /**
     * POST /api/auth/login endpoint.
     * Authenticates user with username and password, returns access and refresh tokens on success.
     *
     * @param loginRequest JSON body with username and password
     * @return ResponseEntity with access_token and refresh_token on success (HTTP 200),
     *         or error message on failure (HTTP 401)
     */
    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates credentials and returns access and refresh tokens for use in subsequent API calls")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful, access and refresh tokens returned",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"access_token\": \"eyJ...\", \"refresh_token\": \"eyJ...\"}"))
        ),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Authenticate using AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.username(),
                    loginRequest.password()
                )
            );

            // Generate access and refresh tokens for authenticated user
            String accessToken = jwtUtil.generateAccessToken(loginRequest.username());
            String refreshToken = jwtUtil.generateRefreshToken(loginRequest.username());
            
            Map<String, String> response = new HashMap<>();
            response.put("access_token", accessToken);
            response.put("refresh_token", refreshToken);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            // Return 401 Unauthorized on authentication failure
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid credentials");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    /**
     * POST /api/auth/refresh endpoint.
     * Accepts a refresh token and returns a new pair of access and refresh tokens.
     * Implements refresh token rotation: the old refresh token is revoked after issuing a new one.
     *
     * @param refreshRequest JSON body with refresh_token
     * @return ResponseEntity with access_token and refresh_token on success (HTTP 200),
     *         or error message on failure (HTTP 401)
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh authentication tokens", description = "Exchanges a valid refresh token for new access and refresh tokens (implements token rotation)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refresh successful, new tokens returned",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"access_token\": \"eyJ...\", \"refresh_token\": \"eyJ...\"}"))
        ),
        @ApiResponse(responseCode = "401", description = "Invalid or revoked refresh token")
    })
    public ResponseEntity<Map<String, String>> refresh(@RequestBody RefreshTokenRequest refreshRequest) {
        try {
            String token = refreshRequest.refresh_token();
            
            // Validate token signature and expiration
            if (!jwtUtil.isTokenValid(token)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Invalid or expired refresh token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // Verify token is a refresh token (has typ="refresh" claim)
            String tokenType = jwtUtil.extractType(token);
            if (tokenType == null || !tokenType.equals("refresh")) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Token is not a refresh token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // Check if token has been revoked
            String jti = jwtUtil.extractJti(token);
            if (jti != null && tokenRevocationStore.isTokenRevoked(jti)) {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Refresh token has been revoked");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            // Extract username and revoke old token
            String username = jwtUtil.extractUsername(token);
            if (jti != null) {
                tokenRevocationStore.revokeToken(jti);
            }

            // Issue new access and refresh token pair
            String newAccessToken = jwtUtil.generateAccessToken(username);
            String newRefreshToken = jwtUtil.generateRefreshToken(username);

            Map<String, String> response = new HashMap<>();
            response.put("access_token", newAccessToken);
            response.put("refresh_token", newRefreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid refresh token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }
}
