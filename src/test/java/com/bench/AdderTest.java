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
}
