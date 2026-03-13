package com.datanymize.tenant;

import com.datanymize.tenant.model.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of tenant lifecycle management.
 * 
 * Manages tenant creation, retrieval, deletion, and isolation enforcement.
 * Validates Requirements 17.1, 17.2, 17.3, 17.5.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantManager implements ITenantManager {
    
    private final TenantContextHolder tenantContextHolder;
    
    /**
     * In-memory storage for tenants.
     * In production, this would be replaced with a database.
     */
    private final Map<String, TenantContext> tenants = new ConcurrentHashMap<>();
    
    /**
     * Mapping of tenant ID to schema name for database-level isolation.
     */
    private final Map<String, String> tenantSchemas = new ConcurrentHashMap<>();
    
    @Override
    public TenantContext createTenant(String tenantName) {
        if (tenantName == null || tenantName.isEmpty()) {
            throw new IllegalArgumentException("Tenant name cannot be null or empty");
        }
        
        String tenantId = generateTenantId();
        String schemaName = generateSchemaName(tenantId);
        LocalDateTime now = LocalDateTime.now();
        
        TenantContext tenantContext = TenantContext.builder()
            .tenantId(tenantId)
            .tenantName(tenantName)
            .createdAt(now)
            .lastAccessedAt(now)
            .active(true)
            .schemaName(schemaName)
            .metadata(new HashMap<>())
            .build();
        
        tenants.put(tenantId, tenantContext);
        tenantSchemas.put(tenantId, schemaName);
        
        log.info("Created new tenant: {} with schema: {}", tenantId, schemaName);
        
        return tenantContext;
    }
    
    @Override
    public Optional<TenantContext> getTenant(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(tenants.get(tenantId));
    }
    
    @Override
    public TenantContext getCurrentTenant() {
        TenantContext context = tenantContextHolder.getTenantContext();
        if (context == null) {
            throw new IllegalStateException("No tenant context set for current request");
        }
        return context;
    }
    
    @Override
    public void setCurrentTenant(TenantContext tenantContext) {
        if (tenantContext == null) {
            throw new IllegalArgumentException("Tenant context cannot be null");
        }
        if (!tenantContext.isValid()) {
            throw new IllegalArgumentException("Invalid tenant context");
        }
        
        // Verify tenant exists
        if (!tenants.containsKey(tenantContext.getTenantId())) {
            throw new IllegalArgumentException("Tenant does not exist: " + tenantContext.getTenantId());
        }
        
        tenantContextHolder.setTenantContext(tenantContext);
        
        // Update last accessed time
        TenantContext stored = tenants.get(tenantContext.getTenantId());
        if (stored != null) {
            stored.setLastAccessedAt(LocalDateTime.now());
        }
    }
    
    @Override
    public void clearCurrentTenant() {
        tenantContextHolder.clear();
    }
    
    @Override
    public List<TenantContext> listAllTenants() {
        return new ArrayList<>(tenants.values());
    }
    
    @Override
    public boolean deleteTenant(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return false;
        }
        
        TenantContext removed = tenants.remove(tenantId);
        if (removed != null) {
            tenantSchemas.remove(tenantId);
            log.info("Deleted tenant: {}", tenantId);
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean validateTenantAccess(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            return false;
        }
        
        try {
            TenantContext currentTenant = getCurrentTenant();
            return currentTenant.getTenantId().equals(tenantId);
        } catch (IllegalStateException e) {
            // No tenant context set
            return false;
        }
    }
    
    @Override
    public String getTenantSchemaName(String tenantId) {
        if (tenantId == null || tenantId.isEmpty()) {
            throw new IllegalArgumentException("Tenant ID cannot be null or empty");
        }
        
        String schemaName = tenantSchemas.get(tenantId);
        if (schemaName == null) {
            throw new IllegalArgumentException("Tenant not found: " + tenantId);
        }
        
        return schemaName;
    }
    
    /**
     * Generates a unique tenant ID.
     * Format: tenant_<UUID>
     */
    private String generateTenantId() {
        return "tenant_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Generates a schema name for a tenant.
     * Format: schema_<tenant_id>
     */
    private String generateSchemaName(String tenantId) {
        return "schema_" + tenantId;
    }
}
