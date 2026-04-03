package com.bench.persistence;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * JPA entity representing a recorded calculation request.
 * Stores operands, operation, result, timestamp, and requesting user.
 */
@Entity
@Table(name = "calculation_records")
public class CalculationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int operandA;

    @Column(nullable = false)
    private int operandB;

    @Column(nullable = false)
    private String operation;

    @Column(nullable = false)
    private int result;

    @Column(nullable = false)
    private Instant requestedAt;

    @Column(nullable = true)
    private String requestedBy;

    /**
     * JPA lifecycle callback: sets requestedAt to current time if not already set.
     */
    @PrePersist
    protected void onCreate() {
        if (this.requestedAt == null) {
            this.requestedAt = Instant.now();
        }
    }

    // Constructors
    public CalculationRecord() {}

    public CalculationRecord(int operandA, int operandB, String operation, int result, String requestedBy) {
        this.operandA = operandA;
        this.operandB = operandB;
        this.operation = operation;
        this.result = result;
        this.requestedBy = requestedBy;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getOperandA() {
        return operandA;
    }

    public void setOperandA(int operandA) {
        this.operandA = operandA;
    }

    public int getOperandB() {
        return operandB;
    }

    public void setOperandB(int operandB) {
        this.operandB = operandB;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }
}
