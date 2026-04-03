package com.bench;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the Adder class.
 */
class AdderTest {

    /**
     * Test adding two positive integers.
     */
    @Test
    void testAddPositive() {
        assertEquals(5, Adder.add(2, 3));
    }

    /**
     * Test adding negative and positive integers to get zero.
     */
    @Test
    void testAddNegative() {
        assertEquals(0, Adder.add(-1, 1));
    }

    /**
     * Test adding two zeros.
     */
    @Test
    void testAddZero() {
        assertEquals(0, Adder.add(0, 0));
    }

    /**
     * Test adding large numbers (edge case with Integer.MAX_VALUE).
     */
    @Test
    void testAddLargeNumbers() {
        assertEquals(Integer.MAX_VALUE, Adder.add(Integer.MAX_VALUE, 0));
    }

    /**
     * Test multiplying two positive integers.
     */
    @Test
    void testMultiply() {
        assertEquals(12, Adder.multiply(3, 4));
    }

    /**
     * Test multiplying by zero.
     */
    @Test
    void testMultiplyByZero() {
        assertEquals(0, Adder.multiply(5, 0));
    }

    /**
     * Test multiplying with negative numbers.
     */
    @Test
    void testMultiplyNegative() {
        assertEquals(-6, Adder.multiply(-2, 3));
    }

    /**
     * Test subtracting two positive integers.
     */
    @Test
    void testSubtract() {
        assertEquals(2, Adder.subtract(5, 3));
    }

    /**
     * Test subtracting resulting in a negative number.
     */
    @Test
    void testSubtractNegativeResult() {
        assertEquals(-2, Adder.subtract(3, 5));
    }

    /**
     * Test subtracting equal numbers resulting in zero.
     */
    @Test
    void testSubtractZero() {
        assertEquals(0, Adder.subtract(5, 5));
    }

    /**
     * Test dividing two positive integers with integer division.
     */
    @Test
    void testDivide() {
        assertEquals(3, Adder.divide(10, 3));
    }

    /**
     * Test dividing two positive integers with exact division.
     */
    @Test
    void testDivideExact() {
        assertEquals(3, Adder.divide(12, 4));
    }

    /**
     * Test dividing by zero throws IllegalArgumentException.
     */
    @Test
    void testDivideByZeroThrows() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Adder.divide(5, 0)
        );
        assertEquals(true, exception.getMessage().contains("Division by zero"));
    }

    /**
     * Test main() with invalid operation argument.
     */
    @Test
    void testInvalidOperation() {
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(outContent));
        Adder.main(new String[]{"1", "2", "invalid"});
        System.setOut(System.out);
        assertEquals("Error\n", outContent.toString());
    }
}
