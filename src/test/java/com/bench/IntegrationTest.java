package com.bench;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Integration tests for the Adder CLI by capturing System.out.
 */
class IntegrationTest {

    private PrintStream originalOut;
    private ByteArrayOutputStream capturedOutput;

    /**
     * Redirect System.out before each test.
     */
    @BeforeEach
    void setUp() {
        originalOut = System.out;
        capturedOutput = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOutput));
    }

    /**
     * Restore System.out after each test.
     */
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    /**
     * Test add operation via main() CLI.
     */
    @Test
    void testAddViaMain() {
        Adder.main(new String[]{"2", "3"});
        String output = capturedOutput.toString();
        assertTrue(output.contains("5"), "Output should contain '5', got: " + output);
    }

    /**
     * Test div operation via main() CLI.
     */
    @Test
    void testDivViaMain() {
        Adder.main(new String[]{"10", "3", "div"});
        String output = capturedOutput.toString();
        assertTrue(output.contains("3"), "Output should contain '3', got: " + output);
    }

    /**
     * Test division by zero via main() CLI.
     */
    @Test
    void testDivByZeroViaMain() {
        Adder.main(new String[]{"5", "0", "div"});
        String output = capturedOutput.toString();
        assertTrue(output.contains("Error"), "Output should contain 'Error', got: " + output);
    }

    /**
     * Test invalid arguments via main() CLI.
     */
    @Test
    void testInvalidArgsViaMain() {
        Adder.main(new String[]{"abc"});
        String output = capturedOutput.toString();
        assertTrue(output.contains("Error"), "Output should contain 'Error', got: " + output);
    }
}
