package com.bench.persistence;

import com.bench.persistence.CalculationRecord;
import com.bench.persistence.CalculationRepository;
import com.bench.persistence.PostgresTestcontainerConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * End-to-end integration test verifying full flow: JWT authentication, calculation execution, and PostgreSQL persistence.
 * Uses Testcontainers singleton PostgreSQL container from PostgresTestcontainerConfig.
 * Tests verify actual database records via CalculationRepository after HTTP requests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(PostgresTestcontainerConfig.class)
@ActiveProfiles("test")
class FullStackDbIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CalculationRepository calculationRepository;

    /**
     * Clear calculation records before each test to ensure isolation.
     */
    @BeforeEach
    void setUp() {
        calculationRepository.deleteAll();
    }

    /**
     * Test that a single calculation is persisted to PostgreSQL after authenticated request.
     * Flow: login → perform calculation → verify record in DB with correct operands, operation, result, and username.
     */
    @Test
    @Transactional
    void testCalculationIsPersisted() throws Exception {
        // Login and get JWT token
        String token = loginAndGetToken("testuser", "SecurePass123!");

        // Perform calculation via HTTP
        mockMvc.perform(get("/api/calculate")
                .param("a", "10")
                .param("b", "5")
                .param("op", "add")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(15));

        // Query repository to verify persistence
        List<CalculationRecord> records = calculationRepository.findByRequestedByOrderByRequestedAtDesc("testuser");
        assertEquals(1, records.size(), "Expected 1 calculation record");

        CalculationRecord record = records.get(0);
        assertEquals(10, record.getOperandA(), "operandA should be 10");
        assertEquals(5, record.getOperandB(), "operandB should be 5");
        assertEquals("add", record.getOperation(), "operation should be 'add'");
        assertEquals(15, record.getResult(), "result should be 15");
        assertEquals("testuser", record.getRequestedBy(), "requestedBy should be 'testuser'");
    }

    /**
     * Test that multiple calculations are persisted correctly.
     * Flow: login → perform 3 different operations (add, mul, sub) → verify all 3 records in DB.
     */
    @Test
    @Transactional
    void testMultipleCalculationsPersisted() throws Exception {
        // Login and get JWT token
        String token = loginAndGetToken("testuser", "SecurePass123!");

        // Perform first calculation: 10 + 5 = 15
        mockMvc.perform(get("/api/calculate")
                .param("a", "10")
                .param("b", "5")
                .param("op", "add")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Perform second calculation: 10 * 5 = 50
        mockMvc.perform(get("/api/calculate")
                .param("a", "10")
                .param("b", "5")
                .param("op", "mul")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Perform third calculation: 10 - 5 = 5
        mockMvc.perform(get("/api/calculate")
                .param("a", "10")
                .param("b", "5")
                .param("op", "sub")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Query repository to verify all 3 records persisted
        List<CalculationRecord> records = calculationRepository.findByRequestedByOrderByRequestedAtDesc("testuser");
        assertEquals(3, records.size(), "Expected 3 calculation records");

        // Verify records (most recent first due to DESC ordering)
        assertEquals("sub", records.get(0).getOperation());
        assertEquals(5, records.get(0).getResult());

        assertEquals("mul", records.get(1).getOperation());
        assertEquals(50, records.get(1).getResult());

        assertEquals("add", records.get(2).getOperation());
        assertEquals(15, records.get(2).getResult());
    }

    /**
     * Test that requestedAt timestamp is set correctly and is recent.
     * Flow: login → perform calculation → verify requestedAt is within last 5 seconds.
     */
    @Test
    @Transactional
    void testCalculationTimestamp() throws Exception {
        // Record time before calculation
        Instant beforeCalculation = Instant.now();

        // Login and get JWT token
        String token = loginAndGetToken("testuser", "SecurePass123!");

        // Perform calculation
        mockMvc.perform(get("/api/calculate")
                .param("a", "7")
                .param("b", "3")
                .param("op", "add")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Record time after calculation
        Instant afterCalculation = Instant.now();

        // Query repository
        List<CalculationRecord> records = calculationRepository.findByRequestedByOrderByRequestedAtDesc("testuser");
        assertEquals(1, records.size());

        Instant requestedAt = records.get(0).getRequestedAt();
        assertNotNull(requestedAt, "requestedAt should not be null");
        assertTrue(requestedAt.isAfter(beforeCalculation.minusSeconds(1)), "requestedAt should be after calculation start");
        assertTrue(requestedAt.isBefore(afterCalculation.plusSeconds(1)), "requestedAt should be before calculation end");
    }

    /**
     * Test that unauthenticated requests are rejected and no record is persisted.
     * Flow: attempt calculation without token → expect 403 Forbidden → verify no new records in DB.
     */
    @Test
    @Transactional
    void testUnauthenticatedCalculationNotPersisted() throws Exception {
        // Record initial count
        long initialCount = calculationRepository.count();

        // Attempt calculation without authentication token
        mockMvc.perform(get("/api/calculate")
                .param("a", "10")
                .param("b", "5")
                .param("op", "add"))
                .andExpect(status().isForbidden());

        // Verify no new records were persisted
        long finalCount = calculationRepository.count();
        assertEquals(initialCount, finalCount, "No new records should be persisted for unauthenticated request");
    }

    /**
     * Helper method: authenticate user and extract JWT token from login response.
     * Posts to /api/auth/login with username and password, extracts token from JSON response.
     *
     * @param username the username to authenticate
     * @param password the password to authenticate
     * @return JWT token string
     * @throws Exception if login request fails
     */
    private String loginAndGetToken(String username, String password) throws Exception {
        // Create login request
        Map<String, String> loginRequest = Map.of("username", username, "password", password);
        String loginBody = objectMapper.writeValueAsString(loginRequest);

        // Post to /api/auth/login
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        // Extract token from response
        String responseBody = loginResult.getResponse().getContentAsString();
        Map<String, String> responseMap = objectMapper.readValue(responseBody, Map.class);
        return responseMap.get("token");
    }
}
