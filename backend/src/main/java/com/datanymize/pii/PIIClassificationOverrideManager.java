package com.datanymize.pii;

import com.datanymize.pii.model.PIICategory;
import com.datanymize.pii.model.PIIClassification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for PII classification overrides.
 * Allows users to manually override automatic PII classifications.
 * 
 * Validates Requirements: 3.7
 */
@Slf4j
@Component
public class PIIClassificationOverrideManager {
    
    private final Map<String, ClassificationOverride> overrides = new ConcurrentHashMap<>();
    
    /**
     * Override a column's PII classification.
     * 
     * @param tableName Table name
     * @param columnName Column name
     * @param newCategory New PII category
     * @param reason Reason for override
     * @param userId User ID making the override
     * @return ClassificationOverride record
     */
    public ClassificationOverride overrideClassification(String tableName, String columnName,
                                                         PIICategory newCategory, String reason,
                                                         String userId) {
        log.info("Overriding classification for {}.{} to {} by user {}", 
            tableName, columnName, newCategory, userId);
        
        String key = getKey(tableName, columnName);
        
        ClassificationOverride override = ClassificationOverride.builder()
            .tableName(tableName)
            .columnName(columnName)
            .newCategory(newCategory)
            .reason(reason)
            .userId(userId)
            .timestamp(LocalDateTime.now())
            .version(1)
            .build();
        
        // Check if override already exists
        if (overrides.containsKey(key)) {
            ClassificationOverride existing = overrides.get(key);
            override.setVersion(existing.getVersion() + 1);
        }
        
        overrides.put(key, override);
        return override;
    }
    
    /**
     * Get an override for a column.
     * 
     * @param tableName Table name
     * @param columnName Column name
     * @return ClassificationOverride or null if not overridden
     */
    public ClassificationOverride getOverride(String tableName, String columnName) {
        return overrides.get(getKey(tableName, columnName));
    }
    
    /**
     * Check if a column has an override.
     * 
     * @param tableName Table name
     * @param columnName Column name
     * @return true if overridden, false otherwise
     */
    public boolean hasOverride(String tableName, String columnName) {
        return overrides.containsKey(getKey(tableName, columnName));
    }
    
    /**
     * Remove an override for a column.
     * 
     * @param tableName Table name
     * @param columnName Column name
     * @return true if override was removed, false if not found
     */
    public boolean removeOverride(String tableName, String columnName) {
        String key = getKey(tableName, columnName);
        if (overrides.containsKey(key)) {
            log.info("Removing override for {}.{}", tableName, columnName);
            overrides.remove(key);
            return true;
        }
        return false;
    }
    
    /**
     * Apply overrides to a classification.
     * 
     * @param tableName Table name
     * @param columnName Column name
     * @param originalClassification Original classification
     * @return Overridden classification if override exists, otherwise original
     */
    public PIIClassification applyOverride(String tableName, String columnName,
                                          PIIClassification originalClassification) {
        ClassificationOverride override = getOverride(tableName, columnName);
        
        if (override != null) {
            log.debug("Applying override for {}.{}", tableName, columnName);
            
            return PIIClassification.builder()
                .category(override.getNewCategory())
                .confidence(100.0)  // Overrides have 100% confidence
                .detectionMethod("manual")
                .evidence(List.of("Manual override: " + override.getReason()))
                .build();
        }
        
        return originalClassification;
    }
    
    /**
     * Get all overrides.
     * 
     * @return List of all overrides
     */
    public List<ClassificationOverride> getAllOverrides() {
        return new ArrayList<>(overrides.values());
    }
    
    /**
     * Get overrides for a specific table.
     * 
     * @param tableName Table name
     * @return List of overrides for the table
     */
    public List<ClassificationOverride> getTableOverrides(String tableName) {
        List<ClassificationOverride> result = new ArrayList<>();
        for (ClassificationOverride override : overrides.values()) {
            if (override.getTableName().equals(tableName)) {
                result.add(override);
            }
        }
        return result;
    }
    
    /**
     * Clear all overrides.
     */
    public void clearAllOverrides() {
        log.info("Clearing all classification overrides");
        overrides.clear();
    }
    
    /**
     * Get override statistics.
     * 
     * @return Map with override statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOverrides", overrides.size());
        
        Map<PIICategory, Integer> categoryCount = new HashMap<>();
        for (ClassificationOverride override : overrides.values()) {
            categoryCount.put(override.getNewCategory(), 
                categoryCount.getOrDefault(override.getNewCategory(), 0) + 1);
        }
        stats.put("byCategory", categoryCount);
        
        return stats;
    }
    
    private String getKey(String tableName, String columnName) {
        return tableName + "." + columnName;
    }
    
    /**
     * Model for a classification override.
     */
    public static class ClassificationOverride {
        private String tableName;
        private String columnName;
        private PIICategory newCategory;
        private String reason;
        private String userId;
        private LocalDateTime timestamp;
        private int version;
        
        public ClassificationOverride() {
        }
        
        public ClassificationOverride(String tableName, String columnName, PIICategory newCategory,
                                     String reason, String userId, LocalDateTime timestamp, int version) {
            this.tableName = tableName;
            this.columnName = columnName;
            this.newCategory = newCategory;
            this.reason = reason;
            this.userId = userId;
            this.timestamp = timestamp;
            this.version = version;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public String getTableName() {
            return tableName;
        }
        
        public void setTableName(String tableName) {
            this.tableName = tableName;
        }
        
        public String getColumnName() {
            return columnName;
        }
        
        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }
        
        public PIICategory getNewCategory() {
            return newCategory;
        }
        
        public void setNewCategory(PIICategory newCategory) {
            this.newCategory = newCategory;
        }
        
        public String getReason() {
            return reason;
        }
        
        public void setReason(String reason) {
            this.reason = reason;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
        
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
        
        public int getVersion() {
            return version;
        }
        
        public void setVersion(int version) {
            this.version = version;
        }
        
        public static class Builder {
            private String tableName;
            private String columnName;
            private PIICategory newCategory;
            private String reason;
            private String userId;
            private LocalDateTime timestamp;
            private int version;
            
            public Builder tableName(String tableName) {
                this.tableName = tableName;
                return this;
            }
            
            public Builder columnName(String columnName) {
                this.columnName = columnName;
                return this;
            }
            
            public Builder newCategory(PIICategory newCategory) {
                this.newCategory = newCategory;
                return this;
            }
            
            public Builder reason(String reason) {
                this.reason = reason;
                return this;
            }
            
            public Builder userId(String userId) {
                this.userId = userId;
                return this;
            }
            
            public Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }
            
            public Builder version(int version) {
                this.version = version;
                return this;
            }
            
            public ClassificationOverride build() {
                return new ClassificationOverride(tableName, columnName, newCategory, reason, userId, timestamp, version);
            }
        }
    }
}
