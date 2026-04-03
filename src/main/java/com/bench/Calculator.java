package com.bench;

/**
 * Calculator utility that delegates arithmetic operations to Adder static methods.
 */
public class Calculator {

    /**
     * Performs arithmetic calculation based on the specified operation.
     * Supported operations: 'add', 'sub', 'mul'. If operation is null, defaults to 'add'.
     *
     * @param a first integer operand
     * @param b second integer operand
     * @param operation the operation to perform ('add', 'sub', 'mul', or null for 'add')
     * @return the result of the calculation
     * @throws IllegalArgumentException if operation is not supported
     */
    public static int calculate(int a, int b, String operation) {
        // Default to 'add' if operation is null
        if (operation == null) {
            operation = "add";
        }
        
        switch (operation) {
            case "add":
                return Adder.add(a, b);
            case "mul":
                return Adder.multiply(a, b);
            case "sub":
                return Adder.subtract(a, b);
            default:
                throw new IllegalArgumentException("Unknown operation: " + operation);
        }
    }
}
