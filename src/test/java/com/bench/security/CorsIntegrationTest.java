package com.bench.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.hamcrest.Matchers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

/**
 * Integration tests for CORS configuration.
 * Tests preflight requests, allowed origins, and allowed methods.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test 1: OPTIONS preflight request from allowed origin returns 200 with correct CORS headers.
     * Verifies that a preflight request from http://localhost:5173 to /api/calculate
     * returns HTTP 200 with all required Access-Control-Allow-* headers.
     */
    @Test
    void testAllowedOriginPreflightReturns200WithCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/calculate")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", Matchers.containsString("GET")))
                .andExpect(header().string("Access-Control-Allow-Methods", Matchers.containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Headers", Matchers.containsString("Authorization")))
                .andExpect(header().string("Access-Control-Allow-Headers", Matchers.containsString("Content-Type")))
                .andExpect(header().string("Access-Control-Expose-Headers", Matchers.containsString("Authorization")))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    /**
     * Test 3: OPTIONS preflight request with disallowed method returns 403 or no Allow-Methods.
     * Verifies that a preflight request from an allowed origin with DELETE method
     * is rejected because DELETE is not in the allowed methods list.
     */
    @Test
    void testDisallowedMethodPreflightReturned() throws Exception {
        mockMvc.perform(options("/api/calculate")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "DELETE"))
                .andExpect(status().isForbidden());
    }

    /**
     * Nested test class for testing non-allowed origins.
     * Uses a separate Spring context with overridden CORS configuration.
     */
    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                    properties = "app.cors.allowed-origins=https://prod.example.com")
    @AutoConfigureMockMvc
    @ActiveProfiles("test")
    class NonAllowedOriginTest {

        @Autowired
        private MockMvc mockMvc;

        /**
         * Test 2: OPTIONS preflight from non-allowed origin is rejected.
         * With app.cors.allowed-origins=https://prod.example.com,
         * a preflight request from http://localhost:5173 should be rejected
         * (no Access-Control-Allow-Origin header or 403).
         */
        @Test
        void testNonAllowedOriginPreflightRejected() throws Exception {
            mockMvc.perform(options("/api/calculate")
                    .header("Origin", "http://localhost:5173")
                    .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isForbidden());
        }
    }
}
