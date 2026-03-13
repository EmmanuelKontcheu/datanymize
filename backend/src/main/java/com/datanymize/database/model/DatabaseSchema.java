package com.datanymize.database.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Represents a complete database schema with all tables, foreign keys, and indices.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatabaseSchema {
    private String databaseName;
    private List<Table> tables;
    private List<ForeignKey> foreignKeys;
    private List<Index> indices;
    private Map<String, String> metadata;
    private String databaseType;
    private String version;
}
