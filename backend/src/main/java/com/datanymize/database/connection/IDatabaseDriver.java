package com.datanymize.database.connection;

import com.datanymize.database.model.ConnectionConfig;
import com.datanymize.database.model.DatabaseMetadata;

/**
 * Interface for database drivers.
 * Provides database-specific connection creation and operations.
 * 
 * Validates Requirements: 1.6, 15.1, 15.2, 15.3
 */
public interface IDatabaseDriver {
    
    /**
     * Create a connection with the given configuration.
     */
    IDatabaseConnection createConnection(ConnectionConfig config) throws Exception;
    
    /**
     * Get the database type this driver supports.
     */
    String getDatabaseType();
    
    /**
     * Validate that the connection has read-only access.
     */
    boolean validateReadOnlyAccess(IDatabaseConnection conn) throws Exception;
    
    /**
     * Extract database schema metadata.
     */
    DatabaseMetadata extractSchema(IDatabaseConnection conn) throws Exception;
    
    /**
     * Create a schema in the target database.
     */
    void createSchema(IDatabaseConnection conn, DatabaseMetadata schema) throws Exception;
    
    /**
     * Drop a schema from the database.
     */
    void dropSchema(IDatabaseConnection conn, String schemaName) throws Exception;
}
