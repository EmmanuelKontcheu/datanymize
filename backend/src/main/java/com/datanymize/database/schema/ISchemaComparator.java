package com.datanymize.database.schema;

import com.datanymize.database.model.DatabaseMetadata;

import java.util.List;

/**
 * Interface for schema comparison and validation.
 * Detects differences between schemas and validates compatibility.
 * 
 * Validates Requirements: 2.6
 */
public interface ISchemaComparator {
    
    /**
     * Compare two schemas and return differences.
     * 
     * @param schema1 First schema to compare
     * @param schema2 Second schema to compare
     * @return SchemaDifferences containing all detected differences
     */
    SchemaDifferences compareSchemata(DatabaseMetadata schema1, DatabaseMetadata schema2);
    
    /**
     * Validate schema constraints and compatibility.
     * 
     * @param schema Schema to validate
     * @return List of constraint issues found (empty if valid)
     */
    List<ConstraintIssue> validateConstraints(DatabaseMetadata schema);
    
    /**
     * Check if two schemas are structurally equivalent.
     * 
     * @param schema1 First schema
     * @param schema2 Second schema
     * @return true if schemas are equivalent, false otherwise
     */
    boolean areEquivalent(DatabaseMetadata schema1, DatabaseMetadata schema2);
    
    /**
     * Model representing differences between two schemas.
     */
    class SchemaDifferences {
        public List<String> addedTables;
        public List<String> removedTables;
        public List<ColumnDifference> columnDifferences;
        public List<ForeignKeyDifference> foreignKeyDifferences;
        public List<IndexDifference> indexDifferences;
        
        public SchemaDifferences(List<String> addedTables, List<String> removedTables,
                                List<ColumnDifference> columnDifferences,
                                List<ForeignKeyDifference> foreignKeyDifferences,
                                List<IndexDifference> indexDifferences) {
            this.addedTables = addedTables;
            this.removedTables = removedTables;
            this.columnDifferences = columnDifferences;
            this.foreignKeyDifferences = foreignKeyDifferences;
            this.indexDifferences = indexDifferences;
        }
        
        public boolean hasDifferences() {
            return !addedTables.isEmpty() || !removedTables.isEmpty() || 
                   !columnDifferences.isEmpty() || !foreignKeyDifferences.isEmpty() || 
                   !indexDifferences.isEmpty();
        }
    }
    
    /**
     * Model representing a column difference.
     */
    class ColumnDifference {
        public String tableName;
        public String columnName;
        public String difference; // e.g., "type changed from varchar to text"
        
        public ColumnDifference(String tableName, String columnName, String difference) {
            this.tableName = tableName;
            this.columnName = columnName;
            this.difference = difference;
        }
    }
    
    /**
     * Model representing a foreign key difference.
     */
    class ForeignKeyDifference {
        public String foreignKeyName;
        public String difference; // e.g., "added" or "removed"
        
        public ForeignKeyDifference(String foreignKeyName, String difference) {
            this.foreignKeyName = foreignKeyName;
            this.difference = difference;
        }
    }
    
    /**
     * Model representing an index difference.
     */
    class IndexDifference {
        public String indexName;
        public String difference; // e.g., "added" or "removed"
        
        public IndexDifference(String indexName, String difference) {
            this.indexName = indexName;
            this.difference = difference;
        }
    }
    
    /**
     * Model representing a constraint issue.
     */
    class ConstraintIssue {
        public String severity; // "ERROR", "WARNING"
        public String message;
        public String affectedElement; // table, column, or constraint name
        
        public ConstraintIssue(String severity, String message, String affectedElement) {
            this.severity = severity;
            this.message = message;
            this.affectedElement = affectedElement;
        }
    }
}
