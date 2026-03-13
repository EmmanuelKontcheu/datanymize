package com.datanymize.api.exception;

import com.datanymize.api.dto.ApiResponse;
import com.datanymize.exception.DatanymizeException;
import com.datanymize.security.ReadOnlyValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST API.
 * Handles all exceptions and returns formatted error responses.
 * 
 * Validates Requirements: 19.1, 19.2, 19.3, 19.4, 19.5
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Handle DatanymizeException.
     */
    @ExceptionHandler(DatanymizeException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatanymizeException(
            DatanymizeException ex, WebRequest request) {
        
        log.error("DatanymizeException: {}", ex.getMessage(), ex);
        
        HttpStatus status = mapSeverityToHttpStatus(ex.getSeverity());
        
        ApiResponse.ErrorDetails errorDetails = ApiResponse.ErrorDetails.builder()
            .code(ex.getErrorCode() != null ? ex.getErrorCode() : "DATANYMIZE_ERROR")
            .statusCode(status.value())
            .details(ex.getContext())
            .suggestion(ex.getSuggestion())
            .build();
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .success(false)
            .message(ex.getMessage())
            .error(errorDetails)
            .build();
        
        return new ResponseEntity<>(response, status);
    }
    
    /**
     * Handle read-only access violations.
     */
    @ExceptionHandler(ReadOnlyValidator.ReadOnlyAccessViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleReadOnlyViolation(
            ReadOnlyValidator.ReadOnlyAccessViolationException ex, WebRequest request) {
        
        log.warn("Read-only access violation: {}", ex.getMessage());
        
        ApiResponse.ErrorDetails errorDetails = ApiResponse.ErrorDetails.builder()
            .code("READ_ONLY_VIOLATION")
            .statusCode(HttpStatus.FORBIDDEN.value())
            .suggestion("Use a connection with write permissions if you need to modify data")
            .build();
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .success(false)
            .message(ex.getMessage())
            .error(errorDetails)
            .build();
        
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }
    
    /**
     * Handle validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        log.warn("Validation error: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        StringBuilder details = new StringBuilder();
        errors.forEach((field, message) -> 
            details.append(field).append(": ").append(message).append("; ")
        );
        
        ApiResponse.ErrorDetails errorDetails = ApiResponse.ErrorDetails.builder()
            .code("VALIDATION_ERROR")
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .details(details.toString())
            .suggestion("Please check the request parameters and try again")
            .build();
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .success(false)
            .message("Request validation failed")
            .error(errorDetails)
            .build();
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * Handle generic exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected exception: {}", ex.getMessage(), ex);
        
        ApiResponse.ErrorDetails errorDetails = ApiResponse.ErrorDetails.builder()
            .code("INTERNAL_ERROR")
            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .details(ex.getClass().getSimpleName())
            .suggestion("Please contact support if the problem persists")
            .build();
        
        ApiResponse<Void> response = ApiResponse.<Void>builder()
            .success(false)
            .message("An unexpected error occurred")
            .error(errorDetails)
            .build();
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Maps DatanymizeException severity to HTTP status.
     */
    private HttpStatus mapSeverityToHttpStatus(DatanymizeException.Severity severity) {
        return switch (severity) {
            case INFO -> HttpStatus.OK;
            case WARNING -> HttpStatus.BAD_REQUEST;
            case ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            case CRITICAL -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    }
}
