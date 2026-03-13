package com.datanymize.export.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Result of an export operation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportResult {
    private String exportId;
    private String exportFormat;
    private String sourceDatabase;
    private long totalRowsExported;
    private long totalTablesExported;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String errorMessage;
    private String exportPath;
    private long exportSizeBytes;
    private double successRate;
    private Map<String, Long> tableStats = new HashMap<>();
    private long duration;

    /**
     * Set the export format from ExportFormat enum.
     */
    public void setFormat(ExportFormat format) {
        if (format != null) {
            this.exportFormat = format.toString();
        }
    }

    /**
     * Set the output path.
     */
    public void setOutputPath(String path) {
        this.exportPath = path;
    }

    /**
     * Set rows exported.
     */
    public void setRowsExported(long rows) {
        this.totalRowsExported = rows;
    }

    /**
     * Set tables exported.
     */
    public void setTablesExported(int tables) {
        this.totalTablesExported = tables;
    }

    /**
     * Mark export as complete.
     */
    public void markComplete() {
        this.status = "COMPLETED";
        this.endTime = LocalDateTime.now();
        if (startTime != null) {
            this.duration = java.time.temporal.ChronoUnit.MILLIS.between(startTime, endTime);
        }
    }

    /**
     * Mark export as failed.
     */
    public void markFailed(String error) {
        this.status = "FAILED";
        this.errorMessage = error;
        this.endTime = LocalDateTime.now();
        if (startTime != null) {
            this.duration = java.time.temporal.ChronoUnit.MILLIS.between(startTime, endTime);
        }
    }

    /**
     * Get table statistics.
     */
    public Map<String, Long> getTableStats() {
        if (tableStats == null) {
            tableStats = new HashMap<>();
        }
        return tableStats;
    }
}
