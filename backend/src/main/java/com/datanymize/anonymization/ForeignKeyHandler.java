package com.datanymize.anonymization;

import com.datanymize.database.model.ForeignKey;
import com.datanymize.database.model.Row;

import java.util.*;

/**
 * Handles foreign key transformations during anonymization.
 * Ensures transformed foreign keys reference valid records.
 */
public class ForeignKeyHandler {
    private final Map<String, Map<Object, Object>> valueMapping = new HashMap<>();

    /**
     * Record a value transformation for a foreign key column.
     * @param tableName Table name
     * @param columnName Column name
     * @param originalValue Original value
     * @param transformedValue Transformed value
     */
    public void recordTransformation(String tableName, String columnName, Object originalValue, Object transformedValue) {
        String key = tableName + "." + columnName;
        valueMapping.computeIfAbsent(key, k -> new HashMap<>()).put(originalValue, transformedValue);
    }

    /**
     * Get the transformed value for a foreign key.
     * @param tableName Table name
     * @param columnName Column name
     * @param originalValue Original value
     * @return Transformed value or null if not found
     */
    public Object getTransformedValue(String tableName, String columnName, Object originalValue) {
        String key = tableName + "." + columnName;
        Map<Object, Object> mapping = valueMapping.get(key);
        if (mapping == null) {
            return null;
        }
        return mapping.get(originalValue);
    }

    /**
     * Check if a foreign key value has been transformed.
     * @param tableName Table name
     * @param columnName Column name
     * @param originalValue Original value
     * @return true if value has been transformed
     */
    public boolean hasTransformation(String tableName, String columnName, Object originalValue) {
        String key = tableName + "." + columnName;
        Map<Object, Object> mapping = valueMapping.get(key);
        return mapping != null && mapping.containsKey(originalValue);
    }

    /**
     * Get all transformations for a column.
     * @param tableName Table name
     * @param columnName Column name
     * @return Map of original to transformed values
     */
    public Map<Object, Object> getColumnTransformations(String tableName, String columnName) {
        String key = tableName + "." + columnName;
        return new HashMap<>(valueMapping.getOrDefault(key, new HashMap<>()));
    }

    /**
     * Clear all recorded transformations.
     */
    public void clear() {
        valueMapping.clear();
    }

    /**
     * Get statistics about recorded transformations.
     * @return Map of column to transformation count
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        for (Map.Entry<String, Map<Object, Object>> entry : valueMapping.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().size());
        }
        return stats;
    }
}
