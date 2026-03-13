package com.datanymize.audit;

import com.datanymize.audit.entity.AuditLogEntity;
import com.datanymize.audit.model.AuditLogEntry;
import com.datanymize.audit.repository.AuditLogRepository;
import com.datanymize.security.CredentialEncryption;
import com.datanymize.tenant.TenantContextHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of audit logging functionality.
 * 
 * Validates Requirements: 16.1, 16.2, 16.5, 16.6
 */
@Slf4j
@Service
public class AuditLogger implements IAuditLogger {
    
    private final AuditLogRepository auditLogRepository;
    private final TenantContextHolder tenantContextHolder;
    private final CredentialEncryption credentialEncryption;
    private final ObjectMapper objectMapper;
    
    public AuditLogger(AuditLogRepository auditLogRepository,
                      TenantContextHolder tenantContextHolder,
                      CredentialEncryption credentialEncryption) {
        this.auditLogRepository = auditLogRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.credentialEncryption = credentialEncryption;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public void logAction(String userId, String action, String resource, boolean success,
                         String errorMessage, Map<String, Object> metadata) {
        try {
            AuditLogEntry entry = AuditLogEntry.builder()
                    .id(UUID.randomUUID().toString())
                    .tenantId(tenantContextHolder.getCurrentTenantId())
                    .userId(userId)
                    .action(action)
                    .timestamp(LocalDateTime.now(ZoneId.of("UTC")))
                    .success(success)
                    .errorMessage(errorMessage)
                    .metadata(metadata != null ? new HashMap<>(metadata) : new HashMap<>())
                    .createdAt(LocalDateTime.now(ZoneId.of("UTC")))
                    .build();
            
            saveAuditLogEntry(entry);
        } catch (Exception e) {
            log.error("Failed to log action: {}", action, e);
        }
    }
    
    @Override
    public void logConnectionCreated(String userId, String connectionId, String connectionType) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("connectionId", connectionId);
            metadata.put("connectionType", connectionType);
            
            logAction(userId, "CONNECTION_CREATED", connectionId, true, null, metadata);
        } catch (Exception e) {
            log.error("Failed to log connection creation: {}", connectionId, e);
        }
    }
    
    @Override
    public void logConnectionDeleted(String userId, String connectionId) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("connectionId", connectionId);
            
            logAction(userId, "CONNECTION_DELETED", connectionId, true, null, metadata);
        } catch (Exception e) {
            log.error("Failed to log connection deletion: {}", connectionId, e);
        }
    }
    
    @Override
    public void logSchemaExtraction(String userId, String sourceDatabase, String targetDatabase, long rowCount) {
        try {
            AuditLogEntry entry = AuditLogEntry.builder()
                    .id(UUID.randomUUID().toString())
                    .tenantId(tenantContextHolder.getCurrentTenantId())
                    .userId(userId)
                    .action("SCHEMA_EXTRACTION")
                    .timestamp(LocalDateTime.now(ZoneId.of("UTC")))
                    .sourceDatabase(sourceDatabase)
                    .targetDatabase(targetDatabase)
                    .rowsProcessed(rowCount)
                    .success(true)
                    .createdAt(LocalDateTime.now(ZoneId.of("UTC")))
                    .build();
            
            saveAuditLogEntry(entry);
        } catch (Exception e) {
            log.error("Failed to log schema extraction from {} to {}", sourceDatabase, targetDatabase, e);
        }
    }
    
    @Override
    public void logPIIScan(String userId, String database, long rowsScanned) {
        try {
            AuditLogEntry entry = AuditLogEntry.builder()
                    .id(UUID.randomUUID().toString())
                    .tenantId(tenantContextHolder.getCurrentTenantId())
                    .userId(userId)
                    .action("PII_SCAN")
                    .timestamp(LocalDateTime.now(ZoneId.of("UTC")))
                    .sourceDatabase(database)
                    .rowsProcessed(rowsScanned)
                    .success(true)
                    .createdAt(LocalDateTime.now(ZoneId.of("UTC")))
                    .build();
            
            saveAuditLogEntry(entry);
        } catch (Exception e) {
            log.error("Failed to log PII scan for database: {}", database, e);
        }
    }
    
    @Override
    public void logAnonymization(String userId, String sourceDatabase, String targetDatabase,
                                long rowsProcessed, boolean success, String errorMessage) {
        try {
            AuditLogEntry entry = AuditLogEntry.builder()
                    .id(UUID.randomUUID().toString())
                    .tenantId(tenantContextHolder.getCurrentTenantId())
                    .userId(userId)
                    .action("ANONYMIZATION")
                    .timestamp(LocalDateTime.now(ZoneId.of("UTC")))
                    .sourceDatabase(sourceDatabase)
                    .targetDatabase(targetDatabase)
                    .rowsProcessed(rowsProcessed)
                    .success(success)
                    .errorMessage(errorMessage)
                    .createdAt(LocalDateTime.now(ZoneId.of("UTC")))
                    .build();
            
            saveAuditLogEntry(entry);
        } catch (Exception e) {
            log.error("Failed to log anonymization from {} to {}", sourceDatabase, targetDatabase, e);
        }
    }
    
    @Override
    public void logDataExport(String userId, String database, String exportFormat, long rowsExported) {
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("exportFormat", exportFormat);
            
            AuditLogEntry entry = AuditLogEntry.builder()
                    .id(UUID.randomUUID().toString())
                    .tenantId(tenantContextHolder.getCurrentTenantId())
                    .userId(userId)
                    .action("DATA_EXPORT")
                    .timestamp(LocalDateTime.now(ZoneId.of("UTC")))
                    .sourceDatabase(database)
                    .rowsProcessed(rowsExported)
                    .success(true)
                    .metadata(metadata)
                    .createdAt(LocalDateTime.now(ZoneId.of("UTC")))
                    .build();
            
            saveAuditLogEntry(entry);
        } catch (Exception e) {
            log.error("Failed to log data export for database: {}", database, e);
        }
    }
    
    @Override
    public List<AuditLogEntry> getAuditLogs(LocalDateTime startDate, LocalDateTime endDate,
                                           String userId, String action) {
        try {
            String tenantId = tenantContextHolder.getCurrentTenantId();
            Page<AuditLogEntity> page = auditLogRepository.findByTenantIdAndDateRange(
                    tenantId, startDate, endDate, PageRequest.of(0, 1000));
            
            return page.getContent().stream()
                    .filter(entity -> userId == null || entity.getUserId().equals(userId))
                    .filter(entity -> action == null || entity.getAction().equals(action))
                    .map(this::convertEntityToEntry)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to retrieve audit logs", e);
            return List.of();
        }
    }
    
    private void saveAuditLogEntry(AuditLogEntry entry) {
        try {
            String metadataJson = entry.getMetadata() != null ? 
                    objectMapper.writeValueAsString(entry.getMetadata()) : "{}";
            
            AuditLogEntity entity = AuditLogEntity.builder()
                    .id(entry.getId())
                    .tenantId(entry.getTenantId())
                    .userId(entry.getUserId())
                    .action(entry.getAction())
                    .timestamp(entry.getTimestamp())
                    .sourceDatabase(entry.getSourceDatabase())
                    .targetDatabase(entry.getTargetDatabase())
                    .rowsProcessed(entry.getRowsProcessed())
                    .success(entry.isSuccess())
                    .errorMessage(entry.getErrorMessage())
                    .ipAddress(entry.getIpAddress())
                    .userAgent(entry.getUserAgent())
                    .metadata(metadataJson)
                    .createdAt(entry.getCreatedAt())
                    .build();
            
            auditLogRepository.save(entity);
            log.debug("Audit log entry saved: {} - {}", entry.getAction(), entry.getId());
        } catch (Exception e) {
            log.error("Failed to save audit log entry", e);
        }
    }
    
    private AuditLogEntry convertEntityToEntry(AuditLogEntity entity) {
        try {
            Map<String, Object> metadata = entity.getMetadata() != null ?
                    objectMapper.readValue(entity.getMetadata(), Map.class) : new HashMap<>();
            
            return AuditLogEntry.builder()
                    .id(entity.getId())
                    .tenantId(entity.getTenantId())
                    .userId(entity.getUserId())
                    .action(entity.getAction())
                    .timestamp(entity.getTimestamp())
                    .sourceDatabase(entity.getSourceDatabase())
                    .targetDatabase(entity.getTargetDatabase())
                    .rowsProcessed(entity.getRowsProcessed())
                    .success(entity.isSuccess())
                    .errorMessage(entity.getErrorMessage())
                    .ipAddress(entity.getIpAddress())
                    .userAgent(entity.getUserAgent())
                    .metadata(metadata)
                    .createdAt(entity.getCreatedAt())
                    .build();
        } catch (Exception e) {
            log.error("Failed to convert entity to entry", e);
            return null;
        }
    }
}
