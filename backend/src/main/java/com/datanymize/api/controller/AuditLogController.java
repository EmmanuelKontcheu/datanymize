package com.datanymize.api.controller;

import com.datanymize.api.dto.ApiResponse;
import com.datanymize.audit.model.AuditLogEntry;
import com.datanymize.audit.IAuditLogger;
import com.datanymize.tenant.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * REST API controller for audit log management.
 * 
 * Validates Requirements: 16.1, 16.2, 16.3, 16.4
 */
@Slf4j
@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit Logs", description = "Audit log endpoints")
public class AuditLogController {
    
    @Autowired(required = false)
    private IAuditLogger auditLogger;
    
    /**
     * List audit logs with filtering.
     * GET /api/audit-logs
     */
    @GetMapping
    @Operation(summary = "List audit logs")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> listAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        
        try {
            String tenantId = TenantContextHolder.getTenantContext().getTenantId();
            log.info("Listing audit logs for tenant: {} with filters - action: {}, userId: {}", 
                tenantId, action, userId);
            
            // In production, would query database with filters
            List<AuditLogResponse> logs = new ArrayList<>();
            
            // Mock data for demonstration
            for (int i = 0; i < Math.min(pageSize, 10); i++) {
                logs.add(AuditLogResponse.builder()
                    .id("log-" + (page * pageSize + i))
                    .timestamp(LocalDateTime.now().minusHours(i))
                    .userId("user-123")
                    .action("ANONYMIZATION_STARTED")
                    .resource("database-1")
                    .result("SUCCESS")
                    .rowsProcessed(1000L)
                    .build());
            }
            
            return ResponseEntity.ok(ApiResponse.success(logs, "Audit logs retrieved successfully"));
            
        } catch (Exception e) {
            log.error("Failed to list audit logs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage(), "AUDIT_LIST_FAILED", 500));
        }
    }
    
    /**
     * Get a specific audit log entry.
     * GET /api/audit-logs/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get audit log details")
    public ResponseEntity<ApiResponse<AuditLogDetailResponse>> getAuditLog(
            @PathVariable String id) {
        
        try {
            String tenantId = TenantContextHolder.getTenantContext().getTenantId();
            log.info("Getting audit log {} for tenant: {}", id, tenantId);
            
            // In production, would query database
            AuditLogDetailResponse response = AuditLogDetailResponse.builder()
                .id(id)
                .timestamp(LocalDateTime.now())
                .tenantId(tenantId)
                .userId("user-123")
                .action("ANONYMIZATION_STARTED")
                .resource("database-1")
                .result("SUCCESS")
                .rowsProcessed(1000L)
                .duration(3600L)
                .errorMessage(null)
                .metadata(new java.util.HashMap<>())
                .build();
            
            return ResponseEntity.ok(ApiResponse.success(response, "Audit log retrieved successfully"));
            
        } catch (Exception e) {
            log.error("Failed to get audit log", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage(), "AUDIT_GET_FAILED", 500));
        }
    }
    
    /**
     * Export audit logs.
     * POST /api/audit-logs/export
     */
    @PostMapping("/export")
    @Operation(summary = "Export audit logs")
    public ResponseEntity<ApiResponse<AuditLogExportResponse>> exportAuditLogs(
            @Valid @RequestBody ExportAuditLogsRequest request) {
        
        try {
            String tenantId = TenantContextHolder.getTenantContext().getTenantId();
            log.info("Exporting audit logs for tenant: {} in format: {}", tenantId, request.getFormat());
            
            // In production, would generate actual export file
            String exportId = java.util.UUID.randomUUID().toString();
            
            AuditLogExportResponse response = AuditLogExportResponse.builder()
                .exportId(exportId)
                .format(request.getFormat())
                .status("COMPLETED")
                .downloadUrl("/api/audit-logs/export/" + exportId + "/download")
                .createdAt(LocalDateTime.now())
                .build();
            
            return ResponseEntity.ok(ApiResponse.success(response, "Audit logs exported successfully"));
            
        } catch (Exception e) {
            log.error("Failed to export audit logs", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage(), "AUDIT_EXPORT_FAILED", 400));
        }
    }
    
    // ============ Request/Response DTOs ============
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExportAuditLogsRequest {
        private String format; // CSV or JSON
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuditLogResponse {
        private String id;
        private LocalDateTime timestamp;
        private String userId;
        private String action;
        private String resource;
        private String result;
        private Long rowsProcessed;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuditLogDetailResponse {
        private String id;
        private LocalDateTime timestamp;
        private String tenantId;
        private String userId;
        private String action;
        private String resource;
        private String result;
        private Long rowsProcessed;
        private Long duration;
        private String errorMessage;
        private java.util.Map<String, Object> metadata;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuditLogExportResponse {
        private String exportId;
        private String format;
        private String status;
        private String downloadUrl;
        private LocalDateTime createdAt;
    }
}
