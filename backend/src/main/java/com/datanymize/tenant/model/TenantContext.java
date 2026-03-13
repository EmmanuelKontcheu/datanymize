package com.datanymize.tenant.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds tenant information in request context.
 * This class represents the current tenant for a request and provides
 * access to tenant-specific metadata and configuration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantContext {
    
    /**
     * Unique identifier for the tenant
     */
    private String tenantId;
    
    /**
     * Human-readable name for the tenant
     */
    private String tenantName;
    
    /**
     * Timestamp when the tenant was created
     */
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the tenant was last accessed
     */
    private LocalDateTime lastAccessedAt;
    
    /**
     * Whether the tenant is currently active
     */
    private boolean active;
    
    /**
     * Database schema name for this tenant (for schema-level isolation)
     */
    private String schemaName;
    
    /**
     * Additional metadata for the tenant
     */
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();
    
    /**
     * Validates that the tenant context is properly initialized
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return tenantId != null && !tenantId.isEmpty() && active;
    }
}
