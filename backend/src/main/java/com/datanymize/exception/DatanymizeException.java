package com.datanymize.exception;

import com.datanymize.security.CredentialSanitizer;

/**
 * Base exception for Datanymize with severity levels and error suggestions.
 * 
 * Validates Requirements: 19.1, 19.4, 19.5
 */
public class DatanymizeException extends Exception {
    
    /**
     * Severity levels for errors.
     */
    public enum Severity {
        INFO("INFO", "Informational message"),
        WARNING("WARNING", "Warning that may require attention"),
        ERROR("ERROR", "Error that prevents operation"),
        CRITICAL("CRITICAL", "Critical error that may affect system stability");
        
        private final String code;
        private final String description;
        
        Severity(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final Severity severity;
    private final String errorCode;
    private final String suggestion;
    private final String context;
    
    /**
     * Creates a DatanymizeException with default severity (ERROR).
     * 
     * @param message The error message
     */
    public DatanymizeException(String message) {
        this(message, Severity.ERROR, null, null, null);
    }
    
    /**
     * Creates a DatanymizeException with a cause.
     * 
     * @param message The error message
     * @param cause The underlying cause
     */
    public DatanymizeException(String message, Throwable cause) {
        this(message, Severity.ERROR, null, null, null, cause);
    }
    
    /**
     * Creates a DatanymizeException with severity and error code.
     * 
     * @param message The error message
     * @param severity The severity level
     * @param errorCode The error code
     */
    public DatanymizeException(String message, Severity severity, String errorCode) {
        this(message, severity, errorCode, null, null);
    }
    
    /**
     * Creates a DatanymizeException with all details.
     * 
     * @param message The error message
     * @param severity The severity level
     * @param errorCode The error code
     * @param suggestion A suggestion for resolving the error
     * @param context Additional context about the error
     */
    public DatanymizeException(String message, Severity severity, String errorCode, 
                              String suggestion, String context) {
        super(CredentialSanitizer.sanitize(message));
        this.severity = severity != null ? severity : Severity.ERROR;
        this.errorCode = errorCode;
        this.suggestion = suggestion;
        this.context = context;
    }
    
    /**
     * Creates a DatanymizeException with all details and a cause.
     * 
     * @param message The error message
     * @param severity The severity level
     * @param errorCode The error code
     * @param suggestion A suggestion for resolving the error
     * @param context Additional context about the error
     * @param cause The underlying cause
     */
    public DatanymizeException(String message, Severity severity, String errorCode, 
                              String suggestion, String context, Throwable cause) {
        super(CredentialSanitizer.sanitize(message), cause);
        this.severity = severity != null ? severity : Severity.ERROR;
        this.errorCode = errorCode;
        this.suggestion = suggestion;
        this.context = context;
    }
    
    /**
     * Gets the severity level of this exception.
     * 
     * @return The severity level
     */
    public Severity getSeverity() {
        return severity;
    }
    
    /**
     * Gets the error code.
     * 
     * @return The error code, or null if not set
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Gets the suggestion for resolving this error.
     * 
     * @return The suggestion, or null if not set
     */
    public String getSuggestion() {
        return suggestion;
    }
    
    /**
     * Gets the additional context about this error.
     * 
     * @return The context, or null if not set
     */
    public String getContext() {
        return context;
    }
    
    /**
     * Gets a formatted error message with all details.
     * 
     * @return The formatted error message
     */
    public String getFormattedMessage() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("[").append(severity.getCode()).append("]");
        
        if (errorCode != null) {
            sb.append(" [").append(errorCode).append("]");
        }
        
        sb.append(" ").append(getMessage());
        
        if (suggestion != null) {
            sb.append("\nSuggestion: ").append(suggestion);
        }
        
        if (context != null) {
            sb.append("\nContext: ").append(context);
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return getFormattedMessage();
    }
}
