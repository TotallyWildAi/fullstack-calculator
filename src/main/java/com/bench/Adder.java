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
     * Main entry point. Expects two integer arguments.
     * Parses args[0] and args[1] as integers, calls add(), and prints result.
     * If args.length != 2 or arguments are not valid integers, prints 'Error'.
     *
     * @param args command-line arguments (expects exactly 2 integers)
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Error");
            return;
        }

        try {
            int a = Integer.parseInt(args[0]);
            int b = Integer.parseInt(args[1]);
            int result = add(a, b);
            System.out.println(result);
        } catch (NumberFormatException e) {
            System.out.println("Error");
        }
    }
}
