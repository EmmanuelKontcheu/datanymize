package com.datanymize.export.model;

/**
 * Enum representing supported export formats for anonymized data.
 * Validates: Requirements 7.1, 7.2, 7.3, 7.4
 */
public enum ExportFormat {
    POSTGRESQL_DUMP("PostgreSQL Dump", "pg_dump compatible SQL"),
    MYSQL_DUMP("MySQL Dump", "mysqldump compatible SQL"),
    MONGODB_DUMP("MongoDB Dump", "mongodump compatible BSON"),
    SQL_STATEMENTS("SQL Statements", "Raw SQL statements"),
    CSV("CSV", "Comma-separated values"),
    JSON("JSON", "JSON format");

    private final String displayName;
    private final String description;

    ExportFormat(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
