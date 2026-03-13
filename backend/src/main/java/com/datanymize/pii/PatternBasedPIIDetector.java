package com.datanymize.pii;

import com.datanymize.database.connection.IDatabaseConnection;
import com.datanymize.database.model.DatabaseMetadata;
import com.datanymize.pii.model.PIIClassification;
import com.datanymize.pii.model.PIICategory;
import com.datanymize.pii.model.PIIScanResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Pattern-based PII detector using regex patterns and column name heuristics.
 * Provides fast, deterministic PII detection without external AI services.
 * 
 * Validates Requirements: 3.3, 3.4, 3.5
 */
@Slf4j
@Component
public class PatternBasedPIIDetector implements IPIIDetector {
    
    // Regex patterns for PII detection
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[(]?[0-9]{3}[)]?[-\\s.]?[0-9]{3}[-\\s.]?[0-9]{4,6}$"
    );
    
    private static final Pattern SSN_PATTERN = Pattern.compile(
        "^[0-9]{3}-[0-9]{2}-[0-9]{4}$"
    );
    
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
        "^[0-9]{13,19}$"
    );
    
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
        Pattern.CASE_INSENSITIVE
    );
    
    // Column name heuristics for PII detection
    private static final Map<String, PIICategory> COLUMN_NAME_HEURISTICS = new HashMap<>();
    
    static {
        // High confidence (90%+) column names
        COLUMN_NAME_HEURISTICS.put("email", PIICategory.EMAIL);
        COLUMN_NAME_HEURISTICS.put("email_address", PIICategory.EMAIL);
        COLUMN_NAME_HEURISTICS.put("e_mail", PIICategory.EMAIL);
        COLUMN_NAME_HEURISTICS.put("mail", PIICategory.EMAIL);
        
        COLUMN_NAME_HEURISTICS.put("phone", PIICategory.PHONE);
        COLUMN_NAME_HEURISTICS.put("phone_number", PIICategory.PHONE);
        COLUMN_NAME_HEURISTICS.put("telephone", PIICategory.PHONE);
        COLUMN_NAME_HEURISTICS.put("mobile", PIICategory.PHONE);
        COLUMN_NAME_HEURISTICS.put("cell", PIICategory.PHONE);
        
        COLUMN_NAME_HEURISTICS.put("ssn", PIICategory.SSN);
        COLUMN_NAME_HEURISTICS.put("social_security", PIICategory.SSN);
        COLUMN_NAME_HEURISTICS.put("social_security_number", PIICategory.SSN);
        
        COLUMN_NAME_HEURISTICS.put("credit_card", PIICategory.CREDIT_CARD);
        COLUMN_NAME_HEURISTICS.put("cc_number", PIICategory.CREDIT_CARD);
        COLUMN_NAME_HEURISTICS.put("card_number", PIICategory.CREDIT_CARD);
        COLUMN_NAME_HEURISTICS.put("creditcard", PIICategory.CREDIT_CARD);
        
        COLUMN_NAME_HEURISTICS.put("name", PIICategory.NAME);
        COLUMN_NAME_HEURISTICS.put("full_name", PIICategory.NAME);
        COLUMN_NAME_HEURISTICS.put("first_name", PIICategory.NAME);
        COLUMN_NAME_HEURISTICS.put("last_name", PIICategory.NAME);
        COLUMN_NAME_HEURISTICS.put("surname", PIICategory.NAME);
        
        COLUMN_NAME_HEURISTICS.put("address", PIICategory.ADDRESS);
        COLUMN_NAME_HEURISTICS.put("street", PIICategory.ADDRESS);
        COLUMN_NAME_HEURISTICS.put("city", PIICategory.ADDRESS);
        COLUMN_NAME_HEURISTICS.put("state", PIICategory.ADDRESS);
        COLUMN_NAME_HEURISTICS.put("zip", PIICategory.ADDRESS);
        COLUMN_NAME_HEURISTICS.put("postal_code", PIICategory.ADDRESS);
    }
    
    // Identifier column name heuristics (80%+ confidence)
    private static final Set<String> IDENTIFIER_KEYWORDS = new HashSet<>(Arrays.asList(
        "id", "user_id", "customer_id", "order_id", "product_id", "account_id",
        "employee_id", "person_id", "record_id", "identifier", "uuid", "guid"
    ));
    
    @Override
    public PIIScanResult scanDatabase(IDatabaseConnection conn, DatabaseMetadata schema) throws Exception {
        log.info("Starting PII scan for database: {}", schema.getDatabaseName());
        
        List<PIIScanResult.ColumnClassification> classifications = new ArrayList<>();
        
        if (schema.getTables() != null) {
            for (DatabaseMetadata.TableMetadata table : schema.getTables()) {
                if (table.getColumns() != null) {
                    for (DatabaseMetadata.ColumnMetadata column : table.getColumns()) {
                        try {
                            // Sample data from the column (first 100 rows)
                            List<String> samples = sampleColumnData(conn, table.getName(), column.getName(), 100);
                            
                            // Classify the column
                            PIIClassification classification = classifyColumn(table.getName(), column, samples);
                            
                            classifications.add(PIIScanResult.ColumnClassification.builder()
                                .tableName(table.getName())
                                .columnName(column.getName())
                                .dataType(column.getDataType())
                                .classification(classification)
                                .build());
                        } catch (Exception e) {
                            log.warn("Failed to classify column {}.{}: {}", 
                                table.getName(), column.getName(), e.getMessage());
                        }
                    }
                }
            }
        }
        
        PIIScanResult result = PIIScanResult.builder()
            .databaseName(schema.getDatabaseName())
            .scanTime(java.time.LocalDateTime.now())
            .classifications(classifications)
            .samplesAnalyzed(100)
            .build();
        
        log.info("PII scan completed. Found {} PII columns, {} potential PII columns",
            result.getPIIColumnCount(), result.getPotentialPIIColumnCount());
        
        return result;
    }
    
    @Override
    public PIIClassification classifyColumn(String tableName, DatabaseMetadata.ColumnMetadata column, 
                                           List<String> samples) throws Exception {
        log.debug("Classifying column {}.{}", tableName, column.getName());
        
        // Check column name heuristics first (highest confidence)
        PIICategory categoryFromName = checkColumnNameHeuristics(column.getName());
        if (categoryFromName != PIICategory.NONE) {
            return PIIClassification.builder()
                .category(categoryFromName)
                .confidence(90.0)
                .detectionMethod("pattern")
                .evidence(List.of("Column name matches known PII pattern: " + column.getName()))
                .build();
        }
        
        // Check identifier heuristics
        if (isIdentifierColumn(column.getName())) {
            return PIIClassification.builder()
                .category(PIICategory.IDENTIFIER)
                .confidence(80.0)
                .detectionMethod("pattern")
                .evidence(List.of("Column name matches identifier pattern: " + column.getName()))
                .build();
        }
        
        // Analyze sample data patterns
        if (samples != null && !samples.isEmpty()) {
            PIIClassification dataPatternClassification = analyzeDataPatterns(samples);
            if (dataPatternClassification.isPII()) {
                return dataPatternClassification;
            }
        }
        
        // No PII detected
        return PIIClassification.builder()
            .category(PIICategory.NONE)
            .confidence(0.0)
            .detectionMethod("pattern")
            .evidence(new ArrayList<>())
            .build();
    }
    
    @Override
    public double calculateConfidence(String columnName, String dataType, List<String> samples) {
        log.debug("Calculating confidence for column: {}", columnName);
        
        double confidence = 0.0;
        
        // Check column name
        PIICategory categoryFromName = checkColumnNameHeuristics(columnName);
        if (categoryFromName != PIICategory.NONE) {
            confidence = 90.0;
        } else if (isIdentifierColumn(columnName)) {
            confidence = 80.0;
        }
        
        // Analyze data patterns if no name match
        if (confidence == 0.0 && samples != null && !samples.isEmpty()) {
            PIIClassification classification = analyzeDataPatterns(samples);
            confidence = classification.getConfidence();
        }
        
        return confidence;
    }
    
    @Override
    public PIICategory detectPattern(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PIICategory.NONE;
        }
        
        String trimmedValue = value.trim();
        
        // Check email pattern
        if (EMAIL_PATTERN.matcher(trimmedValue).matches()) {
            return PIICategory.EMAIL;
        }
        
        // Check phone pattern
        if (PHONE_PATTERN.matcher(trimmedValue).matches()) {
            return PIICategory.PHONE;
        }
        
        // Check SSN pattern
        if (SSN_PATTERN.matcher(trimmedValue).matches()) {
            return PIICategory.SSN;
        }
        
        // Check credit card pattern (13-19 digits)
        if (CREDIT_CARD_PATTERN.matcher(trimmedValue).matches() && trimmedValue.length() >= 13) {
            return PIICategory.CREDIT_CARD;
        }
        
        // Check UUID pattern
        if (UUID_PATTERN.matcher(trimmedValue).matches()) {
            return PIICategory.IDENTIFIER;
        }
        
        return PIICategory.NONE;
    }
    
    /**
     * Check if column name matches known PII patterns.
     */
    private PIICategory checkColumnNameHeuristics(String columnName) {
        if (columnName == null || columnName.trim().isEmpty()) {
            return PIICategory.NONE;
        }
        
        String lowerName = columnName.toLowerCase();
        
        // Direct match
        if (COLUMN_NAME_HEURISTICS.containsKey(lowerName)) {
            return COLUMN_NAME_HEURISTICS.get(lowerName);
        }
        
        // Partial match (contains)
        for (Map.Entry<String, PIICategory> entry : COLUMN_NAME_HEURISTICS.entrySet()) {
            if (lowerName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return PIICategory.NONE;
    }
    
    /**
     * Check if column name indicates an identifier.
     */
    private boolean isIdentifierColumn(String columnName) {
        if (columnName == null || columnName.trim().isEmpty()) {
            return false;
        }
        
        String lowerName = columnName.toLowerCase();
        
        for (String keyword : IDENTIFIER_KEYWORDS) {
            if (lowerName.equals(keyword) || lowerName.endsWith("_" + keyword)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Analyze data patterns in sample values.
     */
    private PIIClassification analyzeDataPatterns(List<String> samples) {
        if (samples == null || samples.isEmpty()) {
            return PIIClassification.builder()
                .category(PIICategory.NONE)
                .confidence(0.0)
                .detectionMethod("pattern")
                .evidence(new ArrayList<>())
                .build();
        }
        
        Map<PIICategory, Integer> categoryMatches = new HashMap<>();
        List<String> evidence = new ArrayList<>();
        
        for (String sample : samples) {
            if (sample != null && !sample.trim().isEmpty()) {
                PIICategory category = detectPattern(sample);
                if (category != PIICategory.NONE) {
                    categoryMatches.put(category, categoryMatches.getOrDefault(category, 0) + 1);
                    if (evidence.size() < 3) {
                        evidence.add(sample);
                    }
                }
            }
        }
        
        if (categoryMatches.isEmpty()) {
            return PIIClassification.builder()
                .category(PIICategory.NONE)
                .confidence(0.0)
                .detectionMethod("pattern")
                .evidence(new ArrayList<>())
                .build();
        }
        
        // Find the most common category
        PIICategory topCategory = categoryMatches.entrySet().stream()
            .max(Comparator.comparingInt(Map.Entry::getValue))
            .map(Map.Entry::getKey)
            .orElse(PIICategory.NONE);
        
        // Calculate confidence based on percentage of matches
        double confidence = (double) categoryMatches.get(topCategory) / samples.size() * 100.0;
        
        return PIIClassification.builder()
            .category(topCategory)
            .confidence(confidence)
            .detectionMethod("pattern")
            .evidence(evidence)
            .build();
    }
    
    /**
     * Sample data from a column.
     * This is a placeholder - actual implementation would query the database.
     */
    private List<String> sampleColumnData(IDatabaseConnection conn, String tableName, 
                                         String columnName, int limit) throws Exception {
        // Placeholder implementation - returns empty list
        // Actual implementation would execute: SELECT columnName FROM tableName LIMIT limit
        log.debug("Sampling data from {}.{}", tableName, columnName);
        return new ArrayList<>();
    }
}
