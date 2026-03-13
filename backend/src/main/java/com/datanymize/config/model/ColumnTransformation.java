package com.datanymize.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for transforming a specific column.
 * Specifies which transformer to apply and its parameters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnTransformation {
    private String columnName;
    private String transformerName;
    private Map<String, Object> transformerParams;
    private boolean deterministic;
    private String seed;

    public ColumnTransformation(String columnName, String transformerName) {
        this.columnName = columnName;
        this.transformerName = transformerName;
        this.transformerParams = new HashMap<>();
        this.deterministic = false;
        this.seed = null;
    }

    public ColumnTransformation(String columnName, String transformerName, 
                               boolean deterministic, String seed) {
        this.columnName = columnName;
        this.transformerName = transformerName;
        this.transformerParams = new HashMap<>();
        this.deterministic = deterministic;
        this.seed = seed;
    }
}
