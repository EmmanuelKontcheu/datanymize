package com.datanymize.database.schema;

import com.datanymize.database.connection.IDatabaseConnection;
import com.datanymize.database.connection.PostgreSQLConnection;
import com.datanymize.database.model.DatabaseMetadata;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PostgreSQL-specific schema extractor.
 * Extracts schema metadata from PostgreSQL databases using information_schema.
 * Implements caching with TTL to avoid repeated queries.
 * 
 * Validates Requirements: 2.1
 */
@Slf4j
public class PostgreSQLSchemaExtractor implements IDatabaseSchemaExtractor {
    
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
     * Create a new PostgreSQL schema extractor with default cache TTL.
     */
    public PostgreSQLSchemaExtractor() {
        this(DEFAULT_CACHE_TTL_MILLIS);
    }
    
    /**
     * Create a new PostgreSQL schema extractor with custom cache TTL.
     * 
     * @param cacheTtlMillis the cache time-to-live in milliseconds
     */
    public PostgreSQLSchemaExtractor(long cacheTtlMillis) {
        this.cacheTtlMillis = cacheTtlMillis;
        this.cache = new ConcurrentHashMap<>();
    }
    
    @Override
    public List<DatabaseMetadata.TableMetadata> extractTables(IDatabaseConnection conn) throws Exception {
        if (!(conn instanceof PostgreSQLConnection)) {
            throw new IllegalArgumentException("Connection must be a PostgreSQLConnection");
        }
        
        PostgreSQLConnection pgConn = (PostgreSQLConnection) conn;
        Connection underlyingConn = pgConn.getUnderlyingConnection();
        
        String cacheKey = "tables_" + getCacheKeyForConnection(underlyingConn);
        
        // Check cache
        CacheEntry<?> cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached tables for connection");
            return (List<DatabaseMetadata.TableMetadata>) cached.data;
        }
        
        List<DatabaseMetadata.TableMetadata> tables = new ArrayList<>();
        
        String query = "SELECT table_name FROM information_schema.tables " +
                      "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' " +
                      "ORDER BY table_name";
        
        try (Statement stmt = underlyingConn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                String tableName = rs.getString("table_name");
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
        log.info("Extracted {} tables from PostgreSQL database", tables.size());
        
        return tables;
    }
    
    @Override
    public List<DatabaseMetadata.ColumnMetadata> extractColumns(IDatabaseConnection conn, String tableName) throws Exception {
        if (!(conn instanceof PostgreSQLConnection)) {
            throw new IllegalArgumentException("Connection must be a PostgreSQLConnection");
        }
        
        PostgreSQLConnection pgConn = (PostgreSQLConnection) conn;
        Connection underlyingConn = pgConn.getUnderlyingConnection();
        
        String cacheKey = "columns_" + getCacheKeyForConnection(underlyingConn) + "_" + tableName;
        
        // Check cache
        CacheEntry<?> cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached columns for table: {}", tableName);
            return (List<DatabaseMetadata.ColumnMetadata>) cached.data;
        }
        
        List<DatabaseMetadata.ColumnMetadata> columns = new ArrayList<>();
        
        String query = "SELECT column_name, data_type, is_nullable, column_default " +
                      "FROM information_schema.columns " +
                      "WHERE table_schema = 'public' AND table_name = ? " +
                      "ORDER BY ordinal_position";
        
        try (PreparedStatement stmt = underlyingConn.prepareStatement(query)) {
            stmt.setString(1, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String columnName = rs.getString("column_name");
                    String dataType = rs.getString("data_type");
                    boolean nullable = "YES".equals(rs.getString("is_nullable"));
                    String defaultValue = rs.getString("column_default");
                    
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
        if (!(conn instanceof PostgreSQLConnection)) {
            throw new IllegalArgumentException("Connection must be a PostgreSQLConnection");
        }
        
        PostgreSQLConnection pgConn = (PostgreSQLConnection) conn;
        Connection underlyingConn = pgConn.getUnderlyingConnection();
        
        String cacheKey = "foreign_keys_" + getCacheKeyForConnection(underlyingConn);
        
        // Check cache
        CacheEntry<?> cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached foreign keys for connection");
            return (List<DatabaseMetadata.ForeignKeyMetadata>) cached.data;
        }
        
        List<DatabaseMetadata.ForeignKeyMetadata> foreignKeys = new ArrayList<>();
        
        // PostgreSQL query to extract foreign keys from information_schema
        String query = "SELECT " +
                      "kcu.constraint_name, " +
                      "kcu.table_name, " +
                      "kcu.column_name, " +
                      "ccu.table_name AS referenced_table_name, " +
                      "ccu.column_name AS referenced_column_name " +
                      "FROM information_schema.key_column_usage kcu " +
                      "JOIN information_schema.referential_constraints rc " +
                      "ON kcu.constraint_name = rc.constraint_name " +
                      "AND kcu.table_schema = rc.constraint_schema " +
                      "JOIN information_schema.constraint_column_usage ccu " +
                      "ON rc.unique_constraint_name = ccu.constraint_name " +
                      "AND rc.unique_constraint_schema = ccu.table_schema " +
                      "WHERE kcu.table_schema = 'public'";
        
        try (Statement stmt = underlyingConn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                String constraintName = rs.getString("constraint_name");
                String sourceTable = rs.getString("table_name");
                String sourceColumn = rs.getString("column_name");
                String targetTable = rs.getString("referenced_table_name");
                String targetColumn = rs.getString("referenced_column_name");
                
                // Get ON DELETE and ON UPDATE rules
                String onDelete = getConstraintRule(underlyingConn, constraintName, "DELETE");
                String onUpdate = getConstraintRule(underlyingConn, constraintName, "UPDATE");
                
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
        log.info("Extracted {} foreign keys from PostgreSQL database", foreignKeys.size());
        
        return foreignKeys;
    }
    
    @Override
    public List<DatabaseMetadata.IndexMetadata> extractIndices(IDatabaseConnection conn) throws Exception {
        if (!(conn instanceof PostgreSQLConnection)) {
            throw new IllegalArgumentException("Connection must be a PostgreSQLConnection");
        }
        
        PostgreSQLConnection pgConn = (PostgreSQLConnection) conn;
        Connection underlyingConn = pgConn.getUnderlyingConnection();
        
        String cacheKey = "indices_" + getCacheKeyForConnection(underlyingConn);
        
        // Check cache
        CacheEntry<?> cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached indices for connection");
            return (List<DatabaseMetadata.IndexMetadata>) cached.data;
        }
        
        List<DatabaseMetadata.IndexMetadata> indices = new ArrayList<>();
        
        String query = "SELECT indexname, tablename, indexdef " +
                      "FROM pg_indexes " +
                      "WHERE schemaname = 'public' " +
                      "ORDER BY tablename, indexname";
        
        try (Statement stmt = underlyingConn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                String indexName = rs.getString("indexname");
                String tableName = rs.getString("tablename");
                String indexDef = rs.getString("indexdef");
                
                // Parse columns from index definition
                List<String> columns = parseIndexColumns(indexDef);
                boolean unique = indexDef.contains("UNIQUE");
                
                DatabaseMetadata.IndexMetadata index = DatabaseMetadata.IndexMetadata.builder()
                    .name(indexName)
                    .tableName(tableName)
                    .columns(columns)
                    .unique(unique)
                    .build();
                
                indices.add(index);
            }
        }
        
        // Cache the result
        cache.put(cacheKey, new CacheEntry<>(indices, cacheTtlMillis));
        log.info("Extracted {} indices from PostgreSQL database", indices.size());
        
        return indices;
    }
    
    /**
     * Extract primary keys for a table.
     */
    private List<String> extractPrimaryKeys(Connection conn, String tableName) throws SQLException {
        List<String> primaryKeys = new ArrayList<>();
        
        String query = "SELECT a.attname " +
                      "FROM pg_index i " +
                      "JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey) " +
                      "JOIN pg_class t ON t.oid = i.indrelid " +
                      "WHERE t.relname = ? AND i.indisprimary " +
                      "ORDER BY a.attnum";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    primaryKeys.add(rs.getString("attname"));
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
        
        String query = "SELECT a.attname " +
                      "FROM pg_index i " +
                      "JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey) " +
                      "JOIN pg_class t ON t.oid = i.indrelid " +
                      "WHERE t.relname = ? AND i.indisunique AND NOT i.indisprimary " +
                      "ORDER BY a.attnum";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    uniqueKeys.add(rs.getString("attname"));
                }
            }
        }
        
        return uniqueKeys;
    }
    
    /**
     * Get the ON DELETE or ON UPDATE rule for a constraint.
     */
    private String getConstraintRule(Connection conn, String constraintName, String ruleType) throws SQLException {
        String query = "SELECT " + (ruleType.equals("DELETE") ? "delete_rule" : "update_rule") + " " +
                      "FROM information_schema.referential_constraints " +
                      "WHERE constraint_name = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, constraintName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        
        return "NO ACTION";
    }
    
    /**
     * Parse column names from an index definition.
     */
    private List<String> parseIndexColumns(String indexDef) {
        List<String> columns = new ArrayList<>();
        
        // Simple parsing: extract column names between parentheses
        int startIdx = indexDef.lastIndexOf('(');
        int endIdx = indexDef.lastIndexOf(')');
        
        if (startIdx >= 0 && endIdx > startIdx) {
            String columnsPart = indexDef.substring(startIdx + 1, endIdx);
            String[] parts = columnsPart.split(",");
            
            for (String part : parts) {
                String column = part.trim().split("\\s+")[0];  // Get first word (column name)
                if (!column.isEmpty()) {
                    columns.add(column);
                }
            }
        }
        
        return columns;
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
        String schema = conn.getSchema();
        return (catalog != null ? catalog : "default") + "_" + (schema != null ? schema : "public");
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
