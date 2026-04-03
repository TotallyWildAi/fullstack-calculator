package com.bench;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the Calculator class.
 */
class CalculatorTest {

    /**
     * Test calculate with 'add' operation.
     */
    @Test
    void testCalculateAdd() {
        assertEquals(5, Calculator.calculate(2, 3, "add"));
    }

    /**
     * Test calculate with null operation defaults to 'add'.
     */
    @Test
    void testCalculateDefaultIsAdd() {
        assertEquals(5, Calculator.calculate(2, 3, null));
    }

    /**
     * Test calculate with 'mul' operation.
     */
    @Test
    void testCalculateMul() {
        assertEquals(12, Calculator.calculate(3, 4, "mul"));
    }

    /**
     * Test calculate with 'sub' operation.
     */
    @Test
    void testCalculateSub() {
        assertEquals(7, Calculator.calculate(10, 3, "sub"));
    }

    /**
     * Test calculate with unsupported operation throws IllegalArgumentException.
     */
    @Test
    void testCalculateUnknownThrows() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Calculator.calculate(1, 1, "div")
        );
        assertEquals(true, exception.getMessage().contains("Unknown operation"));
    }
}
