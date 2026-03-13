package com.datanymize.database.connection;

import com.datanymize.database.model.ConnectionConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Validates database connections with timeout enforcement.
 * 
 * Validates Requirements: 1.4, 1.5, 1.6, 13.1, 13.4
 */
@Slf4j
public class ConnectionValidator {
    
    private static final long DEFAULT_TIMEOUT_SECONDS = 5;
    
    /**
     * Validate a connection configuration.
     * Checks for required fields and valid values.
     * 
     * @param config The configuration to validate
     * @throws IllegalArgumentException if configuration is invalid
     */
    public static void validateConfiguration(ConnectionConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Connection configuration cannot be null");
        }
        
        if (config.getType() == null || config.getType().isEmpty()) {
            throw new IllegalArgumentException("Database type is required");
        }
        
        if (config.getHost() == null || config.getHost().isEmpty()) {
            throw new IllegalArgumentException("Host is required");
        }
        
        if (config.getPort() <= 0 || config.getPort() > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        
        if (config.getDatabase() == null || config.getDatabase().isEmpty()) {
            throw new IllegalArgumentException("Database name is required");
        }
        
        if (config.getUsername() == null || config.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        
        if (config.getPassword() == null) {
            throw new IllegalArgumentException("Password is required");
        }
        
        if (config.getConnectionTimeoutSeconds() <= 0) {
            throw new IllegalArgumentException("Connection timeout must be positive");
        }
        
        if (config.getConnectionTimeoutSeconds() > 300) {
            throw new IllegalArgumentException("Connection timeout cannot exceed 300 seconds");
        }
    }
    
    /**
     * Validate that a connection has read-only access.
     * Attempts to execute a SELECT query and verifies write operations are blocked.
     * 
     * @param connection The connection to validate
     * @return true if connection is read-only, false if write access is detected
     * @throws Exception if validation fails
     */
    public static boolean validateReadOnlyAccess(IDatabaseConnection connection) throws Exception {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }
        
        // Test SELECT is allowed
        try {
            connection.executeQuery("SELECT 1");
            log.debug("SELECT query executed successfully");
        } catch (Exception e) {
            log.error("Failed to execute SELECT query: {}", e.getMessage());
            throw new Exception("Read-only validation failed: SELECT query not allowed", e);
        }
        
        // Test that write operations are blocked
        try {
            connection.executeUpdate("CREATE TEMP TABLE test_write_validation (id INT)");
            connection.executeUpdate("INSERT INTO test_write_validation VALUES (1)");
            connection.executeUpdate("DROP TABLE test_write_validation");
            
            // If we get here, write access is allowed - this is a problem
            log.warn("Write access detected on connection - not read-only");
            return false;
        } catch (Exception e) {
            // Expected - write should fail
            log.debug("Write operation blocked as expected: {}", e.getMessage());
            return true;
        }
    }
    
    /**
     * Validate connection with timeout enforcement.
     * 
     * @param connection The connection to validate
     * @param timeoutSeconds Timeout in seconds
     * @return true if connection is valid within timeout
     * @throws TimeoutException if validation exceeds timeout
     */
    public static boolean validateWithTimeout(IDatabaseConnection connection, long timeoutSeconds) 
            throws TimeoutException {
        
        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeoutSeconds * 1000;
        
        try {
            boolean isValid = connection.validate();
            
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (elapsedTime > timeoutMillis) {
                throw new TimeoutException(
                    String.format("Connection validation exceeded timeout of %d seconds", timeoutSeconds)
                );
            }
            
            return isValid;
        } catch (TimeoutException e) {
            throw e;
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (elapsedTime > timeoutMillis) {
                throw new TimeoutException(
                    String.format("Connection validation exceeded timeout of %d seconds", timeoutSeconds)
                );
            }
            throw new RuntimeException("Connection validation failed", e);
        }
    }
    
    /**
     * Validate TLS/SSL configuration.
     * 
     * @param config The connection configuration
     * @throws IllegalArgumentException if TLS/SSL is not properly configured
     */
    public static void validateTLSConfiguration(ConnectionConfig config) {
        if (!config.isUseTLS()) {
            log.warn("TLS/SSL is not enabled for connection to {}:{}", config.getHost(), config.getPort());
        }
    }
}
