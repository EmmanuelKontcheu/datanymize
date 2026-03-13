package com.datanymize.export.model;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Model representing the result of an export operation.
 * Validates: Requirements 7.1, 7.2, 7.3, 7.4
 */
public class ExportResult {
    private String exportId;
    private ExportFormat format;
    private boolean success;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private long duration;
    private long rowsExported;
    private long tablesExported;
    private String outputPath;
    private Map<String, Long> tableStats;
    private String errorMessage;

    public ExportResult() {
        this.tableStats = new HashMap<>();
        this.startTime = LocalDateTime.now();
    }

    public void markComplete() {
        this.endTime = LocalDateTime.now();
        this.duration = java.time.temporal.ChronoUnit.MILLIS.between(startTime, endTime);
        this.success = true;
    }

    public void markFailed(String errorMessage) {
        this.endTime = LocalDateTime.now();
        this.duration = java.time.temporal.ChronoUnit.MILLIS.between(startTime, endTime);
        this.success = false;
        this.errorMessage = errorMessage;
    }

    // Getters and Setters
    public String getExportId() {
        return exportId;
    }

    public void setExportId(String exportId) {
        this.exportId = exportId;
    }

    public ExportFormat getFormat() {
        return format;
    }

    public void setFormat(ExportFormat format) {
        this.format = format;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getRowsExported() {
        return rowsExported;
    }

    public void setRowsExported(long rowsExported) {
        this.rowsExported = rowsExported;
    }

    public long getTablesExported() {
        return tablesExported;
    }

    public void setTablesExported(long tablesExported) {
        this.tablesExported = tablesExported;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public Map<String, Long> getTableStats() {
        return tableStats;
    }

    public void setTableStats(Map<String, Long> tableStats) {
        this.tableStats = tableStats;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
