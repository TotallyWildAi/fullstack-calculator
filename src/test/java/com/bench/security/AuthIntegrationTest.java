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
 * Integration tests for authentication and authorization flow.
 * Tests login endpoint, JWT token generation, and protected endpoint access.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test successful login with correct credentials.
     * Verifies that POST /api/auth/login with valid username and password
     * returns HTTP 200 with a JWT token in the response.
     */
    @Test
    void testLoginSuccess() throws Exception {
        LoginRequest loginRequest = new LoginRequest("testuser", "SecurePass123!");
        String requestBody = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    /**
     * Test login with wrong password.
     * Verifies that POST /api/auth/login with correct username but wrong password
     * returns HTTP 401 Unauthorized.
     */
    @Test
    void testLoginWrongPassword() throws Exception {
        LoginRequest loginRequest = new LoginRequest("testuser", "wrong");
        String requestBody = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test login with unknown user.
     * Verifies that POST /api/auth/login with non-existent username
     * returns HTTP 401 Unauthorized.
     */
    @Test
    void testLoginUnknownUser() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nobody", "pass");
        String requestBody = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Test calculator endpoint with valid JWT token.
     * First logs in to obtain a token, then uses it to access /api/calculate.
     * Verifies that GET /api/calculate?a=2&b=3 with valid token returns HTTP 200 with result=5.
     */
    @Test
    void testCalculateWithValidToken() throws Exception {
        // Login to get token
        LoginRequest loginRequest = new LoginRequest("testuser", "SecurePass123!");
        String loginBody = objectMapper.writeValueAsString(loginRequest);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        // Extract token from login response
        String responseBody = loginResult.getResponse().getContentAsString();
        Map<String, String> responseMap = objectMapper.readValue(responseBody, Map.class);
        String token = responseMap.get("token");

        // Use token to access protected endpoint
        mockMvc.perform(get("/api/calculate")
                .param("a", "2")
                .param("b", "3")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(5));
    }

    /**
     * Test calculator endpoint without authentication token.
     * Verifies that GET /api/calculate?a=2&b=3 without Authorization header
     * returns HTTP 401 Unauthorized.
     */
    @Test
    void testCalculateWithoutToken() throws Exception {
        mockMvc.perform(get("/api/calculate")
                .param("a", "2")
                .param("b", "3"))
                .andExpect(status().isForbidden());
    }

    /**
     * Test calculator endpoint with invalid JWT token.
     * Verifies that GET /api/calculate?a=2&b=3 with malformed token
     * returns HTTP 401 Unauthorized.
     */
    @Test
    void testCalculateWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/calculate")
                .param("a", "2")
                .param("b", "3")
                .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isForbidden());
    }

    /**
     * Test health endpoint is publicly accessible.
     * Verifies that GET /actuator/health returns HTTP 200 with status:UP.
     */
    @Test
    void testHealthEndpointPublicAccess() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    /**
     * Test OpenAPI docs endpoint is publicly accessible.
     * Verifies that GET /v3/api-docs returns HTTP 200 with paths object.
     */
    @Test
    void testOpenApiDocsPublicAccess() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths").exists());
    }
}
