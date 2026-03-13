package com.datanymize.api.controller;

import com.datanymize.api.dto.ApiResponse;
import com.datanymize.export.IExporter;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST API controller for data export management.
 * 
 * Validates Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7
 */
@Slf4j
@RestController
@RequestMapping("/api/exports")
@Tag(name = "Exports", description = "Data export endpoints")
public class ExportController {
    
    @Autowired(required = false)
    private IExporter exporter;
    
    // In-memory storage for export jobs (in production, use database)
    private final Map<String, ExportJob> exportJobs = new HashMap<>();
    
    /**
     * Start a new export job.
     * POST /api/exports
     */
    @PostMapping
    @Operation(summary = "Start a new export job")
    public ResponseEntity<ApiResponse<ExportResponse>> startExport(
            @Valid @RequestBody StartExportRequest request) {
        
        try {
            String tenantId = TenantContextHolder.getTenantContext().getTenantId();
            log.info("Starting export for tenant: {}", tenantId);
            
            String exportId = UUID.randomUUID().toString();
            
            ExportJob job = ExportJob.builder()
                .id(exportId)
                .tenantId(tenantId)
                .sourceConnectionId(request.getSourceConnectionId())
                .format(request.getFormat())
                .status("PENDING")
                .progress(0)
                .createdAt(LocalDateTime.now())
                .build();
            
            exportJobs.put(exportId, job);
            
            // In production, would trigger async export job
            // For now, simulate starting the job
            job.setStatus("RUNNING");
            job.setProgress(10);
            
            ExportResponse response = ExportResponse.builder()
                .id(exportId)
                .status(job.getStatus())
                .format(job.getFormat())
                .progress(job.getProgress())
                .createdAt(job.getCreatedAt())
                .build();
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Export job started successfully"));
            
        } catch (Exception e) {
            log.error("Failed to start export", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage(), "EXPORT_START_FAILED", 400));
        }
    }
    
    /**
     * Get export job status.
     * GET /api/exports/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get export job status")
    public ResponseEntity<ApiResponse<ExportResponse>> getExportStatus(
            @PathVariable String id) {
        
        try {
            String tenantId = TenantContextHolder.getTenantContext().getTenantId();
            log.info("Getting export status {} for tenant: {}", id, tenantId);
            
            ExportJob job = exportJobs.get(id);
            if (job == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Export job not found", "EXPORT_NOT_FOUND", 404));
            }
            
            // Verify tenant ownership
            if (!job.getTenantId().equals(tenantId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied", "ACCESS_DENIED", 403));
            }
            
            ExportResponse response = ExportResponse.builder()
                .id(job.getId())
                .status(job.getStatus())
                .format(job.getFormat())
                .progress(job.getProgress())
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .build();
            
            return ResponseEntity.ok(ApiResponse.success(response, "Export status retrieved successfully"));
            
        } catch (Exception e) {
            log.error("Failed to get export status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage(), "EXPORT_STATUS_FAILED", 500));
        }
    }
    
    /**
     * Download export file.
     * GET /api/exports/{id}/download
     */
    @GetMapping("/{id}/download")
    @Operation(summary = "Download export file")
    public ResponseEntity<ApiResponse<ExportDownloadResponse>> downloadExport(
            @PathVariable String id) {
        
        try {
            String tenantId = TenantContextHolder.getTenantContext().getTenantId();
            log.info("Downloading export {} for tenant: {}", id, tenantId);
            
            ExportJob job = exportJobs.get(id);
            if (job == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Export job not found", "EXPORT_NOT_FOUND", 404));
            }
            
            // Verify tenant ownership
            if (!job.getTenantId().equals(tenantId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied", "ACCESS_DENIED", 403));
            }
            
            if (!job.getStatus().equals("COMPLETED")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Export not completed yet", "EXPORT_NOT_READY", 400));
            }
            
            // In production, would return actual file content
            ExportDownloadResponse response = ExportDownloadResponse.builder()
                .id(id)
                .downloadUrl("/api/exports/" + id + "/file")
                .format(job.getFormat())
                .size(1024 * 1024) // Mock size
                .build();
            
            return ResponseEntity.ok(ApiResponse.success(response, "Export download URL generated"));
            
        } catch (Exception e) {
            log.error("Failed to download export", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage(), "EXPORT_DOWNLOAD_FAILED", 500));
        }
    }
    
    /**
     * Get real-time export progress.
     * GET /api/exports/{id}/progress
     */
    @GetMapping("/{id}/progress")
    @Operation(summary = "Get real-time export progress")
    public ResponseEntity<ApiResponse<ExportProgressResponse>> getExportProgress(
            @PathVariable String id) {
        
        try {
            String tenantId = TenantContextHolder.getTenantContext().getTenantId();
            log.info("Getting export progress {} for tenant: {}", id, tenantId);
            
            ExportJob job = exportJobs.get(id);
            if (job == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Export job not found", "EXPORT_NOT_FOUND", 404));
            }
            
            // Verify tenant ownership
            if (!job.getTenantId().equals(tenantId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Access denied", "ACCESS_DENIED", 403));
            }
            
            // Simulate progress update
            if (job.getProgress() < 100 && job.getStatus().equals("RUNNING")) {
                job.setProgress(Math.min(100, job.getProgress() + 10));
                if (job.getProgress() >= 100) {
                    job.setStatus("COMPLETED");
                    job.setCompletedAt(LocalDateTime.now());
                }
            }
            
            ExportProgressResponse response = ExportProgressResponse.builder()
                .id(id)
                .status(job.getStatus())
                .progress(job.getProgress())
                .rowsProcessed(job.getRowsProcessed())
                .totalRows(job.getTotalRows())
                .estimatedTimeRemaining(calculateTimeRemaining(job))
                .build();
            
            return ResponseEntity.ok(ApiResponse.success(response, "Export progress retrieved"));
            
        } catch (Exception e) {
            log.error("Failed to get export progress", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage(), "EXPORT_PROGRESS_FAILED", 500));
        }
    }
    
    // ============ Helper Methods ============
    
    private long calculateTimeRemaining(ExportJob job) {
        if (job.getProgress() == 0) return 0;
        long elapsed = System.currentTimeMillis() - job.getCreatedAt().getNano();
        long remaining = (elapsed * (100 - job.getProgress())) / job.getProgress();
        return remaining / 1000; // Convert to seconds
    }
    
    // ============ Request/Response DTOs ============
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StartExportRequest {
        private String sourceConnectionId;
        private String format; // POSTGRESQL_DUMP, MYSQL_DUMP, MONGODB_DUMP, CSV, JSON
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExportResponse {
        private String id;
        private String status;
        private String format;
        private int progress;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExportDownloadResponse {
        private String id;
        private String downloadUrl;
        private String format;
        private long size;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExportProgressResponse {
        private String id;
        private String status;
        private int progress;
        private long rowsProcessed;
        private long totalRows;
        private long estimatedTimeRemaining;
    }
    
    // ============ Internal Job Model ============
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExportJob {
        private String id;
        private String tenantId;
        private String sourceConnectionId;
        private String format;
        private String status;
        private int progress;
        private long rowsProcessed;
        private long totalRows;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
    }
}
