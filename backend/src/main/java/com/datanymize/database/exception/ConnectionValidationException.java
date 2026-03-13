package com.datanymize.database.exception;

/**
 * Exception thrown when connection validation fails.
 * 
 * Provides meaningful error messages and suggestions for resolution.
 */
public class ConnectionValidationException extends Exception {
    
    private final String errorCode;
    private final String suggestion;
    private final long durationMs;
    
    public ConnectionValidationException(String message, String errorCode, String suggestion) {
        super(message);
        this.errorCode = errorCode;
        this.suggestion = suggestion;
        this.durationMs = 0;
    }
    
    public ConnectionValidationException(String message, String errorCode, String suggestion, long durationMs) {
        super(message);
        this.errorCode = errorCode;
        this.suggestion = suggestion;
        this.durationMs = durationMs;
    }
    
    public ConnectionValidationException(String message, String errorCode, String suggestion, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.suggestion = suggestion;
        this.durationMs = 0;
    }
    
    public ConnectionValidationException(String message, String errorCode, String suggestion, long durationMs, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.suggestion = suggestion;
        this.durationMs = durationMs;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getSuggestion() {
        return suggestion;
    }
    
    public long getDurationMs() {
        return durationMs;
    }
    
    @Override
    public String toString() {
        return String.format("ConnectionValidationException [%s]: %s\nSuggestion: %s", 
            errorCode, getMessage(), suggestion);
    }
}
