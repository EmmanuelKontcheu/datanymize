package com.datanymize.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for database connections.
 * Stores connection parameters with encryption for sensitive data.
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
    private Map<String, String> additionalParams;
    private LocalDateTime createdAt;
    private LocalDateTime lastTestedAt;

    public ConnectionConfig(String id, String type, String host, int port, String database, 
                           String username, String password) {
        this.id = id;
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.useTLS = true;
        this.verifyCertificate = true;
        this.additionalParams = new HashMap<>();
        this.createdAt = LocalDateTime.now();
    }
}
