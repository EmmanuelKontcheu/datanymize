package com.datanymize.database.connection;

import com.datanymize.database.model.ConnectionConfig;
import com.datanymize.database.model.ConnectionResult;
import com.datanymize.security.CredentialEncryption;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Manages database connections with pooling, validation, and credential security.
 * 
 * Validates Requirements: 1.6, 15.1, 15.2, 15.3
 */
@Slf4j
public class ConnectionManager implements IConnectionManager {
    
    private final Map<String, IDatabaseDriver> drivers = new HashMap<>();
    private final Map<String, IDatabaseConnection> connectionPool = new HashMap<>();
    private final Map<String, ConnectionConfig> savedConfigs = new HashMap<>();
    private final CredentialEncryption credentialEncryption;
    
    public ConnectionManager() {
        this.credentialEncryption = new CredentialEncryption();
        initializeDrivers();
    }
    
    public ConnectionManager(CredentialEncryption credentialEncryption) {
        this.credentialEncryption = credentialEncryption;
        initializeDrivers();
    }
    
    /**
     * Initialize database drivers for supported database types.
     */
    private void initializeDrivers() {
        drivers.put("postgresql", new PostgreSQLDriver());
        // MySQL and MongoDB drivers will be added in future tasks
    }
    
    @Override
    public ConnectionResult testConnection(ConnectionConfig config) {
        try {
            // Validate configuration
            com.datanymize.database.model.ValidationResult configValidation = 
                ConnectionValidator.validateConfiguration(config);
            
            if (!configValidation.isValid()) {
                return ConnectionResult.builder()
                    .success(false)
                    .errorMessage(configValidation.getErrorMessage())
                    .errorCode(configValidation.getErrorCode())
                    .errorDetails(configValidation.getSuggestion())
                    .durationMs(configValidation.getDurationMs())
                    .build();
            }
            
            // Validate TLS/SSL configuration
            com.datanymize.database.model.ValidationResult tlsValidation = 
                ConnectionValidator.validateTLSConfiguration(config);
            
            if (!tlsValidation.isValid()) {
                log.warn("TLS/SSL warning: {}", tlsValidation.getErrorMessage());
                // TLS warning is not fatal, continue
            }
            
            // Get the appropriate driver
            IDatabaseDriver driver = getDriver(config.getType());
            if (driver == null) {
                return ConnectionResult.builder()
                    .success(false)
                    .errorMessage("Unsupported database type: " + config.getType())
                    .errorCode("UNSUPPORTED_DB_TYPE")
                    .build();
            }
            
            // Create connection with timeout and retry logic
            IDatabaseConnection connection = null;
            try {
                connection = driver.createConnection(config);
                
                // Validate connection with timeout and retry
                long timeoutSeconds = config.getConnectionTimeoutSeconds();
                com.datanymize.database.model.ValidationResult timeoutValidation = 
                    ConnectionValidator.validateWithTimeout(connection, timeoutSeconds);
                
                if (!timeoutValidation.isValid()) {
                    return ConnectionResult.builder()
                        .success(false)
                        .errorMessage(timeoutValidation.getErrorMessage())
                        .errorCode(timeoutValidation.getErrorCode())
                        .errorDetails(timeoutValidation.getSuggestion())
                        .durationMs(timeoutValidation.getDurationMs())
                        .build();
                }
                
                // Validate read-only access
                com.datanymize.database.model.ValidationResult readOnlyValidation = 
                    ConnectionValidator.validateReadOnlyAccess(connection);
                
                if (!readOnlyValidation.isValid()) {
                    return ConnectionResult.builder()
                        .success(false)
                        .errorMessage(readOnlyValidation.getErrorMessage())
                        .errorCode(readOnlyValidation.getErrorCode())
                        .errorDetails(readOnlyValidation.getSuggestion())
                        .durationMs(readOnlyValidation.getDurationMs())
                        .build();
                }
                
                // Update last tested timestamp
                config.setLastTestedAt(LocalDateTime.now());
                
                log.info("Connection test successful for {}:{}/{}", 
                    config.getHost(), config.getPort(), config.getDatabase());
                
                return ConnectionResult.builder()
                    .success(true)
                    .message("Connection successful")
                    .durationMs(timeoutValidation.getDurationMs())
                    .build();
                    
            } catch (Exception e) {
                log.error("Connection test failed", e);
                return ConnectionResult.builder()
                    .success(false)
                    .errorMessage("Connection failed: " + e.getMessage())
                    .errorCode("CONNECTION_FAILED")
                    .build();
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (Exception e) {
                        log.warn("Failed to close test connection: {}", e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Connection test failed", e);
            return ConnectionResult.builder()
                .success(false)
                .errorMessage("Connection failed: " + e.getMessage())
                .errorCode("CONNECTION_ERROR")
                .build();
        }
    }
    
    @Override
    public void saveConnection(String connectionId, ConnectionConfig config) {
        try {
            ConnectionValidator.validateConfiguration(config);
            
            // Encrypt password before storage
            String encryptedPassword = credentialEncryption.encrypt(config.getPassword());
            config.setPassword(encryptedPassword);
            
            // Store configuration
            savedConfigs.put(connectionId, config);
            
            log.info("Connection configuration saved: {}", connectionId);
        } catch (Exception e) {
            log.error("Failed to save connection configuration", e);
            throw new RuntimeException("Failed to save connection", e);
        }
    }
    
    @Override
    public ConnectionConfig getConnection(String connectionId) {
        ConnectionConfig config = savedConfigs.get(connectionId);
        if (config == null) {
            throw new IllegalArgumentException("Connection not found: " + connectionId);
        }
        
        // Decrypt password
        try {
            String decryptedPassword = credentialEncryption.decrypt(config.getPassword());
            config.setPassword(decryptedPassword);
        } catch (Exception e) {
            log.error("Failed to decrypt password for connection: {}", connectionId, e);
            throw new RuntimeException("Failed to retrieve connection credentials", e);
        }
        
        return config;
    }
    
    @Override
    public void deleteConnection(String connectionId) {
        // Close any pooled connection
        IDatabaseConnection pooledConn = connectionPool.remove(connectionId);
        if (pooledConn != null) {
            try {
                pooledConn.close();
            } catch (Exception e) {
                log.warn("Failed to close pooled connection: {}", e.getMessage());
            }
        }
        
        // Remove saved configuration
        savedConfigs.remove(connectionId);
        
        log.info("Connection deleted: {}", connectionId);
    }
    
    @Override
    public IDatabaseConnection getPooledConnection(String connectionId) {
        try {
            // Check if connection is already pooled
            IDatabaseConnection pooledConn = connectionPool.get(connectionId);
            if (pooledConn != null && pooledConn.isConnected()) {
                return pooledConn;
            }
            
            // Get saved configuration
            ConnectionConfig config = getConnection(connectionId);
            
            // Create new connection
            IDatabaseDriver driver = getDriver(config.getType());
            if (driver == null) {
                throw new IllegalArgumentException("Unsupported database type: " + config.getType());
            }
            
            IDatabaseConnection connection = driver.createConnection(config);
            
            // Store in pool
            connectionPool.put(connectionId, connection);
            
            return connection;
        } catch (Exception e) {
            log.error("Failed to get pooled connection: {}", connectionId, e);
            throw new RuntimeException("Failed to get connection", e);
        }
    }
    
    @Override
    public void releaseConnection(IDatabaseConnection connection) {
        // In a real implementation, this would return the connection to the pool
        // For now, we just log it
        log.debug("Connection released back to pool");
    }
    
    @Override
    public boolean validateReadOnlyAccess(IDatabaseConnection connection) {
        try {
            return ConnectionValidator.validateReadOnlyAccess(connection);
        } catch (Exception e) {
            log.error("Failed to validate read-only access", e);
            return false;
        }
    }
    
    /**
     * Get a driver for the specified database type.
     * 
     * @param databaseType The database type (postgresql, mysql, mongodb)
     * @return The driver, or null if not found
     */
    private IDatabaseDriver getDriver(String databaseType) {
        return drivers.get(databaseType.toLowerCase());
    }
    
    /**
     * Close all connections and cleanup resources.
     */
    public void close() {
        // Close all pooled connections
        for (IDatabaseConnection conn : connectionPool.values()) {
            try {
                conn.close();
            } catch (Exception e) {
                log.warn("Failed to close connection: {}", e.getMessage());
            }
        }
        connectionPool.clear();
        
        log.info("Connection manager closed");
    }
}
