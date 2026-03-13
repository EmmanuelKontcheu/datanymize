package com.datanymize.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for initiating PII scans.
 * 
 * Validates Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PIIScanRequest {
    
    @NotBlank(message = "Connection ID is required")
    private String connectionId;
    
    private Integer sampleSize = 100; // Number of rows to sample per table
    
    private Boolean useAI = true; // Use AI-based detection
    
    private Boolean usePatternMatching = true; // Use pattern-based detection
    
    private String description;
}
