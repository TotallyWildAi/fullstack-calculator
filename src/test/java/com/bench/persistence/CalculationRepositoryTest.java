package com.bench.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CalculationRepository against a real PostgreSQL instance.
 * Uses Testcontainers to spin up a PostgreSQL container for the entire test suite.
 * Each test is transactional and rolled back to keep tests isolated.
 * Tests are skipped if Docker is unavailable (container failed to start).
 */
@SpringBootTest
@Import(PostgresTestcontainerConfig.class)
@EnabledIf("com.bench.persistence.PostgresTestcontainerConfig#isContainerRunning")
class CalculationRepositoryTest {

    @Autowired
    private CalculationRepository calculationRepository;

    @Autowired
    private CalculationService calculationService;

    /**
     * Test saving and retrieving a single CalculationRecord.
     * Verifies that all fields are persisted correctly and can be retrieved by ID.
     */
    @Test
    @Transactional
    @Rollback
    void testSaveAndRetrieve() {
        // Arrange: create a calculation record
        CalculationRecord record = new CalculationRecord(5, 3, "add", 8, "alice");
        record.setRequestedAt(Instant.now());

        // Act: save the record
        CalculationRecord saved = calculationRepository.save(record);
        assertNotNull(saved.getId(), "Saved record should have an ID");

        // Assert: retrieve and verify all fields
        CalculationRecord retrieved = calculationRepository.findById(saved.getId()).orElse(null);
        assertNotNull(retrieved, "Record should be retrievable by ID");
        assertEquals(5, retrieved.getOperandA());
        assertEquals(3, retrieved.getOperandB());
        assertEquals("add", retrieved.getOperation());
        assertEquals(8, retrieved.getResult());
        assertEquals("alice", retrieved.getRequestedBy());
        assertNotNull(retrieved.getRequestedAt());
    }

    /**
     * Test finding calculations by user.
     * Saves 3 records (2 for 'alice', 1 for 'bob') and verifies that
     * findByRequestedByOrderByRequestedAtDesc returns only alice's records in descending order.
     */
    @Test
    @Transactional
    @Rollback
    void testFindByUser() {
        // Arrange: save 3 records with different users
        CalculationRecord record1 = new CalculationRecord(2, 3, "add", 5, "alice");
        CalculationRecord record2 = new CalculationRecord(10, 5, "sub", 5, "bob");
        CalculationRecord record3 = new CalculationRecord(4, 4, "mul", 16, "alice");

        calculationRepository.save(record1);
        calculationRepository.save(record2);
        calculationRepository.save(record3);

        // Act: find all calculations by alice
        List<CalculationRecord> aliceRecords = calculationRepository.findByRequestedByOrderByRequestedAtDesc("alice");

        // Assert: should have exactly 2 records for alice
        assertEquals(2, aliceRecords.size(), "Alice should have 2 records");
        assertTrue(aliceRecords.stream().allMatch(r -> "alice".equals(r.getRequestedBy())),
                "All records should belong to alice");
        // Verify descending order by requestedAt
        for (int i = 0; i < aliceRecords.size() - 1; i++) {
            assertTrue(aliceRecords.get(i).getRequestedAt().isAfter(aliceRecords.get(i + 1).getRequestedAt()),
                    "Records should be ordered by requestedAt descending");
        }
    }

    /**
     * Test finding the top 10 most recent calculations.
     * Saves 12 records and verifies that findTop10ByOrderByRequestedAtDesc returns exactly 10.
     */
    @Test
    @Transactional
    @Rollback
    void testFindTop10Recent() {
        // Arrange: save 12 records
        for (int i = 0; i < 12; i++) {
            CalculationRecord record = new CalculationRecord(i, i + 1, "add", i + (i + 1), "user" + i);
            calculationRepository.save(record);
        }

        // Act: find top 10 recent
        List<CalculationRecord> top10 = calculationRepository.findTop10ByOrderByRequestedAtDesc();

        // Assert: should have exactly 10 records
        assertEquals(10, top10.size(), "Should return exactly 10 records");
        // Verify descending order by requestedAt
        for (int i = 0; i < top10.size() - 1; i++) {
            assertTrue(top10.get(i).getRequestedAt().isAfter(top10.get(i + 1).getRequestedAt()),
                    "Records should be ordered by requestedAt descending");
        }
    }

    /**
     * Test that requestedAt timestamp is automatically set on persist.
     * Creates a record without setting requestedAt and verifies that
     * the @PrePersist lifecycle callback sets it to the current time.
     */
    @Test
    @Transactional
    @Rollback
    void testTimestampAutoSet() {
        // Arrange: create a record without setting requestedAt
        CalculationRecord record = new CalculationRecord(7, 2, "div", 3, "charlie");
        // requestedAt is null at this point
        assertNull(record.getRequestedAt(), "requestedAt should be null before save");

        // Act: save the record (triggers @PrePersist)
        Instant beforeSave = Instant.now();
        CalculationRecord saved = calculationRepository.save(record);
        Instant afterSave = Instant.now();

        // Assert: requestedAt should be set to current time
        assertNotNull(saved.getRequestedAt(), "requestedAt should be set after save");
        assertTrue(saved.getRequestedAt().isAfter(beforeSave.minusSeconds(1)),
                "requestedAt should be close to current time");
        assertTrue(saved.getRequestedAt().isBefore(afterSave.plusSeconds(1)),
                "requestedAt should be close to current time");
    }
}
