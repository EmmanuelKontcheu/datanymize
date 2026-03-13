package com.datanymize.database.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for database connections.
 * Supports PostgreSQL, MySQL, and MongoDB.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionConfig {
    private String id;
    private String type;              // postgresql, mysql, mongodb
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;          // Encrypted at rest
    private boolean useTLS;
    private boolean verifyCertificate;
    @Builder.Default
    private Map<String, String> additionalParams = new HashMap<>();
    private LocalDateTime createdAt;
    private LocalDateTime lastTestedAt;
    
    /**
     * Connection timeout in seconds (default 5 seconds)
     */
    @Builder.Default
    private int connectionTimeoutSeconds = 5;
    
    /**
     * Get database type (alias for type field).
     */
    public String getDatabaseType() {
        return this.type;
    }
    
    /**
     * Set database type (alias for type field).
     */
    public void setDatabaseType(String databaseType) {
        this.type = databaseType;
    }
}
