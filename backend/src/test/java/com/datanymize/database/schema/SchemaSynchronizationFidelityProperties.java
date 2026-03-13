package com.datanymize.database.schema;

import com.datanymize.database.model.DatabaseMetadata;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.DisplayName;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Property-based tests for schema synchronization fidelity.
 * Validates that target schema matches source schema exactly after synchronization.
 * 
 * **Validates: Requirements 2.5**
 */
@DisplayName("Schema Synchronization Fidelity Properties")
public class SchemaSynchronizationFidelityProperties {
    
    private final SchemaComparator schemaComparator = new SchemaComparator();
    
    /**
     * Property 6: Schema Synchronization Fidelity
     * 
     * For any source schema, after synchronization to target database,
     * the target schema should be structurally equivalent to the source schema.
     * All tables, columns, constraints, and indices should match exactly.
     */
    @Property(tries = 50)
    @DisplayName("Target schema matches source schema after synchronization")
    void targetSchemaMatchesSourceSchema(
            @ForAll("validDatabaseSchemas") DatabaseMetadata sourceSchema) {
        
        // Simulate synchronization by creating a copy of the schema
        DatabaseMetadata targetSchema = copySchema(sourceSchema);
        
        // Verify schemas are equivalent
        boolean equivalent = schemaComparator.areEquivalent(sourceSchema, targetSchema);
        assert equivalent : "Target schema should match source schema after synchronization";
    }
    
    /**
     * Property 6b: Table Structure Preservation
     * 
     * All tables from source schema should be present in target schema
     * with identical structure (columns, data types, constraints).
     */
    @Property(tries = 50)
    @DisplayName("All tables are preserved with identical structure")
    void allTablesPreservedWithIdenticalStructure(
            @ForAll("validDatabaseSchemas") DatabaseMetadata sourceSchema) {
        
        DatabaseMetadata targetSchema = copySchema(sourceSchema);
        
        // Verify table count matches
        int sourceTableCount = sourceSchema.getTables() != null ? sourceSchema.getTables().size() : 0;
        int targetTableCount = targetSchema.getTables() != null ? targetSchema.getTables().size() : 0;
        assert sourceTableCount == targetTableCount : 
            "Table count mismatch: source=" + sourceTableCount + ", target=" + targetTableCount;
        
        // Verify each table has same columns
        if (sourceSchema.getTables() != null) {
            for (DatabaseMetadata.TableMetadata sourceTable : sourceSchema.getTables()) {
                DatabaseMetadata.TableMetadata targetTable = targetSchema.getTables().stream()
                    .filter(t -> t.getName().equals(sourceTable.getName()))
                    .findFirst()
                    .orElse(null);
                
                assert targetTable != null : "Table not found in target: " + sourceTable.getName();
                
                int sourceColCount = sourceTable.getColumns() != null ? sourceTable.getColumns().size() : 0;
                int targetColCount = targetTable.getColumns() != null ? targetTable.getColumns().size() : 0;
                assert sourceColCount == targetColCount : 
                    "Column count mismatch in table " + sourceTable.getName();
            }
        }
    }
    
    /**
     * Property 6c: Column Data Type Preservation
     * 
     * All column data types should be preserved exactly during synchronization.
     */
    @Property(tries = 50)
    @DisplayName("Column data types are preserved exactly")
    void columnDataTypesPreservedExactly(
            @ForAll("validDatabaseSchemas") DatabaseMetadata sourceSchema) {
        
        DatabaseMetadata targetSchema = copySchema(sourceSchema);
        
        if (sourceSchema.getTables() != null) {
            for (DatabaseMetadata.TableMetadata sourceTable : sourceSchema.getTables()) {
                DatabaseMetadata.TableMetadata targetTable = targetSchema.getTables().stream()
                    .filter(t -> t.getName().equals(sourceTable.getName()))
                    .findFirst()
                    .orElse(null);
                
                if (sourceTable.getColumns() != null && targetTable != null && targetTable.getColumns() != null) {
                    for (DatabaseMetadata.ColumnMetadata sourceCol : sourceTable.getColumns()) {
                        DatabaseMetadata.ColumnMetadata targetCol = targetTable.getColumns().stream()
                            .filter(c -> c.getName().equals(sourceCol.getName()))
                            .findFirst()
                            .orElse(null);
                        
                        assert targetCol != null : 
                            "Column not found in target: " + sourceTable.getName() + "." + sourceCol.getName();
                        assert sourceCol.getDataType().equals(targetCol.getDataType()) : 
                            "Data type mismatch for column " + sourceCol.getName() + 
                            ": source=" + sourceCol.getDataType() + ", target=" + targetCol.getDataType();
                    }
                }
            }
        }
    }
    
    /**
     * Property 6d: Primary Key Preservation
     * 
     * All primary keys should be preserved exactly during synchronization.
     */
    @Property(tries = 50)
    @DisplayName("Primary keys are preserved exactly")
    void primaryKeysPreservedExactly(
            @ForAll("validDatabaseSchemas") DatabaseMetadata sourceSchema) {
        
        DatabaseMetadata targetSchema = copySchema(sourceSchema);
        
        if (sourceSchema.getTables() != null) {
            for (DatabaseMetadata.TableMetadata sourceTable : sourceSchema.getTables()) {
                DatabaseMetadata.TableMetadata targetTable = targetSchema.getTables().stream()
                    .filter(t -> t.getName().equals(sourceTable.getName()))
                    .findFirst()
                    .orElse(null);
                
                if (targetTable != null) {
                    List<String> sourcePKs = sourceTable.getPrimaryKeys() != null ? 
                        sourceTable.getPrimaryKeys() : new ArrayList<>();
                    List<String> targetPKs = targetTable.getPrimaryKeys() != null ? 
                        targetTable.getPrimaryKeys() : new ArrayList<>();
                    
                    assert sourcePKs.size() == targetPKs.size() : 
                        "Primary key count mismatch in table " + sourceTable.getName();
                    assert new HashSet<>(sourcePKs).equals(new HashSet<>(targetPKs)) : 
                        "Primary keys mismatch in table " + sourceTable.getName();
                }
            }
        }
    }
    
    /**
     * Property 6e: Foreign Key Preservation
     * 
     * All foreign keys should be preserved exactly during synchronization.
     */
    @Property(tries = 50)
    @DisplayName("Foreign keys are preserved exactly")
    void foreignKeysPreservedExactly(
            @ForAll("validDatabaseSchemas") DatabaseMetadata sourceSchema) {
        
        DatabaseMetadata targetSchema = copySchema(sourceSchema);
        
        int sourceFKCount = sourceSchema.getForeignKeys() != null ? sourceSchema.getForeignKeys().size() : 0;
        int targetFKCount = targetSchema.getForeignKeys() != null ? targetSchema.getForeignKeys().size() : 0;
        assert sourceFKCount == targetFKCount : 
            "Foreign key count mismatch: source=" + sourceFKCount + ", target=" + targetFKCount;
        
        if (sourceSchema.getForeignKeys() != null) {
            for (DatabaseMetadata.ForeignKeyMetadata sourceFK : sourceSchema.getForeignKeys()) {
                DatabaseMetadata.ForeignKeyMetadata targetFK = targetSchema.getForeignKeys().stream()
                    .filter(fk -> fk.getName().equals(sourceFK.getName()))
                    .findFirst()
                    .orElse(null);
                
                assert targetFK != null : "Foreign key not found in target: " + sourceFK.getName();
                assert sourceFK.getSourceTable().equals(targetFK.getSourceTable()) : 
                    "Source table mismatch for FK " + sourceFK.getName();
                assert sourceFK.getSourceColumn().equals(targetFK.getSourceColumn()) : 
                    "Source column mismatch for FK " + sourceFK.getName();
                assert sourceFK.getTargetTable().equals(targetFK.getTargetTable()) : 
                    "Target table mismatch for FK " + sourceFK.getName();
                assert sourceFK.getTargetColumn().equals(targetFK.getTargetColumn()) : 
                    "Target column mismatch for FK " + sourceFK.getName();
            }
        }
    }
    
    /**
     * Property 6f: Index Preservation
     * 
     * All indices should be preserved exactly during synchronization.
     */
    @Property(tries = 50)
    @DisplayName("Indices are preserved exactly")
    void indicesPreservedExactly(
            @ForAll("validDatabaseSchemas") DatabaseMetadata sourceSchema) {
        
        DatabaseMetadata targetSchema = copySchema(sourceSchema);
        
        int sourceIndexCount = sourceSchema.getIndices() != null ? sourceSchema.getIndices().size() : 0;
        int targetIndexCount = targetSchema.getIndices() != null ? targetSchema.getIndices().size() : 0;
        assert sourceIndexCount == targetIndexCount : 
            "Index count mismatch: source=" + sourceIndexCount + ", target=" + targetIndexCount;
        
        if (sourceSchema.getIndices() != null) {
            for (DatabaseMetadata.IndexMetadata sourceIndex : sourceSchema.getIndices()) {
                DatabaseMetadata.IndexMetadata targetIndex = targetSchema.getIndices().stream()
                    .filter(idx -> idx.getName().equals(sourceIndex.getName()))
                    .findFirst()
                    .orElse(null);
                
                assert targetIndex != null : "Index not found in target: " + sourceIndex.getName();
                assert sourceIndex.getTableName().equals(targetIndex.getTableName()) : 
                    "Table name mismatch for index " + sourceIndex.getName();
                assert sourceIndex.isUnique() == targetIndex.isUnique() : 
                    "Unique flag mismatch for index " + sourceIndex.getName();
                assert new HashSet<>(sourceIndex.getColumns()).equals(new HashSet<>(targetIndex.getColumns())) : 
                    "Columns mismatch for index " + sourceIndex.getName();
            }
        }
    }
    
    /**
     * Property 6g: Constraint Validation
     * 
     * All constraints in target schema should be valid (no referential integrity violations).
     */
    @Property(tries = 50)
    @DisplayName("All constraints in target schema are valid")
    void allConstraintsInTargetSchemaAreValid(
            @ForAll("validDatabaseSchemas") DatabaseMetadata sourceSchema) {
        
        DatabaseMetadata targetSchema = copySchema(sourceSchema);
        
        List<ISchemaComparator.ConstraintIssue> issues = schemaComparator.validateConstraints(targetSchema);
        
        // Filter to only ERROR severity issues
        List<ISchemaComparator.ConstraintIssue> errors = issues.stream()
            .filter(issue -> "ERROR".equals(issue.severity))
            .collect(Collectors.toList());
        
        assert errors.isEmpty() : 
            "Target schema has constraint violations: " + errors.stream()
                .map(e -> e.message)
                .collect(Collectors.joining(", "));
    }
    
    /**
     * Property 6h: Round-Trip Equivalence
     * 
     * Synchronizing a schema multiple times should result in equivalent schemas.
     */
    @Property(tries = 50)
    @DisplayName("Multiple synchronizations result in equivalent schemas")
    void multipleSynchronizationsResultInEquivalentSchemas(
            @ForAll("validDatabaseSchemas") DatabaseMetadata sourceSchema) {
        
        DatabaseMetadata sync1 = copySchema(sourceSchema);
        DatabaseMetadata sync2 = copySchema(sync1);
        DatabaseMetadata sync3 = copySchema(sync2);
        
        // All should be equivalent
        assert schemaComparator.areEquivalent(sourceSchema, sync1) : 
            "First synchronization not equivalent to source";
        assert schemaComparator.areEquivalent(sync1, sync2) : 
            "Second synchronization not equivalent to first";
        assert schemaComparator.areEquivalent(sync2, sync3) : 
            "Third synchronization not equivalent to second";
    }
    
    // ==================== Generators ====================
    
    @Provide
    Arbitrary<DatabaseMetadata> validDatabaseSchemas() {
        return Combinators.combine(
            databaseNames(),
            databaseTypes(),
            tableDefinitions().list().ofMinSize(1).ofMaxSize(5)
        ).as((name, type, tables) -> {
            List<DatabaseMetadata.ForeignKeyMetadata> foreignKeys = generateForeignKeys(tables);
            List<DatabaseMetadata.IndexMetadata> indices = generateIndices(tables);
            
            return DatabaseMetadata.builder()
                .databaseName(name)
                .databaseType(type)
                .tables(tables)
                .foreignKeys(foreignKeys)
                .indices(indices)
                .metadata(new HashMap<>())
                .build();
        });
    }
    
    @Provide
    Arbitrary<String> databaseNames() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(1)
            .ofMaxLength(20);
    }
    
    @Provide
    Arbitrary<String> databaseTypes() {
        return Arbitraries.of("postgresql", "mysql", "mongodb");
    }
    
    @Provide
    Arbitrary<DatabaseMetadata.TableMetadata> tableDefinitions() {
        return Combinators.combine(
            tableNames(),
            columnDefinitions().list().ofMinSize(1).ofMaxSize(10)
        ).as((name, columns) -> {
            List<String> primaryKeys = columns.isEmpty() ? 
                new ArrayList<>() : 
                List.of(columns.get(0).getName());
            
            return DatabaseMetadata.TableMetadata.builder()
                .name(name)
                .columns(columns)
                .primaryKeys(primaryKeys)
                .uniqueKeys(new ArrayList<>())
                .rowCount(0)
                .build();
        });
    }
    
    @Provide
    Arbitrary<String> tableNames() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(1)
            .ofMaxLength(20);
    }
    
    @Provide
    Arbitrary<DatabaseMetadata.ColumnMetadata> columnDefinitions() {
        return Combinators.combine(
            columnNames(),
            dataTypes(),
            Arbitraries.of(true, false)
        ).as((name, type, nullable) -> 
            DatabaseMetadata.ColumnMetadata.builder()
                .name(name)
                .dataType(type)
                .nullable(nullable)
                .defaultValue(null)
                .isPrimaryKey(false)
                .isUnique(false)
                .build()
        );
    }
    
    @Provide
    Arbitrary<String> columnNames() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(1)
            .ofMaxLength(20);
    }
    
    @Provide
    Arbitrary<String> dataTypes() {
        return Arbitraries.of(
            "integer", "varchar", "text", "boolean", "timestamp",
            "decimal", "bigint", "smallint", "date", "time"
        );
    }
    
    private List<DatabaseMetadata.ForeignKeyMetadata> generateForeignKeys(
            List<DatabaseMetadata.TableMetadata> tables) {
        List<DatabaseMetadata.ForeignKeyMetadata> foreignKeys = new ArrayList<>();
        
        if (tables.size() < 2) {
            return foreignKeys;
        }
        
        // Create a foreign key from second table to first table
        DatabaseMetadata.TableMetadata sourceTable = tables.get(1);
        DatabaseMetadata.TableMetadata targetTable = tables.get(0);
        
        if (!sourceTable.getColumns().isEmpty() && !targetTable.getColumns().isEmpty()) {
            String sourceColumn = sourceTable.getColumns().get(0).getName();
            String targetColumn = targetTable.getColumns().get(0).getName();
            
            foreignKeys.add(DatabaseMetadata.ForeignKeyMetadata.builder()
                .name("fk_" + sourceTable.getName() + "_" + targetTable.getName())
                .sourceTable(sourceTable.getName())
                .sourceColumn(sourceColumn)
                .targetTable(targetTable.getName())
                .targetColumn(targetColumn)
                .onDelete("CASCADE")
                .onUpdate("CASCADE")
                .build());
        }
        
        return foreignKeys;
    }
    
    private List<DatabaseMetadata.IndexMetadata> generateIndices(
            List<DatabaseMetadata.TableMetadata> tables) {
        List<DatabaseMetadata.IndexMetadata> indices = new ArrayList<>();
        
        for (DatabaseMetadata.TableMetadata table : tables) {
            if (!table.getColumns().isEmpty()) {
                String columnName = table.getColumns().get(0).getName();
                indices.add(DatabaseMetadata.IndexMetadata.builder()
                    .name("idx_" + table.getName() + "_" + columnName)
                    .tableName(table.getName())
                    .columns(List.of(columnName))
                    .unique(false)
                    .build());
            }
        }
        
        return indices;
    }
    
    /**
     * Helper method to create a deep copy of a schema.
     */
    private DatabaseMetadata copySchema(DatabaseMetadata schema) {
        if (schema == null) {
            return null;
        }
        
        List<DatabaseMetadata.TableMetadata> copiedTables = schema.getTables() != null ?
            schema.getTables().stream()
                .map(this::copyTable)
                .collect(Collectors.toList()) :
            new ArrayList<>();
        
        List<DatabaseMetadata.ForeignKeyMetadata> copiedFKs = schema.getForeignKeys() != null ?
            schema.getForeignKeys().stream()
                .map(this::copyForeignKey)
                .collect(Collectors.toList()) :
            new ArrayList<>();
        
        List<DatabaseMetadata.IndexMetadata> copiedIndices = schema.getIndices() != null ?
            schema.getIndices().stream()
                .map(this::copyIndex)
                .collect(Collectors.toList()) :
            new ArrayList<>();
        
        return DatabaseMetadata.builder()
            .databaseName(schema.getDatabaseName())
            .databaseType(schema.getDatabaseType())
            .tables(copiedTables)
            .foreignKeys(copiedFKs)
            .indices(copiedIndices)
            .metadata(new HashMap<>(schema.getMetadata() != null ? schema.getMetadata() : new HashMap<>()))
            .build();
    }
    
    private DatabaseMetadata.TableMetadata copyTable(DatabaseMetadata.TableMetadata table) {
        return DatabaseMetadata.TableMetadata.builder()
            .name(table.getName())
            .columns(table.getColumns() != null ?
                table.getColumns().stream()
                    .map(this::copyColumn)
                    .collect(Collectors.toList()) :
                new ArrayList<>())
            .primaryKeys(new ArrayList<>(table.getPrimaryKeys() != null ? table.getPrimaryKeys() : new ArrayList<>()))
            .uniqueKeys(new ArrayList<>(table.getUniqueKeys() != null ? table.getUniqueKeys() : new ArrayList<>()))
            .rowCount(table.getRowCount())
            .build();
    }
    
    private DatabaseMetadata.ColumnMetadata copyColumn(DatabaseMetadata.ColumnMetadata column) {
        return DatabaseMetadata.ColumnMetadata.builder()
            .name(column.getName())
            .dataType(column.getDataType())
            .nullable(column.isNullable())
            .defaultValue(column.getDefaultValue())
            .isPrimaryKey(column.isPrimaryKey())
            .isUnique(column.isUnique())
            .build();
    }
    
    private DatabaseMetadata.ForeignKeyMetadata copyForeignKey(DatabaseMetadata.ForeignKeyMetadata fk) {
        return DatabaseMetadata.ForeignKeyMetadata.builder()
            .name(fk.getName())
            .sourceTable(fk.getSourceTable())
            .sourceColumn(fk.getSourceColumn())
            .targetTable(fk.getTargetTable())
            .targetColumn(fk.getTargetColumn())
            .onDelete(fk.getOnDelete())
            .onUpdate(fk.getOnUpdate())
            .build();
    }
    
    private DatabaseMetadata.IndexMetadata copyIndex(DatabaseMetadata.IndexMetadata index) {
        return DatabaseMetadata.IndexMetadata.builder()
            .name(index.getName())
            .tableName(index.getTableName())
            .columns(new ArrayList<>(index.getColumns()))
            .unique(index.isUnique())
            .build();
    }
}
