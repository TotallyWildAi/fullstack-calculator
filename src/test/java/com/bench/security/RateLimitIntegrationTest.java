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
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for per-user rate limiting on /api/calculate endpoint.
 * Tests:
 * 1. Bursting 11 requests in one second from a single user → 11th gets 429 with Retry-After
 * 2. Two different users each get their own bucket (one user's quota doesn't affect the other)
 * 3. /api/auth/login is NOT rate-limited
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
     * Helper method: login a user and extract the JWT token.
     *
     * @param username the username
     * @param password the password
     * @return JWT token string
     */
    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);
        String requestBody = objectMapper.writeValueAsString(loginRequest);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<String, String> responseMap = objectMapper.readValue(responseBody, Map.class);
        return responseMap.get("token");
    }

    /**
     * Test that bursting 11 requests inside one second from a single authenticated user
     * results in the 11th request getting HTTP 429 with Retry-After set.
     */
    @Test
    void testRateLimitExceededOn11thRequest() throws Exception {
        // Login to get token
        String token = loginAndGetToken("testuser", "SecurePass123!");

        // Burst 11 requests rapidly
        for (int i = 1; i <= 11; i++) {
            MvcResult result = mockMvc.perform(get("/api/calculate")
                    .param("a", "2")
                    .param("b", "3")
                    .header("Authorization", "Bearer " + token))
                    .andReturn();

            if (i <= 10) {
                // First 10 requests should succeed
                assertEquals(200, result.getResponse().getStatus(), 
                    "Request " + i + " should succeed");
            } else {
                // 11th request should be rate-limited
                assertEquals(429, result.getResponse().getStatus(),
                    "Request 11 should be rate-limited with 429");
                
                // Verify response body contains error message
                String responseBody = result.getResponse().getContentAsString();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                assertEquals("rate_limit_exceeded", responseMap.get("error"));
                assertTrue(responseMap.containsKey("retry_after_seconds"));
                assertTrue((Integer) responseMap.get("retry_after_seconds") > 0);
                
                // Verify Retry-After header is set
                String retryAfterHeader = result.getResponse().getHeader("Retry-After");
                assertNotNull(retryAfterHeader, "Retry-After header should be present");
                int retryAfterValue = Integer.parseInt(retryAfterHeader);
                assertTrue(retryAfterValue > 0, "Retry-After should be positive");
            }
        }
    }

    /**
     * Test that two different users each get their own rate limit bucket.
     * When user1 consumes their quota, user2 should still be able to make requests.
     */
    @Test
    void testSeparateBucketsForDifferentUsers() throws Exception {
        // Create tokens for both users
        String token1 = loginAndGetToken("testuser", "SecurePass123!");
        String token2 = loginAndGetToken("anotheruser", "AnotherPass123!");

        // User1 exhausts their quota with 10 requests
        for (int i = 0; i < 10; i++) {
            MvcResult result = mockMvc.perform(get("/api/calculate")
                    .param("a", "1")
                    .param("b", "1")
                    .header("Authorization", "Bearer " + token1))
                    .andReturn();
            assertEquals(200, result.getResponse().getStatus(),
                "User1 request " + (i + 1) + " should succeed");
        }

        // User1's 11th request should be rate-limited
        MvcResult result11 = mockMvc.perform(get("/api/calculate")
                .param("a", "1")
                .param("b", "1")
                .header("Authorization", "Bearer " + token1))
                .andReturn();
        assertEquals(429, result11.getResponse().getStatus(),
            "User1's 11th request should be rate-limited");

        // User2 should still be able to make requests (they have their own bucket)
        for (int i = 0; i < 5; i++) {
            MvcResult result = mockMvc.perform(get("/api/calculate")
                    .param("a", "2")
                    .param("b", "2")
                    .header("Authorization", "Bearer " + token2))
                    .andReturn();
            assertEquals(200, result.getResponse().getStatus(),
                "User2 request " + (i + 1) + " should succeed (separate bucket)");
        }
    }

    /**
     * Test that /api/auth/login is NOT rate-limited.
     * Multiple login attempts should not trigger rate limiting.
     */
    @Test
    void testLoginEndpointNotRateLimited() throws Exception {
        // Attempt 20 logins (more than rate limit allows)
        for (int i = 0; i < 20; i++) {
            LoginRequest loginRequest = new LoginRequest("testuser", "SecurePass123!");
            String requestBody = objectMapper.writeValueAsString(loginRequest);

            MvcResult result = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andReturn();

            // All should succeed (no 429 responses)
            assertNotEquals(429, result.getResponse().getStatus(),
                "Login attempt " + (i + 1) + " should not be rate-limited");
        }
    }

    /**
     * Test that unauthenticated requests to /api/calculate are rejected before rate limiting.
     * They should get 403/401 from Spring Security, not 429.
     */
    @Test
    void testUnauthenticatedRequestRejectedBeforeRateLimit() throws Exception {
        // Send multiple requests without authentication
        for (int i = 0; i < 15; i++) {
            MvcResult result = mockMvc.perform(get("/api/calculate")
                    .param("a", "2")
                    .param("b", "3"))
                    .andReturn();

            // Should get 403 from Spring Security, not 429
            assertEquals(403, result.getResponse().getStatus(),
                "Unauthenticated request " + (i + 1) + " should be rejected by Spring Security");
        }
    }

    /**
     * Test that the retry_after_seconds value is reasonable (between 1 and 6 seconds).
     */
    @Test
    void testRetryAfterValueIsReasonable() throws Exception {
        String token = loginAndGetToken("testuser", "SecurePass123!");

        // Exhaust the quota
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/calculate")
                    .param("a", "1")
                    .param("b", "1")
                    .header("Authorization", "Bearer " + token))
                    .andReturn();
        }

        // Get a rate-limited response
        MvcResult result = mockMvc.perform(get("/api/calculate")
                .param("a", "1")
                .param("b", "1")
                .header("Authorization", "Bearer " + token))
                .andReturn();

        assertEquals(429, result.getResponse().getStatus());
        
        String responseBody = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
        int retryAfter = (Integer) responseMap.get("retry_after_seconds");
        
        // Retry-After should be reasonable (1-6 seconds for 6-second refill interval)
        assertTrue(retryAfter >= 1, "Retry-After should be at least 1 second");
        assertTrue(retryAfter <= 6, "Retry-After should be at most 6 seconds");
    }
}
