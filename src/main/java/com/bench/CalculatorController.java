package com.bench;

import com.bench.persistence.CalculationRecord;
import com.bench.persistence.CalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller exposing the Calculator via HTTP API.
 */
@RestController
@Tag(name = "Calculator", description = "Arithmetic and scientific operations: add, subtract, multiply, divide, sqrt, pow, log")
public class CalculatorController {

    @Autowired
    private CalculationService calculationService;

    /**
     * GET /api/calculate endpoint.
     * Accepts two operands and an operation, delegates to Calculator.calculateDouble(),
     * and returns a JSON response with operands, operation, and result.
     * Supported operations: 'add', 'sub', 'mul', 'div', 'sqrt', 'pow', 'log'.
     * For 'sqrt' and 'log', only parameter 'a' is required; 'b' defaults to 0.
     *
     * @param a first operand (or sole operand for sqrt/log)
     * @param b second operand (defaults to 0 for sqrt/log)
     * @param op operation ('add', 'sub', 'mul', 'div', 'sqrt', 'pow', 'log'); defaults to 'add'
     * @return JSON map with a, b, op, result fields
     */
    @GetMapping("/api/calculate")
    @Operation(summary = "Perform arithmetic or scientific operation", description = "Executes the specified operation on operands and returns the result. Supports: add, sub, mul, div, sqrt, pow, log")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Calculation successful",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"a\": 2, \"b\": 3, \"op\": \"add\", \"result\": 5.0}"))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid operation, invalid number format, or domain error (e.g., sqrt of negative)"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - invalid JWT token")
    })
    public Map<String, Object> calculate(
            @RequestParam double a,
            @RequestParam(defaultValue = "0") double b,
            @RequestParam(defaultValue = "add") String op) {
        double result = Calculator.calculateDouble(a, b, op);
        
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
     * GET /api/history endpoint.
     * Returns calculations for the authenticated user, ordered by most recent first.
     * Requires JWT authentication.
     *
     * @return JSON array of calculation records with id, a, b, op, result, createdAt fields
     */
    @GetMapping("/api/history")
    @Operation(summary = "Get calculation history", description = "Returns the user's calculations ordered by most recent first")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "History retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "[{\"id\": 1, \"a\": 2, \"b\": 3, \"op\": \"add\", \"result\": 5.0, \"createdAt\": \"2024-01-01T12:00:00Z\"}]"))
        ),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - invalid JWT token")
    })
    public List<Map<String, Object>> getHistory() {
        // Extract username from SecurityContext
        String username = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            username = auth.getName();
        }
        
        // Get calculations for the authenticated user
        List<CalculationRecord> records = calculationService.getCalculationsByUser(username);
        
        return records.stream()
            .map(record -> {
                Map<String, Object> entry = new HashMap<>();
                entry.put("id", record.getId());
                entry.put("a", record.getOperandA());
                entry.put("b", record.getOperandB());
                entry.put("op", record.getOperation());
                entry.put("result", record.getResult());
                entry.put("createdAt", record.getRequestedAt());
                return entry;
            })
            .collect(Collectors.toList());
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
     * Exception handler for ArithmeticException (e.g., sqrt of negative, log of zero/negative, division by zero).
     * Returns HTTP 400 with error message in JSON body.
     *
     * @param e the ArithmeticException
     * @return ResponseEntity with HTTP 400 and error message
     */
    @ExceptionHandler(ArithmeticException.class)
    public ResponseEntity<Map<String, String>> handleArithmeticException(ArithmeticException e) {
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
