package com.datanymize.audit;

import com.datanymize.audit.model.AuditLogEntry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Interface for audit logging functionality.
 * 
 * Validates Requirements: 16.1, 16.2, 16.5, 16.6
 */
public interface IAuditLogger {
    
    /**
     * Log a generic action.
     * 
     * @param userId The user performing the action
     * @param action The action being performed
     * @param resource The resource being acted upon
     * @param success Whether the action succeeded
     * @param errorMessage Error message if action failed
     * @param metadata Additional metadata about the action
     */
    void logAction(String userId, String action, String resource, boolean success, 
                   String errorMessage, Map<String, Object> metadata);
    
    /**
     * Log a connection creation.
     * 
     * @param userId The user creating the connection
     * @param connectionId The connection ID
     * @param connectionType The type of connection (postgresql, mysql, mongodb)
     */
    void logConnectionCreated(String userId, String connectionId, String connectionType);
    
    /**
     * Log a connection deletion.
     * 
     * @param userId The user deleting the connection
     * @param connectionId The connection ID
     */
    void logConnectionDeleted(String userId, String connectionId);
    
    /**
     * Log a schema extraction.
     * 
     * @param userId The user performing the extraction
     * @param sourceDatabase The source database
     * @param targetDatabase The target database
     * @param rowCount The number of rows extracted
     */
    void logSchemaExtraction(String userId, String sourceDatabase, String targetDatabase, long rowCount);
    
    /**
     * Log a PII scan.
     * 
     * @param userId The user performing the scan
     * @param database The database being scanned
     * @param rowsScanned The number of rows scanned
     */
    void logPIIScan(String userId, String database, long rowsScanned);
    
    /**
     * Log an anonymization operation.
     * 
     * @param userId The user performing the anonymization
     * @param sourceDatabase The source database
     * @param targetDatabase The target database
     * @param rowsProcessed The number of rows processed
     * @param success Whether the anonymization succeeded
     * @param errorMessage Error message if anonymization failed
     */
    void logAnonymization(String userId, String sourceDatabase, String targetDatabase, 
                         long rowsProcessed, boolean success, String errorMessage);
    
    /**
     * Log a data export.
     * 
     * @param userId The user performing the export
     * @param database The database being exported
     * @param exportFormat The export format
     * @param rowsExported The number of rows exported
     */
    void logDataExport(String userId, String database, String exportFormat, long rowsExported);
    
    /**
     * Retrieve audit logs within a date range.
     * 
     * @param startDate The start date
     * @param endDate The end date
     * @param userId Optional user ID filter
     * @param action Optional action filter
     * @return List of audit log entries
     */
    List<AuditLogEntry> getAuditLogs(LocalDateTime startDate, LocalDateTime endDate, 
                                     String userId, String action);
}
