package com.datanymize.tenant.service;

import com.datanymize.tenant.ITenantManager;
import com.datanymize.tenant.model.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for tenant-aware services.
 * 
 * Provides common functionality for services that need to enforce
 * tenant isolation at the business logic layer.
 * 
 * All service operations should be scoped to the current tenant.
 * Validates Requirement 17.2: Ensure only own tenant data is visible
 */
@Slf4j
@RequiredArgsConstructor
public abstract class TenantAwareService {
    
    protected final ITenantManager tenantManager;
    
    /**
     * Gets the current tenant context.
     * 
     * @return The current TenantContext
     * @throws IllegalStateException if no tenant context is set
     */
    protected TenantContext getCurrentTenantContext() {
        return tenantManager.getCurrentTenant();
    }
    
    /**
     * Gets the current tenant ID.
     * 
     * @return The current tenant ID
     * @throws IllegalStateException if no tenant context is set
     */
    protected String getCurrentTenantId() {
        return getCurrentTenantContext().getTenantId();
    }
    
    /**
     * Gets the current tenant's schema name.
     * 
     * @return The schema name for the current tenant
     * @throws IllegalStateException if no tenant context is set
     */
    protected String getCurrentTenantSchema() {
        String tenantId = getCurrentTenantId();
        return tenantManager.getTenantSchemaName(tenantId);
    }
    
    /**
     * Validates that the given tenant ID matches the current tenant.
     * Should be called at the start of any service method that operates on tenant data.
     * 
     * Requirement 17.3: When a user tries to access data of another tenant, 
     * the system SHALL return an error
     * 
     * @param tenantId The tenant ID to validate
     * @throws IllegalArgumentException if tenant ID doesn't match current tenant
     */
    protected void validateTenantAccess(String tenantId) {
        if (!tenantManager.validateTenantAccess(tenantId)) {
            log.warn("Unauthorized tenant access attempt: {} vs current: {}", 
                tenantId, getCurrentTenantId());
            throw new IllegalArgumentException(
                "Access denied: Cannot access data from another tenant"
            );
        }
    }
    
    /**
     * Validates that the given tenant ID exists and is active.
     * 
     * @param tenantId The tenant ID to validate
     * @throws IllegalArgumentException if tenant doesn't exist or is inactive
     */
    protected void validateTenantExists(String tenantId) {
        var tenant = tenantManager.getTenant(tenantId);
        if (tenant.isEmpty() || !tenant.get().isActive()) {
            throw new IllegalArgumentException("Tenant not found or inactive: " + tenantId);
        }
    }
    
    /**
     * Logs a tenant-scoped operation.
     * 
     * @param operation The operation name
     * @param details Additional details about the operation
     */
    protected void logTenantOperation(String operation, String details) {
        log.info("Tenant operation [{}]: {} - Tenant: {}", 
            operation, details, getCurrentTenantId());
    }
}
