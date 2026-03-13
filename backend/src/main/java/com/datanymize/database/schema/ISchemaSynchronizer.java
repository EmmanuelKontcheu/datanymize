package com.datanymize.database.schema;

import com.datanymize.database.connection.IDatabaseConnection;
import com.datanymize.database.model.DatabaseMetadata;

/**
 * Interface for schema synchronization.
 * Handles creating target database schemas from source schemas.
 * 
 * Validates Requirements: 2.5
 */
public interface ISchemaSynchronizer {
    
    /**
     * Synchronize schema from source to target database.
     * Creates all tables, columns, constraints, and indices in target database.
     * 
     * @param sourceConn Connection to source database
     * @param targetConn Connection to target database
     * @param sourceSchema Schema metadata from source database
     * @throws Exception if synchronization fails
     */
    void syncSchema(IDatabaseConnection sourceConn, IDatabaseConnection targetConn, DatabaseMetadata sourceSchema) throws Exception;
    
    /**
     * Create schema in target database from metadata.
     * 
     * @param targetConn Connection to target database
     * @param schema Schema metadata to create
     * @throws Exception if schema creation fails
     */
    void createSchema(IDatabaseConnection targetConn, DatabaseMetadata schema) throws Exception;
    
    /**
     * Drop schema from target database.
     * 
     * @param targetConn Connection to target database
     * @param schemaName Name of schema to drop
     * @throws Exception if schema drop fails
     */
    void dropSchema(IDatabaseConnection targetConn, String schemaName) throws Exception;
}
