package com.datanymize.database.connection;

import com.datanymize.database.model.ConnectionConfig;
import com.datanymize.database.model.ConnectionResult;

/**
 * Interface for managing database connections.
 * Handles connection lifecycle, validation, and credential security.
 * 
 * Validates Requirements: 1.6, 15.1, 15.2, 15.3
 */
public interface IConnectionManager {
    
    /**
     * Test a database connection with the given configuration.
     * Validates credentials, connectivity, and read-only access.
     * 
     * @param config The connection configuration to test
     * @return ConnectionResult with success status and error details if applicable
     */
    ConnectionResult testConnection(ConnectionConfig config);
    
    /**
     * Save a connection configuration for later use.
     * Credentials are encrypted before storage.
     * 
     * @param connectionId Unique identifier for the connection
     * @param config The connection configuration to save
     */
    void saveConnection(String connectionId, ConnectionConfig config);
    
    /**
     * Retrieve a saved connection configuration.
     * Credentials are decrypted from storage.
     * 
     * @param connectionId The connection identifier
     * @return The connection configuration
     */
    ConnectionConfig getConnection(String connectionId);
    
    /**
     * Delete a saved connection configuration.
     * 
     * @param connectionId The connection identifier
     */
    void deleteConnection(String connectionId);
    
    /**
     * Get a pooled connection from the connection pool.
     * 
     * @param connectionId The connection identifier
     * @return An active database connection
     */
    IDatabaseConnection getPooledConnection(String connectionId);
    
    /**
     * Release a connection back to the pool.
     * 
     * @param connection The connection to release
     */
    void releaseConnection(IDatabaseConnection connection);
    
    /**
     * Validate that a connection has read-only access.
     * 
     * @param connection The connection to validate
     * @return true if connection is read-only, false otherwise
     */
    boolean validateReadOnlyAccess(IDatabaseConnection connection);
}
