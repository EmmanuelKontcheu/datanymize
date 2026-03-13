package com.datanymize.database.schema;

import com.datanymize.database.connection.IDatabaseConnection;
import com.datanymize.database.connection.MySQLConnection;
import com.datanymize.database.model.DatabaseMetadata;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MySQL-specific schema extractor.
 * Extracts schema metadata from MySQL databases using information_schema.
 * Implements caching with TTL to avoid repeated queries.
 * 
 * Validates Requirements: 2.2
 */
@Slf4j
public class MySQLSchemaExtractor implements IDatabaseSchemaExtractor {
    
    private static final long DEFAULT_CACHE_TTL_MILLIS = 5 * 60 * 1000; // 5 minutes
    
    /**
     * Cache entry for schema metadata.
     */
    private static class CacheEntry<T> {
        private final T data;
        private final long expirationTime;
        
        CacheEntry(T data, long ttlMillis) {
            this.data = data;
            this.expirationTime = System.currentTimeMillis() + ttlMillis;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
    
    private final long cacheTtlMillis;
    private final Map<String, CacheEntry<?>> cache;
    
    /**
     * Create a new MySQL schema extractor with default cache TTL.
     */
    public MySQLSchemaExtractor() {
        this(DEFAULT_CACHE_TTL_MILLIS);
    }
    
    /**
     * Create a new MySQL schema extractor with custom cache TTL.
     * 
     * @param cacheTtlMillis the cache time-to-live in milliseconds
     */
    public MySQLSchemaExtractor(long cacheTtlMillis) {
        this.cacheTtlMillis = cacheTtlMillis;
        this.cache = new ConcurrentHashMap<>();
    }
    
    @Override
    public List<DatabaseMetadata.TableMetadata> extractTables(IDatabaseConnection conn) throws Exception {
        if (!(conn instanceof MySQLConnection)) {
            throw new IllegalArgumentException("Connection must be a MySQLConnection");
        }
        
        MySQLConnection mysqlConn = (MySQLConnection) conn;
        Connection underlyingConn = mysqlConn.getUnderlyingConnection();
        
        String cacheKey = "tables_" + getCacheKeyForConnection(underlyingConn);
        
        // Check cache
        CacheEntry<?> cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached tables for connection");
            return (List<DatabaseMetadata.TableMetadata>) cached.data;
        }
        
        List<DatabaseMetadata.TableMetadata> tables = new ArrayList<>();
        
        String query = "SELECT TABLE_NAME FROM information_schema.TABLES " +
                      "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE' " +
                      "ORDER BY TABLE_NAME";
        
        try (Statement stmt = underlyingConn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                List<DatabaseMetadata.ColumnMetadata> columns = extractColumns(conn, tableName);
                List<String> primaryKeys = extractPrimaryKeys(underlyingConn, tableName);
                List<String> uniqueKeys = extractUniqueKeys(underlyingConn, tableName);
                long rowCount = getRowCount(underlyingConn, tableName);
                
                DatabaseMetadata.TableMetadata table = DatabaseMetadata.TableMetadata.builder()
                    .name(tableName)
                    .columns(columns)
                    .primaryKeys(primaryKeys)
                    .uniqueKeys(uniqueKeys)
                    .rowCount(rowCount)
                    .build();
                
                tables.add(table);
            }
        }
        
        // Cache the result
        cache.put(cacheKey, new CacheEntry<>(tables, cacheTtlMillis));
        log.info("Extracted {} tables from MySQL database", tables.size());
        
        return tables;
    }
    
    @Override
    public List<DatabaseMetadata.ColumnMetadata> extractColumns(IDatabaseConnection conn, String tableName) throws Exception {
        if (!(conn instanceof MySQLConnection)) {
            throw new IllegalArgumentException("Connection must be a MySQLConnection");
        }
        
        MySQLConnection mysqlConn = (MySQLConnection) conn;
        Connection underlyingConn = mysqlConn.getUnderlyingConnection();
        
        String cacheKey = "columns_" + getCacheKeyForConnection(underlyingConn) + "_" + tableName;
        
        // Check cache
        CacheEntry<?> cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached columns for table: {}", tableName);
            return (List<DatabaseMetadata.ColumnMetadata>) cached.data;
        }
        
        List<DatabaseMetadata.ColumnMetadata> columns = new ArrayList<>();
        
        String query = "SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT " +
                      "FROM information_schema.COLUMNS " +
                      "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? " +
                      "ORDER BY ORDINAL_POSITION";
        
        try (PreparedStatement stmt = underlyingConn.prepareStatement(query)) {
            stmt.setString(1, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    String dataType = rs.getString("COLUMN_TYPE");
                    boolean nullable = "YES".equals(rs.getString("IS_NULLABLE"));
                    String defaultValue = rs.getString("COLUMN_DEFAULT");
                    
                    DatabaseMetadata.ColumnMetadata column = DatabaseMetadata.ColumnMetadata.builder()
                        .name(columnName)
                        .dataType(dataType)
                        .nullable(nullable)
                        .defaultValue(defaultValue)
                        .isPrimaryKey(false)  // Will be set by extractPrimaryKeys
                        .isUnique(false)      // Will be set by extractUniqueKeys
                        .build();
                    
                    columns.add(column);
                }
            }
        }
        
        // Cache the result
        cache.put(cacheKey, new CacheEntry<>(columns, cacheTtlMillis));
        log.debug("Extracted {} columns from table: {}", columns.size(), tableName);
        
        return columns;
    }
    
    @Override
    public List<DatabaseMetadata.ForeignKeyMetadata> extractForeignKeys(IDatabaseConnection conn) throws Exception {
        if (!(conn instanceof MySQLConnection)) {
            throw new IllegalArgumentException("Connection must be a MySQLConnection");
        }
        
        MySQLConnection mysqlConn = (MySQLConnection) conn;
        Connection underlyingConn = mysqlConn.getUnderlyingConnection();
        
        String cacheKey = "foreign_keys_" + getCacheKeyForConnection(underlyingConn);
        
        // Check cache
        CacheEntry<?> cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached foreign keys for connection");
            return (List<DatabaseMetadata.ForeignKeyMetadata>) cached.data;
        }
        
        List<DatabaseMetadata.ForeignKeyMetadata> foreignKeys = new ArrayList<>();
        
        // MySQL query to extract foreign keys from information_schema
        String query = "SELECT " +
                      "CONSTRAINT_NAME, " +
                      "TABLE_NAME, " +
                      "COLUMN_NAME, " +
                      "REFERENCED_TABLE_NAME, " +
                      "REFERENCED_COLUMN_NAME " +
                      "FROM information_schema.KEY_COLUMN_USAGE " +
                      "WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL";
        
        try (Statement stmt = underlyingConn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                String constraintName = rs.getString("CONSTRAINT_NAME");
                String sourceTable = rs.getString("TABLE_NAME");
                String sourceColumn = rs.getString("COLUMN_NAME");
                String targetTable = rs.getString("REFERENCED_TABLE_NAME");
                String targetColumn = rs.getString("REFERENCED_COLUMN_NAME");
                
                // Get ON DELETE and ON UPDATE rules
                String[] rules = getConstraintRules(underlyingConn, sourceTable, constraintName);
                String onDelete = rules[0];
                String onUpdate = rules[1];
                
                DatabaseMetadata.ForeignKeyMetadata fk = DatabaseMetadata.ForeignKeyMetadata.builder()
                    .name(constraintName)
                    .sourceTable(sourceTable)
                    .sourceColumn(sourceColumn)
                    .targetTable(targetTable)
                    .targetColumn(targetColumn)
                    .onDelete(onDelete)
                    .onUpdate(onUpdate)
                    .build();
                
                foreignKeys.add(fk);
            }
        }
        
        // Cache the result
        cache.put(cacheKey, new CacheEntry<>(foreignKeys, cacheTtlMillis));
        log.info("Extracted {} foreign keys from MySQL database", foreignKeys.size());
        
        return foreignKeys;
    }
    
    @Override
    public List<DatabaseMetadata.IndexMetadata> extractIndices(IDatabaseConnection conn) throws Exception {
        if (!(conn instanceof MySQLConnection)) {
            throw new IllegalArgumentException("Connection must be a MySQLConnection");
        }
        
        MySQLConnection mysqlConn = (MySQLConnection) conn;
        Connection underlyingConn = mysqlConn.getUnderlyingConnection();
        
        String cacheKey = "indices_" + getCacheKeyForConnection(underlyingConn);
        
        // Check cache
        CacheEntry<?> cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached indices for connection");
            return (List<DatabaseMetadata.IndexMetadata>) cached.data;
        }
        
        List<DatabaseMetadata.IndexMetadata> indices = new ArrayList<>();
        
        String query = "SELECT " +
                      "INDEX_NAME, " +
                      "TABLE_NAME, " +
                      "COLUMN_NAME, " +
                      "NON_UNIQUE " +
                      "FROM information_schema.STATISTICS " +
                      "WHERE TABLE_SCHEMA = DATABASE() AND INDEX_NAME != 'PRIMARY' " +
                      "ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX";
        
        try (Statement stmt = underlyingConn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            Map<String, DatabaseMetadata.IndexMetadata.IndexMetadataBuilder> indexBuilders = new LinkedHashMap<>();
            
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                String tableName = rs.getString("TABLE_NAME");
                String columnName = rs.getString("COLUMN_NAME");
                boolean unique = rs.getInt("NON_UNIQUE") == 0;
                
                String indexKey = tableName + "." + indexName;
                
                indexBuilders.computeIfAbsent(indexKey, k -> 
                    DatabaseMetadata.IndexMetadata.builder()
                        .name(indexName)
                        .tableName(tableName)
                        .columns(new ArrayList<>())
                        .unique(unique)
                ).columns.add(columnName);
            }
            
            // Build final index list
            for (DatabaseMetadata.IndexMetadata.IndexMetadataBuilder builder : indexBuilders.values()) {
                indices.add(builder.build());
            }
        }
        
        // Cache the result
        cache.put(cacheKey, new CacheEntry<>(indices, cacheTtlMillis));
        log.info("Extracted {} indices from MySQL database", indices.size());
        
        return indices;
    }
    
    /**
     * Extract primary keys for a table.
     */
    private List<String> extractPrimaryKeys(Connection conn, String tableName) throws SQLException {
        List<String> primaryKeys = new ArrayList<>();
        
        String query = "SELECT COLUMN_NAME " +
                      "FROM information_schema.COLUMNS " +
                      "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_KEY = 'PRI' " +
                      "ORDER BY ORDINAL_POSITION";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    primaryKeys.add(rs.getString("COLUMN_NAME"));
                }
            }
        }
        
        return primaryKeys;
    }
    
    /**
     * Extract unique keys for a table.
     */
    private List<String> extractUniqueKeys(Connection conn, String tableName) throws SQLException {
        List<String> uniqueKeys = new ArrayList<>();
        
        String query = "SELECT COLUMN_NAME " +
                      "FROM information_schema.COLUMNS " +
                      "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_KEY = 'UNI' " +
                      "ORDER BY ORDINAL_POSITION";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    uniqueKeys.add(rs.getString("COLUMN_NAME"));
                }
            }
        }
        
        return uniqueKeys;
    }
    
    /**
     * Get the ON DELETE and ON UPDATE rules for a constraint.
     * Returns an array [onDelete, onUpdate].
     */
    private String[] getConstraintRules(Connection conn, String tableName, String constraintName) throws SQLException {
        String onDelete = "NO ACTION";
        String onUpdate = "NO ACTION";
        
        try {
            // Use SHOW CREATE TABLE to extract constraint rules
            String query = "SHOW CREATE TABLE " + tableName;
            
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                
                if (rs.next()) {
                    String createTableDef = rs.getString(2);
                    
                    // Parse the CREATE TABLE statement to find the constraint definition
                    String constraintPattern = "CONSTRAINT `?" + constraintName + "`?";
                    int constraintIdx = createTableDef.toLowerCase().indexOf("constraint");
                    
                    if (constraintIdx >= 0) {
                        // Find the constraint definition
                        int fkIdx = createTableDef.toLowerCase().indexOf("foreign key", constraintIdx);
                        if (fkIdx >= 0) {
                            // Extract the portion containing the constraint
                            int endIdx = createTableDef.indexOf(",", fkIdx);
                            if (endIdx < 0) {
                                endIdx = createTableDef.indexOf(")", fkIdx);
                            }
                            
                            if (endIdx > fkIdx) {
                                String constraintDef = createTableDef.substring(fkIdx, endIdx);
                                
                                // Extract ON DELETE rule
                                int onDeleteIdx = constraintDef.toUpperCase().indexOf("ON DELETE");
                                if (onDeleteIdx >= 0) {
                                    String afterDelete = constraintDef.substring(onDeleteIdx + 9).trim();
                                    String[] parts = afterDelete.split("\\s+");
                                    if (parts.length > 0) {
                                        onDelete = parts[0].toUpperCase();
                                    }
                                }
                                
                                // Extract ON UPDATE rule
                                int onUpdateIdx = constraintDef.toUpperCase().indexOf("ON UPDATE");
                                if (onUpdateIdx >= 0) {
                                    String afterUpdate = constraintDef.substring(onUpdateIdx + 9).trim();
                                    String[] parts = afterUpdate.split("\\s+");
                                    if (parts.length > 0) {
                                        onUpdate = parts[0].toUpperCase();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.warn("Failed to extract constraint rules for {}.{}: {}", tableName, constraintName, e.getMessage());
        }
        
        return new String[]{onDelete, onUpdate};
    }
    
    /**
     * Get the row count for a table.
     */
    private long getRowCount(Connection conn, String tableName) throws SQLException {
        String query = "SELECT COUNT(*) as count FROM " + tableName;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            if (rs.next()) {
                return rs.getLong("count");
            }
        }
        
        return 0;
    }
    
    /**
     * Generate a cache key for a connection based on its database name.
     */
    private String getCacheKeyForConnection(Connection conn) throws SQLException {
        String catalog = conn.getCatalog();
        return catalog != null ? catalog : "default";
    }
    
    /**
     * Clear the cache.
     */
    public void clearCache() {
        cache.clear();
        log.debug("Schema extraction cache cleared");
    }
    
    /**
     * Clear expired cache entries.
     */
    public void clearExpiredCache() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        log.debug("Expired schema extraction cache entries cleared");
    }
}
