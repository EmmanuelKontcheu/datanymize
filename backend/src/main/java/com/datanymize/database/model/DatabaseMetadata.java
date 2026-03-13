package com.datanymize.database.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Metadata about a database schema.
 * Contains information about tables, columns, keys, and constraints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatabaseMetadata {
    private String databaseName;
    private String databaseType;
    private List<TableMetadata> tables;
    private List<ForeignKeyMetadata> foreignKeys;
    private List<IndexMetadata> indices;
    private Map<String, String> metadata;
    
    /**
     * Metadata about a table.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TableMetadata {
        private String name;
        private List<ColumnMetadata> columns;
        private List<String> primaryKeys;
        private List<String> uniqueKeys;
        private long rowCount;
    }
    
    /**
     * Metadata about a column.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ColumnMetadata {
        private String name;
        private String dataType;
        private boolean nullable;
        private String defaultValue;
        private boolean isPrimaryKey;
        private boolean isUnique;
    }
    
    /**
     * Metadata about a foreign key.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ForeignKeyMetadata {
        private String name;
        private String sourceTable;
        private String sourceColumn;
        private String targetTable;
        private String targetColumn;
        private String onDelete;
        private String onUpdate;
    }
    
    /**
     * Metadata about an index.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IndexMetadata {
        private String name;
        private String tableName;
        private List<String> columns;
        private boolean unique;
    }
}
