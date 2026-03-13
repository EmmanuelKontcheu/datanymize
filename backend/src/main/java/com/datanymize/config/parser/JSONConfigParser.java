package com.datanymize.config.parser;

import com.datanymize.config.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.*;

/**
 * Parser for JSON-format anonymization configurations.
 */
public class JSONConfigParser implements IConfigurationParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AnonymizationConfig parse(String content) throws ConfigurationParsingException {
        try {
            JsonNode rootNode = objectMapper.readTree(content);

            if (rootNode == null || rootNode.isNull()) {
                throw new ConfigurationParsingException("Configuration is empty");
            }

            AnonymizationConfig config = new AnonymizationConfig();
            config.setVersion(rootNode.path("version").asText("1.0"));
            config.setDescription(rootNode.path("description").asText(""));

            // Parse tables
            JsonNode tablesNode = rootNode.path("tables");
            if (tablesNode.isObject()) {
                tablesNode.fields().forEachRemaining(entry -> {
                    try {
                        TableConfig tableConfig = parseTableConfig(entry.getKey(), entry.getValue());
                        config.addTableConfig(tableConfig);
                    } catch (ConfigurationParsingException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            // Parse transformers
            JsonNode transformersNode = rootNode.path("transformers");
            if (transformersNode.isObject()) {
                transformersNode.fields().forEachRemaining(entry -> {
                    try {
                        TransformerConfig transformerConfig = parseTransformerConfig(entry.getKey(), entry.getValue());
                        config.addTransformerConfig(transformerConfig);
                    } catch (ConfigurationParsingException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            // Parse subset
            JsonNode subsetNode = rootNode.path("subset");
            if (subsetNode.isObject()) {
                config.setSubset(parseSubsetConfig(subsetNode));
            }

            // Parse referential integrity
            JsonNode riNode = rootNode.path("referentialIntegrity");
            if (riNode.isObject()) {
                config.setReferentialIntegrity(parseReferentialIntegrityConfig(riNode));
            }

            return config;
        } catch (JsonProcessingException e) {
            throw new ConfigurationParsingException(
                "JSON parsing error: " + e.getMessage(),
                e.getLocation().getLineNr(),
                e.getLocation().getColumnNr(),
                "JSON_SYNTAX_ERROR"
            );
        } catch (RuntimeException e) {
            if (e.getCause() instanceof ConfigurationParsingException) {
                throw (ConfigurationParsingException) e.getCause();
            }
            throw new ConfigurationParsingException("Configuration parsing error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ConfigurationParsingException("Configuration parsing error: " + e.getMessage(), e);
        }
    }

    private TableConfig parseTableConfig(String tableName, JsonNode node) throws ConfigurationParsingException {
        TableConfig tableConfig = new TableConfig(tableName);

        // Parse columns
        JsonNode columnsNode = node.path("columns");
        if (columnsNode.isObject()) {
            columnsNode.fields().forEachRemaining(entry -> {
                try {
                    ColumnTransformation colTransform = parseColumnTransformation(entry.getKey(), entry.getValue());
                    tableConfig.addColumnTransformation(colTransform);
                } catch (ConfigurationParsingException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // Parse primary keys
        JsonNode primaryKeysNode = node.path("primaryKeys");
        if (primaryKeysNode.isArray()) {
            List<String> primaryKeys = new ArrayList<>();
            primaryKeysNode.forEach(key -> primaryKeys.add(key.asText()));
            tableConfig.setPrimaryKeys(primaryKeys);
        }

        // Parse unique keys
        JsonNode uniqueKeysNode = node.path("uniqueKeys");
        if (uniqueKeysNode.isArray()) {
            List<String> uniqueKeys = new ArrayList<>();
            uniqueKeysNode.forEach(key -> uniqueKeys.add(key.asText()));
            tableConfig.setUniqueKeys(uniqueKeys);
        }

        return tableConfig;
    }

    private ColumnTransformation parseColumnTransformation(String columnName, JsonNode node) throws ConfigurationParsingException {
        ColumnTransformation transformation = new ColumnTransformation();
        transformation.setColumnName(columnName);
        transformation.setTransformerName(node.path("transformer").asText());
        transformation.setDeterministic(node.path("deterministic").asBoolean(false));
        transformation.setSeed(node.path("seed").asText(null));

        JsonNode paramsNode = node.path("params");
        if (paramsNode.isObject()) {
            Map<String, Object> params = objectMapper.convertValue(paramsNode, Map.class);
            transformation.setTransformerParams(params);
        }

        return transformation;
    }

    private TransformerConfig parseTransformerConfig(String transformerName, JsonNode node) throws ConfigurationParsingException {
        TransformerConfig config = new TransformerConfig();
        config.setName(transformerName);
        config.setDescription(node.path("description").asText(""));
        config.setDeterministic(node.path("deterministic").asBoolean(false));
        config.setSeed(node.path("seed").asText(null));

        JsonNode parametersNode = node.path("parameters");
        if (parametersNode.isObject()) {
            Map<String, Object> parameters = objectMapper.convertValue(parametersNode, Map.class);
            config.setParameters(parameters);
        }

        return config;
    }

    private SubsetConfig parseSubsetConfig(JsonNode node) throws ConfigurationParsingException {
        SubsetConfig config = new SubsetConfig();
        config.setPercentage(node.path("percentage").asDouble(100.0));
        config.setSeed(node.path("seed").asLong(System.currentTimeMillis()));
        config.setIncludeForeignKeyDependencies(node.path("includeForeignKeyDependencies").asBoolean(true));

        JsonNode filtersNode = node.path("filters");
        if (filtersNode.isArray()) {
            filtersNode.forEach(filterNode -> {
                SubsetConfig.FilterCriteria filter = new SubsetConfig.FilterCriteria();
                filter.setColumn(filterNode.path("column").asText());
                filter.setOperator(filterNode.path("operator").asText());
                filter.setValue(filterNode.path("value").asText());
                config.addFilter(filter);
            });
        }

        return config;
    }

    private ReferentialIntegrityConfig parseReferentialIntegrityConfig(JsonNode node) throws ConfigurationParsingException {
        ReferentialIntegrityConfig config = new ReferentialIntegrityConfig();
        config.setMissingForeignKeyStrategy(node.path("missingForeignKeyStrategy").asText("INCLUDE"));
        config.setValidateAfterAnonymization(node.path("validateAfterAnonymization").asBoolean(true));
        config.setPreserveConstraints(node.path("preserveConstraints").asBoolean(true));
        return config;
    }

    @Override
    public String getFormat() {
        return "JSON";
    }
}
