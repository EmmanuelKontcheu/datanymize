package com.datanymize.security;

import java.util.regex.Pattern;

/**
 * Sanitizes credentials from logs, error messages, and exports.
 * Removes passwords and other sensitive information.
 * 
 * Validates Requirements: 14.2, 14.3
 */
public class CredentialSanitizer {
    
    // Patterns for common credential formats
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "(?i)(password|passwd|pwd|secret|token|api[_-]?key|auth[_-]?token)\\s*[:=]\\s*[^\\s,}\\]]+",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern CONNECTION_STRING_PATTERN = Pattern.compile(
        "(?i)(jdbc|mongodb|mysql|postgresql)://[^:]+:([^@]+)@",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile(
        "(?i)(bearer|authorization)\\s+[^\\s]+",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern BASIC_AUTH_PATTERN = Pattern.compile(
        "(?i)(basic)\\s+[^\\s]+",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
        "\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"
    );
    
    private static final Pattern SSN_PATTERN = Pattern.compile(
        "\\b\\d{3}-\\d{2}-\\d{4}\\b"
    );
    
    private static final String MASKED_VALUE = "***REDACTED***";
    
    /**
     * Sanitizes a string by removing sensitive information.
     * 
     * @param input The input string to sanitize
     * @return The sanitized string with sensitive data masked
     */
    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        String sanitized = input;
        
        // Mask passwords
        sanitized = PASSWORD_PATTERN.matcher(sanitized)
            .replaceAll("$1=" + MASKED_VALUE);
        
        // Mask connection strings with passwords
        sanitized = CONNECTION_STRING_PATTERN.matcher(sanitized)
            .replaceAll("$1://$2:" + MASKED_VALUE + "@");
        
        // Mask bearer tokens
        sanitized = BEARER_TOKEN_PATTERN.matcher(sanitized)
            .replaceAll("$1 " + MASKED_VALUE);
        
        // Mask basic auth
        sanitized = BASIC_AUTH_PATTERN.matcher(sanitized)
            .replaceAll("$1 " + MASKED_VALUE);
        
        // Mask credit cards
        sanitized = CREDIT_CARD_PATTERN.matcher(sanitized)
            .replaceAll("****-****-****-****");
        
        // Mask SSNs
        sanitized = SSN_PATTERN.matcher(sanitized)
            .replaceAll("***-**-****");
        
        return sanitized;
    }
    
    /**
     * Sanitizes an exception message by removing sensitive information.
     * 
     * @param exception The exception to sanitize
     * @return The sanitized exception message
     */
    public static String sanitizeException(Exception exception) {
        if (exception == null) {
            return null;
        }
        
        String message = exception.getMessage();
        if (message == null) {
            return exception.getClass().getSimpleName();
        }
        
        return sanitize(message);
    }
    
    /**
     * Sanitizes a stack trace by removing sensitive information.
     * 
     * @param stackTrace The stack trace to sanitize
     * @return The sanitized stack trace
     */
    public static String sanitizeStackTrace(String stackTrace) {
        if (stackTrace == null || stackTrace.isEmpty()) {
            return stackTrace;
        }
        
        return sanitize(stackTrace);
    }
    
    /**
     * Checks if a string contains sensitive information.
     * 
     * @param input The input string to check
     * @return true if sensitive information is detected, false otherwise
     */
    public static boolean containsSensitiveData(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        return PASSWORD_PATTERN.matcher(input).find() ||
               CONNECTION_STRING_PATTERN.matcher(input).find() ||
               BEARER_TOKEN_PATTERN.matcher(input).find() ||
               BASIC_AUTH_PATTERN.matcher(input).find() ||
               CREDIT_CARD_PATTERN.matcher(input).find() ||
               SSN_PATTERN.matcher(input).find();
    }
}
