package com.datanymize.audit.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * JPA entity for audit log entries.
 * Immutable - only INSERT operations allowed, no UPDATE or DELETE.
 * 
 * Validates Requirements: 16.1, 16.2, 16.5, 16.6
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_action", columnList = "action"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Immutable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogEntity {
    
    @Id
    private String id;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "action", nullable = false)
    private String action;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "source_database")
    private String sourceDatabase;
    
    @Column(name = "target_database")
    private String targetDatabase;
    
    @Column(name = "rows_processed")
    private long rowsProcessed;
    
    @Column(name = "success", nullable = false)
    private boolean success;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
