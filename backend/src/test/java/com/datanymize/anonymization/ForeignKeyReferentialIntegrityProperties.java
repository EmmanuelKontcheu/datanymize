package com.datanymize.anonymization;

import com.datanymize.database.model.ForeignKey;
import com.datanymize.database.model.Row;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for foreign key referential integrity.
 * **Validates: Requirements 5.2, 5.3, 5.5**
 *
 * Property 15: Foreign Key Referential Integrity
 * For any foreign key column transformation, the transformed value should reference a valid record in the target table.
 *
 * Property 16: Cross-Table Determinism
 * For any deterministic transformation applied to the same value in different tables, the output should be identical.
 */
@PropertyDefaults(tries = 50)
class ForeignKeyReferentialIntegrityProperties {

    /**
     * Property 15a: Transformed FKs reference valid records
     * After transformation, all foreign key values should reference valid records in the target table.
     */
    @Property
    @Label("Transformed FKs reference valid records")
    void testTransformedFKsReferenceValidRecords(
        @ForAll @StringLength(min = 1, max = 50) String sourceTable,
        @ForAll @StringLength(min = 1, max = 50) String targetTable,
        @ForAll @StringLength(min = 1, max = 50) String fkColumn,
        @ForAll @IntRange(min = 1, max = 100) int recordCount
    ) {
        ForeignKeyHandler handler = new ForeignKeyHandler();

        // Record transformations for primary key in target table
        Set<Object> validTargetIds = new HashSet<>();
        for (int i = 1; i <= recordCount; i++) {
            Object originalId = "id_" + i;
            Object transformedId = "transformed_id_" + i;
            handler.recordTransformation(targetTable, "id", originalId, transformedId);
            validTargetIds.add(transformedId);
        }

        // Record transformations for foreign key in source table
        for (int i = 1; i <= recordCount; i++) {
            Object originalFkValue = "id_" + i;
            Object transformedFkValue = "transformed_id_" + i;
            handler.recordTransformation(sourceTable, fkColumn, originalFkValue, transformedFkValue);
        }

        // Verify all transformed FK values reference valid records
        Map<Object, Object> fkTransformations = handler.getColumnTransformations(sourceTable, fkColumn);
        for (Object transformedFkValue : fkTransformations.values()) {
            assertTrue(
                validTargetIds.contains(transformedFkValue),
                "Transformed FK value should reference valid record: " + transformedFkValue
            );
        }
    }

    /**
     * Property 15b: FK transformation mapping is consistent
     * The same original FK value should always transform to the same transformed value.
     */
    @Property
    @Label("FK transformation mapping is consistent")
    void testFKTransformationConsistency(
        @ForAll @StringLength(min = 1, max = 50) String tableName,
        @ForAll @StringLength(min = 1, max = 50) String columnName,
        @ForAll @StringLength(min = 1, max = 50) String originalValue
    ) {
        ForeignKeyHandler handler = new ForeignKeyHandler();

        // Record transformation
        Object transformedValue1 = "transformed_" + originalValue;
        handler.recordTransformation(tableName, columnName, originalValue, transformedValue1);

        // Retrieve transformation multiple times
        Object retrieved1 = handler.getTransformedValue(tableName, columnName, originalValue);
        Object retrieved2 = handler.getTransformedValue(tableName, columnName, originalValue);
        Object retrieved3 = handler.getTransformedValue(tableName, columnName, originalValue);

        // All retrievals should return the same value
        assertEquals(transformedValue1, retrieved1, "First retrieval should match");
        assertEquals(transformedValue1, retrieved2, "Second retrieval should match");
        assertEquals(transformedValue1, retrieved3, "Third retrieval should match");
    }

    /**
     * Property 16a: Same value transforms identically across tables
     * When the same value is transformed in different tables, the output should be identical.
     */
    @Property
    @Label("Same value transforms identically across tables")
    void testCrossTableDeterminism(
        @ForAll @StringLength(min = 1, max = 50) String value,
        @ForAll @StringLength(min = 1, max = 50) String table1,
        @ForAll @StringLength(min = 1, max = 50) String table2,
        @ForAll @StringLength(min = 1, max = 50) String column
    ) {
        Assume.that(!table1.equals(table2));

        ForeignKeyHandler handler = new ForeignKeyHandler();

        // Transform same value in both tables with same transformed output
        Object transformedValue = "transformed_" + value;
        handler.recordTransformation(table1, column, value, transformedValue);
        handler.recordTransformation(table2, column, value, transformedValue);

        // Retrieve transformations
        Object result1 = handler.getTransformedValue(table1, column, value);
        Object result2 = handler.getTransformedValue(table2, column, value);

        // Results should be identical
        assertEquals(
            result1,
            result2,
            "Same value should transform identically across tables"
        );
    }

    /**
     * Property 16b: Deterministic transformation produces same output for same input
     * Applying the same transformation multiple times should produce identical output.
     */
    @Property
    @Label("Deterministic transformation produces same output for same input")
    void testDeterministicTransformationConsistency(
        @ForAll @StringLength(min = 1, max = 50) String tableName,
        @ForAll @StringLength(min = 1, max = 50) String columnName,
        @ForAll @StringLength(min = 1, max = 50) String value
    ) {
        ForeignKeyHandler handler = new ForeignKeyHandler();

        // Record same transformation multiple times
        Object transformedValue = "transformed_" + value;
        handler.recordTransformation(tableName, columnName, value, transformedValue);
        handler.recordTransformation(tableName, columnName, value, transformedValue);
        handler.recordTransformation(tableName, columnName, value, transformedValue);

        // Retrieve transformation
        Object result = handler.getTransformedValue(tableName, columnName, value);

        // Result should be consistent
        assertEquals(
            transformedValue,
            result,
            "Deterministic transformation should produce same output"
        );
    }

    /**
     * Property 15c: FK handler tracks all transformations
     * The FK handler should track all recorded transformations.
     */
    @Property
    @Label("FK handler tracks all transformations")
    void testFKHandlerTracksAllTransformations(
        @ForAll @StringLength(min = 1, max = 50) String tableName,
        @ForAll @StringLength(min = 1, max = 50) String columnName,
        @ForAll @IntRange(min = 1, max = 50) int transformationCount
    ) {
        ForeignKeyHandler handler = new ForeignKeyHandler();

        // Record multiple transformations
        for (int i = 1; i <= transformationCount; i++) {
            Object originalValue = "original_" + i;
            Object transformedValue = "transformed_" + i;
            handler.recordTransformation(tableName, columnName, originalValue, transformedValue);
        }

        // Get all transformations
        Map<Object, Object> transformations = handler.getColumnTransformations(tableName, columnName);

        // Should have all transformations
        assertEquals(
            transformationCount,
            transformations.size(),
            "Handler should track all transformations"
        );
    }

    /**
     * Property 15d: FK handler detects missing transformations
     * The FK handler should return null for untransformed values.
     */
    @Property
    @Label("FK handler detects missing transformations")
    void testFKHandlerDetectsMissingTransformations(
        @ForAll @StringLength(min = 1, max = 50) String tableName,
        @ForAll @StringLength(min = 1, max = 50) String columnName,
        @ForAll @StringLength(min = 1, max = 50) String untransformedValue
    ) {
        ForeignKeyHandler handler = new ForeignKeyHandler();

        // Try to get transformation for value that was never recorded
        Object result = handler.getTransformedValue(tableName, columnName, untransformedValue);

        // Should return null
        assertNull(
            result,
            "Handler should return null for untransformed values"
        );
    }

    /**
     * Property 15e: FK handler hasTransformation method works correctly
     * The hasTransformation method should correctly identify transformed values.
     */
    @Property
    @Label("FK handler hasTransformation method works correctly")
    void testFKHandlerHasTransformation(
        @ForAll @StringLength(min = 1, max = 50) String tableName,
        @ForAll @StringLength(min = 1, max = 50) String columnName,
        @ForAll @StringLength(min = 1, max = 50) String value
    ) {
        ForeignKeyHandler handler = new ForeignKeyHandler();

        // Initially should not have transformation
        assertFalse(
            handler.hasTransformation(tableName, columnName, value),
            "Should not have transformation initially"
        );

        // Record transformation
        handler.recordTransformation(tableName, columnName, value, "transformed_" + value);

        // Now should have transformation
        assertTrue(
            handler.hasTransformation(tableName, columnName, value),
            "Should have transformation after recording"
        );
    }

    /**
     * Property 15f: Multiple columns can have independent transformations
     * Different columns should have independent transformation mappings.
     */
    @Property
    @Label("Multiple columns can have independent transformations")
    void testMultipleColumnsIndependence(
        @ForAll @StringLength(min = 1, max = 50) String tableName,
        @ForAll @StringLength(min = 1, max = 50) String column1,
        @ForAll @StringLength(min = 1, max = 50) String column2,
        @ForAll @StringLength(min = 1, max = 50) String value
    ) {
        Assume.that(!column1.equals(column2));

        ForeignKeyHandler handler = new ForeignKeyHandler();

        // Record different transformations for same value in different columns
        Object transformed1 = "transformed_col1_" + value;
        Object transformed2 = "transformed_col2_" + value;

        handler.recordTransformation(tableName, column1, value, transformed1);
        handler.recordTransformation(tableName, column2, value, transformed2);

        // Retrieve transformations
        Object result1 = handler.getTransformedValue(tableName, column1, value);
        Object result2 = handler.getTransformedValue(tableName, column2, value);

        // Results should be different
        assertNotEquals(
            result1,
            result2,
            "Different columns should have independent transformations"
        );
        assertEquals(transformed1, result1, "Column 1 transformation should match");
        assertEquals(transformed2, result2, "Column 2 transformation should match");
    }

    /**
     * Property 15g: FK handler statistics are accurate
     * The statistics method should return accurate transformation counts.
     */
    @Property
    @Label("FK handler statistics are accurate")
    void testFKHandlerStatistics(
        @ForAll @StringLength(min = 1, max = 50) String tableName,
        @ForAll @StringLength(min = 1, max = 50) String columnName,
        @ForAll @IntRange(min = 1, max = 50) int transformationCount
    ) {
        ForeignKeyHandler handler = new ForeignKeyHandler();

        // Record transformations
        for (int i = 1; i <= transformationCount; i++) {
            handler.recordTransformation(tableName, columnName, "value_" + i, "transformed_" + i);
        }

        // Get statistics
        Map<String, Integer> stats = handler.getStatistics();
        String key = tableName + "." + columnName;

        assertTrue(
            stats.containsKey(key),
            "Statistics should contain the column"
        );
        assertEquals(
            transformationCount,
            stats.get(key),
            "Statistics should show correct transformation count"
        );
    }

    /**
     * Property 15h: FK handler can be cleared
     * After clearing, the handler should have no transformations.
     */
    @Property
    @Label("FK handler can be cleared")
    void testFKHandlerClear(
        @ForAll @StringLength(min = 1, max = 50) String tableName,
        @ForAll @StringLength(min = 1, max = 50) String columnName,
        @ForAll @StringLength(min = 1, max = 50) String value
    ) {
        ForeignKeyHandler handler = new ForeignKeyHandler();

        // Record transformation
        handler.recordTransformation(tableName, columnName, value, "transformed_" + value);

        // Verify it exists
        assertTrue(
            handler.hasTransformation(tableName, columnName, value),
            "Transformation should exist before clear"
        );

        // Clear
        handler.clear();

        // Verify it's gone
        assertFalse(
            handler.hasTransformation(tableName, columnName, value),
            "Transformation should not exist after clear"
        );
    }
}
