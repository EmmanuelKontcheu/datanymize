package com.datanymize.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for subset selection.
 * Specifies how to select a subset of rows for anonymization.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubsetConfig {
    private double percentage;                    // Percentage of rows to select (0-100)
    private long seed;                            // Seed for reproducible selection
    private List<FilterCriteria> filters;         // Optional filter criteria
    private boolean includeForeignKeyDependencies; // Include FK dependencies

    public SubsetConfig(double percentage) {
        this.percentage = percentage;
        this.seed = System.currentTimeMillis();
        this.filters = new ArrayList<>();
        this.includeForeignKeyDependencies = true;
    }

    public SubsetConfig(double percentage, long seed) {
        this.percentage = percentage;
        this.seed = seed;
        this.filters = new ArrayList<>();
        this.includeForeignKeyDependencies = true;
    }

    public void addFilter(FilterCriteria filter) {
        this.filters.add(filter);
    }

    /**
     * Filter criteria for subset selection.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FilterCriteria {
        private String column;
        private String operator;  // =, !=, <, >, <=, >=, IN, LIKE
        private Object value;
    }
}
