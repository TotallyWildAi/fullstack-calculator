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

    /**
     * Performs arithmetic or scientific calculation based on the specified operation.
     * Supported operations: 'add', 'sub', 'mul', 'div', 'sqrt', 'pow', 'log'.
     * If operation is null, defaults to 'add'.
     *
     * @param a first double operand (or sole operand for sqrt and log)
     * @param b second double operand (ignored for sqrt and log)
     * @param operation the operation to perform ('add', 'sub', 'mul', 'div', 'sqrt', 'pow', 'log', or null for 'add')
     * @return the result of the calculation as a double
     * @throws IllegalArgumentException if operation is not supported
     * @throws ArithmeticException if sqrt of negative, log of zero/negative, or division by zero
     */
    public static double calculateDouble(double a, double b, String operation) {
        // Default to 'add' if operation is null
        if (operation == null) {
            operation = "add";
        }
        
        double result;
        switch (operation) {
            case "add":
                result = a + b;
                break;
            case "sub":
                result = a - b;
                break;
            case "mul":
                result = a * b;
                break;
            case "div":
                if (b == 0) {
                    throw new ArithmeticException("Division by zero");
                }
                result = a / b;
                break;
            case "sqrt":
                if (a < 0) {
                    throw new ArithmeticException("Square root of negative number");
                }
                result = Math.sqrt(a);
                break;
            case "pow":
                result = Math.pow(a, b);
                break;
            case "log":
                if (a <= 0) {
                    throw new ArithmeticException("Logarithm of non-positive number");
                }
                result = Math.log10(a);
                break;
            default:
                throw new IllegalArgumentException("Unknown operation: " + operation);
        }
        
        return result;
    }
}
