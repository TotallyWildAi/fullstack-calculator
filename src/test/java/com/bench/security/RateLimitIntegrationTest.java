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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Integration tests for rate limiting on /api/calculate endpoint.
 * Tests per-user rate limiting with token-bucket algorithm (10 requests per minute).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test that bursting 11 requests within one second results in the 11th request
     * getting a 429 Too Many Requests response with Retry-After header and error body.
     * This verifies that rate limiting enforces the 10 requests per minute quota.
     */
    @Test
    void testBurstRateLimitExceeded() throws Exception {
        // Login to get token
        String token = obtainToken("testuser", "SecurePass123!");

        // Send 10 successful requests
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/calculate")
                    .param("a", String.valueOf(i))
                    .param("b", "1")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").exists());
        }

        // 11th request should be rate limited
        MvcResult result = mockMvc.perform(get("/api/calculate")
                .param("a", "10")
                .param("b", "1")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isTooManyRequests())
                .andReturn();

        // Verify response body has error message
        String responseBody = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
        assertEquals("rate_limit_exceeded", responseMap.get("error"));
        assertTrue(responseMap.containsKey("retry_after_seconds"));
        assertTrue(responseMap.get("retry_after_seconds") instanceof Integer);
        int retryAfterSeconds = (Integer) responseMap.get("retry_after_seconds");
        assertTrue(retryAfterSeconds > 0, "retry_after_seconds should be positive");

        // Verify Retry-After header is set
        String retryAfterHeader = result.getResponse().getHeader("Retry-After");
        assertNotNull(retryAfterHeader, "Retry-After header should be present");
        assertEquals(String.valueOf(retryAfterSeconds), retryAfterHeader);
    }

    /**
     * Test that two different users each have their own independent rate limit bucket.
     * One user's 11 requests should not affect another user's ability to make requests.
     * This verifies per-user rate limiting isolation.
     */
    @Test
    void testPerUserRateLimitIsolation() throws Exception {
        // Login as first user
        String token1 = obtainToken("testuser", "SecurePass123!");

        // Login as second user (need to create or use another test user)
        String token2 = obtainToken("testuser2", "SecurePass123!");

        // User 1: Send 10 successful requests
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/calculate")
                    .param("a", String.valueOf(i))
                    .param("b", "1")
                    .header("Authorization", "Bearer " + token1))
                    .andExpect(status().isOk());
        }

        // User 1: 11th request should be rate limited
        mockMvc.perform(get("/api/calculate")
                .param("a", "10")
                .param("b", "1")
                .header("Authorization", "Bearer " + token1))
                .andExpect(status().isTooManyRequests());

        // User 2: Should still be able to make 10 requests (independent bucket)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/calculate")
                    .param("a", String.valueOf(i))
                    .param("b", "2")
                    .header("Authorization", "Bearer " + token2))
                    .andExpect(status().isOk());
        }

        // User 2: 11th request should also be rate limited
        mockMvc.perform(get("/api/calculate")
                .param("a", "10")
                .param("b", "2")
                .header("Authorization", "Bearer " + token2))
                .andExpect(status().isTooManyRequests());
    }

    /**
     * Test that the /api/auth/login endpoint is NOT rate-limited.
     * Login attempts are public and should not be subject to per-user rate limiting.
     */
    @Test
    void testLoginEndpointNotRateLimited() throws Exception {
        LoginRequest loginRequest = new LoginRequest("testuser", "SecurePass123!");
        String requestBody = objectMapper.writeValueAsString(loginRequest);

        // Send 15 login requests in rapid succession - all should succeed or fail auth
        // but not return 429
        for (int i = 0; i < 15; i++) {
            MvcResult result = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andReturn();

            int status = result.getResponse().getStatus();
            // Either 200 (success) or 401 (auth failure), but NOT 429 (rate limit)
            assertNotEquals(429, status, "Login endpoint should not return 429 rate limit");
        }
    }

    /**
     * Helper method to obtain a JWT token by logging in.
     * If login fails, it might indicate the user doesn't exist, but we assume
     * testuser and testuser2 are available in test configuration.
     *
     * @param username the username
     * @param password the password
     * @return JWT token string
     * @throws Exception if login fails
     */
    private String obtainToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);
        String requestBody = objectMapper.writeValueAsString(loginRequest);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        Map<String, String> responseMap = objectMapper.readValue(responseBody, Map.class);
        return responseMap.get("access_token");
    }
}
