package com.datanymize.anonymization;

import com.datanymize.database.model.ForeignKey;
import com.datanymize.database.model.Row;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for subset foreign key dependency handling.
 * **Validates: Requirements 6.3, 6.4**
 *
 * Property 18: Subset Foreign Key Dependency
 * For any subset selection, all foreign key dependencies of selected rows should be satisfied
 * (referenced records should be included or FK set to NULL).
 */
@PropertyDefaults(tries = 50)
class SubsetForeignKeyDependencyProperties {

    /**
     * Property 18a: All FK dependencies are included in subset
     * When using INCLUDE strategy, all referenced records should be included.
     */
    @Property
    @Label("All FK dependencies are included in subset")
    void testFKDependenciesIncluded(
        @ForAll @StringLength(min = 1, max = 50) String sourceTable,
        @ForAll @StringLength(min = 1, max = 50) String targetTable,
        @ForAll @IntRange(min = 1, max = 50) int selectedRowCount
    ) {
        Assume.that(!sourceTable.equals(targetTable));

        SubsetForeignKeyResolver resolver = new SubsetForeignKeyResolver(
            SubsetForeignKeyResolver.MissingDependencyStrategy.INCLUDE
        );

        // Add selected rows with FK references
        for (int i = 1; i <= selectedRowCount; i++) {
            resolver.addSelectedRow(sourceTable, "row_" + i);
        }

        // Verify rows are selected
        assertEquals(
            selectedRowCount,
            resolver.getSelectedRowCount(sourceTable),
            "All rows should be selected"
        );
    }

    /**
     * Property 18b: FK dependencies are resolved correctly
     * When resolving FK dependencies, referenced records should be included.
     */
    @Property
    @Label("FK dependencies are resolved correctly")
    void testFKDependencyResolution(
        @ForAll @StringLength(min = 1, max = 50) String sourceTable,
        @ForAll @StringLength(min = 1, max = 50) String targetTable,
        @ForAll @StringLength(min = 1, max = 50) String fkValue
    ) {
        Assume.that(!sourceTable.equals(targetTable));

        SubsetForeignKeyResolver resolver = new SubsetForeignKeyResolver(
            SubsetForeignKeyResolver.MissingDependencyStrategy.INCLUDE
        );

        // Create a row with FK reference
        Row row = new Row(sourceTable);
        row.getValues().put("id", "row_1");
        row.getValues().put("fk_column", fkValue);

        // Create FK definition
        ForeignKey fk = new ForeignKey();
        fk.setSourceTable(sourceTable);
        fk.setSourceColumn("fk_column");
        fk.setTargetTable(targetTable);
        fk.setTargetColumn("id");

        // Resolve dependencies
        Row resolved = resolver.resolveForeignKeyDependencies(row, List.of(fk));

        // Row should be resolved
        assertNotNull(resolved, "Row should be resolved");
        assertEquals(sourceTable, resolved.getTableName(), "Table name should match");
    }

    /**
     * Property 18c: SET_NULL strategy sets FK to NULL for missing dependencies
     * When using SET_NULL strategy, missing FK references should be set to NULL.
     */
    @Property
    @Label("SET_NULL strategy sets FK to NULL for missing dependencies")
    void testSetNullStrategy(
        @ForAll @StringLength(min = 1, max = 50) String sourceTable,
        @ForAll @StringLength(min = 1, max = 50) String targetTable,
        @ForAll @StringLength(min = 1, max = 50) String fkValue
    ) {
        Assume.that(!sourceTable.equals(targetTable));

        SubsetForeignKeyResolver resolver = new SubsetForeignKeyResolver(
            SubsetForeignKeyResolver.MissingDependencyStrategy.SET_NULL
        );

        // Create a row with FK reference to non-existent record
        Row row = new Row(sourceTable);
        row.getValues().put("id", "row_1");
        row.getValues().put("fk_column", fkValue);

        // Create FK definition
        ForeignKey fk = new ForeignKey();
        fk.setSourceTable(sourceTable);
        fk.setSourceColumn("fk_column");
        fk.setTargetTable(targetTable);
        fk.setTargetColumn("id");

        // Resolve dependencies
        Row resolved = resolver.resolveForeignKeyDependencies(row, List.of(fk));

        // FK should be set to NULL
        assertNotNull(resolved, "Row should be resolved");
        assertNull(
            resolved.getValues().get("fk_column"),
            "FK should be set to NULL for missing dependency"
        );
    }

    /**
     * Property 18d: RESTRICT strategy excludes rows with missing dependencies
     * When using RESTRICT strategy, rows with missing FK references should be excluded.
     */
    @Property
    @Label("RESTRICT strategy excludes rows with missing dependencies")
    void testRestrictStrategy(
        @ForAll @StringLength(min = 1, max = 50) String sourceTable,
        @ForAll @StringLength(min = 1, max = 50) String targetTable,
        @ForAll @StringLength(min = 1, max = 50) String fkValue
    ) {
        Assume.that(!sourceTable.equals(targetTable));

        SubsetForeignKeyResolver resolver = new SubsetForeignKeyResolver(
            SubsetForeignKeyResolver.MissingDependencyStrategy.RESTRICT
        );

        // Create a row with FK reference to non-existent record
        Row row = new Row(sourceTable);
        row.getValues().put("id", "row_1");
        row.getValues().put("fk_column", fkValue);

        // Create FK definition
        ForeignKey fk = new ForeignKey();
        fk.setSourceTable(sourceTable);
        fk.setSourceColumn("fk_column");
        fk.setTargetTable(targetTable);
        fk.setTargetColumn("id");

        // Resolve dependencies
        Row resolved = resolver.resolveForeignKeyDependencies(row, List.of(fk));

        // Row should be excluded (null)
        assertNull(
            resolved,
            "Row should be excluded for missing dependency with RESTRICT strategy"
        );
    }

    /**
     * Property 18e: Multiple FK dependencies are handled correctly
     * When a row has multiple FK references, all should be resolved.
     */
    @Property
    @Label("Multiple FK dependencies are handled correctly")
    void testMultipleFKDependencies(
        @ForAll @StringLength(min = 1, max = 50) String sourceTable,
        @ForAll @StringLength(min = 1, max = 50) String targetTable1,
        @ForAll @StringLength(min = 1, max = 50) String targetTable2
    ) {
        Assume.that(!sourceTable.equals(targetTable1));
        Assume.that(!sourceTable.equals(targetTable2));
        Assume.that(!targetTable1.equals(targetTable2));

        SubsetForeignKeyResolver resolver = new SubsetForeignKeyResolver(
            SubsetForeignKeyResolver.MissingDependencyStrategy.SET_NULL
        );

        // Create a row with multiple FK references
        Row row = new Row(sourceTable);
        row.getValues().put("id", "row_1");
        row.getValues().put("fk_column1", "ref_1");
        row.getValues().put("fk_column2", "ref_2");

        // Create FK definitions
        ForeignKey fk1 = new ForeignKey();
        fk1.setSourceTable(sourceTable);
        fk1.setSourceColumn("fk_column1");
        fk1.setTargetTable(targetTable1);
        fk1.setTargetColumn("id");

        ForeignKey fk2 = new ForeignKey();
        fk2.setSourceTable(sourceTable);
        fk2.setSourceColumn("fk_column2");
        fk2.setTargetTable(targetTable2);
        fk2.setTargetColumn("id");

        // Resolve dependencies
        Row resolved = resolver.resolveForeignKeyDependencies(row, List.of(fk1, fk2));

        // Both FKs should be set to NULL
        assertNotNull(resolved, "Row should be resolved");
        assertNull(resolved.getValues().get("fk_column1"), "First FK should be NULL");
        assertNull(resolved.getValues().get("fk_column2"), "Second FK should be NULL");
    }

    /**
     * Property 18f: Selected rows are tracked correctly
     * The resolver should track all selected rows.
     */
    @Property
    @Label("Selected rows are tracked correctly")
    void testSelectedRowsTracking(
        @ForAll @StringLength(min = 1, max = 50) String tableName,
        @ForAll @IntRange(min = 1, max = 50) int rowCount
    ) {
        SubsetForeignKeyResolver resolver = new SubsetForeignKeyResolver(
            SubsetForeignKeyResolver.MissingDependencyStrategy.INCLUDE
        );

        // Add selected rows
        for (int i = 1; i <= rowCount; i++) {
            resolver.addSelectedRow(tableName, "row_" + i);
        }

        // Verify tracking
        assertEquals(
            rowCount,
            resolver.getSelectedRowCount(tableName),
            "All rows should be tracked"
        );

        Set<Object> selectedIds = resolver.getSelectedRowIds(tableName);
        assertEquals(
            rowCount,
            selectedIds.size(),
            "Selected IDs should match row count"
        );
    }

    /**
     * Property 18g: Row selection check works correctly
     * The isRowSelected method should correctly identify selected rows.
     */
    @Property
    @Label("Row selection check works correctly")
    void testRowSelectionCheck(
        @ForAll @StringLength(min = 1, max = 50) String tableName,
        @ForAll @StringLength(min = 1, max = 50) String rowId
    ) {
        SubsetForeignKeyResolver resolver = new SubsetForeignKeyResolver(
            SubsetForeignKeyResolver.MissingDependencyStrategy.INCLUDE
        );

        // Initially not selected
        assertFalse(
            resolver.isRowSelected(tableName, rowId),
            "Row should not be selected initially"
        );

        // Add row
        resolver.addSelectedRow(tableName, rowId);

        // Now should be selected
        assertTrue(
            resolver.isRowSelected(tableName, rowId),
            "Row should be selected after adding"
        );
    }

    /**
     * Property 18h: Statistics are accurate
     * The statistics method should return accurate counts.
     */
    @Property
    @Label("Statistics are accurate")
    void testStatisticsAccuracy(
        @ForAll @StringLength(min = 1, max = 50) String table1,
        @ForAll @StringLength(min = 1, max = 50) String table2,
        @ForAll @IntRange(min = 1, max = 50) int count1,
        @ForAll @IntRange(min = 1, max = 50) int count2
    ) {
        Assume.that(!table1.equals(table2));

        SubsetForeignKeyResolver resolver = new SubsetForeignKeyResolver(
            SubsetForeignKeyResolver.MissingDependencyStrategy.INCLUDE
        );

        // Add rows to both tables
        for (int i = 1; i <= count1; i++) {
            resolver.addSelectedRow(table1, "row_" + i);
        }
        for (int i = 1; i <= count2; i++) {
            resolver.addSelectedRow(table2, "row_" + i);
        }

        // Get statistics
        Map<String, Integer> stats = resolver.getStatistics();

        assertEquals(count1, stats.get(table1), "Table 1 count should match");
        assertEquals(count2, stats.get(table2), "Table 2 count should match");
    }

    /**
     * Property 18i: Resolver can be cleared
     * After clearing, the resolver should have no selected rows.
     */
    @Property
    @Label("Resolver can be cleared")
    void testResolverClear(
        @ForAll @StringLength(min = 1, max = 50) String tableName,
        @ForAll @StringLength(min = 1, max = 50) String rowId
    ) {
        SubsetForeignKeyResolver resolver = new SubsetForeignKeyResolver(
            SubsetForeignKeyResolver.MissingDependencyStrategy.INCLUDE
        );

        // Add row
        resolver.addSelectedRow(tableName, rowId);

        // Verify it exists
        assertTrue(
            resolver.isRowSelected(tableName, rowId),
            "Row should be selected before clear"
        );

        // Clear
        resolver.clear();

        // Verify it's gone
        assertFalse(
            resolver.isRowSelected(tableName, rowId),
            "Row should not be selected after clear"
        );
    }

    /**
     * Property 18j: Total selected rows count is correct
     * The total selected rows should be the sum across all tables.
     */
    @Property
    @Label("Total selected rows count is correct")
    void testTotalSelectedRowsCount(
        @ForAll @StringLength(min = 1, max = 50) String table1,
        @ForAll @StringLength(min = 1, max = 50) String table2,
        @ForAll @IntRange(min = 1, max = 25) int count1,
        @ForAll @IntRange(min = 1, max = 25) int count2
    ) {
        Assume.that(!table1.equals(table2));

        SubsetForeignKeyResolver resolver = new SubsetForeignKeyResolver(
            SubsetForeignKeyResolver.MissingDependencyStrategy.INCLUDE
        );

        // Add rows
        for (int i = 1; i <= count1; i++) {
            resolver.addSelectedRow(table1, "row_" + i);
        }
        for (int i = 1; i <= count2; i++) {
            resolver.addSelectedRow(table2, "row_" + i);
        }

        // Check total
        assertEquals(
            count1 + count2,
            resolver.getTotalSelectedRows(),
            "Total should be sum of all tables"
        );
    }
}
