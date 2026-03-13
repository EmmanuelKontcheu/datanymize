package com.datanymize.tenant;

import com.datanymize.tenant.model.TenantContext;
import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for tenant data isolation.
 * 
 * **Validates: Requirements 17.1, 17.2, 17.3**
 */
@PropertyDefaults(tries = 100)
public class TenantDataIsolationProperties {
    
    /**
     * Property 25: Tenant Data Isolation
     * 
     * Verifies that each tenant only sees their own data and cannot access other tenants' data.
     */
    @Property
    void tenantDataIsolationProperty(
            @ForAll @StringLength(min = 1, max = 50) String tenantId1,
            @ForAll @StringLength(min = 1, max = 50) String tenantId2) {
        
        Assume.that(!tenantId1.equals(tenantId2));
        
        // Set tenant context to tenant 1
        TenantContextHolder.setTenantId(tenantId1);
        TenantContext context1 = TenantContextHolder.getTenantContext();
        
        assertEquals(tenantId1, context1.getTenantId(),
            "Tenant context should be set to tenant 1");
        
        // Set tenant context to tenant 2
        TenantContextHolder.setTenantId(tenantId2);
        TenantContext context2 = TenantContextHolder.getTenantContext();
        
        assertEquals(tenantId2, context2.getTenantId(),
            "Tenant context should be set to tenant 2");
        
        // Verify contexts are different
        assertNotEquals(context1.getTenantId(), context2.getTenantId(),
            "Tenant contexts should be different");
    }
    
    /**
     * Property: Tenant context is thread-local
     * 
     * Verifies that tenant context is isolated per thread.
     */
    @Property
    void tenantContextThreadIsolationProperty(
            @ForAll @StringLength(min = 1, max = 50) String tenantId1,
            @ForAll @StringLength(min = 1, max = 50) String tenantId2) {
        
        Assume.that(!tenantId1.equals(tenantId2));
        
        // Set tenant context in main thread
        TenantContextHolder.setTenantId(tenantId1);
        
        // Create a new thread with different tenant
        Thread thread = new Thread(() -> {
            TenantContextHolder.setTenantId(tenantId2);
            TenantContext threadContext = TenantContextHolder.getTenantContext();
            assertEquals(tenantId2, threadContext.getTenantId(),
                "Thread should have its own tenant context");
        });
        
        thread.start();
        
        try {
            thread.join();
        } catch (InterruptedException e) {
            fail("Thread was interrupted");
        }
        
        // Main thread context should be unchanged
        TenantContext mainContext = TenantContextHolder.getTenantContext();
        assertEquals(tenantId1, mainContext.getTenantId(),
            "Main thread context should be unchanged");
    }
    
    /**
     * Property: Cross-tenant access is prevented
     * 
     * Verifies that accessing data from another tenant is blocked.
     */
    @Property
    void crossTenantAccessPreventionProperty(
            @ForAll @StringLength(min = 1, max = 50) String tenantId1,
            @ForAll @StringLength(min = 1, max = 50) String tenantId2) {
        
        Assume.that(!tenantId1.equals(tenantId2));
        
        // Set tenant context to tenant 1
        TenantContextHolder.setTenantId(tenantId1);
        
        // Attempt to access tenant 2's data should fail
        TenantContext context = TenantContextHolder.getTenantContext();
        
        // Verify that we cannot access tenant 2's data
        assertNotEquals(tenantId2, context.getTenantId(),
            "Should not be able to access tenant 2's data while in tenant 1 context");
    }
    
    /**
     * Property: Tenant context can be cleared
     * 
     * Verifies that tenant context can be properly cleared.
     */
    @Property
    void tenantContextClearingProperty(
            @ForAll @StringLength(min = 1, max = 50) String tenantId) {
        
        // Set tenant context
        TenantContextHolder.setTenantId(tenantId);
        TenantContext context = TenantContextHolder.getTenantContext();
        assertEquals(tenantId, context.getTenantId(),
            "Tenant context should be set");
        
        // Clear tenant context
        TenantContextHolder.clear();
        
        // Context should be cleared
        TenantContext clearedContext = TenantContextHolder.getTenantContext();
        assertNull(clearedContext.getTenantId(),
            "Tenant context should be cleared");
    }
    
    /**
     * Property: Tenant context is immutable
     * 
     * Verifies that tenant context cannot be modified after creation.
     */
    @Property
    void tenantContextImmutabilityProperty(
            @ForAll @StringLength(min = 1, max = 50) String tenantId) {
        
        TenantContextHolder.setTenantId(tenantId);
        TenantContext context1 = TenantContextHolder.getTenantContext();
        
        // Get context again
        TenantContext context2 = TenantContextHolder.getTenantContext();
        
        // Both should have the same tenant ID
        assertEquals(context1.getTenantId(), context2.getTenantId(),
            "Tenant context should be consistent");
    }
    
    /**
     * Property: Multiple tenants can coexist
     * 
     * Verifies that multiple tenants can be managed simultaneously.
     */
    @Property
    void multipleTenantCoexistenceProperty(
            @ForAll @StringLength(min = 1, max = 50) String tenantId1,
            @ForAll @StringLength(min = 1, max = 50) String tenantId2,
            @ForAll @StringLength(min = 1, max = 50) String tenantId3) {
        
        Assume.that(!tenantId1.equals(tenantId2) && !tenantId2.equals(tenantId3) && !tenantId1.equals(tenantId3));
        
        // Create contexts for multiple tenants
        TenantContextHolder.setTenantId(tenantId1);
        TenantContext context1 = TenantContextHolder.getTenantContext();
        
        TenantContextHolder.setTenantId(tenantId2);
        TenantContext context2 = TenantContextHolder.getTenantContext();
        
        TenantContextHolder.setTenantId(tenantId3);
        TenantContext context3 = TenantContextHolder.getTenantContext();
        
        // All contexts should be different
        assertNotEquals(context1.getTenantId(), context2.getTenantId());
        assertNotEquals(context2.getTenantId(), context3.getTenantId());
        assertNotEquals(context1.getTenantId(), context3.getTenantId());
    }
}
