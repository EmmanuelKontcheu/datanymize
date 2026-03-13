package com.datanymize.anonymization.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Result of an anonymization operation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnonymizationResult {
    private String anonymizationId;
    private String sourceDatabase;
    private String targetDatabase;
    private long totalRowsProcessed;
    private long totalRowsSkipped;
    private long totalErrors;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String errorMessage;
    private Map<String, Long> tableStatistics;
    private double successRate;
    private long duration;

    /**
     * Increment rows processed counter.
     */
    public void incrementRowsProcessed(long count) {
        this.totalRowsProcessed += count;
    }

    /**
     * Add table statistics.
     */
    public void addTableStatistic(String tableName, long rowCount) {
        if (tableStatistics == null) {
            tableStatistics = new HashMap<>();
        }
        tableStatistics.put(tableName, rowCount);
    }
}
