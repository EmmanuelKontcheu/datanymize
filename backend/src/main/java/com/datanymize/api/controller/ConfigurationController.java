package com.datanymize.api.controller;

import com.datanymize.api.dto.ApiResponse;
import com.datanymize.config.model.AnonymizationConfig;
import com.datanymize.config.parser.IConfigurationParser;
import com.datanymize.config.parser.JSONConfigParser;
import com.datanymize.config.parser.YAMLConfigParser;
import com.datanymize.config.validator.ConfigValidator;
import com.datanymize.config.versioning.ConfigDiff;
import com.datanymize.config.versioning.ConfigVersion;
import com.datanymize.config.versioning.ConfigVersionManager;
import com.datanymize.exception.DatanymizeException;
import com.datanymize.tenant.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * REST API controller for anonymization configuration management.
 * 
 * Validates Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7
 */
@Slf4j
@RestController
@RequestMapping("/api/configurations")
@Tag(name = "Configurations", description = "Anonymization configuration management endpoints")
public class ConfigurationController {
    
    @Autowired
    private ConfigVersionManager configVersionManager;
    
    @Autowired
    private ConfigValidator configValidator;
    
    private final IConfigurationParser yamlParser = new YAMLConfigParser();
    private final IConfigurationParser jsonParser = new JSONConfigParser();
    
    /**
     * Create a new anonymization configuration.
     * POST /api/configurations
     */
    @PostMapping
    @Operation(summary = "Create a new anonymization configuration")
    public ResponseEntity<ApiResponse<ConfigurationResponse>> createConfiguration(
            @Valid @RequestBody CreateConfigurationRequest request) {
        
        try {
            String tenantId = TenantContextHolder.getTenantContext().getTenantId();
            log.info("Creating configuration for tenant: {}", tenantId);
            
            // Parse configuration based on format
            IConfigurationParser parser = request.getFormat().equalsIgnoreCase("yaml") 
                ? yamlParser 
                : jsonParser;
            
            AnonymizationConfig config = parser.parse(request.getContent());
            
            // Validate configuration
            var validationResult = configValidator.validate(config);
            if (!validationResult.isValid()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(
                        "Configuration validation failed: " + validationResult.getErrors(),
                        "CONFIG_VALIDATION_FAILED",
                        400
                    ));
            }
            
            // Generate configuration ID
            String configId = UUID.randomUUID().toString();
            config.setId(configId);
            
            // Store configuration with versioning
            configVersionManager.saveConfiguration(configId, config);
            
            ConfigurationResponse response = ConfigurationResponse.builder()
                .id(configId)
                .version(1)
                .format(request.getFormat())
                .createdAt(LocalDateTime.now())
                .build();
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Configuration created successfully"));
            
        } catch (Exception e) {
            log.error("Failed to create configuration", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage(), "CONFIG_CREATE_FAILED", 400));
        }
    }
    
    /**
     * Get a specific configuration by ID.
     * GET /api/configurations/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get configuration details")
    public ResponseEntity<ApiResponse<ConfigurationDetailResponse>> getConfiguration(
            @PathVariable String id) {
        
        try {
            String tenantId = TenantContextHolder.getTenantContext().getTenantId();
            log.info("Getting configuration {} for tenant: {}", id, tenantId);
            
            AnonymizationConfig config = configVersionManager.getLatestConfiguration(id);
            if (config == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Configuration not found", "CONFIG_NOT_FOUND", 404));
            }
            
            ConfigurationDetailResponse response = ConfigurationDetailResponse.builder()
                .id(id)
                .version(configVersionManager.getLatestVersion(id))
                .content(config.toString())
                .createdAt(LocalDateTime.now())
                .build();
            
            return ResponseEntity.ok(ApiResponse.success(response, "Configuration retrieved successfully"));
            
        } catch (Exception e) {
            log.error("Failed to get configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage(), "CONFIG_GET_FAILED", 500));
        }
    }
    
    /**
     * Update an existing configuration.
     * PUT /api/configurations/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an anonymization configuration")
    public ResponseEntity<ApiResponse<ConfigurationResponse>> updateConfiguration(
            @PathVariable String id,
            @Valid @RequestBody UpdateConfigurationRequest request) {
        
        try {
            String tenantId = TenantContextHolder.getTenantContext().getTenantId();
            log.info("Updating configuration {} for tenant: {}", id, tenantId);
            
            // Get current configuration
            AnonymizationConfig currentConfig = configVersionManager.getLatestConfiguration(id);
            if (currentConfig == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Configuration not found", "CONFIG_NOT_FOUND", 404));
            }
            
            // Parse new configuration
            IConfigurationParser parser = request.getFormat().equalsIgnoreCase("yaml") 
                ? yamlParser 
                : jsonParser;
            
            AnonymizationConfig newConfig = parser.parse(request.getContent());
            newConfig.setId(id);
            
            // Validate new configuration
            var validationResult = configValidator.validate(newConfig);
            if (!validationResult.isValid()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(
                        "Configuration validation failed: " + validationResult.getErrors(),
                        "CONFIG_VALIDATION_FAILED",
                        400
                    ));
            }
            
            // Save new version
            configVersionManager.saveConfiguration(id, newConfig);
            int newVersion = configVersionManager.getLatestVersion(id);
            
            ConfigurationResponse response = ConfigurationResponse.builder()
                .id(id)
                .version(newVersion)
                .format(request.getFormat())
                .createdAt(LocalDateTime.now())
                .build();
            
            return ResponseEntity.ok(ApiResponse.success(response, "Configuration updated successfully"));
            
        } catch (Exception e) {
            log.error("Failed to update configuration", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage(), "CONFIG_UPDATE_FAILED", 400));
        }
    }
    
    /**
     * Get configuration version history.
     * GET /api/configurations/{id}/versions
     */
    @GetMapping("/{id}/versions")
    @Operation(summary = "Get configuration version history")
    public ResponseEntity<ApiResponse<List<ConfigVersionResponse>>> getVersionHistory(
            @PathVariable String id) {
        
        try {
            String tenantId = TenantContextHolder.getTenantContext().getTenantId();
            log.info("Getting version history for configuration {} for tenant: {}", id, tenantId);
            
            List<ConfigVersion> versions = configVersionManager.getConfigurationHistory(id);
            if (versions == null || versions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Configuration not found", "CONFIG_NOT_FOUND", 404));
            }
            
            List<ConfigVersionResponse> responses = new ArrayList<>();
            for (ConfigVersion version : versions) {
                responses.add(ConfigVersionResponse.builder()
                    .versionNumber(version.getVersionNumber())
                    .timestamp(version.getTimestamp())
                    .changes(version.getChanges())
                    .build());
            }
            
            return ResponseEntity.ok(ApiResponse.success(responses, "Version history retrieved successfully"));
            
        } catch (Exception e) {
            log.error("Failed to get version history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage(), "CONFIG_HISTORY_FAILED", 500));
        }
    }
    
    /**
     * Restore a previous configuration version.
     * POST /api/configurations/{id}/restore
     */
    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore a previous configuration version")
    public ResponseEntity<ApiResponse<ConfigurationResponse>> restoreVersion(
            @PathVariable String id,
            @Valid @RequestBody RestoreVersionRequest request) {
        
        try {
            String tenantId = TenantContextHolder.getTenantContext().getTenantId();
            log.info("Restoring configuration {} to version {} for tenant: {}", 
                id, request.getVersionNumber(), tenantId);
            
            AnonymizationConfig restoredConfig = configVersionManager.restoreVersion(id, request.getVersionNumber());
            if (restoredConfig == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Version not found", "VERSION_NOT_FOUND", 404));
            }
            
            int newVersion = configVersionManager.getLatestVersion(id);
            
            ConfigurationResponse response = ConfigurationResponse.builder()
                .id(id)
                .version(newVersion)
                .format("yaml")
                .createdAt(LocalDateTime.now())
                .build();
            
            return ResponseEntity.ok(ApiResponse.success(response, "Configuration restored successfully"));
            
        } catch (Exception e) {
            log.error("Failed to restore configuration version", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage(), "CONFIG_RESTORE_FAILED", 400));
        }
    }
    
    // ============ Request/Response DTOs ============
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateConfigurationRequest {
        private String content;
        private String format; // "yaml" or "json"
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateConfigurationRequest {
        private String content;
        private String format; // "yaml" or "json"
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RestoreVersionRequest {
        private int versionNumber;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConfigurationResponse {
        private String id;
        private int version;
        private String format;
        private LocalDateTime createdAt;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConfigurationDetailResponse {
        private String id;
        private int version;
        private String content;
        private LocalDateTime createdAt;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConfigVersionResponse {
        private int versionNumber;
        private LocalDateTime timestamp;
        private String changes;
    }
}
