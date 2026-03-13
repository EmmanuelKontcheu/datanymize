package com.datanymize.audit;

import com.datanymize.audit.entity.AuditLogEntity;
import com.datanymize.audit.model.AuditLogEntry;
import com.datanymize.audit.repository.AuditLogRepository;
import com.datanymize.security.CredentialEncryption;
import com.datanymize.tenant.TenantContextHolder;
import com.datanymize.tenant.model.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
 * Unit tests for AuditLogger implementation.
 */
@ExtendWith(MockitoExtension.class)
class AuditLoggerTest {
    
    @Mock
    private AuditLogRepository auditLogRepository;
    
    @Mock
    private TenantContextHolder tenantContextHolder;
    
    @Mock
    private CredentialEncryption credentialEncryption;
    
    private AuditLogger auditLogger;
    
    @BeforeEach
    void setUp() {
        auditLogger = new AuditLogger(auditLogRepository, tenantContextHolder, credentialEncryption);
        when(tenantContextHolder.getCurrentTenantId()).thenReturn("tenant-1");
    }
    
    @Test
    void testLogAction() {
        String userId = "user-1";
        String action = "TEST_ACTION";
        String resource = "test-resource";
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key", "value");
        
        auditLogger.logAction(userId, action, resource, true, null, metadata);
        
        verify(auditLogRepository, times(1)).save(any(AuditLogEntity.class));
    }
    
    @Test
    void testLogConnectionCreated() {
        String userId = "user-1";
        String connectionId = "conn-1";
        String connectionType = "postgresql";
        
        auditLogger.logConnectionCreated(userId, connectionId, connectionType);
        
        verify(auditLogRepository, times(1)).save(any(AuditLogEntity.class));
    }
    
    @Test
    void testLogConnectionDeleted() {
        String userId = "user-1";
        String connectionId = "conn-1";
        
        auditLogger.logConnectionDeleted(userId, connectionId);
        
        verify(auditLogRepository, times(1)).save(any(AuditLogEntity.class));
    }
    
    @Test
    void testLogSchemaExtraction() {
        String userId = "user-1";
        String sourceDatabase = "source-db";
        String targetDatabase = "target-db";
        long rowCount = 1000;
        
        auditLogger.logSchemaExtraction(userId, sourceDatabase, targetDatabase, rowCount);
        
        verify(auditLogRepository, times(1)).save(any(AuditLogEntity.class));
    }
    
    @Test
    void testLogPIIScan() {
        String userId = "user-1";
        String database = "test-db";
        long rowsScanned = 5000;
        
        auditLogger.logPIIScan(userId, database, rowsScanned);
        
        verify(auditLogRepository, times(1)).save(any(AuditLogEntity.class));
    }
    
    @Test
    void testLogAnonymization_Success() {
        String userId = "user-1";
        String sourceDatabase = "source-db";
        String targetDatabase = "target-db";
        long rowsProcessed = 2000;
        
        auditLogger.logAnonymization(userId, sourceDatabase, targetDatabase, rowsProcessed, true, null);
        
        verify(auditLogRepository, times(1)).save(any(AuditLogEntity.class));
    }
    
    @Test
    void testLogAnonymization_Failure() {
        String userId = "user-1";
        String sourceDatabase = "source-db";
        String targetDatabase = "target-db";
        long rowsProcessed = 1000;
        String errorMessage = "Connection timeout";
        
        auditLogger.logAnonymization(userId, sourceDatabase, targetDatabase, rowsProcessed, false, errorMessage);
        
        verify(auditLogRepository, times(1)).save(any(AuditLogEntity.class));
    }
    
    @Test
    void testLogDataExport() {
        String userId = "user-1";
        String database = "test-db";
        String exportFormat = "SQL_DUMP";
        long rowsExported = 3000;
        
        auditLogger.logDataExport(userId, database, exportFormat, rowsExported);
        
        verify(auditLogRepository, times(1)).save(any(AuditLogEntity.class));
    }
    
    @Test
    void testGetAuditLogs() {
        LocalDateTime startDate = LocalDateTime.now(ZoneId.of("UTC")).minusDays(7);
        LocalDateTime endDate = LocalDateTime.now(ZoneId.of("UTC"));
        
        AuditLogEntity entity = AuditLogEntity.builder()
                .id("log-1")
                .tenantId("tenant-1")
                .userId("user-1")
                .action("TEST_ACTION")
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
        assertEquals(1, logs.size());
        assertEquals("TEST_ACTION", logs.get(0).getAction());
    }
    
    @Test
    void testGetAuditLogs_WithUserFilter() {
        LocalDateTime startDate = LocalDateTime.now(ZoneId.of("UTC")).minusDays(7);
        LocalDateTime endDate = LocalDateTime.now(ZoneId.of("UTC"));
        String userId = "user-1";
        
        AuditLogEntity entity = AuditLogEntity.builder()
                .id("log-1")
                .tenantId("tenant-1")
                .userId(userId)
                .action("TEST_ACTION")
                .timestamp(LocalDateTime.now(ZoneId.of("UTC")))
                .success(true)
                .createdAt(LocalDateTime.now(ZoneId.of("UTC")))
                .build();
        
        Page<AuditLogEntity> page = new PageImpl<>(List.of(entity));
        when(auditLogRepository.findByTenantIdAndDateRange(
                "tenant-1", startDate, endDate, PageRequest.of(0, 1000)))
                .thenReturn(page);
        
        List<AuditLogEntry> logs = auditLogger.getAuditLogs(startDate, endDate, userId, null);
        
        assertNotNull(logs);
        assertEquals(1, logs.size());
        assertEquals(userId, logs.get(0).getUserId());
    }
    
    @Test
    void testGetAuditLogs_WithActionFilter() {
        LocalDateTime startDate = LocalDateTime.now(ZoneId.of("UTC")).minusDays(7);
        LocalDateTime endDate = LocalDateTime.now(ZoneId.of("UTC"));
        String action = "ANONYMIZATION";
        
        AuditLogEntity entity = AuditLogEntity.builder()
                .id("log-1")
                .tenantId("tenant-1")
                .userId("user-1")
                .action(action)
                .timestamp(LocalDateTime.now(ZoneId.of("UTC")))
                .success(true)
                .createdAt(LocalDateTime.now(ZoneId.of("UTC")))
                .build();
        
        Page<AuditLogEntity> page = new PageImpl<>(List.of(entity));
        when(auditLogRepository.findByTenantIdAndDateRange(
                "tenant-1", startDate, endDate, PageRequest.of(0, 1000)))
                .thenReturn(page);
        
        List<AuditLogEntry> logs = auditLogger.getAuditLogs(startDate, endDate, null, action);
        
        assertNotNull(logs);
        assertEquals(1, logs.size());
        assertEquals(action, logs.get(0).getAction());
    }
    
    @Test
    void testAuditLogEntry_AllFieldsSet() {
        AuditLogEntry entry = AuditLogEntry.builder()
                .id("log-1")
                .tenantId("tenant-1")
                .userId("user-1")
                .action("TEST_ACTION")
                .timestamp(LocalDateTime.now(ZoneId.of("UTC")))
                .sourceDatabase("source-db")
                .targetDatabase("target-db")
                .rowsProcessed(1000)
                .success(true)
                .errorMessage(null)
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .metadata(new HashMap<>())
                .createdAt(LocalDateTime.now(ZoneId.of("UTC")))
                .build();
        
        assertNotNull(entry.getId());
        assertEquals("tenant-1", entry.getTenantId());
        assertEquals("user-1", entry.getUserId());
        assertEquals("TEST_ACTION", entry.getAction());
        assertEquals(1000, entry.getRowsProcessed());
        assertTrue(entry.isSuccess());
        assertEquals("192.168.1.1", entry.getIpAddress());
        assertEquals("Mozilla/5.0", entry.getUserAgent());
    }
}
