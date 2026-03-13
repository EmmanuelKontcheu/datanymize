package com.datanymize.config.parser;

import com.datanymize.config.model.*;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import java.util.*;

/**
 * Parser for YAML-format anonymization configurations.
 */
public class YAMLConfigParser implements IConfigurationParser {

    @Override
    public AnonymizationConfig parse(String content) throws ConfigurationParsingException {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(content);

            if (data == null) {
                throw new ConfigurationParsingException("Configuration is empty");
            }

            AnonymizationConfig config = new AnonymizationConfig();
            config.setVersion((String) data.getOrDefault("version", "1.0"));
            config.setDescription((String) data.getOrDefault("description", ""));

            // Parse tables
            Map<String, Object> tablesData = (Map<String, Object>) data.get("tables");
            if (tablesData != null) {
                for (Map.Entry<String, Object> entry : tablesData.entrySet()) {
                    TableConfig tableConfig = parseTableConfig(entry.getKey(), (Map<String, Object>) entry.getValue());
                    config.addTableConfig(tableConfig);
                }
            }

            // Parse transformers
            Map<String, Object> transformersData = (Map<String, Object>) data.get("transformers");
            if (transformersData != null) {
                for (Map.Entry<String, Object> entry : transformersData.entrySet()) {
                    TransformerConfig transformerConfig = parseTransformerConfig(entry.getKey(), (Map<String, Object>) entry.getValue());
                    config.addTransformerConfig(transformerConfig);
                }
            }

            // Parse subset
            Map<String, Object> subsetData = (Map<String, Object>) data.get("subset");
            if (subsetData != null) {
                config.setSubset(parseSubsetConfig(subsetData));
            }

            // Parse referential integrity
            Map<String, Object> riData = (Map<String, Object>) data.get("referentialIntegrity");
            if (riData != null) {
                config.setReferentialIntegrity(parseReferentialIntegrityConfig(riData));
            }

            return config;
        } catch (MarkedYAMLException e) {
            throw new ConfigurationParsingException(
                e.getMessage(),
                e.getProblemMark().getLine() + 1,
                e.getProblemMark().getColumn() + 1,
                "YAML_SYNTAX_ERROR"
            );
        } catch (YAMLException e) {
            throw new ConfigurationParsingException("YAML parsing error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ConfigurationParsingException("Configuration parsing error: " + e.getMessage(), e);
        }
    }

    private TableConfig parseTableConfig(String tableName, Map<String, Object> data) throws ConfigurationParsingException {
        TableConfig tableConfig = new TableConfig(tableName);

        // Parse columns
        Map<String, Object> columnsData = (Map<String, Object>) data.get("columns");
        if (columnsData != null) {
            for (Map.Entry<String, Object> entry : columnsData.entrySet()) {
                ColumnTransformation colTransform = parseColumnTransformation(entry.getKey(), (Map<String, Object>) entry.getValue());
                tableConfig.addColumnTransformation(colTransform);
            }
        }

        // Parse primary keys
        List<String> primaryKeys = (List<String>) data.get("primaryKeys");
        if (primaryKeys != null) {
            tableConfig.setPrimaryKeys(primaryKeys);
        }

        // Parse unique keys
        List<String> uniqueKeys = (List<String>) data.get("uniqueKeys");
        if (uniqueKeys != null) {
            tableConfig.setUniqueKeys(uniqueKeys);
        }

        return tableConfig;
    }

    private ColumnTransformation parseColumnTransformation(String columnName, Map<String, Object> data) throws ConfigurationParsingException {
        ColumnTransformation transformation = new ColumnTransformation();
        transformation.setColumnName(columnName);
        transformation.setTransformerName((String) data.get("transformer"));
        transformation.setDeterministic((Boolean) data.getOrDefault("deterministic", false));
        transformation.setSeed((String) data.get("seed"));

        Map<String, Object> params = (Map<String, Object>) data.get("params");
        if (params != null) {
            transformation.setTransformerParams(params);
        }

        return transformation;
    }

    private TransformerConfig parseTransformerConfig(String transformerName, Map<String, Object> data) throws ConfigurationParsingException {
        TransformerConfig config = new TransformerConfig();
        config.setName(transformerName);
        config.setDescription((String) data.getOrDefault("description", ""));
        config.setDeterministic((Boolean) data.getOrDefault("deterministic", false));
        config.setSeed((String) data.get("seed"));

        Map<String, Object> params = (Map<String, Object>) data.get("parameters");
        if (params != null) {
            config.setParameters(params);
        }

        return config;
    }

    private SubsetConfig parseSubsetConfig(Map<String, Object> data) throws ConfigurationParsingException {
        SubsetConfig config = new SubsetConfig();
        
        Object percentageObj = data.get("percentage");
        if (percentageObj != null) {
            if (percentageObj instanceof Number) {
                config.setPercentage(((Number) percentageObj).doubleValue());
            } else {
                config.setPercentage(Double.parseDouble(percentageObj.toString()));
            }
        }

        Object seedObj = data.get("seed");
        if (seedObj != null) {
            if (seedObj instanceof Number) {
                config.setSeed(((Number) seedObj).longValue());
            } else {
                config.setSeed(Long.parseLong(seedObj.toString()));
            }
        }

        config.setIncludeForeignKeyDependencies((Boolean) data.getOrDefault("includeForeignKeyDependencies", true));

        List<Map<String, Object>> filtersData = (List<Map<String, Object>>) data.get("filters");
        if (filtersData != null) {
            for (Map<String, Object> filterData : filtersData) {
                SubsetConfig.FilterCriteria filter = new SubsetConfig.FilterCriteria();
                filter.setColumn((String) filterData.get("column"));
                filter.setOperator((String) filterData.get("operator"));
                filter.setValue(filterData.get("value"));
                config.addFilter(filter);
            }
        }

        return config;
    }

    private ReferentialIntegrityConfig parseReferentialIntegrityConfig(Map<String, Object> data) throws ConfigurationParsingException {
        ReferentialIntegrityConfig config = new ReferentialIntegrityConfig();
        config.setMissingForeignKeyStrategy((String) data.getOrDefault("missingForeignKeyStrategy", "INCLUDE"));
        config.setValidateAfterAnonymization((Boolean) data.getOrDefault("validateAfterAnonymization", true));
        config.setPreserveConstraints((Boolean) data.getOrDefault("preserveConstraints", true));
        return config;
    }

    @Override
    public String getFormat() {
        return "YAML";
    }
}
