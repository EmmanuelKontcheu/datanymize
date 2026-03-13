package com.datanymize.database.schema;

import com.datanymize.database.connection.IDatabaseConnection;
import com.datanymize.database.model.DatabaseMetadata;

/**
 * Interface for database-specific schema extraction.
 * Provides methods to extract schema metadata from different database types.
 * 
 * Validates Requirements: 2.1, 2.2, 2.3
 */
public interface IDatabaseSchemaExtractor {
    
    /**
     * Extract all tables from the database.
     * 
     * @param conn the database connection
     * @return list of table metadata
     * @throws Exception if extraction fails
     */
    java.util.List<DatabaseMetadata.TableMetadata> extractTables(IDatabaseConnection conn) throws Exception;
    
    /**
     * Extract all columns for a specific table.
     * 
     * @param conn the database connection
     * @param tableName the name of the table
     * @return list of column metadata
     * @throws Exception if extraction fails
     */
    java.util.List<DatabaseMetadata.ColumnMetadata> extractColumns(IDatabaseConnection conn, String tableName) throws Exception;
    
    /**
     * Extract all foreign keys from the database.
     * 
     * @param conn the database connection
     * @return list of foreign key metadata
     * @throws Exception if extraction fails
     */
    java.util.List<DatabaseMetadata.ForeignKeyMetadata> extractForeignKeys(IDatabaseConnection conn) throws Exception;
    
    /**
     * Extract all indices from the database.
     * 
     * @param conn the database connection
     * @return list of index metadata
     * @throws Exception if extraction fails
     */
    java.util.List<DatabaseMetadata.IndexMetadata> extractIndices(IDatabaseConnection conn) throws Exception;
}
