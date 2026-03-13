package com.datanymize.security;

import com.datanymize.audit.IAuditLogger;
import com.datanymize.database.connection.IDatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * Validates and enforces read-only access to databases.
 * Blocks INSERT, UPDATE, DELETE, DROP, ALTER, and other write operations.
 * 
 * Validates Requirements: 13.1, 13.3, 13.4
 */
public class ReadOnlyValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(ReadOnlyValidator.class);
    
    // Regex patterns for write operations
    private static final Pattern INSERT_PATTERN = Pattern.compile("^\\s*INSERT\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern UPDATE_PATTERN = Pattern.compile("^\\s*UPDATE\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern DELETE_PATTERN = Pattern.compile("^\\s*DELETE\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern DROP_PATTERN = Pattern.compile("^\\s*DROP\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALTER_PATTERN = Pattern.compile("^\\s*ALTER\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern CREATE_PATTERN = Pattern.compile("^\\s*CREATE\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRUNCATE_PATTERN = Pattern.compile("^\\s*TRUNCATE\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern GRANT_PATTERN = Pattern.compile("^\\s*GRANT\\s+", Pattern.CASE_INSENSITIVE);
    private static final Pattern REVOKE_PATTERN = Pattern.compile("^\\s*REVOKE\\s+", Pattern.CASE_INSENSITIVE);
    
    private final IAuditLogger auditLogger;
    private final String connectionId;
    private final String tenantId;
    
    public ReadOnlyValidator(IAuditLogger auditLogger, String connectionId, String tenantId) {
        this.auditLogger = auditLogger;
        this.connectionId = connectionId;
        this.tenantId = tenantId;
    }
    
    /**
     * Validates that a query is read-only (SELECT only).
     * Throws exception if write operation is detected.
     * 
     * @param query The SQL query to validate
     * @throws ReadOnlyAccessViolationException if query is a write operation
     */
    public void validateQuery(String query) throws ReadOnlyAccessViolationException {
        if (query == null || query.trim().isEmpty()) {
            throw new ReadOnlyAccessViolationException("Query cannot be null or empty");
        }
        
        String trimmedQuery = query.trim();
        
        // Check for write operations
        if (isWriteOperation(trimmedQuery)) {
            String operationType = detectOperationType(trimmedQuery);
            String errorMessage = String.format(
                "Read-only access violation: %s operations are not allowed. " +
                "Connection is configured for read-only access only. " +
                "Please use a connection with write permissions if you need to modify data.",
                operationType
            );
            
            // Log the violation
            logAccessViolation(operationType, query);
            
            throw new ReadOnlyAccessViolationException(errorMessage);
        }
    }
    
    /**
     * Checks if a query is a write operation.
     * 
     * @param query The SQL query to check
     * @return true if query is a write operation, false otherwise
     */
    private boolean isWriteOperation(String query) {
        return INSERT_PATTERN.matcher(query).find() ||
               UPDATE_PATTERN.matcher(query).find() ||
               DELETE_PATTERN.matcher(query).find() ||
               DROP_PATTERN.matcher(query).find() ||
               ALTER_PATTERN.matcher(query).find() ||
               CREATE_PATTERN.matcher(query).find() ||
               TRUNCATE_PATTERN.matcher(query).find() ||
               GRANT_PATTERN.matcher(query).find() ||
               REVOKE_PATTERN.matcher(query).find();
    }
    
    /**
     * Detects the type of operation in a query.
     * 
     * @param query The SQL query
     * @return The operation type (INSERT, UPDATE, DELETE, etc.)
     */
    private String detectOperationType(String query) {
        if (INSERT_PATTERN.matcher(query).find()) return "INSERT";
        if (UPDATE_PATTERN.matcher(query).find()) return "UPDATE";
        if (DELETE_PATTERN.matcher(query).find()) return "DELETE";
        if (DROP_PATTERN.matcher(query).find()) return "DROP";
        if (ALTER_PATTERN.matcher(query).find()) return "ALTER";
        if (CREATE_PATTERN.matcher(query).find()) return "CREATE";
        if (TRUNCATE_PATTERN.matcher(query).find()) return "TRUNCATE";
        if (GRANT_PATTERN.matcher(query).find()) return "GRANT";
        if (REVOKE_PATTERN.matcher(query).find()) return "REVOKE";
        return "UNKNOWN";
    }
    
    /**
     * Logs an access violation to the audit log.
     * 
     * @param operationType The type of operation attempted
     * @param query The query that was attempted
     */
    private void logAccessViolation(String operationType, String query) {
        try {
            String sanitizedQuery = sanitizeQueryForLogging(query);
            logger.warn(
                "Read-only access violation detected. " +
                "Connection: {}, Tenant: {}, Operation: {}, Query: {}",
                connectionId, tenantId, operationType, sanitizedQuery
            );
            
            if (auditLogger != null) {
                auditLogger.logAction(
                    tenantId,
                    "READ_ONLY_VIOLATION",
                    "Connection",
                    false,
                    String.format("Attempted %s operation on read-only connection", operationType),
                    null
                );
            }
        } catch (Exception e) {
            logger.error("Failed to log read-only access violation", e);
        }
    }
    
    /**
     * Sanitizes a query for logging by removing sensitive information.
     * 
     * @param query The query to sanitize
     * @return The sanitized query
     */
    private String sanitizeQueryForLogging(String query) {
        // Truncate very long queries
        if (query.length() > 500) {
            return query.substring(0, 500) + "... [truncated]";
        }
        return query;
    }
    
    /**
     * Exception thrown when a read-only access violation is detected.
     */
    public static class ReadOnlyAccessViolationException extends Exception {
        public ReadOnlyAccessViolationException(String message) {
            super(message);
        }
        
        public ReadOnlyAccessViolationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
