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
            ConnectionValidator.validateConfiguration(config);
            
            // Validate TLS/SSL configuration
            ConnectionValidator.validateTLSConfiguration(config);
            
            // Get the appropriate driver
            IDatabaseDriver driver = getDriver(config.getType());
            if (driver == null) {
                return ConnectionResult.builder()
                    .success(false)
                    .errorMessage("Unsupported database type: " + config.getType())
                    .build();
            }
            
            // Create connection with timeout
            IDatabaseConnection connection = null;
            try {
                connection = driver.createConnection(config);
                
                // Validate connection with timeout
                long timeoutSeconds = config.getConnectionTimeoutSeconds();
                boolean isValid = ConnectionValidator.validateWithTimeout(connection, timeoutSeconds);
                
                if (!isValid) {
                    return ConnectionResult.builder()
                        .success(false)
                        .errorMessage("Connection validation failed")
                        .build();
                }
                
                // Validate read-only access
                boolean isReadOnly = driver.validateReadOnlyAccess(connection);
                if (!isReadOnly) {
                    return ConnectionResult.builder()
                        .success(false)
                        .errorMessage("Connection does not have read-only access. " +
                                    "Datanymize requires read-only access to source databases.")
                        .build();
                }
                
                // Update last tested timestamp
                config.setLastTestedAt(LocalDateTime.now());
                
                log.info("Connection test successful for {}:{}/{}", 
                    config.getHost(), config.getPort(), config.getDatabase());
                
                return ConnectionResult.builder()
                    .success(true)
                    .message("Connection successful")
                    .build();
                    
            } catch (TimeoutException e) {
                log.warn("Connection timeout: {}", e.getMessage());
                return ConnectionResult.builder()
                    .success(false)
                    .errorMessage("Connection timeout: " + e.getMessage())
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
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid connection configuration: {}", e.getMessage());
            return ConnectionResult.builder()
                .success(false)
                .errorMessage("Invalid configuration: " + e.getMessage())
                .build();
        } catch (Exception e) {
            log.error("Connection test failed", e);
            return ConnectionResult.builder()
                .success(false)
                .errorMessage("Connection failed: " + e.getMessage())
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
