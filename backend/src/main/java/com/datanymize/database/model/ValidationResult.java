package com.datanymize.database.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Result of a connection validation operation.
 * 
 * Contains validation status, error details, and suggestions for resolution.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationResult {
    
    /**
     * Whether validation was successful
     */
    private boolean valid;
    
    /**
     * Validation type (CONFIGURATION, CONNECTIVITY, READ_ONLY, TIMEOUT, TLS)
     */
    private ValidationType validationType;
    
    /**
     * Error message if validation failed
     */
    private String errorMessage;
    
    /**
     * Error code for programmatic handling
     */
    private String errorCode;
    
    /**
     * Suggestion for resolving the error
     */
    private String suggestion;
    
    /**
     * Duration of validation in milliseconds
     */
    private long durationMs;
    
    /**
     * Timestamp of validation
     */
    private LocalDateTime validatedAt;
    
    /**
     * Number of retry attempts made
     */
    private int retryAttempts;
    
    /**
     * Whether read-only access was verified
     */
    private boolean readOnlyVerified;
    
    /**
     * Whether TLS/SSL is enabled
     */
    private boolean tlsEnabled;
    
    /**
     * Additional details about the validation
     */
    private String details;
    
    /**
     * Validation type enumeration
     */
    public enum ValidationType {
        CONFIGURATION,      // Configuration validation
        CONNECTIVITY,       // Connection establishment
        READ_ONLY,         // Read-only access verification
        TIMEOUT,           // Timeout enforcement
        TLS,               // TLS/SSL configuration
        RETRY              // Retry logic validation
    }
    
    /**
     * Create a successful validation result
     */
    public static ValidationResult success(ValidationType type, long durationMs) {
        return ValidationResult.builder()
            .valid(true)
            .validationType(type)
            .durationMs(durationMs)
            .validatedAt(LocalDateTime.now())
            .retryAttempts(0)
            .build();
    }
    
    /**
     * Create a failed validation result
     */
    public static ValidationResult failure(ValidationType type, String errorCode, String errorMessage, String suggestion) {
        return ValidationResult.builder()
            .valid(false)
            .validationType(type)
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .suggestion(suggestion)
            .validatedAt(LocalDateTime.now())
            .retryAttempts(0)
            .build();
    }
    
    /**
     * Create a failed validation result with duration
     */
    public static ValidationResult failure(ValidationType type, String errorCode, String errorMessage, String suggestion, long durationMs) {
        return ValidationResult.builder()
            .valid(false)
            .validationType(type)
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .suggestion(suggestion)
            .durationMs(durationMs)
            .validatedAt(LocalDateTime.now())
            .retryAttempts(0)
            .build();
    }
}
