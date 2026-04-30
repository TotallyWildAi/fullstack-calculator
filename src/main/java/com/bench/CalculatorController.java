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
@Tag(name = "Calculator", description = "Arithmetic operations: add, subtract, multiply, divide")
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
    @Operation(summary = "Perform arithmetic operation", description = "Executes the specified operation on two operands and returns the result")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Calculation successful",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"a\": 2, \"b\": 3, \"op\": \"add\", \"result\": 5}"))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid operation or invalid number format"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - invalid JWT token")
    })
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
     * GET /api/history endpoint.
     * Returns the last 50 calculations ordered by most recent first.
     * Requires JWT authentication.
     *
     * @return JSON array of calculation records with id, a, b, op, result, createdAt fields
     */
    @GetMapping("/api/history")
    @Operation(summary = "Get calculation history", description = "Returns the last 50 calculations ordered by most recent first")
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "History retrieved successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(example = "[{\"id\": 1, \"a\": 2, \"b\": 3, \"op\": \"add\", \"result\": 5, \"createdAt\": \"2024-01-01T12:00:00Z\"}]"))
        ),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Forbidden - invalid JWT token")
    })
    public List<Map<String, Object>> getHistory() {
        List<CalculationRecord> records = calculationService.getHistory();
        
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
