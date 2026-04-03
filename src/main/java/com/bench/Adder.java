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
     * Main entry point. Expects two or three integer arguments.
     * Parses args[0] and args[1] as integers. If args.length == 3 and args[2] equals 'mul',
     * calls multiply(a, b); otherwise calls add(a, b). If args.length is not 2 or 3,
     * arguments are not valid integers, or args[2] is invalid, prints 'Error'.
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
            int result;
            
            if (args.length == 3) {
                if ("mul".equals(args[2])) {
                    result = multiply(a, b);
                } else {
                    System.out.println("Error");
                    return;
                }
            } else {
                result = add(a, b);
            }
            
            System.out.println(result);
        } catch (NumberFormatException e) {
            System.out.println("Error");
        }
    }
}
