package com.datanymize.pii;

import com.datanymize.database.connection.IDatabaseConnection;
import com.datanymize.database.model.DatabaseMetadata;
import com.datanymize.pii.model.PIIScanResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executor for PII scans on databases.
 * Orchestrates the scanning process with progress tracking and cancellation support.
 * 
 * Validates Requirements: 3.1, 3.6
 */
@Slf4j
@Component
public class PIIScanExecutor {
    
    private final IPIIDetector piiDetector;
    private final Map<String, ScanProgress> scanProgress = new ConcurrentHashMap<>();
    
    public PIIScanExecutor(IPIIDetector piiDetector) {
        this.piiDetector = piiDetector;
    }
    
    /**
     * Execute a PII scan on a database.
     * 
     * @param scanId Unique identifier for this scan
     * @param conn Database connection to scan
     * @param schema Database schema metadata
     * @param progressListener Optional listener for progress updates
     * @return PIIScanResult with classifications
     * @throws Exception if scan fails
     */
    public PIIScanResult executeScan(String scanId, IDatabaseConnection conn, DatabaseMetadata schema,
                                     ProgressListener progressListener) throws Exception {
        log.info("Starting PII scan: {}", scanId);
        
        ScanProgress progress = new ScanProgress(scanId);
        scanProgress.put(scanId, progress);
        
        try {
            // Perform the scan
            PIIScanResult result = piiDetector.scanDatabase(conn, schema);
            
            progress.setComplete(true);
            progress.setResult(result);
            
            if (progressListener != null) {
                progressListener.onComplete(result);
            }
            
            log.info("PII scan completed: {}", scanId);
            return result;
        } catch (Exception e) {
            progress.setError(e.getMessage());
            
            if (progressListener != null) {
                progressListener.onError(e);
            }
            
            log.error("PII scan failed: {}", scanId, e);
            throw e;
        } finally {
            // Keep progress for a while, then clean up
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    scanProgress.remove(scanId);
                }
            }, 3600000); // 1 hour
        }
    }
    
    /**
     * Get the progress of a scan.
     * 
     * @param scanId Scan identifier
     * @return ScanProgress or null if scan not found
     */
    public ScanProgress getProgress(String scanId) {
        return scanProgress.get(scanId);
    }
    
    /**
     * Cancel a scan.
     * 
     * @param scanId Scan identifier
     * @return true if scan was cancelled, false if not found
     */
    public boolean cancelScan(String scanId) {
        ScanProgress progress = scanProgress.get(scanId);
        if (progress != null) {
            progress.setCancelled(true);
            log.info("Scan cancelled: {}", scanId);
            return true;
        }
        return false;
    }
    
    /**
     * Model for tracking scan progress.
     */
    public static class ScanProgress {
        private final String scanId;
        private final long startTime;
        private long tablesProcessed = 0;
        private long totalTables = 0;
        private long columnsProcessed = 0;
        private long totalColumns = 0;
        private boolean cancelled = false;
        private boolean complete = false;
        private String error = null;
        private PIIScanResult result = null;
        
        public ScanProgress(String scanId) {
            this.scanId = scanId;
            this.startTime = System.currentTimeMillis();
        }
        
        public String getScanId() {
            return scanId;
        }
        
        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }
        
        public double getProgress() {
            if (totalColumns == 0) {
                return 0.0;
            }
            return (double) columnsProcessed / totalColumns * 100.0;
        }
        
        public long getTablesProcessed() {
            return tablesProcessed;
        }
        
        public void setTablesProcessed(long tablesProcessed) {
            this.tablesProcessed = tablesProcessed;
        }
        
        public long getTotalTables() {
            return totalTables;
        }
        
        public void setTotalTables(long totalTables) {
            this.totalTables = totalTables;
        }
        
        public long getColumnsProcessed() {
            return columnsProcessed;
        }
        
        public void setColumnsProcessed(long columnsProcessed) {
            this.columnsProcessed = columnsProcessed;
        }
        
        public long getTotalColumns() {
            return totalColumns;
        }
        
        public void setTotalColumns(long totalColumns) {
            this.totalColumns = totalColumns;
        }
        
        public boolean isCancelled() {
            return cancelled;
        }
        
        public void setCancelled(boolean cancelled) {
            this.cancelled = cancelled;
        }
        
        public boolean isComplete() {
            return complete;
        }
        
        public void setComplete(boolean complete) {
            this.complete = complete;
        }
        
        public String getError() {
            return error;
        }
        
        public void setError(String error) {
            this.error = error;
        }
        
        public PIIScanResult getResult() {
            return result;
        }
        
        public void setResult(PIIScanResult result) {
            this.result = result;
        }
    }
    
    /**
     * Interface for progress listeners.
     */
    public interface ProgressListener {
        void onProgress(ScanProgress progress);
        void onComplete(PIIScanResult result);
        void onError(Exception error);
    }
}
