package com.datanymize.pii.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Model representing the result of a PII scan on a database.
 * Contains classifications for all columns analyzed.
 * 
 * Validates Requirements: 3.1, 3.2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PIIScanResult {
    
    /**
     * Name of the database scanned
     */
    private String databaseName;
    
    /**
     * Timestamp when the scan was performed
     */
    private LocalDateTime scanTime;
    
    /**
     * List of column classifications
     */
    private List<ColumnClassification> classifications;
    
    /**
     * Number of sample rows analyzed per table
     */
    private long samplesAnalyzed;
    
    /**
     * Get count of PII columns (high confidence)
     */
    public long getPIIColumnCount() {
        return classifications != null ? 
            classifications.stream()
                .filter(c -> c.getClassification().isPII() && c.getClassification().isHighConfidence())
                .count() : 0;
    }
    
    /**
     * Get count of potential PII columns (medium confidence)
     */
    public long getPotentialPIIColumnCount() {
        return classifications != null ? 
            classifications.stream()
                .filter(c -> c.getClassification().isPII() && c.getClassification().isMediumConfidence())
                .count() : 0;
    }
    
    /**
     * Get count of non-PII columns
     */
    public long getNonPIIColumnCount() {
        return classifications != null ? 
            classifications.stream()
                .filter(c -> !c.getClassification().isPII())
                .count() : 0;
    }
    
    /**
     * Model representing classification of a single column.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ColumnClassification {
        
        /**
         * Table name
         */
        private String tableName;
        
        /**
         * Column name
         */
        private String columnName;
        
        /**
         * Column data type
         */
        private String dataType;
        
        /**
         * PII classification
         */
        private PIIClassification classification;
    }
}
