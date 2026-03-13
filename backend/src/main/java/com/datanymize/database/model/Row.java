package com.datanymize.database.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single row of data from a database table.
 * Contains the table name and column values.
 * 
 * Validates Requirements: 1.1, 1.2, 1.3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Row {
    
    /**
     * The name of the table this row belongs to.
     */
    private String tableName;
    
    /**
     * The column values for this row.
     * Key: column name, Value: column value
     */
    @Builder.Default
    private Map<String, Object> values = new HashMap<>();
    
    /**
     * The original column values before any transformations.
     * Used for audit purposes.
     * Key: column name, Value: original column value
     */
    @Builder.Default
    private Map<String, Object> originalValues = new HashMap<>();
    
    /**
     * Get a column value by name.
     * 
     * @param columnName the name of the column
     * @return the value of the column, or null if not found
     */
    public Object getValue(String columnName) {
        return values.get(columnName);
    }
    
    /**
     * Set a column value.
     * 
     * @param columnName the name of the column
     * @param value the value to set
     */
    public void setValue(String columnName, Object value) {
        values.put(columnName, value);
    }
    
    /**
     * Get the original column value by name.
     * 
     * @param columnName the name of the column
     * @return the original value of the column, or null if not found
     */
    public Object getOriginalValue(String columnName) {
        return originalValues.get(columnName);
    }
    
    /**
     * Set the original column value.
     * 
     * @param columnName the name of the column
     * @param value the original value to set
     */
    public void setOriginalValue(String columnName, Object value) {
        originalValues.put(columnName, value);
    }
    
    /**
     * Check if a column exists in this row.
     * 
     * @param columnName the name of the column
     * @return true if the column exists, false otherwise
     */
    public boolean hasColumn(String columnName) {
        return values.containsKey(columnName);
    }
    
    /**
     * Get the number of columns in this row.
     * 
     * @return the number of columns
     */
    public int getColumnCount() {
        return values.size();
    }
}
