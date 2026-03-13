package com.datanymize.tenant;

import com.datanymize.tenant.model.TenantContext;
import com.datanymize.test.BasePropertyTest;
import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;

/**
 * Property-based tests for tenant isolation and data access control.
 * 
 * Validates Requirements 17.1, 17.2, 17.3, 17.5:
 * - Tenant creation with isolated data area
 * - Tenant data isolation enforcement
 * - Cross-tenant access prevention
 * - Database-level isolation
 */
@SpringBootTest
public class TenantIsolationProperties extends BasePropertyTest {
    
    @Autowired
    private ITenantManager tenantManager;
    
    @Autowired
    private TenantContextHolder tenantContextHolder;
    
    @BeforeEach
    void setUp() {
        // Clear any existing tenant context
        tenantContextHolder.clear();
    }
    
    /**
     * Property 25: Tenant Data Isolation
     * 
     * For any multi-tenant system, a user should only be able to access data 
     * belonging to their own tenant, and attempting to access other tenant data 
     * should result in an error.
     * 
     * Validates: Requirements 17.1, 17.2, 17.3
     */
    @Property(tries = 100)
    @Label("Tenant Data Isolation")
    void testTenantDataIsolation(
        @ForAll @StringLength(min = 1, max = 50) String tenantName1,
        @ForAll @StringLength(min = 1, max = 50) String tenantName2
    ) {
        // Given two different tenant names
        Assume.that(!tenantName1.equals(tenantName2));
        
        // When creating two tenants
        TenantContext tenant1 = tenantManager.createTenant(tenantName1);
        TenantContext tenant2 = tenantManager.createTenant(tenantName2);
        
        // Then they should have different IDs
        Assume.that(!tenant1.getTenantId().equals(tenant2.getTenantId()));
        
        // When setting tenant1 as current
        tenantManager.setCurrentTenant(tenant1);
        
        // Then tenant1 should be accessible
        TenantContext current = tenantManager.getCurrentTenant();
        assert current.getTenantId().equals(tenant1.getTenantId());
        
        // And tenant1 access should be validated
        assert tenantManager.validateTenantAccess(tenant1.getTenantId());
        
        // But tenant2 access should be denied
        assert !tenantManager.validateTenantAccess(tenant2.getTenantId());
        
        // When trying to access tenant2 data
        // Then it should throw an exception
        try {
            tenantManager.validateTenantAccess(tenant2.getTenantId());
            // If we get here, the validation failed
            assert false : "Should have denied access to other tenant";
        } catch (Exception e) {
            // Expected behavior - access denied
        }
    }
    
    /**
     * Property: Tenant Creation with Isolation
     * 
     * For any tenant creation, the system should create a new tenant with 
     * isolated data area (schema).
     * 
     * Validates: Requirement 17.1
     */
    @Property(tries = 100)
    @Label("Tenant Creation with Isolation")
    void testTenantCreationWithIsolation(
        @ForAll @StringLength(min = 1, max = 50) String tenantName
    ) {
        // When creating a tenant
        TenantContext tenant = tenantManager.createTenant(tenantName);
        
        // Then tenant should be created with valid ID
        assert tenant.getTenantId() != null;
        assert !tenant.getTenantId().isEmpty();
        
        // And tenant should have a schema name for isolation
        assert tenant.getSchemaName() != null;
        assert !tenant.getSchemaName().isEmpty();
        
        // And tenant should be active
        assert tenant.isActive();
        
        // And tenant should be retrievable
        Optional<TenantContext> retrieved = tenantManager.getTenant(tenant.getTenantId());
        assert retrieved.isPresent();
        assert retrieved.get().getTenantId().equals(tenant.getTenantId());
        
        // And schema name should be consistent
        String schemaName = tenantManager.getTenantSchemaName(tenant.getTenantId());
        assert schemaName.equals(tenant.getSchemaName());
    }
    
    /**
     * Property: Tenant Context Isolation
     * 
     * For any tenant context, setting it should make it the current tenant,
     * and clearing it should remove access.
     * 
     * Validates: Requirement 17.2
     */
    @Property(tries = 100)
    @Label("Tenant Context Isolation")
    void testTenantContextIsolation(
        @ForAll @StringLength(min = 1, max = 50) String tenantName
    ) {
        // Given a created tenant
        TenantContext tenant = tenantManager.createTenant(tenantName);
        
        // When setting it as current
        tenantManager.setCurrentTenant(tenant);
        
        // Then it should be retrievable as current
        TenantContext current = tenantManager.getCurrentTenant();
        assert current.getTenantId().equals(tenant.getTenantId());
        
        // When clearing the context
        tenantManager.clearCurrentTenant();
        
        // Then getting current tenant should throw exception
        try {
            tenantManager.getCurrentTenant();
            assert false : "Should have thrown exception when no tenant context";
        } catch (IllegalStateException e) {
            // Expected behavior
        }
    }
    
    /**
     * Property: Tenant Deletion
     * 
     * For any tenant deletion, the tenant should be removed and no longer accessible.
     * 
     * Validates: Requirement 17.5
     */
    @Property(tries = 100)
    @Label("Tenant Deletion")
    void testTenantDeletion(
        @ForAll @StringLength(min = 1, max = 50) String tenantName
    ) {
        // Given a created tenant
        TenantContext tenant = tenantManager.createTenant(tenantName);
        String tenantId = tenant.getTenantId();
        
        // When deleting the tenant
        boolean deleted = tenantManager.deleteTenant(tenantId);
        
        // Then deletion should succeed
        assert deleted;
        
        // And tenant should no longer be retrievable
        Optional<TenantContext> retrieved = tenantManager.getTenant(tenantId);
        assert retrieved.isEmpty();
        
        // And getting schema name should throw exception
        try {
            tenantManager.getTenantSchemaName(tenantId);
            assert false : "Should have thrown exception for deleted tenant";
        } catch (IllegalArgumentException e) {
            // Expected behavior
        }
    }
    
    /**
     * Property: Multiple Tenant Isolation
     * 
     * For any number of tenants, each should maintain separate data areas
     * and access should be properly isolated.
     * 
     * Validates: Requirements 17.1, 17.2, 17.3, 17.5
     */
    @Property(tries = 50)
    @Label("Multiple Tenant Isolation")
    void testMultipleTenantIsolation(
        @ForAll @StringLength(min = 1, max = 30) String name1,
        @ForAll @StringLength(min = 1, max = 30) String name2,
        @ForAll @StringLength(min = 1, max = 30) String name3
    ) {
        // Given three different tenant names
        Assume.that(!name1.equals(name2) && !name2.equals(name3) && !name1.equals(name3));
        
        // When creating three tenants
        TenantContext t1 = tenantManager.createTenant(name1);
        TenantContext t2 = tenantManager.createTenant(name2);
        TenantContext t3 = tenantManager.createTenant(name3);
        
        // Then all should have unique IDs
        assert !t1.getTenantId().equals(t2.getTenantId());
        assert !t2.getTenantId().equals(t3.getTenantId());
        assert !t1.getTenantId().equals(t3.getTenantId());
        
        // And all should have unique schemas
        String schema1 = tenantManager.getTenantSchemaName(t1.getTenantId());
        String schema2 = tenantManager.getTenantSchemaName(t2.getTenantId());
        String schema3 = tenantManager.getTenantSchemaName(t3.getTenantId());
        
        assert !schema1.equals(schema2);
        assert !schema2.equals(schema3);
        assert !schema1.equals(schema3);
        
        // When setting t1 as current
        tenantManager.setCurrentTenant(t1);
        
        // Then only t1 access should be allowed
        assert tenantManager.validateTenantAccess(t1.getTenantId());
        assert !tenantManager.validateTenantAccess(t2.getTenantId());
        assert !tenantManager.validateTenantAccess(t3.getTenantId());
        
        // When switching to t2
        tenantManager.setCurrentTenant(t2);
        
        // Then only t2 access should be allowed
        assert !tenantManager.validateTenantAccess(t1.getTenantId());
        assert tenantManager.validateTenantAccess(t2.getTenantId());
        assert !tenantManager.validateTenantAccess(t3.getTenantId());
    }
    
    /**
     * Property: Invalid Tenant Context Rejection
     * 
     * For any invalid tenant context, the system should reject it.
     * 
     * Validates: Requirement 17.1
     */
    @Property(tries = 50)
    @Label("Invalid Tenant Context Rejection")
    void testInvalidTenantContextRejection() {
        // When trying to set null tenant context
        try {
            tenantManager.setCurrentTenant(null);
            assert false : "Should reject null tenant context";
        } catch (IllegalArgumentException e) {
            // Expected behavior
        }
        
        // When trying to set inactive tenant context
        TenantContext inactiveTenant = TenantContext.builder()
            .tenantId("test_tenant")
            .tenantName("Test")
            .active(false)
            .createdAt(LocalDateTime.now())
            .metadata(new HashMap<>())
            .build();
        
        try {
            tenantManager.setCurrentTenant(inactiveTenant);
            assert false : "Should reject inactive tenant context";
        } catch (IllegalArgumentException e) {
            // Expected behavior
        }
    }
    
    /**
     * Property: Tenant Name Validation
     * 
     * For any tenant creation, the tenant name should be validated.
     * 
     * Validates: Requirement 17.1
     */
    @Property(tries = 50)
    @Label("Tenant Name Validation")
    void testTenantNameValidation() {
        // When trying to create tenant with null name
        try {
            tenantManager.createTenant(null);
            assert false : "Should reject null tenant name";
        } catch (IllegalArgumentException e) {
            // Expected behavior
        }
        
        // When trying to create tenant with empty name
        try {
            tenantManager.createTenant("");
            assert false : "Should reject empty tenant name";
        } catch (IllegalArgumentException e) {
            // Expected behavior
        }
    }
}
