package com.datanymize.pii.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Model representing PII classification for a column.
 * Contains category, confidence score, detection method, and evidence.
 * 
 * Validates Requirements: 3.1, 3.2
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PIIClassification {
    
    /**
     * PII category (email, phone, ssn, etc.)
     */
    private PIICategory category;
    
    /**
     * Confidence score (0-100%)
     */
    private double confidence;
    
    /**
     * Detection method (pattern, ai, manual)
     */
    private String detectionMethod;
    
    /**
     * Evidence supporting the classification (sample values, patterns found)
     */
    private List<String> evidence;
    
    /**
     * Check if classification is high confidence (>80%)
     */
    public boolean isHighConfidence() {
        return confidence > 80.0;
    }
    
    /**
     * Check if classification is medium confidence (60-80%)
     */
    public boolean isMediumConfidence() {
        return confidence >= 60.0 && confidence <= 80.0;
    }
    
    /**
     * Check if classification is low confidence (<60%)
     */
    public boolean isLowConfidence() {
        return confidence < 60.0;
    }
    
    /**
     * Check if this is a PII classification (not NONE)
     */
    public boolean isPII() {
        return category != null && category != PIICategory.NONE;
    }
}
