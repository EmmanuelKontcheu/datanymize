package com.datanymize.database.schema;

import com.datanymize.database.model.DatabaseMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of schema versioning and caching.
 * Manages schema versions with TTL-based caching.
 * 
 * Validates Requirements: 2.4
 */
@Slf4j
@Component
public class SchemaVersionManager implements ISchemaVersionManager {
    
    private static final long CACHE_TTL_MILLIS = 3600000; // 1 hour
    
    private final Map<String, SchemaVersionHistory> versionHistories = new ConcurrentHashMap<>();
    private final ISchemaComparator schemaComparator;
    
    public SchemaVersionManager(ISchemaComparator schemaComparator) {
        this.schemaComparator = schemaComparator;
    }
    
    @Override
    public int saveSchemaVersion(String schemaId, DatabaseMetadata schema) {
        log.debug("Saving schema version for: {}", schemaId);
        
        if (schemaId == null || schemaId.trim().isEmpty()) {
            throw new IllegalArgumentException("Schema ID cannot be null or empty");
        }
        
        if (schema == null) {
            throw new IllegalArgumentException("Schema cannot be null");
        }
        
        SchemaVersionHistory history = versionHistories.computeIfAbsent(schemaId, 
            k -> new SchemaVersionHistory());
        
        int version = history.addVersion(schema);
        log.debug("Schema version {} saved for: {}", version, schemaId);
        
        return version;
    }
    
    @Override
    public DatabaseMetadata getSchemaVersion(String schemaId, int version) {
        log.debug("Retrieving schema version {} for: {}", version, schemaId);
        
        SchemaVersionHistory history = versionHistories.get(schemaId);
        if (history == null) {
            log.warn("No version history found for schema: {}", schemaId);
            return null;
        }
        
        return history.getVersion(version);
    }
    
    @Override
    public DatabaseMetadata getLatestSchema(String schemaId) {
        log.debug("Retrieving latest schema for: {}", schemaId);
        
        SchemaVersionHistory history = versionHistories.get(schemaId);
        if (history == null) {
            log.warn("No version history found for schema: {}", schemaId);
            return null;
        }
        
        return history.getLatestVersion();
    }
    
    @Override
    public List<SchemaVersion> getSchemaHistory(String schemaId) {
        log.debug("Retrieving schema history for: {}", schemaId);
        
        SchemaVersionHistory history = versionHistories.get(schemaId);
        if (history == null) {
            log.warn("No version history found for schema: {}", schemaId);
            return new ArrayList<>();
        }
        
        return history.getAllVersions();
    }
    
    @Override
    public DatabaseMetadata restoreSchemaVersion(String schemaId, int version) {
        log.info("Restoring schema version {} for: {}", version, schemaId);
        
        SchemaVersionHistory history = versionHistories.get(schemaId);
        if (history == null) {
            throw new IllegalArgumentException("No version history found for schema: " + schemaId);
        }
        
        DatabaseMetadata schema = history.getVersion(version);
        if (schema == null) {
            throw new IllegalArgumentException("Version " + version + " not found for schema: " + schemaId);
        }
        
        // Save as new version
        int newVersion = history.addVersion(schema);
        log.info("Schema restored to version {} (saved as version {})", version, newVersion);
        
        return schema;
    }
    
    @Override
    public ISchemaComparator.SchemaDifferences compareVersions(String schemaId, int version1, int version2) {
        log.debug("Comparing schema versions {} and {} for: {}", version1, version2, schemaId);
        
        DatabaseMetadata schema1 = getSchemaVersion(schemaId, version1);
        DatabaseMetadata schema2 = getSchemaVersion(schemaId, version2);
        
        if (schema1 == null || schema2 == null) {
            throw new IllegalArgumentException("One or both versions not found for schema: " + schemaId);
        }
        
        return schemaComparator.compareSchemata(schema1, schema2);
    }
    
    @Override
    public void clearCache(String schemaId) {
        log.debug("Clearing cache for schema: {}", schemaId);
        versionHistories.remove(schemaId);
    }
    
    @Override
    public void clearAllCaches() {
        log.debug("Clearing all schema caches");
        versionHistories.clear();
    }
    
    /**
     * Internal class for managing version history of a schema.
     */
    private static class SchemaVersionHistory {
        private final Map<Integer, CachedSchema> versions = new LinkedHashMap<>();
        private int nextVersion = 1;
        
        synchronized int addVersion(DatabaseMetadata schema) {
            int version = nextVersion++;
            versions.put(version, new CachedSchema(schema, System.currentTimeMillis()));
            return version;
        }
        
        synchronized DatabaseMetadata getVersion(int version) {
            CachedSchema cached = versions.get(version);
            if (cached == null) {
                return null;
            }
            
            // Check if cache is expired
            if (cached.isExpired()) {
                versions.remove(version);
                return null;
            }
            
            return cached.schema;
        }
        
        synchronized DatabaseMetadata getLatestVersion() {
            if (versions.isEmpty()) {
                return null;
            }
            
            // Get the last entry (most recent)
            int latestVersion = versions.keySet().stream()
                .max(Integer::compareTo)
                .orElse(-1);
            
            if (latestVersion == -1) {
                return null;
            }
            
            return getVersion(latestVersion);
        }
        
        synchronized List<SchemaVersion> getAllVersions() {
            List<SchemaVersion> result = new ArrayList<>();
            
            for (Map.Entry<Integer, CachedSchema> entry : versions.entrySet()) {
                if (!entry.getValue().isExpired()) {
                    result.add(new SchemaVersion(
                        entry.getKey(),
                        entry.getValue().createdAt,
                        "Version " + entry.getKey(),
                        entry.getValue().schema
                    ));
                }
            }
            
            return result;
        }
    }
    
    /**
     * Internal class for caching schemas with TTL.
     */
    private static class CachedSchema {
        private final DatabaseMetadata schema;
        private final LocalDateTime createdAt;
        private final long createdAtMillis;
        
        CachedSchema(DatabaseMetadata schema, long createdAtMillis) {
            this.schema = schema;
            this.createdAtMillis = createdAtMillis;
            this.createdAt = LocalDateTime.now();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - createdAtMillis > CACHE_TTL_MILLIS;
        }
    }
}
