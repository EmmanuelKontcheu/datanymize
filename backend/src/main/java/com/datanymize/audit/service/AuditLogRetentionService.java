package com.datanymize.audit.service;

import com.datanymize.audit.entity.AuditLogEntity;
import com.datanymize.audit.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Service for managing audit log retention policies.
 * 
 * Validates Requirements: 16.5, 16.6
 */
@Slf4j
@Service
public class AuditLogRetentionService {
    
    private final AuditLogRepository auditLogRepository;
    
    @Value("${audit.retention-days:365}")
    private int retentionDays;
    
    public AuditLogRetentionService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }
    
    /**
     * Execute retention policy cleanup.
     * Runs daily at 2 AM UTC.
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    @Transactional
    public void executeRetentionPolicy() {
        try {
            log.info("Starting audit log retention policy execution. Retention period: {} days", retentionDays);
            
            LocalDateTime cutoffDate = LocalDateTime.now(ZoneId.of("UTC")).minusDays(retentionDays);
            List<AuditLogEntity> logsToDelete = auditLogRepository.findOlderThan(cutoffDate);
            
            if (logsToDelete.isEmpty()) {
                log.info("No audit logs to delete. All logs are within retention period.");
                return;
            }
            
            log.info("Found {} audit logs older than {} to delete", logsToDelete.size(), cutoffDate);
            
            auditLogRepository.deleteAll(logsToDelete);
            
            log.info("Successfully deleted {} audit logs older than {}", logsToDelete.size(), cutoffDate);
        } catch (Exception e) {
            log.error("Error executing audit log retention policy", e);
        }
    }
    
    /**
     * Get the current retention period in days.
     * 
     * @return The retention period in days
     */
    public int getRetentionDays() {
        return retentionDays;
    }
    
    /**
     * Set the retention period in days.
     * 
     * @param days The retention period in days
     */
    public void setRetentionDays(int days) {
        if (days < 365) {
            log.warn("Retention period {} days is less than recommended 365 days", days);
        }
        this.retentionDays = days;
    }
}
