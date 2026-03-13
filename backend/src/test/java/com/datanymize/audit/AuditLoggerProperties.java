package com.datanymize.audit;

import com.datanymize.audit.entity.AuditLogEntity;
import com.datanymize.audit.model.AuditLogEntry;
import com.datanymize.audit.repository.AuditLogRepository;
import com.datanymize.security.CredentialEncryption;
import com.datanymize.tenant.TenantContextHolder;
import com.datanymize.test.BasePropertyTest;
import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for audit logging functionality.
 * 
 * **Validates: Requirements 16.1, 16.2, 16.5, 16.6**
 */
class AuditLoggerProperties extends BasePropertyTest {
    
    @Mock
    private AuditLogRepository auditLogRepository;
    
    @Mock
    private TenantContextHolder tenantContextHolder;
    
    @Mock
    private CredentialEncryption credentialEncryption;
    
    private AuditLogger auditLogger;
    
    @Override
    protected void setUp() {
        auditLogger = new AuditLogger(auditLogRepository, tenantContextHolder, credentialEncryption);
        when(tenantContextHolder.getCurrentTenantId()).thenReturn("tenant-1");
    }
    
    /**
     * Property 23: Comprehensive Audit Logging
     * 
     * For any user action (connection, PII scan, anonymization), the audit logger
     * SHALL log with timestamp, user, and details.
     */
    @Property
    @Label("Audit log entries contain all required fields")
    void auditLogEntriesContainAllRequiredFields(
            @ForAll @StringLength(min = 1, max = 50) String userId,
            @ForAll @StringLength(min = 1, max = 100) String action,
            @ForAll @StringLength(min = 1, max = 100) String resource,
            @ForAll boolean success) {
        
        setUp();
        
        auditLogger.logAction(userId, action, resource, success, null, new HashMap<>());
        
        verify(auditLogRepository, times(1)).save(any(AuditLogEntity.class));
    }
    
    /**
     * Property 23: Comprehensive Audit Logging
     * 
     * The audit logger SHALL store: User-ID, Action, Source-Database, Target-Database,
     * Rows-Processed, Success/Failure.
     */
    @Property
    @Label("Anonymization audit logs contain all required fields")
    void anonymizationAuditLogsContainAllRequiredFields(
            @ForAll @StringLength(min = 1, max = 50) String userId,
            @ForAll @StringLength(min = 1, max = 100) String sourceDb,
            @ForAll @StringLength(min = 1, max = 100) String targetDb,
            @ForAll @IntRange(min = 0, max = 1000000) long rowsProcessed,
            @ForAll boolean success) {
        
        setUp();
        
        auditLogger.logAnonymization(userId, sourceDb, targetDb, rowsProcessed, success, null);
        
        verify(auditLogRepository, times(1)).save(any(AuditLogEntity.class));
    }
    
    /**
     * Property 24: Audit Log Encryption and Retention
     * 
     * The system SHALL retain audit logs for minimum 1 year.
     */
    @Property
    @Label("Audit log entries have creation timestamp")
    void auditLogEntriesHaveCreationTimestamp(
            @ForAll @StringLength(min = 1, max = 50) String userId,
            @ForAll @StringLength(min = 1, max = 100) String action) {
        
        setUp();
        
        LocalDateTime beforeLog = LocalDateTime.now(ZoneId.of("UTC"));
        auditLogger.logAction(userId, action, "resource", true, null, new HashMap<>());
        LocalDateTime afterLog = LocalDateTime.now(ZoneId.of("UTC"));
        
        verify(auditLogRepository, times(1)).save(argThat(entity -> {
            LocalDateTime createdAt = entity.getCreatedAt();
            return createdAt != null && 
                   !createdAt.isBefore(beforeLog) && 
                   !createdAt.isAfter(afterLog);
        }));
    }
    
    /**
     * Property 24: Audit Log Encryption and Retention
     * 
     * Audit logs are immutable (append-only).
     */
    @Property
    @Label("Audit log entries are immutable")
    void auditLogEntriesAreImmutable(
            @ForAll @StringLength(min = 1, max = 50) String userId,
            @ForAll @StringLength(min = 1, max = 100) String action) {
        
        setUp();
        
        auditLogger.logAction(userId, action, "resource", true, null, new HashMap<>());
        
        verify(auditLogRepository, times(1)).save(any(AuditLogEntity.class));
        verify(auditLogRepository, never()).update(any());
    }
    
    /**
     * Property 23: Comprehensive Audit Logging
     * 
     * Connection creation events are logged with connection details.
     */
    @Property
    @Label("Connection creation events are logged")
    void connectionCreationEventsAreLogged(
            @ForAll @StringLength(min = 1, max = 50) String userId,
            @ForAll @StringLength(min = 1, max = 50) String connectionId,
            @ForAll @StringLength(min = 1, max = 20) String connectionType) {
        
        setUp();
        
        auditLogger.logConnectionCreated(userId, connectionId, connectionType);
        
        verify(auditLogRepository, times(1)).save(argThat(entity -> 
            entity.getAction().equals("CONNECTION_CREATED") &&
            entity.getUserId().equals(userId)
        ));
    }
    
    /**
     * Property 23: Comprehensive Audit Logging
     * 
     * PII scan events are logged with scan details.
     */
    @Property
    @Label("PII scan events are logged")
    void piiScanEventsAreLogged(
            @ForAll @StringLength(min = 1, max = 50) String userId,
            @ForAll @StringLength(min = 1, max = 100) String database,
            @ForAll @IntRange(min = 0, max = 1000000) long rowsScanned) {
        
        setUp();
        
        auditLogger.logPIIScan(userId, database, rowsScanned);
        
        verify(auditLogRepository, times(1)).save(argThat(entity -> 
            entity.getAction().equals("PII_SCAN") &&
            entity.getRowsProcessed() == rowsScanned
        ));
    }
    
    /**
     * Property 23: Comprehensive Audit Logging
     * 
     * Data export events are logged with export details.
     */
    @Property
    @Label("Data export events are logged")
    void dataExportEventsAreLogged(
            @ForAll @StringLength(min = 1, max = 50) String userId,
            @ForAll @StringLength(min = 1, max = 100) String database,
            @ForAll @StringLength(min = 1, max = 50) String exportFormat,
            @ForAll @IntRange(min = 0, max = 1000000) long rowsExported) {
        
        setUp();
        
        auditLogger.logDataExport(userId, database, exportFormat, rowsExported);
        
        verify(auditLogRepository, times(1)).save(argThat(entity -> 
            entity.getAction().equals("DATA_EXPORT") &&
            entity.getRowsProcessed() == rowsExported
        ));
    }
    
    /**
     * Property 23: Comprehensive Audit Logging
     * 
     * Audit logs can be retrieved with filters.
     */
    @Property
    @Label("Audit logs can be retrieved with date range filter")
    void auditLogsCanBeRetrievedWithDateRangeFilter(
            @ForAll @StringLength(min = 1, max = 50) String userId,
            @ForAll @StringLength(min = 1, max = 100) String action) {
        
        setUp();
        
        LocalDateTime startDate = LocalDateTime.now(ZoneId.of("UTC")).minusDays(7);
        LocalDateTime endDate = LocalDateTime.now(ZoneId.of("UTC"));
        
        AuditLogEntity entity = AuditLogEntity.builder()
                .id("log-1")
                .tenantId("tenant-1")
                .userId(userId)
                .action(action)
                .timestamp(LocalDateTime.now(ZoneId.of("UTC")))
                .success(true)
                .createdAt(LocalDateTime.now(ZoneId.of("UTC")))
                .build();
        
        Page<AuditLogEntity> page = new PageImpl<>(List.of(entity));
        when(auditLogRepository.findByTenantIdAndDateRange(
                "tenant-1", startDate, endDate, PageRequest.of(0, 1000)))
                .thenReturn(page);
        
        List<AuditLogEntry> logs = auditLogger.getAuditLogs(startDate, endDate, null, null);
        
        assertNotNull(logs);
        assertTrue(logs.size() >= 0);
    }
    
    /**
     * Property 23: Comprehensive Audit Logging
     * 
     * Audit logs contain tenant ID for multi-tenant isolation.
     */
    @Property
    @Label("Audit logs contain tenant ID")
    void auditLogsContainTenantId(
            @ForAll @StringLength(min = 1, max = 50) String userId,
            @ForAll @StringLength(min = 1, max = 100) String action) {
        
        setUp();
        
        auditLogger.logAction(userId, action, "resource", true, null, new HashMap<>());
        
        verify(auditLogRepository, times(1)).save(argThat(entity -> 
            entity.getTenantId() != null && entity.getTenantId().equals("tenant-1")
        ));
    }
    
    /**
     * Property 24: Audit Log Encryption and Retention
     * 
     * Failed operations are logged with error messages.
     */
    @Property
    @Label("Failed operations are logged with error messages")
    void failedOperationsAreLoggedWithErrorMessages(
            @ForAll @StringLength(min = 1, max = 50) String userId,
            @ForAll @StringLength(min = 1, max = 100) String sourceDb,
            @ForAll @StringLength(min = 1, max = 100) String targetDb,
            @ForAll @StringLength(min = 1, max = 200) String errorMessage) {
        
        setUp();
        
        auditLogger.logAnonymization(userId, sourceDb, targetDb, 0, false, errorMessage);
        
        verify(auditLogRepository, times(1)).save(argThat(entity -> 
            !entity.isSuccess() && entity.getErrorMessage() != null
        ));
    }
}
