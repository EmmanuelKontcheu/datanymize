package com.datanymize.anonymization.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of an anonymization operation.
 * Contains statistics and status information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnonymizationResult {
    private String anonymizationId;
    private boolean success;
    private long rowsProcessed;
    private long rowsSkipped;
    private long duration;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<String> errors;
    private Map<String, Long> tableStats;
    private String status;  // PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED

    public AnonymizationResult(String anonymizationId) {
        this.anonymizationId = anonymizationId;
        this.success = false;
        this.rowsProcessed = 0;
        this.rowsSkipped = 0;
        this.duration = 0;
        this.errors = new ArrayList<>();
        this.tableStats = new HashMap<>();
        this.status = "PENDING";
        this.startTime = LocalDateTime.now();
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    public void recordTableStats(String tableName, long rowCount) {
        this.tableStats.put(tableName, rowCount);
    }

    public void markCompleted() {
        this.endTime = LocalDateTime.now();
        this.duration = java.time.temporal.ChronoUnit.MILLIS.between(startTime, endTime);
        this.status = "COMPLETED";
        this.success = errors.isEmpty();
    }

    public void markFailed() {
        this.endTime = LocalDateTime.now();
        this.duration = java.time.temporal.ChronoUnit.MILLIS.between(startTime, endTime);
        this.status = "FAILED";
        this.success = false;
    }

    public void markCancelled() {
        this.endTime = LocalDateTime.now();
        this.duration = java.time.temporal.ChronoUnit.MILLIS.between(startTime, endTime);
        this.status = "CANCELLED";
        this.success = false;
    }
}
