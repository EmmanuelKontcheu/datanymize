package com.datanymize.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for a transformer (anonymization rule).
 * Defines how a column should be transformed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransformerConfig {
    private String name;                          // Transformer name (e.g., fake_name, hash)
    private Map<String, Object> parameters;       // Transformer-specific parameters
    private boolean deterministic;                // Whether transformation is deterministic
    private String seed;                          // Seed for deterministic transformations
    private String description;                   // Human-readable description

    public TransformerConfig(String name) {
        this.name = name;
        this.parameters = new HashMap<>();
        this.deterministic = false;
        this.seed = null;
        this.description = "";
    }

    public TransformerConfig(String name, boolean deterministic, String seed) {
        this.name = name;
        this.parameters = new HashMap<>();
        this.deterministic = deterministic;
        this.seed = seed;
        this.description = "";
    }
}
