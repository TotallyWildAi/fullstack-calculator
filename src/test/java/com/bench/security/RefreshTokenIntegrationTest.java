package com.bench.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Integration tests for refresh token flow.
 * Tests token generation, refresh token rotation, revocation, and type validation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RefreshTokenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test successful login returns both access and refresh tokens.
     * Verifies that POST /api/auth/login with valid credentials
     * returns HTTP 200 with both access_token and refresh_token fields.
     */
    @Test
    void testLoginReturnsAccessAndRefreshTokens() throws Exception {
        LoginRequest loginRequest = new LoginRequest("testuser", "SecurePass123!");
        String requestBody = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists());
    }

    /**
     * Test refresh endpoint with valid refresh token.
     * Verifies that POST /api/auth/refresh with a valid refresh token
     * returns HTTP 200 with new access_token and refresh_token.
     */
    @Test
    void testRefreshTokenSuccess() throws Exception {
        // Login to get tokens
        LoginRequest loginRequest = new LoginRequest("testuser", "SecurePass123!");
        String loginBody = objectMapper.writeValueAsString(loginRequest);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        // Extract tokens from login response
        String responseBody = loginResult.getResponse().getContentAsString();
        Map<String, String> responseMap = objectMapper.readValue(responseBody, Map.class);
        String refreshToken = responseMap.get("refresh_token");

        // Use refresh token to get new tokens
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);
        String refreshBody = objectMapper.writeValueAsString(refreshRequest);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.refresh_token").exists());
    }

    /**
     * Test refresh token rotation: old refresh token is revoked after one use.
     * Verifies that after using a refresh token once, attempting to use it again
     * returns HTTP 401 Unauthorized.
     */
    @Test
    void testRefreshTokenRotation() throws Exception {
        // Login to get tokens
        LoginRequest loginRequest = new LoginRequest("testuser", "SecurePass123!");
        String loginBody = objectMapper.writeValueAsString(loginRequest);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        // Extract refresh token from login response
        String responseBody = loginResult.getResponse().getContentAsString();
        Map<String, String> responseMap = objectMapper.readValue(responseBody, Map.class);
        String oldRefreshToken = responseMap.get("refresh_token");

        // First refresh: should succeed
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(oldRefreshToken);
        String refreshBody = objectMapper.writeValueAsString(refreshRequest);

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody))
                .andExpect(status().isOk())
                .andReturn();

        // Second refresh with old token: should fail (token is revoked)
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test multiple refresh cycles.
     * Verifies that refresh tokens can be used multiple times in sequence,
     * with each refresh issuing a new pair of tokens.
     */
    @Test
    void testMultipleRefreshCycles() throws Exception {
        // Login to get initial tokens
        LoginRequest loginRequest = new LoginRequest("testuser", "SecurePass123!");
        String loginBody = objectMapper.writeValueAsString(loginRequest);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        Map<String, String> responseMap = objectMapper.readValue(responseBody, Map.class);
        String currentRefreshToken = responseMap.get("refresh_token");

        // Perform multiple refresh cycles
        for (int i = 0; i < 3; i++) {
            RefreshTokenRequest refreshRequest = new RefreshTokenRequest(currentRefreshToken);
            String refreshBody = objectMapper.writeValueAsString(refreshRequest);

            MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(refreshBody))
                    .andExpect(status().isOk())
                    .andReturn();

            // Extract new refresh token for next cycle
            String refreshResponseBody = refreshResult.getResponse().getContentAsString();
            Map<String, String> refreshResponseMap = objectMapper.readValue(refreshResponseBody, Map.class);
            currentRefreshToken = refreshResponseMap.get("refresh_token");
        }
    }

    /**
     * Test access token is rejected at refresh endpoint.
     * Verifies that POST /api/auth/refresh with an access token (instead of refresh token)
     * returns HTTP 401 Unauthorized due to token type mismatch.
     */
    @Test
    void testAccessTokenRejectedAtRefreshEndpoint() throws Exception {
        // Login to get tokens
        LoginRequest loginRequest = new LoginRequest("testuser", "SecurePass123!");
        String loginBody = objectMapper.writeValueAsString(loginRequest);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        // Extract access token from login response
        String responseBody = loginResult.getResponse().getContentAsString();
        Map<String, String> responseMap = objectMapper.readValue(responseBody, Map.class);
        String accessToken = responseMap.get("access_token");

        // Try to use access token at refresh endpoint: should fail
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(accessToken);
        String refreshBody = objectMapper.writeValueAsString(refreshRequest);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test refresh token is rejected at protected endpoint.
     * Verifies that GET /api/calculate with a refresh token (instead of access token)
     * returns HTTP 403 Forbidden due to token type mismatch.
     */
    @Test
    void testRefreshTokenRejectedAtProtectedEndpoint() throws Exception {
        // Login to get tokens
        LoginRequest loginRequest = new LoginRequest("testuser", "SecurePass123!");
        String loginBody = objectMapper.writeValueAsString(loginRequest);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        // Extract refresh token from login response
        String responseBody = loginResult.getResponse().getContentAsString();
        Map<String, String> responseMap = objectMapper.readValue(responseBody, Map.class);
        String refreshToken = responseMap.get("refresh_token");

        // Try to use refresh token at protected endpoint: should fail
        mockMvc.perform(get("/api/calculate")
                .param("a", "2")
                .param("b", "3")
                .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isForbidden());
    }

    /**
     * Test new access token from refresh can access protected endpoint.
     * Verifies that after refreshing tokens, the new access token can be used
     * to access protected endpoints like /api/calculate.
     */
    @Test
    void testNewAccessTokenCanAccessProtectedEndpoint() throws Exception {
        // Login to get tokens
        LoginRequest loginRequest = new LoginRequest("testuser", "SecurePass123!");
        String loginBody = objectMapper.writeValueAsString(loginRequest);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        // Extract refresh token from login response
        String responseBody = loginResult.getResponse().getContentAsString();
        Map<String, String> responseMap = objectMapper.readValue(responseBody, Map.class);
        String refreshToken = responseMap.get("refresh_token");

        // Refresh tokens
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);
        String refreshBody = objectMapper.writeValueAsString(refreshRequest);

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody))
                .andExpect(status().isOk())
                .andReturn();

        // Extract new access token
        String refreshResponseBody = refreshResult.getResponse().getContentAsString();
        Map<String, String> refreshResponseMap = objectMapper.readValue(refreshResponseBody, Map.class);
        String newAccessToken = refreshResponseMap.get("access_token");

        // Use new access token to access protected endpoint
        mockMvc.perform(get("/api/calculate")
                .param("a", "2")
                .param("b", "3")
                .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(5));
    }

    /**
     * Test refresh endpoint with invalid refresh token.
     * Verifies that POST /api/auth/refresh with a malformed token
     * returns HTTP 401 Unauthorized.
     */
    @Test
    void testRefreshWithInvalidToken() throws Exception {
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("invalid.token.here");
        String refreshBody = objectMapper.writeValueAsString(refreshRequest);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test refresh endpoint with missing refresh token.
     * Verifies that POST /api/auth/refresh with an empty refresh token
     * returns HTTP 401 Unauthorized.
     */
    @Test
    void testRefreshWithMissingToken() throws Exception {
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("");
        String refreshBody = objectMapper.writeValueAsString(refreshRequest);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(refreshBody))
                .andExpect(status().isUnauthorized());
    }
}
