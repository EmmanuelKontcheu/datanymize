package com.datanymize.pii.ai;

import com.datanymize.pii.model.PIIClassification;

import java.util.List;

/**
 * Interface for AI-based PII classification providers.
 * Supports multiple AI services (OpenAI, Anthropic, etc.) with fallback to pattern matching.
 * 
 * Validates Requirements: 3.1, 3.2
 */
public interface IAIProvider {
    
    /**
     * Classify a column using AI analysis.
     * 
     * @param columnName Name of the column
     * @param dataType Data type of the column
     * @param samples Sample values from the column (first 100 rows)
     * @param maxTokens Maximum tokens to use for the request
     * @return PIIClassification with category and confidence
     * @throws Exception if classification fails
     */
    PIIClassification classifyColumn(String columnName, String dataType, List<String> samples, 
                                     int maxTokens) throws Exception;
    
    /**
     * Get the confidence score from the last classification.
     * 
     * @return Confidence score (0-100%)
     */
    double getConfidenceScore();
    
    /**
     * Get the name of this AI provider.
     * 
     * @return Provider name (e.g., "OpenAI", "Anthropic")
     */
    String getProviderName();
    
    /**
     * Check if this provider is available and configured.
     * 
     * @return true if provider is available, false otherwise
     */
    boolean isAvailable();
    
    /**
     * Get the current token usage for this provider.
     * 
     * @return Token usage statistics
     */
    TokenUsage getTokenUsage();
    
    /**
     * Model for tracking token usage.
     */
    class TokenUsage {
        public long promptTokens;
        public long completionTokens;
        public long totalTokens;
        
        public TokenUsage(long promptTokens, long completionTokens) {
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = promptTokens + completionTokens;
        }
    }
}
