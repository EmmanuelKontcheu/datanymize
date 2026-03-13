package com.datanymize.database.connection;

import com.datanymize.database.exception.ConnectionValidationException;
import com.datanymize.database.model.ConnectionConfig;
import com.datanymize.database.model.ValidationResult;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

/**
 * Validates database connections with timeout enforcement, read-only access validation,
 * and exponential backoff retry logic.
 * 
 * Validates Requirements: 1.4, 1.5, 1.6, 13.1, 13.4
 */
@Slf4j
public class ConnectionValidator {
    
    private static final long DEFAULT_TIMEOUT_SECONDS = 5;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 100;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final long MAX_BACKOFF_MS = 5000;
    
    /**
     * Validate a connection configuration.
     * Checks for required fields and valid values.
     * 
     * @param config The configuration to validate
     * @return ValidationResult with validation outcome
     */
    public static ValidationResult validateConfiguration(ConnectionConfig config) {
        long startTime = System.currentTimeMillis();
        
        try {
            if (config == null) {
                return ValidationResult.failure(
                    ValidationResult.ValidationType.CONFIGURATION,
                    "CONFIG_NULL",
                    "Connection configuration cannot be null",
                    "Ensure a valid connection configuration is provided"
                );
            }
            
            if (config.getType() == null || config.getType().isEmpty()) {
                return ValidationResult.failure(
                    ValidationResult.ValidationType.CONFIGURATION,
                    "CONFIG_MISSING_TYPE",
                    "Database type is required",
                    "Specify a database type: postgresql, mysql, or mongodb"
                );
            }
            
            if (config.getHost() == null || config.getHost().isEmpty()) {
                return ValidationResult.failure(
                    ValidationResult.ValidationType.CONFIGURATION,
                    "CONFIG_MISSING_HOST",
                    "Host is required",
                    "Provide the database server hostname or IP address"
                );
            }
            
            if (config.getPort() <= 0 || config.getPort() > 65535) {
                return ValidationResult.failure(
                    ValidationResult.ValidationType.CONFIGURATION,
                    "CONFIG_INVALID_PORT",
                    "Port must be between 1 and 65535, got: " + config.getPort(),
                    "Use a valid port number (e.g., 5432 for PostgreSQL, 3306 for MySQL, 27017 for MongoDB)"
                );
            }
            
            if (config.getDatabase() == null || config.getDatabase().isEmpty()) {
                return ValidationResult.failure(
                    ValidationResult.ValidationType.CONFIGURATION,
                    "CONFIG_MISSING_DATABASE",
                    "Database name is required",
                    "Specify the database name to connect to"
                );
            }
            
            if (config.getUsername() == null || config.getUsername().isEmpty()) {
                return ValidationResult.failure(
                    ValidationResult.ValidationType.CONFIGURATION,
                    "CONFIG_MISSING_USERNAME",
                    "Username is required",
                    "Provide the database user credentials"
                );
            }
            
            if (config.getPassword() == null) {
                return ValidationResult.failure(
                    ValidationResult.ValidationType.CONFIGURATION,
                    "CONFIG_MISSING_PASSWORD",
                    "Password is required",
                    "Provide the database user password"
                );
            }
            
            if (config.getConnectionTimeoutSeconds() <= 0) {
                return ValidationResult.failure(
                    ValidationResult.ValidationType.CONFIGURATION,
                    "CONFIG_INVALID_TIMEOUT",
                    "Connection timeout must be positive, got: " + config.getConnectionTimeoutSeconds(),
                    "Set a positive timeout value in seconds (default: 5 seconds)"
                );
            }
            
            if (config.getConnectionTimeoutSeconds() > 300) {
                return ValidationResult.failure(
                    ValidationResult.ValidationType.CONFIGURATION,
                    "CONFIG_TIMEOUT_TOO_LARGE",
                    "Connection timeout cannot exceed 300 seconds, got: " + config.getConnectionTimeoutSeconds(),
                    "Use a timeout value between 1 and 300 seconds"
                );
            }
            
            long duration = System.currentTimeMillis() - startTime;
            return ValidationResult.success(ValidationResult.ValidationType.CONFIGURATION, duration);
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Configuration validation error", e);
            return ValidationResult.failure(
                ValidationResult.ValidationType.CONFIGURATION,
                "CONFIG_VALIDATION_ERROR",
                "Configuration validation failed: " + e.getMessage(),
                "Review the connection configuration and try again",
                duration
            );
        }
    }
    
    /**
     * Validate that a connection has read-only access.
     * Attempts to execute a SELECT query and verifies write operations are blocked.
     * 
     * @param connection The connection to validate
     * @return ValidationResult with read-only validation outcome
     */
    public static ValidationResult validateReadOnlyAccess(IDatabaseConnection connection) {
        long startTime = System.currentTimeMillis();
        
        try {
            if (connection == null) {
                return ValidationResult.failure(
                    ValidationResult.ValidationType.READ_ONLY,
                    "READONLY_NULL_CONNECTION",
                    "Connection cannot be null",
                    "Ensure a valid database connection is established"
                );
            }
            
            // Test SELECT is allowed
            try {
                connection.executeQuery("SELECT 1");
                log.debug("SELECT query executed successfully");
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("Failed to execute SELECT query: {}", e.getMessage());
                return ValidationResult.failure(
                    ValidationResult.ValidationType.READ_ONLY,
                    "READONLY_SELECT_FAILED",
                    "SELECT query not allowed: " + e.getMessage(),
                    "Verify that the database user has SELECT permissions",
                    duration
                );
            }
            
            // Test that write operations are blocked
            boolean writeBlocked = false;
            try {
                connection.executeUpdate("CREATE TEMP TABLE test_write_validation (id INT)");
                connection.executeUpdate("INSERT INTO test_write_validation VALUES (1)");
                connection.executeUpdate("DROP TABLE test_write_validation");
                
                // If we get here, write access is allowed - this is a problem
                log.warn("Write access detected on connection - not read-only");
                writeBlocked = false;
            } catch (Exception e) {
                // Expected - write should fail
                log.debug("Write operation blocked as expected: {}", e.getMessage());
                writeBlocked = true;
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (!writeBlocked) {
                return ValidationResult.failure(
                    ValidationResult.ValidationType.READ_ONLY,
                    "READONLY_WRITE_ALLOWED",
                    "Connection has write access - read-only access required",
                    "Configure the database user with read-only permissions (SELECT only). " +
                    "Remove INSERT, UPDATE, DELETE, DROP, and ALTER permissions."
                );
            }
            
            ValidationResult result = ValidationResult.success(ValidationResult.ValidationType.READ_ONLY, duration);
            result.setReadOnlyVerified(true);
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Read-only validation error", e);
            return ValidationResult.failure(
                ValidationResult.ValidationType.READ_ONLY,
                "READONLY_VALIDATION_ERROR",
                "Read-only validation failed: " + e.getMessage(),
                "Check database connectivity and user permissions",
                duration
            );
        }
    }
    
    /**
     * Validate connection with timeout enforcement.
     * 
     * @param connection The connection to validate
     * @param timeoutSeconds Timeout in seconds
     * @return ValidationResult with timeout validation outcome
     */
    public static ValidationResult validateWithTimeout(IDatabaseConnection connection, long timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeoutSeconds * 1000;
        
        try {
            if (connection == null) {
                return ValidationResult.failure(
                    ValidationResult.ValidationType.TIMEOUT,
                    "TIMEOUT_NULL_CONNECTION",
                    "Connection cannot be null",
                    "Ensure a valid database connection is established"
                );
            }
            
            boolean isValid = connection.validate();
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (elapsedTime > timeoutMillis) {
                return ValidationResult.failure(
                    ValidationResult.ValidationType.TIMEOUT,
                    "TIMEOUT_EXCEEDED",
                    String.format("Connection validation exceeded timeout of %d seconds (took %d ms)", 
                        timeoutSeconds, elapsedTime),
                    String.format("Increase the timeout value or check network connectivity. " +
                        "Current timeout: %d seconds", timeoutSeconds),
                    elapsedTime
                );
            }
            
            if (!isValid) {
                return ValidationResult.failure(
                    ValidationResult.ValidationType.TIMEOUT,
                    "TIMEOUT_VALIDATION_FAILED",
                    "Connection validation failed within timeout",
                    "Verify database connectivity and credentials"
                );
            }
            
            ValidationResult result = ValidationResult.success(ValidationResult.ValidationType.TIMEOUT, elapsedTime);
            return result;
            
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (elapsedTime > timeoutMillis) {
                return ValidationResult.failure(
                    ValidationResult.ValidationType.TIMEOUT,
                    "TIMEOUT_EXCEEDED",
                    String.format("Connection validation exceeded timeout of %d seconds", timeoutSeconds),
                    "Increase the timeout value or check network connectivity",
                    elapsedTime
                );
            }
            log.error("Connection validation error", e);
            return ValidationResult.failure(
                ValidationResult.ValidationType.TIMEOUT,
                "TIMEOUT_VALIDATION_ERROR",
                "Connection validation failed: " + e.getMessage(),
                "Verify database connectivity and credentials",
                elapsedTime
            );
        }
    }
    
    /**
     * Validate TLS/SSL configuration.
     * 
     * @param config The connection configuration
     * @return ValidationResult with TLS validation outcome
     */
    public static ValidationResult validateTLSConfiguration(ConnectionConfig config) {
        long startTime = System.currentTimeMillis();
        
        try {
            if (config == null) {
                return ValidationResult.failure(
                    ValidationResult.ValidationType.TLS,
                    "TLS_NULL_CONFIG",
                    "Connection configuration cannot be null",
                    "Ensure a valid connection configuration is provided"
                );
            }
            
            ValidationResult result = ValidationResult.success(ValidationResult.ValidationType.TLS, 
                System.currentTimeMillis() - startTime);
            result.setTlsEnabled(config.isUseTLS());
            
            if (!config.isUseTLS()) {
                log.warn("TLS/SSL is not enabled for connection to {}:{}", config.getHost(), config.getPort());
                result.setValid(false);
                result.setErrorCode("TLS_NOT_ENABLED");
                result.setErrorMessage("TLS/SSL is not enabled for connection to " + config.getHost() + ":" + config.getPort());
                result.setSuggestion("Enable TLS/SSL for secure database connections. " +
                    "Set useTLS=true in the connection configuration.");
            }
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("TLS validation error", e);
            return ValidationResult.failure(
                ValidationResult.ValidationType.TLS,
                "TLS_VALIDATION_ERROR",
                "TLS/SSL validation failed: " + e.getMessage(),
                "Review the TLS/SSL configuration and try again",
                duration
            );
        }
    }
    
    /**
     * Validate connection with exponential backoff retry logic.
     * 
     * @param connection The connection to validate
     * @param timeoutSeconds Timeout in seconds
     * @return ValidationResult with retry validation outcome
     * @throws ConnectionValidationException if validation fails after all retries
     */
    public static ValidationResult validateWithRetry(IDatabaseConnection connection, long timeoutSeconds) 
            throws ConnectionValidationException {
        
        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;
        Exception lastException = null;
        
        while (attempt < MAX_RETRY_ATTEMPTS) {
            try {
                long startTime = System.currentTimeMillis();
                
                ValidationResult result = validateWithTimeout(connection, timeoutSeconds);
                
                if (result.isValid()) {
                    result.setRetryAttempts(attempt);
                    log.info("Connection validation succeeded on attempt {}", attempt + 1);
                    return result;
                }
                
                lastException = new Exception(result.getErrorMessage());
                
            } catch (Exception e) {
                lastException = e;
                log.warn("Connection validation attempt {} failed: {}", attempt + 1, e.getMessage());
            }
            
            attempt++;
            
            if (attempt < MAX_RETRY_ATTEMPTS) {
                try {
                    log.debug("Retrying connection validation in {} ms", backoffMs);
                    Thread.sleep(backoffMs);
                    backoffMs = Math.min((long)(backoffMs * BACKOFF_MULTIPLIER), MAX_BACKOFF_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ConnectionValidationException(
                        "Connection validation interrupted",
                        "VALIDATION_INTERRUPTED",
                        "The connection validation was interrupted. Please try again.",
                        e
                    );
                }
            }
        }
        
        // All retries exhausted
        String errorMsg = lastException != null ? lastException.getMessage() : "Connection validation failed";
        throw new ConnectionValidationException(
            "Connection validation failed after " + MAX_RETRY_ATTEMPTS + " attempts: " + errorMsg,
            "VALIDATION_FAILED_MAX_RETRIES",
            "Check database connectivity, credentials, and network configuration. " +
            "Verify the database server is running and accessible.",
            lastException
        );
    }
}
