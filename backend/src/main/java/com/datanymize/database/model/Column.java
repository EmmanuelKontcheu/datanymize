package com.datanymize.database.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a database column with its metadata and properties.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Column {
    private String name;
    private String dataType;
    private boolean nullable;
    private String defaultValue;
    private boolean primaryKey;
    private boolean unique;
    private int ordinalPosition;
    private String characterMaximumLength;
    private String numericPrecision;
    private String numericScale;
}
