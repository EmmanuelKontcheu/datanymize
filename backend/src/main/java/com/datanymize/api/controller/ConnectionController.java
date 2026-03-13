package com.datanymize.api.controller;

import com.datanymize.api.dto.ApiResponse;
import com.datanymize.api.dto.ConnectionRequest;
import com.datanymize.api.dto.ConnectionResponse;
import com.datanymize.database.connection.IConnectionManager;
import com.datanymize.database.model.ConnectionConfig;
import com.datanymize.database.model.ConnectionResult;
import com.datanymize.exception.DatanymizeException;
import com.datanymize.tenant.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * REST API controller for database connection management.
 * 
 * Validates Requirements: 1.1, 1.2, 1.3, 1.4, 1.5
 */
@Slf4j
@RestController
@RequestMapping("/api/connections")
@Tag(name = "Connections", description = "Database connection management endpoints")
public class ConnectionController {
    
    @Autowired
    private IConnectionManager connectionManager;
    
    /**
     * Create a new database connection.
     * POST /api/connections
     */
    @PostMapping
    @Operation(summary = "Create a new database connection")
    public ResponseEntity<ApiResponse<ConnectionResponse>> createConnection(
            @Valid @RequestBody ConnectionRequest request) {
        
        try {
            String tenantId = TenantContextHolder.getTenantContext().getTenantId();
            log.info("Creating connection for tenant: {}", tenantId);
            
            // Convert request to config
            ConnectionConfig config = convertRequestToConfig(request);
            
            // Save connection
            String connectionId = java.util.UUID.randomUUID().toString();
            connectionManager.saveConnection(connectionId, config);
            
            // Return response
            ConnectionResponse response = convertConfigToResponse(connectionId, config);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Connection created successfully"));
            
        } catch (Exception e) {
            log.error("Failed to create connection", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage(), "CONNECTION_CREATE_FAILED", 400));
        }
    }
    
    /**
     * Get all connections for the current tenant.
     * GET /api/connections
     */
    @GetMapping
    @Operation(summary = "List all database connections")
    public ResponseEntity<ApiResponse<List<ConnectionResponse>>> listConnections() {
        
        try {
            String tenantId = TenantContextHolder.getTenantContext().getTenantId();
            log.info("Listing connections for tenant: {}", tenantId);
            
            // In a real implementation, would query database for tenant's connections
            List<ConnectionResponse> connections = new ArrayList<>();
            
            return ResponseEntity.ok(ApiResponse.success(connections, "Connections retrieved successfully"));
            
        } catch (Exception e) {
            log.error("Failed to list connections", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage(), "CONNECTION_LIST_FAILED", 500));
        }
    }
    
    /**
     * Get a specific connection by ID.
     * GET /api/connections/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get connection details")
    public ResponseEntity<ApiResponse<ConnectionResponse>> getConnection(
            @PathVariable String id) {
        
        try {
            String tenantId = TenantContextHolder.getTenantContext().getTenantId();
            log.info("Getting connection {} for tenant: {}", id, tenantId);
            
            ConnectionConfig config = connectionManager.getConnection(id);
            if (config == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Connection not found", "CONNECTION_NOT_FOUND", 404));
            }
            
            ConnectionResponse response = convertConfigToResponse(id, config);
            return ResponseEntity.ok(ApiResponse.success(response, "Connection retrieved successfully"));
            
        } catch (Exception e) {
            log.error("Failed to get connection", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage(), "CONNECTION_GET_FAILED", 500));
        }
    }
    
    /**
     * Test a database connection.
     * POST /api/connections/{id}/test
     */
    @PostMapping("/{id}/test")
    @Operation(summary = "Test database connection")
    public ResponseEntity<ApiResponse<ConnectionTestResponse>> testConnection(
            @PathVariable String id) {
        
        try {
            String tenantId = TenantContextHolder.getTenantContext().getTenantId();
            log.info("Testing connection {} for tenant: {}", id, tenantId);
            
            ConnectionConfig config = connectionManager.getConnection(id);
            if (config == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Connection not found", "CONNECTION_NOT_FOUND", 404));
            }
            
            // Test the connection
            ConnectionResult result = connectionManager.testConnection(config);
            
            ConnectionTestResponse response = ConnectionTestResponse.builder()
                .success(result.isSuccess())
                .message(result.getMessage())
                .build();
            
            return ResponseEntity.ok(ApiResponse.success(response, "Connection test completed"));
            
        } catch (Exception e) {
            log.error("Failed to test connection", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage(), "CONNECTION_TEST_FAILED", 500));
        }
    }
    
    /**
     * Delete a database connection.
     * DELETE /api/connections/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a database connection")
    public ResponseEntity<ApiResponse<Void>> deleteConnection(
            @PathVariable String id) {
        
        try {
            String tenantId = TenantContextHolder.getTenantContext().getTenantId();
            log.info("Deleting connection {} for tenant: {}", id, tenantId);
            
            connectionManager.deleteConnection(id);
            
            return ResponseEntity.ok(ApiResponse.success("Connection deleted successfully"));
            
        } catch (Exception e) {
            log.error("Failed to delete connection", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage(), "CONNECTION_DELETE_FAILED", 500));
        }
    }
    
    // Helper methods
    
    private ConnectionConfig convertRequestToConfig(ConnectionRequest request) {
        ConnectionConfig config = new ConnectionConfig();
        config.setHost(request.getHost());
        config.setPort(request.getPort());
        config.setUsername(request.getUsername());
        config.setPassword(request.getPassword());
        config.setDatabase(request.getDatabase());
        config.setDatabaseType(request.getDatabaseType());
        config.setUseTLS(request.getUseTLS() != null ? request.getUseTLS() : true);
        config.setVerifyCertificate(request.getVerifyCertificate() != null ? request.getVerifyCertificate() : true);
        return config;
    }
    
    private ConnectionResponse convertConfigToResponse(String id, ConnectionConfig config) {
        return ConnectionResponse.builder()
            .id(id)
            .host(config.getHost())
            .port(config.getPort())
            .username(config.getUsername())
            .database(config.getDatabase())
            .databaseType(config.getDatabaseType())
            .useTLS(config.isUseTLS())
            .verifyCertificate(config.isVerifyCertificate())
            .status("UNTESTED")
            .build();
    }
    
    /**
     * Response DTO for connection test results.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class ConnectionTestResponse {
        private boolean success;
        private String message;
    }
}
