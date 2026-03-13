package com.datanymize.database.schema;

import com.datanymize.database.model.DatabaseMetadata;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface for schema versioning and caching.
 * Manages schema versions and provides caching with TTL.
 * 
 * Validates Requirements: 2.4
 */
public interface ISchemaVersionManager {
    
    /**
     * Save a schema version.
     * 
     * @param schemaId Unique identifier for the schema
     * @param schema Schema metadata to save
     * @return Version number assigned to this schema
     */
    int saveSchemaVersion(String schemaId, DatabaseMetadata schema);
    
    /**
     * Get a specific schema version.
     * 
     * @param schemaId Unique identifier for the schema
     * @param version Version number to retrieve
     * @return Schema metadata for the specified version
     */
    DatabaseMetadata getSchemaVersion(String schemaId, int version);
    
    /**
     * Get the latest schema version.
     * 
     * @param schemaId Unique identifier for the schema
     * @return Latest schema metadata
     */
    DatabaseMetadata getLatestSchema(String schemaId);
    
    /**
     * Get all versions of a schema.
     * 
     * @param schemaId Unique identifier for the schema
     * @return List of all schema versions
     */
    List<SchemaVersion> getSchemaHistory(String schemaId);
    
    /**
     * Restore a schema to a previous version.
     * 
     * @param schemaId Unique identifier for the schema
     * @param version Version number to restore
     * @return Restored schema metadata
     */
    DatabaseMetadata restoreSchemaVersion(String schemaId, int version);
    
    /**
     * Compare two schema versions.
     * 
     * @param schemaId Unique identifier for the schema
     * @param version1 First version to compare
     * @param version2 Second version to compare
     * @return Differences between versions
     */
    ISchemaComparator.SchemaDifferences compareVersions(String schemaId, int version1, int version2);
    
    /**
     * Clear schema cache for a specific schema.
     * 
     * @param schemaId Unique identifier for the schema
     */
    void clearCache(String schemaId);
    
    /**
     * Clear all schema caches.
     */
    void clearAllCaches();
    
    /**
     * Model representing a schema version.
     */
    class SchemaVersion {
        public int version;
        public LocalDateTime timestamp;
        public String description;
        public DatabaseMetadata schema;
        
        public SchemaVersion(int version, LocalDateTime timestamp, String description, DatabaseMetadata schema) {
            this.version = version;
            this.timestamp = timestamp;
            this.description = description;
            this.schema = schema;
        }
    }
}
