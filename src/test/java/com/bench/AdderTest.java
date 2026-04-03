package com.bench;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
