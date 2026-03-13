package com.datanymize.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating/updating database connections.
 * 
 * Validates Requirements: 1.1, 1.2, 1.3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionRequest {
    
    @NotBlank(message = "Connection name is required")
    private String name;
    
    @NotBlank(message = "Database type is required")
    private String databaseType; // POSTGRESQL, MYSQL, MONGODB
    
    @NotBlank(message = "Host is required")
    private String host;
    
    @NotNull(message = "Port is required")
    @Min(value = 1, message = "Port must be between 1 and 65535")
    @Max(value = 65535, message = "Port must be between 1 and 65535")
    private Integer port;
    
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    private String database;
    
    private Boolean useTLS = true;
    
    private Boolean verifyCertificate = true;
    
    private String description;
}
