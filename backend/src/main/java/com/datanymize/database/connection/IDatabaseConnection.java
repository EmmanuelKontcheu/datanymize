package com.datanymize.database.connection;

import com.datanymize.database.model.DatabaseMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Interface for database connections.
 * Provides unified access to different database types.
 * 
 * Validates Requirements: 1.6, 15.1, 15.2, 15.3
 */
public interface IDatabaseConnection {
    
    /**
     * Execute a query and return results.
     */
    ResultSet executeQuery(String query) throws SQLException;
    
    /**
     * Execute an update query (INSERT, UPDATE, DELETE).
     */
    int executeUpdate(String query) throws SQLException;
    
    /**
     * Begin a transaction.
     */
    void beginTransaction() throws SQLException;
    
    /**
     * Commit the current transaction.
     */
    void commit() throws SQLException;
    
    /**
     * Rollback the current transaction.
     */
    void rollback() throws SQLException;
    
    /**
     * Close the connection.
     */
    void close() throws SQLException;
    
    /**
     * Check if the connection is still active.
     */
    boolean isConnected() throws SQLException;
    
    /**
     * Validate the connection with a simple query.
     */
    boolean validate() throws SQLException;
    
    /**
     * Get metadata about the database schema.
     */
    DatabaseMetadata getMetadata() throws SQLException;
}
