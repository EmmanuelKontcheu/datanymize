package com.datanymize.config.versioning;

import com.datanymize.config.model.AnonymizationConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a version of an anonymization configuration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigVersion {
    private String configId;
    private int versionNumber;
    private AnonymizationConfig config;
    private LocalDateTime createdAt;
    private String createdBy;
    private String description;
    private Map<String, Object> changes;

    public ConfigVersion(String configId, int versionNumber, AnonymizationConfig config, String createdBy) {
        this.configId = configId;
        this.versionNumber = versionNumber;
        this.config = config;
        this.createdAt = LocalDateTime.now();
        this.createdBy = createdBy;
        this.changes = new HashMap<>();
    }
}
