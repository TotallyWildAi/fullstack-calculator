package com.bench;

/**
 * Calculator utility that delegates arithmetic operations to Adder static methods.
 */
public class Calculator {

    /**
     * Performs arithmetic calculation based on the specified operation.
     * Supported operations: 'add', 'sub', 'mul', 'div'. If operation is null, defaults to 'add'.
     *
     * @param a first integer operand
     * @param b second integer operand
     * @param operation the operation to perform ('add', 'sub', 'mul', or null for 'add')
     * @return the result of the calculation
     * @throws IllegalArgumentException if operation is not supported
     */
    public static int calculate(int a, int b, String operation) {
        return calculate(a, b, operation, null);
    }

    /**
     * Performs arithmetic calculation based on the specified operation with optional history logging.
     * Supported operations: 'add', 'sub', 'mul', 'div'. If operation is null, defaults to 'add'.
     *
     * @param a first integer operand
     * @param b second integer operand
     * @param operation the operation to perform ('add', 'sub', 'mul', or null for 'add')
     * @param log optional HistoryLog to record the computation; if null, no recording occurs
     * @return the result of the calculation
     * @throws IllegalArgumentException if operation is not supported
     */
    public static int calculate(int a, int b, String operation, HistoryLog log) {
        // Default to 'add' if operation is null
        if (operation == null) {
            operation = "add";
        }
        
        int result;
        switch (operation) {
            case "add":
                result = Adder.add(a, b);
                break;
            case "mul":
                result = Adder.multiply(a, b);
                break;
            case "sub":
                result = Adder.subtract(a, b);
                break;
            case "div":
                result = Adder.divide(a, b);
                break;
            default:
                throw new IllegalArgumentException("Unknown operation: " + operation);
        }
        
        // Record the computation if log is provided
        if (log != null) {
            log.record(a, b, operation, result);
        }
        
        return result;
    }
}
