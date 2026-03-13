package com.datanymize.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Complete anonymization configuration.
 * Specifies how to anonymize a database.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnonymizationConfig {
    private String id;
    private String version;
    private String description;
    private Map<String, TableConfig> tables;
    private Map<String, TransformerConfig> transformers;
    private SubsetConfig subset;
    private ReferentialIntegrityConfig referentialIntegrity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public AnonymizationConfig(String id, String version) {
        this.id = id;
        this.version = version;
        this.tables = new HashMap<>();
        this.transformers = new HashMap<>();
        this.referentialIntegrity = new ReferentialIntegrityConfig();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void addTableConfig(TableConfig tableConfig) {
        this.tables.put(tableConfig.getTableName(), tableConfig);
    }

    public TableConfig getTableConfig(String tableName) {
        return this.tables.get(tableName);
    }

    public void addTransformerConfig(TransformerConfig transformerConfig) {
        this.transformers.put(transformerConfig.getName(), transformerConfig);
    }

    public TransformerConfig getTransformerConfig(String transformerName) {
        return this.transformers.get(transformerName);
    }
}
