package com.datanymize.anonymization;

import com.datanymize.config.model.SubsetConfig;

import java.util.*;

/**
 * Selects a subset of rows for anonymization.
 * Supports percentage-based and filter-based selection with reproducibility.
 */
public class SubsetSelector {
    private final SubsetConfig config;
    private final Random random;

    public SubsetSelector(SubsetConfig config) {
        this.config = config;
        this.random = new Random(config.getSeed());
    }

    /**
     * Determine if a row should be included in the subset.
     * @param rowIndex Row index (0-based)
     * @param totalRows Total number of rows
     * @return true if row should be included
     */
    public boolean shouldIncludeRow(long rowIndex, long totalRows) {
        if (config.getPercentage() >= 100) {
            return true;
        }

        if (config.getPercentage() <= 0) {
            return false;
        }

        // Use deterministic selection based on seed
        Random rowRandom = new Random(config.getSeed() + rowIndex);
        return rowRandom.nextDouble() * 100 < config.getPercentage();
    }

    /**
     * Get the percentage of rows to select.
     * @return Percentage (0-100)
     */
    public double getPercentage() {
        return config.getPercentage();
    }

    /**
     * Get the seed for reproducible selection.
     * @return Seed value
     */
    public long getSeed() {
        return config.getSeed();
    }

    /**
     * Check if foreign key dependencies should be included.
     * @return true if FK dependencies should be included
     */
    public boolean shouldIncludeForeignKeyDependencies() {
        return config.isIncludeForeignKeyDependencies();
    }

    /**
     * Get filter criteria.
     * @return List of filter criteria or empty list
     */
    public List<SubsetConfig.FilterCriteria> getFilters() {
        return config.getFilters() != null ? config.getFilters() : new ArrayList<>();
    }

    /**
     * Estimate the number of rows that will be selected.
     * @param totalRows Total number of rows
     * @return Estimated number of selected rows
     */
    public long estimateSelectedRows(long totalRows) {
        return Math.round(totalRows * config.getPercentage() / 100.0);
    }
}
