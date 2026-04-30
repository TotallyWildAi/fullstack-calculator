package com.bench.persistence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Service layer for calculation persistence operations.
 * Wraps CalculationRepository to provide business logic for recording and retrieving calculations.
 */
@Service
public class CalculationService {

    @Autowired
    private CalculationRepository calculationRepository;

    /**
     * Record a calculation result to the database.
     *
     * @param a first operand
     * @param b second operand
     * @param operation the operation performed ('add', 'sub', 'mul', 'div')
     * @param result the calculation result
     * @param username the user who requested the calculation (may be null for anonymous)
     * @return the saved CalculationRecord
     */
    public CalculationRecord recordCalculation(int a, int b, String operation, int result, String username) {
        CalculationRecord record = new CalculationRecord(a, b, operation, result, username);
        return calculationRepository.save(record);
    }

    /**
     * Retrieve the 10 most recent calculations across all users.
     *
     * @return list of up to 10 CalculationRecord ordered by most recent first
     */
    public List<CalculationRecord> getRecentCalculations() {
        return calculationRepository.findTop10ByOrderByRequestedAtDesc();
    }

    /**
     * Retrieve the 50 most recent calculations across all users.
     *
     * @return list of up to 50 CalculationRecord ordered by most recent first
     */
    public List<CalculationRecord> getHistory() {
        return calculationRepository.findTop50ByOrderByRequestedAtDesc();
    }

    /**
     * Retrieve all calculations performed by a specific user.
     *
     * @param username the username to filter by
     * @return list of CalculationRecord for the user, ordered by most recent first
     */
    public List<CalculationRecord> getCalculationsByUser(String username) {
        return calculationRepository.findByRequestedByOrderByRequestedAtDesc(username);
    }
}
