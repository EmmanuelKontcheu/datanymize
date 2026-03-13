package com.datanymize.database.schema;

import com.datanymize.database.model.DatabaseMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of schema comparison and validation.
 * Detects differences between schemas and validates constraint compatibility.
 * 
 * Validates Requirements: 2.6
 */
@Slf4j
@Component
public class SchemaComparator implements ISchemaComparator {
    
    @Override
    public SchemaDifferences compareSchemata(DatabaseMetadata schema1, DatabaseMetadata schema2) {
        log.debug("Comparing schemas: {} vs {}", 
                  schema1 != null ? schema1.getDatabaseName() : "null",
                  schema2 != null ? schema2.getDatabaseName() : "null");
        
        if (schema1 == null || schema2 == null) {
            return new SchemaDifferences(new ArrayList<>(), new ArrayList<>(), 
                                        new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
        
        List<String> addedTables = new ArrayList<>();
        List<String> removedTables = new ArrayList<>();
        List<ColumnDifference> columnDifferences = new ArrayList<>();
        List<ForeignKeyDifference> foreignKeyDifferences = new ArrayList<>();
        List<IndexDifference> indexDifferences = new ArrayList<>();
        
        // Get table names
        Set<String> tables1 = schema1.getTables() != null ? 
            schema1.getTables().stream().map(DatabaseMetadata.TableMetadata::getName).collect(Collectors.toSet()) : 
            new HashSet<>();
        Set<String> tables2 = schema2.getTables() != null ? 
            schema2.getTables().stream().map(DatabaseMetadata.TableMetadata::getName).collect(Collectors.toSet()) : 
            new HashSet<>();
        
        // Find added and removed tables
        tables2.stream().filter(t -> !tables1.contains(t)).forEach(addedTables::add);
        tables1.stream().filter(t -> !tables2.contains(t)).forEach(removedTables::add);
        
        // Compare columns in common tables
        for (String tableName : tables1) {
            if (tables2.contains(tableName)) {
                compareTableColumns(schema1, schema2, tableName, columnDifferences);
            }
        }
        
        // Compare foreign keys
        compareForeignKeys(schema1, schema2, foreignKeyDifferences);
        
        // Compare indices
        compareIndices(schema1, schema2, indexDifferences);
        
        return new SchemaDifferences(addedTables, removedTables, columnDifferences, 
                                    foreignKeyDifferences, indexDifferences);
    }
    
    @Override
    public List<ConstraintIssue> validateConstraints(DatabaseMetadata schema) {
        log.debug("Validating constraints for schema: {}", schema != null ? schema.getDatabaseName() : "null");
        
        List<ConstraintIssue> issues = new ArrayList<>();
        
        if (schema == null || schema.getTables() == null) {
            return issues;
        }
        
        // Validate foreign keys reference valid tables and columns
        if (schema.getForeignKeys() != null) {
            for (DatabaseMetadata.ForeignKeyMetadata fk : schema.getForeignKeys()) {
                validateForeignKey(schema, fk, issues);
            }
        }
        
        // Validate indices reference valid tables and columns
        if (schema.getIndices() != null) {
            for (DatabaseMetadata.IndexMetadata index : schema.getIndices()) {
                validateIndex(schema, index, issues);
            }
        }
        
        // Validate primary keys reference valid columns
        for (DatabaseMetadata.TableMetadata table : schema.getTables()) {
            validatePrimaryKeys(schema, table, issues);
        }
        
        return issues;
    }
    
    @Override
    public boolean areEquivalent(DatabaseMetadata schema1, DatabaseMetadata schema2) {
        if (schema1 == null && schema2 == null) {
            return true;
        }
        if (schema1 == null || schema2 == null) {
            return false;
        }
        
        SchemaDifferences differences = compareSchemata(schema1, schema2);
        return !differences.hasDifferences();
    }
    
    private void compareTableColumns(DatabaseMetadata schema1, DatabaseMetadata schema2, 
                                     String tableName, List<ColumnDifference> differences) {
        DatabaseMetadata.TableMetadata table1 = schema1.getTables().stream()
            .filter(t -> t.getName().equals(tableName))
            .findFirst()
            .orElse(null);
        
        DatabaseMetadata.TableMetadata table2 = schema2.getTables().stream()
            .filter(t -> t.getName().equals(tableName))
            .findFirst()
            .orElse(null);
        
        if (table1 == null || table2 == null) {
            return;
        }
        
        Set<String> cols1 = table1.getColumns() != null ? 
            table1.getColumns().stream().map(DatabaseMetadata.ColumnMetadata::getName).collect(Collectors.toSet()) : 
            new HashSet<>();
        Set<String> cols2 = table2.getColumns() != null ? 
            table2.getColumns().stream().map(DatabaseMetadata.ColumnMetadata::getName).collect(Collectors.toSet()) : 
            new HashSet<>();
        
        // Check for added/removed columns
        for (String col : cols2) {
            if (!cols1.contains(col)) {
                differences.add(new ColumnDifference(tableName, col, "added"));
            }
        }
        
        for (String col : cols1) {
            if (!cols2.contains(col)) {
                differences.add(new ColumnDifference(tableName, col, "removed"));
            }
        }
        
        // Check for type changes in common columns
        for (String col : cols1) {
            if (cols2.contains(col)) {
                DatabaseMetadata.ColumnMetadata colMeta1 = table1.getColumns().stream()
                    .filter(c -> c.getName().equals(col))
                    .findFirst()
                    .orElse(null);
                
                DatabaseMetadata.ColumnMetadata colMeta2 = table2.getColumns().stream()
                    .filter(c -> c.getName().equals(col))
                    .findFirst()
                    .orElse(null);
                
                if (colMeta1 != null && colMeta2 != null && 
                    !colMeta1.getDataType().equals(colMeta2.getDataType())) {
                    differences.add(new ColumnDifference(tableName, col, 
                        "type changed from " + colMeta1.getDataType() + " to " + colMeta2.getDataType()));
                }
            }
        }
    }
    
    private void compareForeignKeys(DatabaseMetadata schema1, DatabaseMetadata schema2, 
                                   List<ForeignKeyDifference> differences) {
        Set<String> fks1 = schema1.getForeignKeys() != null ? 
            schema1.getForeignKeys().stream().map(DatabaseMetadata.ForeignKeyMetadata::getName).collect(Collectors.toSet()) : 
            new HashSet<>();
        Set<String> fks2 = schema2.getForeignKeys() != null ? 
            schema2.getForeignKeys().stream().map(DatabaseMetadata.ForeignKeyMetadata::getName).collect(Collectors.toSet()) : 
            new HashSet<>();
        
        // Find added and removed foreign keys
        fks2.stream().filter(fk -> !fks1.contains(fk))
            .forEach(fk -> differences.add(new ForeignKeyDifference(fk, "added")));
        fks1.stream().filter(fk -> !fks2.contains(fk))
            .forEach(fk -> differences.add(new ForeignKeyDifference(fk, "removed")));
    }
    
    private void compareIndices(DatabaseMetadata schema1, DatabaseMetadata schema2, 
                               List<IndexDifference> differences) {
        Set<String> indices1 = schema1.getIndices() != null ? 
            schema1.getIndices().stream().map(DatabaseMetadata.IndexMetadata::getName).collect(Collectors.toSet()) : 
            new HashSet<>();
        Set<String> indices2 = schema2.getIndices() != null ? 
            schema2.getIndices().stream().map(DatabaseMetadata.IndexMetadata::getName).collect(Collectors.toSet()) : 
            new HashSet<>();
        
        // Find added and removed indices
        indices2.stream().filter(idx -> !indices1.contains(idx))
            .forEach(idx -> differences.add(new IndexDifference(idx, "added")));
        indices1.stream().filter(idx -> !indices2.contains(idx))
            .forEach(idx -> differences.add(new IndexDifference(idx, "removed")));
    }
    
    private void validateForeignKey(DatabaseMetadata schema, DatabaseMetadata.ForeignKeyMetadata fk, 
                                   List<ConstraintIssue> issues) {
        // Check if source table exists
        boolean sourceTableExists = schema.getTables().stream()
            .anyMatch(t -> t.getName().equals(fk.getSourceTable()));
        if (!sourceTableExists) {
            issues.add(new ConstraintIssue("ERROR", 
                "Foreign key references non-existent source table: " + fk.getSourceTable(), 
                fk.getName()));
            return;
        }
        
        // Check if target table exists
        boolean targetTableExists = schema.getTables().stream()
            .anyMatch(t -> t.getName().equals(fk.getTargetTable()));
        if (!targetTableExists) {
            issues.add(new ConstraintIssue("ERROR", 
                "Foreign key references non-existent target table: " + fk.getTargetTable(), 
                fk.getName()));
            return;
        }
        
        // Check if source column exists
        DatabaseMetadata.TableMetadata sourceTable = schema.getTables().stream()
            .filter(t -> t.getName().equals(fk.getSourceTable()))
            .findFirst()
            .orElse(null);
        
        if (sourceTable != null) {
            boolean sourceColumnExists = sourceTable.getColumns().stream()
                .anyMatch(c -> c.getName().equals(fk.getSourceColumn()));
            if (!sourceColumnExists) {
                issues.add(new ConstraintIssue("ERROR", 
                    "Foreign key references non-existent source column: " + fk.getSourceColumn(), 
                    fk.getName()));
            }
        }
        
        // Check if target column exists
        DatabaseMetadata.TableMetadata targetTable = schema.getTables().stream()
            .filter(t -> t.getName().equals(fk.getTargetTable()))
            .findFirst()
            .orElse(null);
        
        if (targetTable != null) {
            boolean targetColumnExists = targetTable.getColumns().stream()
                .anyMatch(c -> c.getName().equals(fk.getTargetColumn()));
            if (!targetColumnExists) {
                issues.add(new ConstraintIssue("ERROR", 
                    "Foreign key references non-existent target column: " + fk.getTargetColumn(), 
                    fk.getName()));
            }
        }
    }
    
    private void validateIndex(DatabaseMetadata schema, DatabaseMetadata.IndexMetadata index, 
                              List<ConstraintIssue> issues) {
        // Check if table exists
        DatabaseMetadata.TableMetadata table = schema.getTables().stream()
            .filter(t -> t.getName().equals(index.getTableName()))
            .findFirst()
            .orElse(null);
        
        if (table == null) {
            issues.add(new ConstraintIssue("ERROR", 
                "Index references non-existent table: " + index.getTableName(), 
                index.getName()));
            return;
        }
        
        // Check if all columns exist
        for (String columnName : index.getColumns()) {
            boolean columnExists = table.getColumns().stream()
                .anyMatch(c -> c.getName().equals(columnName));
            if (!columnExists) {
                issues.add(new ConstraintIssue("ERROR", 
                    "Index references non-existent column: " + columnName, 
                    index.getName()));
            }
        }
    }
    
    private void validatePrimaryKeys(DatabaseMetadata schema, DatabaseMetadata.TableMetadata table, 
                                    List<ConstraintIssue> issues) {
        if (table.getPrimaryKeys() == null || table.getPrimaryKeys().isEmpty()) {
            return;
        }
        
        for (String pkColumn : table.getPrimaryKeys()) {
            boolean columnExists = table.getColumns().stream()
                .anyMatch(c -> c.getName().equals(pkColumn));
            if (!columnExists) {
                issues.add(new ConstraintIssue("ERROR", 
                    "Primary key references non-existent column: " + pkColumn, 
                    table.getName()));
            }
        }
    }
}
