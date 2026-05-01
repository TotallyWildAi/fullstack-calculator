package com.bench;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
     * Test calculate with 'div' operation.
     */
    @Test
    void testCalculateDiv() {
        assertEquals(5, Calculator.calculate(10, 2, "div"));
    }

    /**
     * Test calculate with unsupported operation throws IllegalArgumentException.
     */
    @Test
    void testCalculateUnknownThrows() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Calculator.calculate(1, 1, "unknown")
        );
        assertTrue(exception.getMessage().contains("Unknown operation"));
    }

    /**
     * Test calculateDouble with 'add' operation.
     */
    @Test
    void testCalculateDoubleAdd() {
        assertEquals(5.0, Calculator.calculateDouble(2.0, 3.0, "add"), 1e-9);
    }

    /**
     * Test calculateDouble with 'sub' operation.
     */
    @Test
    void testCalculateDoubleSub() {
        assertEquals(7.0, Calculator.calculateDouble(10.0, 3.0, "sub"), 1e-9);
    }

    /**
     * Test calculateDouble with 'mul' operation.
     */
    @Test
    void testCalculateDoubleMul() {
        assertEquals(12.0, Calculator.calculateDouble(3.0, 4.0, "mul"), 1e-9);
    }

    /**
     * Test calculateDouble with 'div' operation.
     */
    @Test
    void testCalculateDoubleDiv() {
        assertEquals(5.0, Calculator.calculateDouble(10.0, 2.0, "div"), 1e-9);
    }

    /**
     * Test calculateDouble with 'sqrt' operation.
     */
    @Test
    void testSqrt() {
        assertEquals(3.0, Calculator.calculateDouble(9.0, 0.0, "sqrt"), 1e-9);
    }

    /**
     * Test calculateDouble with 'sqrt' of negative number throws ArithmeticException.
     */
    @Test
    void testSqrtNegativeThrows() {
        ArithmeticException exception = assertThrows(
            ArithmeticException.class,
            () -> Calculator.calculateDouble(-1.0, 0.0, "sqrt")
        );
        assertTrue(exception.getMessage().contains("Square root of negative"));
    }

    /**
     * Test calculateDouble with 'pow' operation.
     */
    @Test
    void testPow() {
        assertEquals(1024.0, Calculator.calculateDouble(2.0, 10.0, "pow"), 1e-9);
    }

    /**
     * Test calculateDouble with 'log' operation (base 10).
     */
    @Test
    void testLog() {
        assertEquals(2.0, Calculator.calculateDouble(100.0, 0.0, "log"), 1e-9);
    }

    /**
     * Test calculateDouble with 'log' of zero throws ArithmeticException.
     */
    @Test
    void testLogZeroThrows() {
        ArithmeticException exception = assertThrows(
            ArithmeticException.class,
            () -> Calculator.calculateDouble(0.0, 0.0, "log")
        );
        assertTrue(exception.getMessage().contains("Logarithm of non-positive"));
    }

    /**
     * Test calculateDouble with 'log' of negative number throws ArithmeticException.
     */
    @Test
    void testLogNegativeThrows() {
        ArithmeticException exception = assertThrows(
            ArithmeticException.class,
            () -> Calculator.calculateDouble(-5.0, 0.0, "log")
        );
        assertTrue(exception.getMessage().contains("Logarithm of non-positive"));
    }

    /**
     * Test calculateDouble with unsupported operation throws IllegalArgumentException.
     */
    @Test
    void testCalculateDoubleUnknownThrows() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Calculator.calculateDouble(1.0, 1.0, "unknown")
        );
        assertTrue(exception.getMessage().contains("Unknown operation"));
    }
}
