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
 * Configuration for transforming a specific table.
 * Specifies which columns to transform and how.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableConfig {
    private String tableName;
    private Map<String, ColumnTransformation> columns;
    private List<String> primaryKeys;
    private List<String> uniqueKeys;

    public TableConfig(String tableName) {
        this.tableName = tableName;
        this.columns = new HashMap<>();
        this.primaryKeys = new ArrayList<>();
        this.uniqueKeys = new ArrayList<>();
    }

    public void addColumnTransformation(ColumnTransformation transformation) {
        this.columns.put(transformation.getColumnName(), transformation);
    }

    public ColumnTransformation getColumnTransformation(String columnName) {
        return this.columns.get(columnName);
    }
}
