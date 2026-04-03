package com.bench;

import com.bench.persistence.CalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller exposing the Calculator via HTTP API.
 */
@RestController
public class CalculatorController {

    @Autowired
    private CalculationService calculationService;

    /**
     * GET /api/calculate endpoint.
     * Accepts two integers and an optional operation, delegates to Calculator.calculate(),
     * and returns a JSON response with operands, operation, and result.
     *
     * @param a first operand
     * @param b second operand
     * @param op operation ('add', 'mul', 'sub', 'div'); defaults to 'add'
     * @return JSON map with a, b, op, result fields
     */
    @GetMapping("/api/calculate")
    public Map<String, Object> calculate(
            @RequestParam int a,
            @RequestParam int b,
            @RequestParam(defaultValue = "add") String op) {
        int result = Calculator.calculate(a, b, op);
        
        // Extract username from JWT token in SecurityContext
        String username = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            username = auth.getName();
        }
        
        // Record calculation to database
        calculationService.recordCalculation(a, b, op, result, username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("a", a);
        response.put("b", b);
        response.put("op", op);
        response.put("result", result);
        return response;
    }

    /**
     * Exception handler for IllegalArgumentException (e.g., unknown operation).
     * Returns HTTP 400 with error message in JSON body.
     *
     * @param e the IllegalArgumentException
     * @return ResponseEntity with HTTP 400 and error message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Exception handler for NumberFormatException (e.g., invalid number format in request params).
     * Returns HTTP 400 with generic error message in JSON body.
     *
     * @param e the NumberFormatException
     * @return ResponseEntity with HTTP 400 and error message
     */
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<Map<String, String>> handleNumberFormatException(NumberFormatException e) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Invalid number format");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}
