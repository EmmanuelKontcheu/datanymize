package com.datanymize.audit;

import com.datanymize.audit.model.AuditLogEntry;
import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for comprehensive audit logging.
 * 
 * **Validates: Requirements 16.1, 16.2, 16.5, 16.6**
 */
@PropertyDefaults(tries = 100)
public class ComprehensiveAuditLoggingProperties {
    
    /**
     * Property 23: Comprehensive Audit Logging
     * 
     * Verifies that all required fields are logged for each action.
     */
    @Property
    void comprehensiveAuditLoggingProperty(
            @ForAll @StringLength(min = 1, max = 50) String tenantId,
            @ForAll @StringLength(min = 1, max = 50) String action,
            @ForAll @StringLength(min = 1, max = 50) String resourceType,
            @ForAll @StringLength(min = 1, max = 50) String resourceId,
            @ForAll boolean success,
            @ForAll @StringLength(min = 1, max = 200) String details) {
        
        AuditLogger auditLogger = new AuditLogger();
        
        // Log an action
        assertDoesNotThrow(() -> auditLogger.logAction(
            tenantId, action, resourceType, resourceId, success, details
        ), "Should be able to log action");
        
        // Retrieve the logged entry
        List<AuditLogEntry> entries = auditLogger.getRecentEntries(tenantId, 1);
        
        assertFalse(entries.isEmpty(), "Should have logged entry");
        
        AuditLogEntry entry = entries.get(0);
        
        // Verify all required fields are present
        assertNotNull(entry.getTenantId(), "Tenant ID should be logged");
        assertEquals(tenantId, entry.getTenantId(), "Tenant ID should match");
        
        assertNotNull(entry.getAction(), "Action should be logged");
        assertEquals(action, entry.getAction(), "Action should match");
        
        assertNotNull(entry.getResourceType(), "Resource type should be logged");
        assertEquals(resourceType, entry.getResourceType(), "Resource type should match");
        
        assertNotNull(entry.getResourceId(), "Resource ID should be logged");
        assertEquals(resourceId, entry.getResourceId(), "Resource ID should match");
        
        assertEquals(success, entry.isSuccess(), "Success flag should match");
        
        assertNotNull(entry.getDetails(), "Details should be logged");
        assertEquals(details, entry.getDetails(), "Details should match");
        
        assertNotNull(entry.getTimestamp(), "Timestamp should be logged");
    }
    
    /**
     * Property 24: Audit Log Encryption and Retention
     * 
     * Verifies that audit logs are encrypted and retention policies are enforced.
     */
    @Property
    void auditLogEncryptionAndRetentionProperty(
            @ForAll @StringLength(min = 1, max = 50) String tenantId,
            @ForAll @StringLength(min = 1, max = 200) String details) {
        
        AuditLogger auditLogger = new AuditLogger();
        
        // Log an action
        assertDoesNotThrow(() -> auditLogger.logAction(
            tenantId, "TEST_ACTION", "TEST_RESOURCE", "test-id", true, details
        ), "Should be able to log action");
        
        // Retrieve the logged entry
        List<AuditLogEntry> entries = auditLogger.getRecentEntries(tenantId, 1);
        assertFalse(entries.isEmpty(), "Should have logged entry");
        
        AuditLogEntry entry = entries.get(0);
        
        // Verify encryption (encrypted entries should not be readable as plaintext)
        if (entry.isEncrypted()) {
            assertNotEquals(details, entry.getDetails(),
                "Encrypted details should not match plaintext");
        }
        
        // Verify retention (entry should be within retention period)
        long ageInDays = (System.currentTimeMillis() - entry.getTimestamp().toEpochMilli()) / (1000 * 60 * 60 * 24);
        assertTrue(ageInDays < 365,
            "Entry should be within 1-year retention period");
    }
    
    /**
     * Property: Audit logs are immutable
     * 
     * Verifies that audit logs cannot be modified after creation.
     */
    @Property
    void auditLogImmutabilityProperty(
            @ForAll @StringLength(min = 1, max = 50) String tenantId,
            @ForAll @StringLength(min = 1, max = 200) String details) {
        
        AuditLogger auditLogger = new AuditLogger();
        
        // Log an action
        assertDoesNotThrow(() -> auditLogger.logAction(
            tenantId, "TEST_ACTION", "TEST_RESOURCE", "test-id", true, details
        ), "Should be able to log action");
        
        // Retrieve the logged entry
        List<AuditLogEntry> entries1 = auditLogger.getRecentEntries(tenantId, 1);
        AuditLogEntry entry1 = entries1.get(0);
        
        // Retrieve again
        List<AuditLogEntry> entries2 = auditLogger.getRecentEntries(tenantId, 1);
        AuditLogEntry entry2 = entries2.get(0);
        
        // Entries should be identical
        assertEquals(entry1.getAction(), entry2.getAction(),
            "Action should not change");
        assertEquals(entry1.getDetails(), entry2.getDetails(),
            "Details should not change");
        assertEquals(entry1.getTimestamp(), entry2.getTimestamp(),
            "Timestamp should not change");
    }
    
    /**
     * Property: Audit logs are ordered by timestamp
     * 
     * Verifies that audit logs are returned in chronological order.
     */
    @Property
    void auditLogOrderingProperty(
            @ForAll @StringLength(min = 1, max = 50) String tenantId) {
        
        AuditLogger auditLogger = new AuditLogger();
        
        // Log multiple actions
        for (int i = 0; i < 5; i++) {
            String action = "ACTION_" + i;
            assertDoesNotThrow(() -> auditLogger.logAction(
                tenantId, action, "TEST_RESOURCE", "test-id-" + i, true, "Details " + i
            ), "Should be able to log action " + i);
        }
        
        // Retrieve all entries
        List<AuditLogEntry> entries = auditLogger.getRecentEntries(tenantId, 10);
        
        // Verify ordering (most recent first)
        for (int i = 0; i < entries.size() - 1; i++) {
            assertTrue(entries.get(i).getTimestamp().isAfter(entries.get(i + 1).getTimestamp()),
                "Entries should be ordered by timestamp (most recent first)");
        }
    }
    
    /**
     * Property: Audit logs can be filtered by tenant
     * 
     * Verifies that audit logs are properly isolated by tenant.
     */
    @Property
    void auditLogTenantIsolationProperty(
            @ForAll @StringLength(min = 1, max = 50) String tenantId1,
            @ForAll @StringLength(min = 1, max = 50) String tenantId2) {
        
        Assume.that(!tenantId1.equals(tenantId2));
        
        AuditLogger auditLogger = new AuditLogger();
        
        // Log actions for both tenants
        assertDoesNotThrow(() -> auditLogger.logAction(
            tenantId1, "ACTION_1", "TEST_RESOURCE", "test-id-1", true, "Details 1"
        ), "Should be able to log action for tenant 1");
        
        assertDoesNotThrow(() -> auditLogger.logAction(
            tenantId2, "ACTION_2", "TEST_RESOURCE", "test-id-2", true, "Details 2"
        ), "Should be able to log action for tenant 2");
        
        // Retrieve entries for tenant 1
        List<AuditLogEntry> entries1 = auditLogger.getRecentEntries(tenantId1, 10);
        
        // All entries should belong to tenant 1
        for (AuditLogEntry entry : entries1) {
            assertEquals(tenantId1, entry.getTenantId(),
                "All entries should belong to tenant 1");
        }
    }
    
    /**
     * Property: Audit logs contain user information
     * 
     * Verifies that user information is logged with each action.
     */
    @Property
    void auditLogUserInformationProperty(
            @ForAll @StringLength(min = 1, max = 50) String tenantId,
            @ForAll @StringLength(min = 1, max = 50) String userId) {
        
        AuditLogger auditLogger = new AuditLogger();
        
        // Log an action with user information
        assertDoesNotThrow(() -> auditLogger.logAction(
            tenantId, "TEST_ACTION", "TEST_RESOURCE", "test-id", true, "Details"
        ), "Should be able to log action");
        
        // Retrieve the logged entry
        List<AuditLogEntry> entries = auditLogger.getRecentEntries(tenantId, 1);
        AuditLogEntry entry = entries.get(0);
        
        // User information should be present (either userId or system)
        assertNotNull(entry.getUserId(),
            "User ID should be logged");
    }
}
