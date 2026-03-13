package com.datanymize.config.validator;

import com.datanymize.config.model.*;
import com.datanymize.config.parser.ConfigurationParsingException;

import java.util.*;

/**
 * Validator for anonymization configurations.
 * Checks all constraints and validates configuration correctness.
 */
public class ConfigValidator {
    private final Set<String> builtInTransformers = Set.of(
        "fake_name", "fake_email", "fake_phone", "hash", "mask", 
        "null", "constant", "random_string", "random_number"
    );

    /**
     * Validate an anonymization configuration.
     * @param config Configuration to validate
     * @throws ConfigurationParsingException if validation fails
     */
    public void validate(AnonymizationConfig config) throws ConfigurationParsingException {
        if (config == null) {
            throw new ConfigurationParsingException("Configuration cannot be null");
        }

        // Validate tables
        if (config.getTables() == null || config.getTables().isEmpty()) {
            throw new ConfigurationParsingException("Configuration must contain at least one table");
        }

        for (TableConfig tableConfig : config.getTables().values()) {
            validateTableConfig(tableConfig, config);
        }

        // Validate subset
        if (config.getSubset() != null) {
            validateSubsetConfig(config.getSubset());
        }

        // Validate referential integrity
        if (config.getReferentialIntegrity() != null) {
            validateReferentialIntegrityConfig(config.getReferentialIntegrity());
        }
    }

    private void validateTableConfig(TableConfig tableConfig, AnonymizationConfig config) throws ConfigurationParsingException {
        if (tableConfig.getTableName() == null || tableConfig.getTableName().isEmpty()) {
            throw new ConfigurationParsingException("Table name cannot be empty");
        }

        if (tableConfig.getColumns() == null || tableConfig.getColumns().isEmpty()) {
            throw new ConfigurationParsingException("Table '" + tableConfig.getTableName() + "' must contain at least one column transformation");
        }

        for (ColumnTransformation colTransform : tableConfig.getColumns().values()) {
            validateColumnTransformation(colTransform, tableConfig, config);
        }
    }

    private void validateColumnTransformation(ColumnTransformation colTransform, TableConfig tableConfig, AnonymizationConfig config) throws ConfigurationParsingException {
        if (colTransform.getColumnName() == null || colTransform.getColumnName().isEmpty()) {
            throw new ConfigurationParsingException("Column name cannot be empty in table '" + tableConfig.getTableName() + "'");
        }

        if (colTransform.getTransformerName() == null || colTransform.getTransformerName().isEmpty()) {
            throw new ConfigurationParsingException("Transformer name cannot be empty for column '" + colTransform.getColumnName() + "' in table '" + tableConfig.getTableName() + "'");
        }

        // Check if transformer exists
        if (!isTransformerAvailable(colTransform.getTransformerName(), config)) {
            throw new ConfigurationParsingException("Unknown transformer '" + colTransform.getTransformerName() + "' for column '" + colTransform.getColumnName() + "'. Available transformers: " + String.join(", ", builtInTransformers));
        }

        // Validate deterministic configuration
        if (colTransform.isDeterministic() && (colTransform.getSeed() == null || colTransform.getSeed().isEmpty())) {
            throw new ConfigurationParsingException("Deterministic transformation for column '" + colTransform.getColumnName() + "' must have a seed value");
        }
    }

    private void validateSubsetConfig(SubsetConfig subsetConfig) throws ConfigurationParsingException {
        if (subsetConfig.getPercentage() < 0 || subsetConfig.getPercentage() > 100) {
            throw new ConfigurationParsingException("Subset percentage must be between 0 and 100, got: " + subsetConfig.getPercentage());
        }

        if (subsetConfig.getFilters() != null) {
            for (SubsetConfig.FilterCriteria filter : subsetConfig.getFilters()) {
                validateFilterCriteria(filter);
            }
        }
    }

    private void validateFilterCriteria(SubsetConfig.FilterCriteria filter) throws ConfigurationParsingException {
        if (filter.getColumn() == null || filter.getColumn().isEmpty()) {
            throw new ConfigurationParsingException("Filter column name cannot be empty");
        }

        if (filter.getOperator() == null || filter.getOperator().isEmpty()) {
            throw new ConfigurationParsingException("Filter operator cannot be empty for column '" + filter.getColumn() + "'");
        }

        Set<String> validOperators = Set.of("=", "!=", "<", ">", "<=", ">=", "IN", "LIKE");
        if (!validOperators.contains(filter.getOperator())) {
            throw new ConfigurationParsingException("Invalid filter operator '" + filter.getOperator() + "'. Valid operators: " + String.join(", ", validOperators));
        }

        if (filter.getValue() == null) {
            throw new ConfigurationParsingException("Filter value cannot be null for column '" + filter.getColumn() + "'");
        }
    }

    private void validateReferentialIntegrityConfig(ReferentialIntegrityConfig config) throws ConfigurationParsingException {
        if (config.getMissingForeignKeyStrategy() == null || config.getMissingForeignKeyStrategy().isEmpty()) {
            throw new ConfigurationParsingException("Missing foreign key strategy cannot be empty");
        }

        Set<String> validStrategies = Set.of("INCLUDE", "SET_NULL", "RESTRICT");
        if (!validStrategies.contains(config.getMissingForeignKeyStrategy())) {
            throw new ConfigurationParsingException("Invalid missing foreign key strategy '" + config.getMissingForeignKeyStrategy() + "'. Valid strategies: " + String.join(", ", validStrategies));
        }
    }

    private boolean isTransformerAvailable(String transformerName, AnonymizationConfig config) {
        // Check built-in transformers
        if (builtInTransformers.contains(transformerName)) {
            return true;
        }

        // Check custom transformers
        if (config.getTransformers() != null && config.getTransformers().containsKey(transformerName)) {
            return true;
        }

        return false;
    }

    /**
     * Get list of available transformers.
     * @return Set of transformer names
     */
    public Set<String> getAvailableTransformers() {
        return new HashSet<>(builtInTransformers);
    }
}
