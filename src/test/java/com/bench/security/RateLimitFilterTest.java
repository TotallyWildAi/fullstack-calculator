package com.bench.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Integration tests for rate limiting on /api/calculate endpoint.
 * Tests per-user token-bucket rate limiting (10 requests per minute, 1 token every 6 seconds).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    /**
     * Before each test, obtain a fresh JWT token for testuser.
     */
    @BeforeEach
    void setUp() throws Exception {
        LoginRequest loginRequest = new LoginRequest("testuser", "SecurePass123!");
        String loginBody = objectMapper.writeValueAsString(loginRequest);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        Map<String, String> responseMap = objectMapper.readValue(responseBody, Map.class);
        token = responseMap.get("token");
    }

    /**
     * Test that bursting 11 requests in quick succession results in the 11th getting 429.
     * Confirms the bucket allows 10 requests per minute.
     */
    @Test
    void testRateLimitExceeded11thRequest() throws Exception {
        // Send 10 requests (should all succeed)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/calculate")
                    .param("a", "2")
                    .param("b", String.valueOf(i))
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").exists());
        }

        // 11th request should be rate limited (429)
        MvcResult rateLimitedResult = mockMvc.perform(get("/api/calculate")
                .param("a", "2")
                .param("b", "3")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isTooManyRequests())
                .andReturn();

        // Verify response body contains error message and retry_after_seconds
        String responseBody = rateLimitedResult.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
        assertEquals("rate_limit_exceeded", responseMap.get("error"));
        assertTrue(responseMap.containsKey("retry_after_seconds"));
        Object retryAfterObj = responseMap.get("retry_after_seconds");
        assertTrue(retryAfterObj instanceof Number, "retry_after_seconds should be a number");
        long retryAfter = ((Number) retryAfterObj).longValue();
        assertTrue(retryAfter > 0, "retry_after_seconds should be positive");
    }

    /**
     * Test that the "Retry-After" response header is set when rate limited.
     */
    @Test
    void testRetryAfterHeaderSet() throws Exception {
        // Exhaust the bucket (10 requests)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/calculate")
                    .param("a", "2")
                    .param("b", String.valueOf(i))
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        // 11th request should have Retry-After header
        MvcResult rateLimitedResult = mockMvc.perform(get("/api/calculate")
                .param("a", "2")
                .param("b", "3")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isTooManyRequests())
                .andReturn();

        String retryAfterHeader = rateLimitedResult.getResponse().getHeader("Retry-After");
        assertTrue(retryAfterHeader != null && !retryAfterHeader.isEmpty(),
                "Retry-After header should be set");
        long retryAfter = Long.parseLong(retryAfterHeader);
        assertTrue(retryAfter > 0, "Retry-After should be positive");
    }

    /**
     * Test that two different users each have their own independent rate limit bucket.
     * One user's quota doesn't affect another user's quota.
     */
    @Test
    void testSeparateBucketsPerUser() throws Exception {
        // Get token for user1 (testuser)
        String user1Token = token;

        // Get token for user2 (anotheruser)
        LoginRequest user2LoginRequest = new LoginRequest("anotheruser", "SecurePass456!");
        String user2LoginBody = objectMapper.writeValueAsString(user2LoginRequest);

        MvcResult user2LoginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(user2LoginBody))
                .andExpect(status().isOk())
                .andReturn();

        String user2ResponseBody = user2LoginResult.getResponse().getContentAsString();
        Map<String, String> user2ResponseMap = objectMapper.readValue(user2ResponseBody, Map.class);
        String user2Token = user2ResponseMap.get("token");

        // Send 10 requests from user1 (exhaust their bucket)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/calculate")
                    .param("a", "1")
                    .param("b", String.valueOf(i))
                    .header("Authorization", "Bearer " + user1Token))
                    .andExpect(status().isOk());
        }

        // User1's 11th request should fail with 429
        mockMvc.perform(get("/api/calculate")
                .param("a", "1")
                .param("b", "10")
                .header("Authorization", "Bearer " + user1Token))
                .andExpect(status().isTooManyRequests());

        // But user2 should still have their full bucket (10 requests available)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/calculate")
                    .param("a", "2")
                    .param("b", String.valueOf(i))
                    .header("Authorization", "Bearer " + user2Token))
                    .andExpect(status().isOk());
        }

        // User2's 11th request should also fail with 429
        mockMvc.perform(get("/api/calculate")
                .param("a", "2")
                .param("b", "10")
                .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isTooManyRequests());
    }

    /**
     * Test that /api/auth/login is NOT rate-limited.
     * Login attempts should be allowed even from the same user/IP after many requests.
     */
    @Test
    void testLoginNotRateLimited() throws Exception {
        // Make a login request many times without rate limiting
        for (int i = 0; i < 20; i++) {
            LoginRequest loginRequest = new LoginRequest("testuser", "SecurePass123!");
            String loginBody = objectMapper.writeValueAsString(loginRequest);

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
                    .andExpect(status().isOk());
        }
    }

    /**
     * Test that /api/history is NOT rate-limited (only /api/calculate is).
     */
    @Test
    void testHistoryNotRateLimited() throws Exception {
        // Send 20 requests to /api/history (should all succeed, no rate limiting)
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/api/history")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }
}
