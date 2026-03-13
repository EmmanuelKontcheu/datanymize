package com.datanymize.tenant;

import com.datanymize.tenant.model.TenantContext;

import java.util.List;
import java.util.Optional;

/**
 * Interface for tenant lifecycle management.
 * Handles tenant creation, retrieval, deletion, and isolation enforcement.
 * 
 * Validates Requirements 17.1, 17.2, 17.3, 17.5:
 * - Tenant creation with isolated data area
 * - Tenant data isolation enforcement
 * - Cross-tenant access prevention
 * - Database-level isolation
 */
public interface ITenantManager {
    
    /**
     * Creates a new tenant with isolated data area.
     * 
     * Requirement 17.1: When a user registers, the Tenant_Manager SHALL create 
     * a new tenant with isolated data area
     * 
     * @param tenantName Human-readable name for the tenant
     * @return The created TenantContext
     * @throws IllegalArgumentException if tenantName is null or empty
     */
    TenantContext createTenant(String tenantName);
    
    /**
     * Retrieves a tenant by ID.
     * 
     * @param tenantId The tenant identifier
     * @return Optional containing the TenantContext if found
     */
    Optional<TenantContext> getTenant(String tenantId);
    
    /**
     * Retrieves the current tenant from the request context.
     * 
     * Requirement 17.2: When a user accesses data, the Tenant_Manager SHALL 
     * ensure only data of own tenant is visible
     * 
     * @return The current TenantContext
     * @throws IllegalStateException if no tenant is set in the current context
     */
    TenantContext getCurrentTenant();
    
    /**
     * Sets the current tenant in the request context.
     * 
     * @param tenantContext The tenant context to set
     * @throws IllegalArgumentException if tenantContext is invalid
     */
    void setCurrentTenant(TenantContext tenantContext);
    
    /**
     * Clears the current tenant from the request context.
     */
    void clearCurrentTenant();
    
    /**
     * Lists all tenants (admin operation).
     * 
     * @return List of all TenantContexts
     */
    List<TenantContext> listAllTenants();
    
    /**
     * Deletes a tenant and all associated data.
     * 
     * Requirement 17.5: The system SHALL isolate tenant data at database level
     * 
     * @param tenantId The tenant identifier
     * @return true if deletion was successful, false if tenant not found
     * @throws IllegalStateException if deletion fails
     */
    boolean deleteTenant(String tenantId);
    
    /**
     * Validates that a user has access to a specific tenant.
     * 
     * Requirement 17.3: When a user tries to access data of another tenant, 
     * the system SHALL return an error
     * 
     * @param tenantId The tenant identifier to validate access for
     * @return true if access is allowed, false otherwise
     */
    boolean validateTenantAccess(String tenantId);
    
    /**
     * Gets the database schema name for a tenant.
     * Used for schema-level isolation.
     * 
     * @param tenantId The tenant identifier
     * @return The schema name for the tenant
     */
    String getTenantSchemaName(String tenantId);
}
