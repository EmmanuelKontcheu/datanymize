package com.datanymize.database.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Represents a database table with its columns and constraints.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Table {
    private String name;
    private List<Column> columns;
    private List<String> primaryKeys;
    private List<String> uniqueKeys;
    private long rowCount;
    private String tableType;
    private String engine;
    private String charset;
    private String collation;
}
