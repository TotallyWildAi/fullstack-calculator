package com.bench.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Spring Data JPA repository for CalculationRecord entities.
 * Provides CRUD operations and custom query methods for calculation history.
 */
@Repository
public interface CalculationRepository extends JpaRepository<CalculationRecord, Long> {

    /**
     * Find all calculations by a specific user, ordered by most recent first.
     *
     * @param username the username to filter by
     * @return list of CalculationRecord ordered by requestedAt descending
     */
    List<CalculationRecord> findByRequestedByOrderByRequestedAtDesc(String username);

    /**
     * Find the 10 most recent calculations across all users.
     *
     * @return list of up to 10 CalculationRecord ordered by requestedAt descending
     */
    List<CalculationRecord> findTop10ByOrderByRequestedAtDesc();
}
