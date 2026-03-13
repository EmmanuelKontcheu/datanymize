package com.datanymize.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for database connections.
 * 
 * Validates Requirements: 1.1, 1.2, 1.3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectionResponse {
    
    private String id;
    
    private String name;
    
    private String databaseType;
    
    private String host;
    
    private Integer port;
    
    private String username;
    
    private String database;
    
    private Boolean useTLS;
    
    private Boolean verifyCertificate;
    
    private String description;
    
    private String status; // CONNECTED, FAILED, UNTESTED
    
    private String lastTestResult;
    
    private Instant lastTestedAt;
    
    private Instant createdAt;
    
    private Instant updatedAt;
}
