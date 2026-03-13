package com.datanymize.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Model representing an audit log entry.
 * 
 * Validates Requirements: 16.1, 16.2, 16.5, 16.6
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogEntry {
    
    private String id;
    private String tenantId;
    private String userId;
    private String action;
    private LocalDateTime timestamp;
    private String sourceDatabase;
    private String targetDatabase;
    private long rowsProcessed;
    private boolean success;
    private String errorMessage;
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    
    @Override
    public String toString() {
        return "AuditLogEntry{" +
                "id='" + id + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", userId='" + userId + '\'' +
                ", action='" + action + '\'' +
                ", timestamp=" + timestamp +
                ", sourceDatabase='" + sourceDatabase + '\'' +
                ", targetDatabase='" + targetDatabase + '\'' +
                ", rowsProcessed=" + rowsProcessed +
                ", success=" + success +
                ", ipAddress='" + ipAddress + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
