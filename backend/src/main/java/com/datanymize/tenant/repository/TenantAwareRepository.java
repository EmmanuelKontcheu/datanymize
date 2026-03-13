package com.datanymize.tenant.repository;

import com.datanymize.tenant.ITenantManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for tenant-aware repositories.
 * 
 * Provides common functionality for repositories that need to enforce
 * tenant isolation at the data access layer.
 * 
 * All data access operations should be scoped to the current tenant.
 * Validates Requirement 17.5: Database-level isolation
 */
@Slf4j
@RequiredArgsConstructor
public abstract class TenantAwareRepository {
    
    protected final ITenantManager tenantManager;
    
    /**
     * Gets the current tenant ID.
     * 
     * @return The current tenant ID
     * @throws IllegalStateException if no tenant context is set
     */
    protected String getCurrentTenantId() {
        return tenantManager.getCurrentTenant().getTenantId();
    }
    
    /**
     * Gets the current tenant's schema name.
     * Used for schema-level isolation in SQL queries.
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
     * Should be called before any data access operation.
     * 
     * @param tenantId The tenant ID to validate
     * @throws IllegalArgumentException if tenant ID doesn't match current tenant
     */
    protected void validateTenantAccess(String tenantId) {
        if (!tenantManager.validateTenantAccess(tenantId)) {
            throw new IllegalArgumentException(
                "Access denied: Tenant ID does not match current tenant"
            );
        }
    }
    
    /**
     * Builds a schema-qualified table name for the current tenant.
     * 
     * @param tableName The table name
     * @return The schema-qualified table name (e.g., schema_tenant_123.table_name)
     */
    protected String getSchemaQualifiedTableName(String tableName) {
        return getCurrentTenantSchema() + "." + tableName;
    }
}
