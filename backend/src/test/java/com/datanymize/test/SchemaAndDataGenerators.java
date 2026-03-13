package com.datanymize.test;

import com.datanymize.database.model.Column;
import com.datanymize.database.model.Table;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Specialized generators for database schemas and data.
 * 
 * Provides property-based test generators for:
 * - Database schemas (tables, columns, constraints)
 * - Column definitions with various data types
 * - Sample data for testing
 * - Foreign key relationships
 * - Indices and constraints
 * 
 * These generators are used for schema extraction, synchronization,
 * and data anonymization tests.
 */
public class SchemaAndDataGenerators extends BasePropertyTest {
    
    // ============ Column Generators ============
    
    /**
     * Generator for valid column definitions.
     * 
     * Generates columns with:
     * - Valid column names
     * - Various SQL data types
     * - Nullable/non-nullable options
     * - Default values (optional)
     * - Primary key/unique constraints (optional)
     * 
     * @return Arbitrary<Column> for valid column definitions
     */
    public Arbitrary<Column> validColumns() {
        return Combinators.combine(
                columnNames(),
                sqlDataTypes(),
                Arbitraries.of(true, false),
                Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .ofMinLength(0)
                        .ofMaxLength(20)
                        .optional(),
                Arbitraries.of(true, false),
                Arbitraries.of(true, false)
        ).as((name, dataType, nullable, defaultValue, isPK, isUnique) ->
                Column.builder()
                        .name(name)
                        .dataType(dataType)
                        .nullable(nullable)
                        .defaultValue(defaultValue.orElse(null))
                        .isPrimaryKey(isPK)
                        .isUnique(isUnique)
                        .build()
        );
    }
    
    /**
     * Generator for primary key columns.
     * 
     * Generates columns that are suitable as primary keys:
     * - Integer or UUID data types
     * - Non-nullable
     * - Unique constraint
     * 
     * @return Arbitrary<Column> for primary key columns
     */
    public Arbitrary<Column> primaryKeyColumns() {
        return Combinators.combine(
                columnNames(),
                Arbitraries.of("INT", "BIGINT", "UUID")
        ).as((name, dataType) ->
                Column.builder()
                        .name(name)
                        .dataType(dataType)
                        .nullable(false)
                        .isPrimaryKey(true)
                        .isUnique(true)
                        .build()
        );
    }
    
    /**
     * Generator for foreign key columns.
     * 
     * Generates columns that reference other tables:
     * - Integer or UUID data types
     * - Non-nullable (typically)
     * - References another table
     * 
     * @return Arbitrary<Column> for foreign key columns
     */
    public Arbitrary<Column> foreignKeyColumns() {
        return Combinators.combine(
                columnNames(),
                Arbitraries.of("INT", "BIGINT", "UUID"),
                Arbitraries.of(true, false)
        ).as((name, dataType, nullable) ->
                Column.builder()
                        .name(name)
                        .dataType(dataType)
                        .nullable(nullable)
                        .build()
        );
    }
    
    /**
     * Generator for PII columns (for testing PII detection).
     * 
     * Generates columns that contain personally identifiable information:
     * - Email columns
     * - Phone columns
     * - SSN columns
     * - Name columns
     * - Address columns
     * 
     * @return Arbitrary<Column> for PII columns
     */
    public Arbitrary<Column> piiColumns() {
        return Arbitraries.oneOf(
                // Email column
                Arbitraries.just(Column.builder()
                        .name("email")
                        .dataType("VARCHAR")
                        .nullable(false)
                        .build()),
                
                // Phone column
                Arbitraries.just(Column.builder()
                        .name("phone")
                        .dataType("VARCHAR")
                        .nullable(true)
                        .build()),
                
                // SSN column
                Arbitraries.just(Column.builder()
                        .name("ssn")
                        .dataType("VARCHAR")
                        .nullable(true)
                        .build()),
                
                // Name column
                Arbitraries.just(Column.builder()
                        .name("name")
                        .dataType("VARCHAR")
                        .nullable(false)
                        .build()),
                
                // Address column
                Arbitraries.just(Column.builder()
                        .name("address")
                        .dataType("TEXT")
                        .nullable(true)
                        .build()),
                
                // Credit card column
                Arbitraries.just(Column.builder()
                        .name("credit_card")
                        .dataType("VARCHAR")
                        .nullable(true)
                        .build())
        );
    }
    
    // ============ Table Generators ============
    
    /**
     * Generator for valid table definitions.
     * 
     * Generates tables with:
     * - Valid table names
     * - 1-10 columns
     * - At least one primary key
     * - Optional foreign keys
     * 
     * @return Arbitrary<Table> for valid table definitions
     */
    public Arbitrary<Table> validTables() {
        return Combinators.combine(
                tableNames(),
                validColumns()
                        .list()
                        .ofMinSize(1)
                        .ofMaxSize(10),
                Arbitraries.longs().between(0, 1000000)
        ).as((name, columns, rowCount) -> {
            // Ensure at least one column is a primary key
            if (columns.stream().noneMatch(Column::isPrimaryKey)) {
                columns.get(0).setPrimaryKey(true);
            }
            
            return Table.builder()
                    .name(name)
                    .columns(columns)
                    .primaryKeys(columns.stream()
                            .filter(Column::isPrimaryKey)
                            .map(Column::getName)
                            .toList())
                    .uniqueKeys(columns.stream()
                            .filter(Column::isUnique)
                            .map(Column::getName)
                            .toList())
                    .rowCount(rowCount)
                    .build();
        });
    }
    
    /**
     * Generator for simple tables (for quick tests).
     * 
     * Generates minimal tables with:
     * - 2-5 columns
     * - One primary key
     * - No foreign keys
     * 
     * @return Arbitrary<Table> for simple table definitions
     */
    public Arbitrary<Table> simpleTables() {
        return Combinators.combine(
                tableNames(),
                primaryKeyColumns(),
                validColumns()
                        .list()
                        .ofMinSize(1)
                        .ofMaxSize(4)
        ).as((name, pkColumn, otherColumns) -> {
            List<Column> allColumns = new ArrayList<>();
            allColumns.add(pkColumn);
            allColumns.addAll(otherColumns);
            
            return Table.builder()
                    .name(name)
                    .columns(allColumns)
                    .primaryKeys(List.of(pkColumn.getName()))
                    .rowCount(100)
                    .build();
        });
    }
    
    /**
     * Generator for tables with PII columns (for PII detection tests).
     * 
     * Generates tables containing personally identifiable information:
     * - Primary key column
     * - 1-3 PII columns
     * - Optional non-PII columns
     * 
     * @return Arbitrary<Table> for tables with PII columns
     */
    public Arbitrary<Table> tablesWithPII() {
        return Combinators.combine(
                tableNames(),
                primaryKeyColumns(),
                piiColumns()
                        .list()
                        .ofMinSize(1)
                        .ofMaxSize(3),
                validColumns()
                        .list()
                        .ofMinSize(0)
                        .ofMaxSize(2)
        ).as((name, pkColumn, piiColumns, otherColumns) -> {
            List<Column> allColumns = new ArrayList<>();
            allColumns.add(pkColumn);
            allColumns.addAll(piiColumns);
            allColumns.addAll(otherColumns);
            
            return Table.builder()
                    .name(name)
                    .columns(allColumns)
                    .primaryKeys(List.of(pkColumn.getName()))
                    .rowCount(1000)
                    .build();
        });
    }
    
    // ============ Sample Data Generators ============
    
    /**
     * Generator for sample data rows.
     * 
     * Generates maps representing database rows with:
     * - Column names as keys
     * - Appropriate data types as values
     * 
     * @return Arbitrary<Map<String, Object>> for sample data rows
     */
    public Arbitrary<Map<String, Object>> sampleDataRows() {
        return Combinators.combine(
                Arbitraries.integers().between(1, 1000000),
                emailAddresses(),
                phoneNumbers(),
                Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .ofMinLength(1)
                        .ofMaxLength(50),
                Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .ofMinLength(1)
                        .ofMaxLength(100)
        ).as((id, email, phone, name, address) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", id);
            row.put("email", email);
            row.put("phone", phone);
            row.put("name", name);
            row.put("address", address);
            return row;
        });
    }
    
    /**
     * Generator for PII sample data.
     * 
     * Generates realistic PII data for testing detection:
     * - Email addresses
     * - Phone numbers
     * - SSN-like strings
     * - Credit card-like strings
     * - Names
     * 
     * @return Arbitrary<String> for PII sample data
     */
    public Arbitrary<String> piiBatchData() {
        return Arbitraries.oneOf(
                emailAddresses(),
                phoneNumbers(),
                ssnLikeStrings(),
                creditCardLikeStrings(),
                Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .ofMinLength(1)
                        .ofMaxLength(50)
        );
    }
    
    /**
     * Generator for non-PII sample data.
     * 
     * Generates data that should not be detected as PII:
     * - Numbers
     * - Dates
     * - Generic strings
     * - Booleans
     * 
     * @return Arbitrary<String> for non-PII sample data
     */
    public Arbitrary<String> nonPIIBatchData() {
        return Arbitraries.oneOf(
                Arbitraries.integers().between(1, 1000000).map(String::valueOf),
                Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .withCharRange('0', '9')
                        .ofMinLength(1)
                        .ofMaxLength(20),
                Arbitraries.of("true", "false", "yes", "no", "active", "inactive")
        );
    }
    
    // ============ Constraint Generators ============
    
    /**
     * Generator for foreign key relationships.
     * 
     * Generates foreign key definitions with:
     * - Source table and column
     * - Target table and column
     * - ON DELETE and ON UPDATE actions
     * 
     * @return Arbitrary<Map<String, String>> for foreign key definitions
     */
    public Arbitrary<Map<String, String>> foreignKeyRelationships() {
        return Combinators.combine(
                tableNames(),
                columnNames(),
                tableNames(),
                columnNames(),
                Arbitraries.of("CASCADE", "SET NULL", "RESTRICT", "NO ACTION"),
                Arbitraries.of("CASCADE", "SET NULL", "RESTRICT", "NO ACTION")
        ).as((sourceTable, sourceCol, targetTable, targetCol, onDelete, onUpdate) -> {
            Map<String, String> fk = new HashMap<>();
            fk.put("sourceTable", sourceTable);
            fk.put("sourceColumn", sourceCol);
            fk.put("targetTable", targetTable);
            fk.put("targetColumn", targetCol);
            fk.put("onDelete", onDelete);
            fk.put("onUpdate", onUpdate);
            return fk;
        });
    }
    
    /**
     * Generator for index definitions.
     * 
     * Generates index definitions with:
     * - Index name
     * - Indexed columns
     * - Unique constraint option
     * 
     * @return Arbitrary<Map<String, Object>> for index definitions
     */
    public Arbitrary<Map<String, Object>> indexDefinitions() {
        return Combinators.combine(
                Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .ofMinLength(1)
                        .ofMaxLength(20),
                columnNames()
                        .list()
                        .ofMinSize(1)
                        .ofMaxSize(3),
                Arbitraries.of(true, false)
        ).as((indexName, columns, isUnique) -> {
            Map<String, Object> index = new HashMap<>();
            index.put("name", "idx_" + indexName);
            index.put("columns", columns);
            index.put("unique", isUnique);
            return index;
        });
    }
}
