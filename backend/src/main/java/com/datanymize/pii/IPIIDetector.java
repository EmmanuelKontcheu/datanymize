package com.datanymize.pii;

import com.datanymize.database.connection.IDatabaseConnection;
import com.datanymize.database.model.DatabaseMetadata;
import com.datanymize.pii.model.PIIClassification;
import com.datanymize.pii.model.PIIScanResult;

import java.util.List;

/**
 * Interface for PII (Personally Identifiable Information) detection.
 * Provides methods for scanning databases and classifying columns as PII.
 * 
 * Validates Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7
 */
public interface IPIIDetector {
    
    /**
     * Scan a database for PII columns.
     * Analyzes all tables and columns, classifying each as PII or non-PII.
     * 
     * @param conn Database connection to scan
     * @param schema Database schema metadata
     * @return PIIScanResult containing classifications for all columns
     * @throws Exception if scan fails
     */
    PIIScanResult scanDatabase(IDatabaseConnection conn, DatabaseMetadata schema) throws Exception;
    
    /**
     * Classify a single column as PII or non-PII.
     * Considers column name, data type, and sample data.
     * 
     * @param tableName Name of the table containing the column
     * @param column Column metadata
     * @param samples Sample values from the column (first 100 rows)
     * @return PIIClassification with category and confidence
     * @throws Exception if classification fails
     */
    PIIClassification classifyColumn(String tableName, DatabaseMetadata.ColumnMetadata column, 
                                     List<String> samples) throws Exception;
    
    /**
     * Calculate confidence score for a column being PII.
     * Based on column name, data type, and sample data patterns.
     * 
     * @param columnName Name of the column
     * @param dataType Data type of the column
     * @param samples Sample values from the column
     * @return Confidence score (0-100%)
     */
    double calculateConfidence(String columnName, String dataType, List<String> samples);
    
    /**
     * Detect PII pattern in a single value.
     * Uses regex patterns to detect email, phone, SSN, credit card, etc.
     * 
     * @param value Value to analyze
     * @return PIICategory if pattern matches, NONE otherwise
     */
    PIIClassification.PIICategory detectPattern(String value);
}
