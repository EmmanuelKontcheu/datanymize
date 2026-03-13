package com.datanymize.pii;

import com.datanymize.database.connection.IDatabaseConnection;
import com.datanymize.database.model.DatabaseMetadata;
import com.datanymize.pii.ai.IAIProvider;
import com.datanymize.pii.model.PIICategory;
import com.datanymize.pii.model.PIIClassification;
import com.datanymize.pii.model.PIIScanResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * AI-based PII detector with fallback to pattern matching.
 * Uses AI providers (OpenAI, Anthropic) for semantic analysis with pattern matching fallback.
 * 
 * Validates Requirements: 3.1, 3.2, 3.5
 */
@Slf4j
@Component
public class AIBasedPIIDetector implements IPIIDetector {
    
    private final IAIProvider aiProvider;
    private final PatternBasedPIIDetector patternDetector;
    private static final int MAX_TOKENS = 100;
    private static final int CACHE_SIZE = 1000;
    
    // Cache for classifications to avoid redundant AI calls
    private final Map<String, PIIClassification> classificationCache = 
        new LinkedHashMap<String, PIIClassification>(CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > CACHE_SIZE;
            }
        };
    
    public AIBasedPIIDetector(IAIProvider aiProvider, PatternBasedPIIDetector patternDetector) {
        this.aiProvider = aiProvider;
        this.patternDetector = patternDetector;
    }
    
    @Override
    public PIIScanResult scanDatabase(IDatabaseConnection conn, DatabaseMetadata schema) throws Exception {
        log.info("Starting AI-based PII scan for database: {}", schema.getDatabaseName());
        
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
        
        log.info("AI-based PII scan completed. Found {} PII columns, {} potential PII columns",
            result.getPIIColumnCount(), result.getPotentialPIIColumnCount());
        
        return result;
    }
    
    @Override
    public PIIClassification classifyColumn(String tableName, DatabaseMetadata.ColumnMetadata column, 
                                           List<String> samples) throws Exception {
        log.debug("Classifying column {}.{} using AI", tableName, column.getName());
        
        // Check cache first
        String cacheKey = tableName + "." + column.getName();
        if (classificationCache.containsKey(cacheKey)) {
            log.debug("Using cached classification for {}", cacheKey);
            return classificationCache.get(cacheKey);
        }
        
        PIIClassification classification = null;
        
        // Try AI provider first
        if (aiProvider != null && aiProvider.isAvailable()) {
            try {
                classification = aiProvider.classifyColumn(
                    column.getName(), 
                    column.getDataType(), 
                    samples != null ? samples : new ArrayList<>(), 
                    MAX_TOKENS
                );
                log.debug("AI classification for {}.{}: {} (confidence: {}%)", 
                    tableName, column.getName(), classification.getCategory(), classification.getConfidence());
            } catch (Exception e) {
                log.warn("AI provider failed, falling back to pattern matching: {}", e.getMessage());
                classification = null;
            }
        }
        
        // Fallback to pattern matching if AI fails or is unavailable
        if (classification == null) {
            classification = patternDetector.classifyColumn(tableName, column, samples);
            log.debug("Pattern-based classification for {}.{}: {} (confidence: {}%)", 
                tableName, column.getName(), classification.getCategory(), classification.getConfidence());
        }
        
        // Cache the result
        classificationCache.put(cacheKey, classification);
        
        return classification;
    }
    
    @Override
    public double calculateConfidence(String columnName, String dataType, List<String> samples) {
        log.debug("Calculating confidence for column: {}", columnName);
        
        // Try AI provider first
        if (aiProvider != null && aiProvider.isAvailable()) {
            try {
                PIIClassification classification = aiProvider.classifyColumn(
                    columnName, 
                    dataType, 
                    samples != null ? samples : new ArrayList<>(), 
                    MAX_TOKENS
                );
                return classification.getConfidence();
            } catch (Exception e) {
                log.debug("AI provider failed for confidence calculation: {}", e.getMessage());
            }
        }
        
        // Fallback to pattern matching
        return patternDetector.calculateConfidence(columnName, dataType, samples);
    }
    
    @Override
    public PIICategory detectPattern(String value) {
        // Delegate to pattern detector
        return patternDetector.detectPattern(value);
    }
    
    /**
     * Clear the classification cache.
     */
    public void clearCache() {
        log.debug("Clearing classification cache");
        classificationCache.clear();
    }
    
    /**
     * Get cache statistics.
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", classificationCache.size());
        stats.put("maxCacheSize", CACHE_SIZE);
        return stats;
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
