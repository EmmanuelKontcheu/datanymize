package com.datanymize.pii.ai;

import com.datanymize.pii.model.PIICategory;
import com.datanymize.pii.model.PIIClassification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock AI provider for testing and development.
 * Returns deterministic classifications based on column name and data patterns.
 * 
 * Validates Requirements: 3.1, 3.2
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "datanymize.pii.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAIProvider implements IAIProvider {
    
    private double lastConfidence = 0.0;
    private long promptTokens = 0;
    private long completionTokens = 0;
    
    @Override
    public PIIClassification classifyColumn(String columnName, String dataType, List<String> samples, 
                                           int maxTokens) throws Exception {
        log.debug("Mock AI classification for column: {}", columnName);
        
        // Simulate token usage
        promptTokens += 50;
        completionTokens += 30;
        
        // Simple heuristic-based classification
        String lowerName = columnName.toLowerCase();
        
        if (lowerName.contains("email")) {
            lastConfidence = 85.0;
            return PIIClassification.builder()
                .category(PIICategory.EMAIL)
                .confidence(lastConfidence)
                .detectionMethod("ai")
                .evidence(List.of("Column name contains 'email'"))
                .build();
        }
        
        if (lowerName.contains("phone")) {
            lastConfidence = 82.0;
            return PIIClassification.builder()
                .category(PIICategory.PHONE)
                .confidence(lastConfidence)
                .detectionMethod("ai")
                .evidence(List.of("Column name contains 'phone'"))
                .build();
        }
        
        if (lowerName.contains("ssn") || lowerName.contains("social")) {
            lastConfidence = 95.0;
            return PIIClassification.builder()
                .category(PIICategory.SSN)
                .confidence(lastConfidence)
                .detectionMethod("ai")
                .evidence(List.of("Column name contains 'ssn' or 'social'"))
                .build();
        }
        
        if (lowerName.contains("credit") || lowerName.contains("card")) {
            lastConfidence = 90.0;
            return PIIClassification.builder()
                .category(PIICategory.CREDIT_CARD)
                .confidence(lastConfidence)
                .detectionMethod("ai")
                .evidence(List.of("Column name contains 'credit' or 'card'"))
                .build();
        }
        
        if (lowerName.contains("name")) {
            lastConfidence = 75.0;
            return PIIClassification.builder()
                .category(PIICategory.NAME)
                .confidence(lastConfidence)
                .detectionMethod("ai")
                .evidence(List.of("Column name contains 'name'"))
                .build();
        }
        
        if (lowerName.contains("address")) {
            lastConfidence = 80.0;
            return PIIClassification.builder()
                .category(PIICategory.ADDRESS)
                .confidence(lastConfidence)
                .detectionMethod("ai")
                .evidence(List.of("Column name contains 'address'"))
                .build();
        }
        
        // Default: not PII
        lastConfidence = 0.0;
        return PIIClassification.builder()
            .category(PIICategory.NONE)
            .confidence(lastConfidence)
            .detectionMethod("ai")
            .evidence(new ArrayList<>())
            .build();
    }
    
    @Override
    public double getConfidenceScore() {
        return lastConfidence;
    }
    
    @Override
    public String getProviderName() {
        return "Mock AI Provider";
    }
    
    @Override
    public boolean isAvailable() {
        return true;
    }
    
    @Override
    public TokenUsage getTokenUsage() {
        return new TokenUsage(promptTokens, completionTokens);
    }
}
