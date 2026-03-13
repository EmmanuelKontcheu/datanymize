package com.datanymize.tenant;

import com.datanymize.tenant.model.TenantContext;
import org.springframework.stereotype.Component;

/**
 * Thread-local holder for tenant context.
 * Stores the current tenant context for the executing thread/request.
 * 
 * This is used to propagate tenant information through the request lifecycle
 * without passing it as a parameter through every method call.
 */
@Component
public class TenantContextHolder {
    
    private static final ThreadLocal<TenantContext> TENANT_CONTEXT = new ThreadLocal<>();
    
    /**
     * Sets the tenant context for the current thread.
     * 
     * @param tenantContext The tenant context to set
     */
    public void setTenantContext(TenantContext tenantContext) {
        if (tenantContext != null && !tenantContext.isValid()) {
            throw new IllegalArgumentException("Invalid tenant context");
        }
        TENANT_CONTEXT.set(tenantContext);
    }
    
    /**
     * Gets the tenant context for the current thread.
     * 
     * @return The current tenant context, or null if not set
     */
    public TenantContext getTenantContext() {
        return TENANT_CONTEXT.get();
    }
    
    /**
     * Gets the current tenant ID.
     * 
     * @return The current tenant ID, or null if not set
     */
    public String getCurrentTenantId() {
        TenantContext context = TENANT_CONTEXT.get();
        return context != null ? context.getTenantId() : null;
    }
    
    /**
     * Checks if a tenant context is currently set.
     * 
     * @return true if tenant context is set, false otherwise
     */
    public boolean hasTenantContext() {
        return TENANT_CONTEXT.get() != null;
    }
    
    /**
     * Clears the tenant context for the current thread.
     * Should be called in a finally block or filter cleanup.
     */
    public void clear() {
        TENANT_CONTEXT.remove();
    }
}
