package com.bench.persistence;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test configuration that provides a singleton PostgreSQL container for the entire test suite.
 * The container starts once when this class is loaded and is reused across all tests.
 * This avoids the overhead of starting/stopping a container per test class.
 */
@TestConfiguration
public class PostgresTestcontainerConfig {

    // Singleton container instance — starts once for the entire test suite
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("calculator_test")
            .withUsername("test")
            .withPassword("test");

    // Flag to track whether container started successfully
    private static final AtomicBoolean containerStarted = new AtomicBoolean(false);

    // Static initializer block — starts the container when the class is first loaded
    // Gracefully handles Docker unavailability by catching exceptions
    static {
        try {
            postgres.start();
            containerStarted.set(true);
        } catch (Exception e) {
            // Docker not available; tests will be skipped via @EnabledIfEnvironmentVariable
            System.err.println("Warning: Could not start PostgreSQL container: " + e.getMessage());
            containerStarted.set(false);
        }
    }

    /**
     * Dynamically set Spring datasource properties from the running container.
     * This method is called by Spring to override application.properties values during tests.
     * Only registers properties if the container started successfully.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (containerStarted.get()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
    }

    /**
     * Check if the PostgreSQL container started successfully.
     * Used by tests to determine if they should run.
     */
    public static boolean isContainerRunning() {
        return containerStarted.get();
    }
}
