package com.datanymize.database.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a foreign key relationship between tables.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForeignKey {
    private String name;
    private String sourceTable;
    private String sourceColumn;
    private String targetTable;
    private String targetColumn;
    private String onDelete;
    private String onUpdate;
}
