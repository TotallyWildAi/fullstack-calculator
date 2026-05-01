package com.bench.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for CORS configuration with non-allowed origin.
 * Uses a separate Spring context with app.cors.allowed-origins=https://prod.example.com
 * to verify that requests from http://localhost:5173 are rejected.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.cors.allowed-origins=https://prod.example.com"
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsNonAllowedOriginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test 2: Non-allowed origin is rejected.
     * With app.cors.allowed-origins=https://prod.example.com,
     * sends an OPTIONS request from http://localhost:5173 to /api/calculate.
     * Verifies that response returns 403 status, confirming the origin is rejected.
     */
    @Test
    void testNonAllowedOriginPreflightRejected() throws Exception {
        mockMvc.perform(options("/api/calculate")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
