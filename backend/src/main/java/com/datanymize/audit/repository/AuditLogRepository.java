package com.datanymize.audit.repository;

import com.datanymize.audit.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for audit log entities.
 * 
 * Validates Requirements: 16.1, 16.2, 16.5, 16.6
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, String> {
    
    /**
     * Find all audit logs for a specific tenant.
     * 
     * @param tenantId The tenant ID
     * @param pageable Pagination information
     * @return Page of audit log entities
     */
    Page<AuditLogEntity> findByTenantId(String tenantId, Pageable pageable);
    
    /**
     * Find all audit logs for a specific user.
     * 
     * @param userId The user ID
     * @param pageable Pagination information
     * @return Page of audit log entities
     */
    Page<AuditLogEntity> findByUserId(String userId, Pageable pageable);
    
    /**
     * Find all audit logs for a specific action.
     * 
     * @param action The action
     * @param pageable Pagination information
     * @return Page of audit log entities
     */
    Page<AuditLogEntity> findByAction(String action, Pageable pageable);
    
    /**
     * Find all audit logs within a date range.
     * 
     * @param startDate The start date
     * @param endDate The end date
     * @param pageable Pagination information
     * @return Page of audit log entities
     */
    @Query("SELECT a FROM AuditLogEntity a WHERE a.timestamp >= :startDate AND a.timestamp <= :endDate ORDER BY a.timestamp DESC")
    Page<AuditLogEntity> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate, 
                                         Pageable pageable);
    
    /**
     * Find all audit logs for a tenant within a date range.
     * 
     * @param tenantId The tenant ID
     * @param startDate The start date
     * @param endDate The end date
     * @param pageable Pagination information
     * @return Page of audit log entities
     */
    @Query("SELECT a FROM AuditLogEntity a WHERE a.tenantId = :tenantId AND a.timestamp >= :startDate AND a.timestamp <= :endDate ORDER BY a.timestamp DESC")
    Page<AuditLogEntity> findByTenantIdAndDateRange(@Param("tenantId") String tenantId,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate,
                                                    Pageable pageable);
    
    /**
     * Find all audit logs for a tenant and user within a date range.
     * 
     * @param tenantId The tenant ID
     * @param userId The user ID
     * @param startDate The start date
     * @param endDate The end date
     * @param pageable Pagination information
     * @return Page of audit log entities
     */
    @Query("SELECT a FROM AuditLogEntity a WHERE a.tenantId = :tenantId AND a.userId = :userId AND a.timestamp >= :startDate AND a.timestamp <= :endDate ORDER BY a.timestamp DESC")
    Page<AuditLogEntity> findByTenantIdAndUserIdAndDateRange(@Param("tenantId") String tenantId,
                                                             @Param("userId") String userId,
                                                             @Param("startDate") LocalDateTime startDate,
                                                             @Param("endDate") LocalDateTime endDate,
                                                             Pageable pageable);
    
    /**
     * Find all audit logs older than a specific date for deletion.
     * 
     * @param cutoffDate The cutoff date
     * @return List of audit log entities to delete
     */
    @Query("SELECT a FROM AuditLogEntity a WHERE a.createdAt < :cutoffDate")
    List<AuditLogEntity> findOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}
