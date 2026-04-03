package com.bench;

/**
 * Simple adder utility with main method for command-line usage.
 */
public class Adder {

    /**
     * Adds two integers.
     *
     * @param a first integer
     * @param b second integer
     * @return sum of a and b
     */
    public static int add(int a, int b) {
        return a + b;
    }

    /**
     * Multiplies two integers.
     *
     * @param a first integer
     * @param b second integer
     * @return product of a and b
     */
    public static int multiply(int a, int b) {
        return a * b;
    }

    /**
     * Subtracts two integers.
     *
     * @param a first integer
     * @param b second integer
     * @return difference of a and b
     */
    public static int subtract(int a, int b) {
        return a - b;
    }

    /**
     * Main entry point. Expects two or three integer arguments.
     * Parses args[0] and args[1] as integers. If args.length == 3, args[2] specifies the operation
     * ('add', 'mul', 'sub'); otherwise defaults to 'add'.
     * Delegates to Calculator.calculate() for operation execution.
     * If args.length is not 2 or 3, arguments are not valid integers, or operation is unsupported, prints 'Error'.
     *
     * @param args command-line arguments (expects 2 or 3 arguments)
     */
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Error");
            return;
        }

        try {
            int a = Integer.parseInt(args[0]);
            int b = Integer.parseInt(args[1]);
            String operation = (args.length == 3) ? args[2] : "add";
            
            int result = Calculator.calculate(a, b, operation);
            System.out.println(result);
        } catch (NumberFormatException e) {
            System.out.println("Error");
        } catch (IllegalArgumentException e) {
            System.out.println("Error");
        }
    }
}
