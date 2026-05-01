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
    private double operandA;

    @Column(nullable = false)
    private double operandB;

    @Column(nullable = false)
    private String operation;

    @Column(nullable = false)
    private double result;

    @Column(nullable = false)
    private Instant requestedAt;

    @Column(nullable = false)
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

    public CalculationRecord(double operandA, double operandB, String operation, double result, String requestedBy) {
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

    public double getOperandA() {
        return operandA;
    }

    public void setOperandA(double operandA) {
        this.operandA = operandA;
    }

    public double getOperandB() {
        return operandB;
    }

    public void setOperandB(double operandB) {
        this.operandB = operandB;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public double getResult() {
        return result;
    }

    public void setResult(double result) {
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
