package com.datanymize.exception;

import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for error message quality.
 * 
 * **Validates: Requirements 19.1, 19.4, 19.5**
 */
@PropertyDefaults(tries = 100)
public class ErrorMessageQualityProperties {
    
    /**
     * Property 28: Error Message Quality
     * 
     * Verifies that error messages are meaningful and contain helpful information.
     */
    @Property
    void errorMessageQualityProperty(
            @ForAll @StringLength(min = 10, max = 200) String errorMessage,
            @ForAll("severities") DatanymizeException.Severity severity,
            @ForAll @StringLength(min = 1, max = 50) String errorCode) {
        
        DatanymizeException exception = new DatanymizeException(
            errorMessage, severity, errorCode
        );
        
        String formattedMessage = exception.getFormattedMessage();
        
        // Message should contain severity
        assertTrue(formattedMessage.contains(severity.getCode()),
            "Error message should contain severity level");
        
        // Message should contain error code
        assertTrue(formattedMessage.contains(errorCode),
            "Error message should contain error code");
        
        // Message should contain the error message
        assertTrue(formattedMessage.contains(errorMessage),
            "Error message should contain the error description");
        
        // Message should be reasonably long (not truncated)
        assertTrue(formattedMessage.length() > 20,
            "Error message should be descriptive");
    }
    
    /**
     * Property: Error messages include suggestions
     * 
     * Verifies that error messages include helpful suggestions.
     */
    @Property
    void errorMessageSuggestionsProperty(
            @ForAll @StringLength(min = 10, max = 200) String errorMessage,
            @ForAll @StringLength(min = 10, max = 200) String suggestion) {
        
        DatanymizeException exception = new DatanymizeException(
            errorMessage,
            DatanymizeException.Severity.ERROR,
            "TEST_ERROR",
            suggestion,
            null
        );
        
        String formattedMessage = exception.getFormattedMessage();
        
        // Message should contain suggestion
        assertTrue(formattedMessage.contains("Suggestion:"),
            "Error message should indicate suggestion");
        assertTrue(formattedMessage.contains(suggestion),
            "Error message should contain the suggestion");
    }
    
    /**
     * Property: Error messages include context
     * 
     * Verifies that error messages include contextual information.
     */
    @Property
    void errorMessageContextProperty(
            @ForAll @StringLength(min = 10, max = 200) String errorMessage,
            @ForAll @StringLength(min = 10, max = 200) String context) {
        
        DatanymizeException exception = new DatanymizeException(
            errorMessage,
            DatanymizeException.Severity.ERROR,
            "TEST_ERROR",
            null,
            context
        );
        
        String formattedMessage = exception.getFormattedMessage();
        
        // Message should contain context
        assertTrue(formattedMessage.contains("Context:"),
            "Error message should indicate context");
        assertTrue(formattedMessage.contains(context),
            "Error message should contain the context");
    }
    
    /**
     * Property: Error messages are sanitized
     * 
     * Verifies that sensitive information is removed from error messages.
     */
    @Property
    void errorMessageSanitizationProperty(
            @ForAll @StringLength(min = 8, max = 128) String password) {
        
        String errorWithPassword = "Failed to connect with password=" + password;
        
        DatanymizeException exception = new DatanymizeException(errorWithPassword);
        String message = exception.getMessage();
        
        // Password should not appear in the message
        assertFalse(message.contains(password),
            "Error message should not contain password");
        
        // Message should indicate sanitization
        assertTrue(message.contains("***") || message.contains("REDACTED") || !message.contains("password="),
            "Error message should sanitize sensitive data");
    }
    
    /**
     * Property: Different severity levels are distinguishable
     * 
     * Verifies that different severity levels produce different messages.
     */
    @Property
    void severityLevelDistinctionProperty(
            @ForAll @StringLength(min = 10, max = 200) String errorMessage) {
        
        DatanymizeException infoException = new DatanymizeException(
            errorMessage, DatanymizeException.Severity.INFO, "INFO_CODE"
        );
        
        DatanymizeException errorException = new DatanymizeException(
            errorMessage, DatanymizeException.Severity.ERROR, "ERROR_CODE"
        );
        
        DatanymizeException criticalException = new DatanymizeException(
            errorMessage, DatanymizeException.Severity.CRITICAL, "CRITICAL_CODE"
        );
        
        String infoMsg = infoException.getFormattedMessage();
        String errorMsg = errorException.getFormattedMessage();
        String criticalMsg = criticalException.getFormattedMessage();
        
        // Messages should be different
        assertNotEquals(infoMsg, errorMsg,
            "Different severity levels should produce different messages");
        assertNotEquals(errorMsg, criticalMsg,
            "Different severity levels should produce different messages");
        
        // Messages should contain their respective severity
        assertTrue(infoMsg.contains("INFO"));
        assertTrue(errorMsg.contains("ERROR"));
        assertTrue(criticalMsg.contains("CRITICAL"));
    }
    
    /**
     * Property: Error codes are preserved
     * 
     * Verifies that error codes are correctly preserved and retrievable.
     */
    @Property
    void errorCodePreservationProperty(
            @ForAll @StringLength(min = 10, max = 200) String errorMessage,
            @ForAll @StringLength(min = 1, max = 50) String errorCode) {
        
        DatanymizeException exception = new DatanymizeException(
            errorMessage, DatanymizeException.Severity.ERROR, errorCode
        );
        
        assertEquals(errorCode, exception.getErrorCode(),
            "Error code should be preserved");
        
        assertTrue(exception.getFormattedMessage().contains(errorCode),
            "Error code should appear in formatted message");
    }
    
    /**
     * Property: Exceptions with causes preserve cause information
     * 
     * Verifies that exception causes are preserved.
     */
    @Property
    void exceptionCausePreservationProperty(
            @ForAll @StringLength(min = 10, max = 200) String errorMessage) {
        
        Exception cause = new RuntimeException("Root cause");
        DatanymizeException exception = new DatanymizeException(
            errorMessage, cause
        );
        
        assertNotNull(exception.getCause(),
            "Exception cause should be preserved");
        assertEquals("Root cause", exception.getCause().getMessage(),
            "Cause message should be preserved");
    }
    
    // Providers for test data
    
    @Provide
    Arbitrary<DatanymizeException.Severity> severities() {
        return Arbitraries.of(
            DatanymizeException.Severity.INFO,
            DatanymizeException.Severity.WARNING,
            DatanymizeException.Severity.ERROR,
            DatanymizeException.Severity.CRITICAL
        );
    }
}
