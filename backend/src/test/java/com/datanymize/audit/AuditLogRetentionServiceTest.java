package com.datanymize.audit;

import com.datanymize.audit.entity.AuditLogEntity;
import com.datanymize.audit.repository.AuditLogRepository;
import com.datanymize.audit.service.AuditLogRetentionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditLogRetentionService.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogRetentionServiceTest {
    
    @Mock
    private AuditLogRepository auditLogRepository;
    
    private AuditLogRetentionService retentionService;
    
    @BeforeEach
    void setUp() {
        retentionService = new AuditLogRetentionService(auditLogRepository);
    }
    
    @Test
    void testExecuteRetentionPolicy_NoLogsToDelete() {
        when(auditLogRepository.findOlderThan(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());
        
        retentionService.executeRetentionPolicy();
        
        verify(auditLogRepository, times(1)).findOlderThan(any(LocalDateTime.class));
        verify(auditLogRepository, never()).deleteAll(any());
    }
    
    @Test
    void testExecuteRetentionPolicy_DeletesOldLogs() {
        LocalDateTime oldDate = LocalDateTime.now(ZoneId.of("UTC")).minusDays(400);
        
        AuditLogEntity oldLog = AuditLogEntity.builder()
                .id("log-1")
                .tenantId("tenant-1")
                .userId("user-1")
                .action("TEST_ACTION")
                .timestamp(oldDate)
                .success(true)
                .createdAt(oldDate)
                .build();
        
        List<AuditLogEntity> logsToDelete = List.of(oldLog);
        when(auditLogRepository.findOlderThan(any(LocalDateTime.class)))
                .thenReturn(logsToDelete);
        
        retentionService.executeRetentionPolicy();
        
        verify(auditLogRepository, times(1)).findOlderThan(any(LocalDateTime.class));
        verify(auditLogRepository, times(1)).deleteAll(logsToDelete);
    }
    
    @Test
    void testExecuteRetentionPolicy_DeletesMultipleOldLogs() {
        LocalDateTime oldDate = LocalDateTime.now(ZoneId.of("UTC")).minusDays(400);
        
        List<AuditLogEntity> logsToDelete = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            logsToDelete.add(AuditLogEntity.builder()
                    .id("log-" + i)
                    .tenantId("tenant-1")
                    .userId("user-1")
                    .action("TEST_ACTION")
                    .timestamp(oldDate)
                    .success(true)
                    .createdAt(oldDate)
                    .build());
        }
        
        when(auditLogRepository.findOlderThan(any(LocalDateTime.class)))
                .thenReturn(logsToDelete);
        
        retentionService.executeRetentionPolicy();
        
        verify(auditLogRepository, times(1)).findOlderThan(any(LocalDateTime.class));
        verify(auditLogRepository, times(1)).deleteAll(logsToDelete);
    }
    
    @Test
    void testGetRetentionDays() {
        int retentionDays = retentionService.getRetentionDays();
        
        assertEquals(365, retentionDays);
    }
    
    @Test
    void testSetRetentionDays() {
        retentionService.setRetentionDays(730);
        
        assertEquals(730, retentionService.getRetentionDays());
    }
    
    @Test
    void testSetRetentionDays_LessThanRecommended() {
        retentionService.setRetentionDays(180);
        
        assertEquals(180, retentionService.getRetentionDays());
    }
    
    @Test
    void testExecuteRetentionPolicy_HandlesException() {
        when(auditLogRepository.findOlderThan(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));
        
        assertDoesNotThrow(() -> retentionService.executeRetentionPolicy());
    }
    
    @Test
    void testRetentionPolicyUsesCorrectCutoffDate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        LocalDateTime expectedCutoff = now.minusDays(365);
        
        when(auditLogRepository.findOlderThan(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());
        
        retentionService.executeRetentionPolicy();
        
        verify(auditLogRepository, times(1)).findOlderThan(argThat(date -> {
            // Check that the cutoff date is approximately 365 days ago
            long daysDifference = java.time.temporal.ChronoUnit.DAYS.between(date, now);
            return daysDifference >= 364 && daysDifference <= 366;
        }));
    }
}
