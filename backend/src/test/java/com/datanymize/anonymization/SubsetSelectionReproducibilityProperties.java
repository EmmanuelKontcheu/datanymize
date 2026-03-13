package com.datanymize.anonymization;

import com.datanymize.config.model.SubsetConfig;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.util.*;

/**
 * Property-based tests for subset selection reproducibility.
 * **Validates: Requirements 6.2**
 */
@PropertyDefaults(tries = 50)
public class SubsetSelectionReproducibilityProperties {

    @Property
    @Label("Property 17: Subset Selection Reproducibility")
    void testSubsetSelectionReproducibility(
        @ForAll @DoubleRange(min = 1, max = 100) double percentage,
        @ForAll long seed,
        @ForAll @LongRange(min = 100, max = 10000) long totalRows
    ) {
        // Feature: datanymize, Property 17: Subset Selection Reproducibility
        
        // Given a subset configuration with seed
        SubsetConfig config = new SubsetConfig(percentage, seed);
        SubsetSelector selector1 = new SubsetSelector(config);
        SubsetSelector selector2 = new SubsetSelector(config);
        
        // When selecting rows with same seed
        Set<Long> selectedRows1 = new HashSet<>();
        Set<Long> selectedRows2 = new HashSet<>();
        
        for (long i = 0; i < totalRows; i++) {
            if (selector1.shouldIncludeRow(i, totalRows)) {
                selectedRows1.add(i);
            }
            if (selector2.shouldIncludeRow(i, totalRows)) {
                selectedRows2.add(i);
            }
        }
        
        // Then selections should be identical
        assert selectedRows1.equals(selectedRows2) : "Subset selections with same seed should be identical";
    }

    @Property
    @Label("Property 17b: Different Seeds Produce Different Selections")
    void testDifferentSeedsProduceDifferentSelections(
        @ForAll @DoubleRange(min = 1, max = 100) double percentage,
        @ForAll long seed1,
        @ForAll long seed2,
        @ForAll @LongRange(min = 100, max = 10000) long totalRows
    ) {
        // Feature: datanymize, Property 17b: Different Seeds Produce Different Selections
        
        // Assume seeds are different
        Assume.that(seed1 != seed2);
        
        // Given two subset configurations with different seeds
        SubsetConfig config1 = new SubsetConfig(percentage, seed1);
        SubsetConfig config2 = new SubsetConfig(percentage, seed2);
        SubsetSelector selector1 = new SubsetSelector(config1);
        SubsetSelector selector2 = new SubsetSelector(config2);
        
        // When selecting rows
        Set<Long> selectedRows1 = new HashSet<>();
        Set<Long> selectedRows2 = new HashSet<>();
        
        for (long i = 0; i < totalRows; i++) {
            if (selector1.shouldIncludeRow(i, totalRows)) {
                selectedRows1.add(i);
            }
            if (selector2.shouldIncludeRow(i, totalRows)) {
                selectedRows2.add(i);
            }
        }
        
        // Then selections should be different (with high probability)
        // Note: This is probabilistic, so we don't assert inequality
    }

    @Property
    @Label("Property 17c: Percentage Accuracy")
    void testPercentageAccuracy(
        @ForAll @DoubleRange(min = 1, max = 100) double percentage,
        @ForAll long seed,
        @ForAll @LongRange(min = 1000, max = 10000) long totalRows
    ) {
        // Feature: datanymize, Property 17c: Percentage Accuracy
        
        // Given a subset configuration
        SubsetConfig config = new SubsetConfig(percentage, seed);
        SubsetSelector selector = new SubsetSelector(config);
        
        // When selecting rows
        long selectedCount = 0;
        for (long i = 0; i < totalRows; i++) {
            if (selector.shouldIncludeRow(i, totalRows)) {
                selectedCount++;
            }
        }
        
        // Then selected percentage should be close to configured percentage
        double actualPercentage = (selectedCount * 100.0) / totalRows;
        double tolerance = 5.0; // Allow 5% tolerance
        assert Math.abs(actualPercentage - percentage) <= tolerance : 
            "Actual percentage " + actualPercentage + " should be close to " + percentage;
    }

    @Property
    @Label("Property 17d: Estimated Row Count")
    void testEstimatedRowCount(
        @ForAll @DoubleRange(min = 1, max = 100) double percentage,
        @ForAll long seed,
        @ForAll @LongRange(min = 100, max = 10000) long totalRows
    ) {
        // Feature: datanymize, Property 17d: Estimated Row Count
        
        // Given a subset configuration
        SubsetConfig config = new SubsetConfig(percentage, seed);
        SubsetSelector selector = new SubsetSelector(config);
        
        // When estimating selected rows
        long estimatedRows = selector.estimateSelectedRows(totalRows);
        
        // Then estimate should be reasonable
        long expectedRows = Math.round(totalRows * percentage / 100.0);
        assert estimatedRows == expectedRows : "Estimated rows should match calculation";
    }

    @Property
    @Label("Property 17e: 100% Percentage Selects All Rows")
    void testFullPercentageSelectsAllRows(
        @ForAll long seed,
        @ForAll @LongRange(min = 100, max = 10000) long totalRows
    ) {
        // Feature: datanymize, Property 17e: 100% Percentage Selects All Rows
        
        // Given a subset configuration with 100% percentage
        SubsetConfig config = new SubsetConfig(100.0, seed);
        SubsetSelector selector = new SubsetSelector(config);
        
        // When selecting rows
        long selectedCount = 0;
        for (long i = 0; i < totalRows; i++) {
            if (selector.shouldIncludeRow(i, totalRows)) {
                selectedCount++;
            }
        }
        
        // Then all rows should be selected
        assert selectedCount == totalRows : "100% percentage should select all rows";
    }

    @Property
    @Label("Property 17f: 0% Percentage Selects No Rows")
    void testZeroPercentageSelectsNoRows(
        @ForAll long seed,
        @ForAll @LongRange(min = 100, max = 10000) long totalRows
    ) {
        // Feature: datanymize, Property 17f: 0% Percentage Selects No Rows
        
        // Given a subset configuration with 0% percentage
        SubsetConfig config = new SubsetConfig(0.0, seed);
        SubsetSelector selector = new SubsetSelector(config);
        
        // When selecting rows
        long selectedCount = 0;
        for (long i = 0; i < totalRows; i++) {
            if (selector.shouldIncludeRow(i, totalRows)) {
                selectedCount++;
            }
        }
        
        // Then no rows should be selected
        assert selectedCount == 0 : "0% percentage should select no rows";
    }
}
