package com.bench;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Integration tests for the CalculatorController REST API using MockMvc.
 * Tests verify HTTP status codes and JSON response bodies for various calculator operations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
class CalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test add operation via REST API.
     * Verifies that GET /api/calculate?a=2&b=3&op=add returns 200 with result=5.
     */
    @Test
    @WithMockUser
    void testAddViaApi() throws Exception {
        mockMvc.perform(get("/api/calculate")
                .param("a", "2")
                .param("b", "3")
                .param("op", "add"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(5));
    }

    /**
     * Test default operation is add when op parameter is omitted.
     * Verifies that GET /api/calculate?a=2&b=3 (no op) returns 200 with result=5.
     */
    @Test
    @WithMockUser
    void testDefaultOpIsAdd() throws Exception {
        mockMvc.perform(get("/api/calculate")
                .param("a", "2")
                .param("b", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(5));
    }

    /**
     * Test multiply operation via REST API.
     * Verifies that GET /api/calculate?a=3&b=4&op=mul returns 200 with result=12.
     */
    @Test
    @WithMockUser
    void testMulViaApi() throws Exception {
        mockMvc.perform(get("/api/calculate")
                .param("a", "3")
                .param("b", "4")
                .param("op", "mul"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(12));
    }

    /**
     * Test subtract operation via REST API.
     * Verifies that GET /api/calculate?a=10&b=3&op=sub returns 200 with result=7.
     */
    @Test
    @WithMockUser
    void testSubViaApi() throws Exception {
        mockMvc.perform(get("/api/calculate")
                .param("a", "10")
                .param("b", "3")
                .param("op", "sub"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(7));
    }

    /**
     * Test divide operation via REST API.
     * Verifies that GET /api/calculate?a=10&b=2&op=div returns 200 with result=5.
     */
    @Test
    @WithMockUser
    void testDivViaApi() throws Exception {
        mockMvc.perform(get("/api/calculate")
                .param("a", "10")
                .param("b", "2")
                .param("op", "div"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(5));
    }

    /**
     * Test invalid operation returns HTTP 400 with error message.
     * Verifies that GET /api/calculate?a=1&b=1&op=pow returns 400 with error field in JSON.
     */
    @Test
    @WithMockUser
    void testInvalidOpReturns400() throws Exception {
        mockMvc.perform(get("/api/calculate")
                .param("a", "1")
                .param("b", "1")
                .param("op", "pow"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    /**
     * Test missing required parameter returns HTTP 400.
     * Verifies that GET /api/calculate?a=1 (missing b) returns 400.
     */
    @Test
    @WithMockUser
    void testMissingParamReturns400() throws Exception {
        mockMvc.perform(get("/api/calculate")
                .param("a", "1"))
                .andExpect(status().isBadRequest());
    }
}

