package com.datanymize.database.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Represents a database index.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Index {
    private String name;
    private String tableName;
    private List<String> columns;
    private boolean unique;
    private String indexType;
}
