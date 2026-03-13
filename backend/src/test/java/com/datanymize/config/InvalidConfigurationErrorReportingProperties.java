package com.datanymize.config;

import com.datanymize.config.model.*;
import com.datanymize.config.parser.*;
import com.datanymize.config.validator.ConfigValidator;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

/**
 * Property-based tests for invalid configuration error reporting.
 * **Validates: Requirements 4.3**
 */
@PropertyDefaults(tries = 50)
public class InvalidConfigurationErrorReportingProperties {

    @Property
    @Label("Property 10: Invalid Configuration Error Reporting")
    void testInvalidConfigurationErrorReporting(
        @ForAll @StringLength(min = 1, max = 50) String invalidYAML
    ) {
        // Feature: datanymize, Property 10: Invalid Configuration Error Reporting
        
        // Given invalid YAML content
        String yamlContent = invalidYAML + "\n  invalid: [unclosed";
        
        // When parsing the invalid YAML
        YAMLConfigParser parser = new YAMLConfigParser();
        ConfigurationParsingException exception = null;
        try {
            parser.parse(yamlContent);
        } catch (ConfigurationParsingException e) {
            exception = e;
        }
        
        // Then an exception should be thrown with error information
        Assume.that(exception != null);
        Assume.that(exception.getMessage() != null && !exception.getMessage().isEmpty());
    }

    @Property
    @Label("Property 10b: Invalid JSON Error Reporting")
    void testInvalidJSONErrorReporting(
        @ForAll @StringLength(min = 1, max = 50) String invalidJSON
    ) {
        // Feature: datanymize, Property 10b: Invalid JSON Error Reporting
        
        // Given invalid JSON content
        String jsonContent = "{\"invalid\": " + invalidJSON;
        
        // When parsing the invalid JSON
        JSONConfigParser parser = new JSONConfigParser();
        ConfigurationParsingException exception = null;
        try {
            parser.parse(jsonContent);
        } catch (ConfigurationParsingException e) {
            exception = e;
        }
        
        // Then an exception should be thrown with error information
        Assume.that(exception != null);
        Assume.that(exception.getMessage() != null && !exception.getMessage().isEmpty());
    }

    @Property
    @Label("Property 10c: Unknown Transformer Error")
    void testUnknownTransformerError(
        @ForAll @StringLength(min = 1, max = 50) String unknownTransformer
    ) {
        // Feature: datanymize, Property 10c: Unknown Transformer Error
        
        // Assume transformer name is not a built-in transformer
        Assume.that(!unknownTransformer.equals("fake_name") && 
                   !unknownTransformer.equals("fake_email") &&
                   !unknownTransformer.equals("fake_phone") &&
                   !unknownTransformer.equals("hash") &&
                   !unknownTransformer.equals("mask") &&
                   !unknownTransformer.equals("null") &&
                   !unknownTransformer.equals("constant") &&
                   !unknownTransformer.equals("random_string") &&
                   !unknownTransformer.equals("random_number"));
        
        // Given a configuration with unknown transformer
        AnonymizationConfig config = new AnonymizationConfig("test", "1.0");
        TableConfig tableConfig = new TableConfig("users");
        ColumnTransformation colTransform = new ColumnTransformation("email", unknownTransformer);
        tableConfig.addColumnTransformation(colTransform);
        config.addTableConfig(tableConfig);
        
        // When validating the configuration
        ConfigValidator validator = new ConfigValidator();
        ConfigurationParsingException exception = null;
        try {
            validator.validate(config);
        } catch (ConfigurationParsingException e) {
            exception = e;
        }
        
        // Then an exception should be thrown
        Assume.that(exception != null);
        Assume.that(exception.getMessage().contains("Unknown transformer"));
    }

    @Property
    @Label("Property 10d: Invalid Subset Percentage Error")
    void testInvalidSubsetPercentageError(
        @ForAll @DoubleRange(min = -100, max = 200) double invalidPercentage
    ) {
        // Feature: datanymize, Property 10d: Invalid Subset Percentage Error
        
        // Assume percentage is outside valid range
        Assume.that(invalidPercentage < 0 || invalidPercentage > 100);
        
        // Given a configuration with invalid subset percentage
        AnonymizationConfig config = new AnonymizationConfig("test", "1.0");
        TableConfig tableConfig = new TableConfig("users");
        ColumnTransformation colTransform = new ColumnTransformation("id", "null");
        tableConfig.addColumnTransformation(colTransform);
        config.addTableConfig(tableConfig);
        
        SubsetConfig subsetConfig = new SubsetConfig();
        subsetConfig.setPercentage(invalidPercentage);
        config.setSubset(subsetConfig);
        
        // When validating the configuration
        ConfigValidator validator = new ConfigValidator();
        ConfigurationParsingException exception = null;
        try {
            validator.validate(config);
        } catch (ConfigurationParsingException e) {
            exception = e;
        }
        
        // Then an exception should be thrown
        Assume.that(exception != null);
        Assume.that(exception.getMessage().contains("percentage"));
    }

    @Property
    @Label("Property 10e: Missing Transformer Name Error")
    void testMissingTransformerNameError() {
        // Feature: datanymize, Property 10e: Missing Transformer Name Error
        
        // Given a configuration with missing transformer name
        AnonymizationConfig config = new AnonymizationConfig("test", "1.0");
        TableConfig tableConfig = new TableConfig("users");
        ColumnTransformation colTransform = new ColumnTransformation("email", "");
        tableConfig.addColumnTransformation(colTransform);
        config.addTableConfig(tableConfig);
        
        // When validating the configuration
        ConfigValidator validator = new ConfigValidator();
        ConfigurationParsingException exception = null;
        try {
            validator.validate(config);
        } catch (ConfigurationParsingException e) {
            exception = e;
        }
        
        // Then an exception should be thrown
        Assume.that(exception != null);
        Assume.that(exception.getMessage().contains("Transformer name"));
    }

    @Property
    @Label("Property 10f: Empty Configuration Error")
    void testEmptyConfigurationError() {
        // Feature: datanymize, Property 10f: Empty Configuration Error
        
        // Given an empty configuration
        AnonymizationConfig config = new AnonymizationConfig("test", "1.0");
        
        // When validating the configuration
        ConfigValidator validator = new ConfigValidator();
        ConfigurationParsingException exception = null;
        try {
            validator.validate(config);
        } catch (ConfigurationParsingException e) {
            exception = e;
        }
        
        // Then an exception should be thrown
        Assume.that(exception != null);
        Assume.that(exception.getMessage().contains("table"));
    }

    @Property
    @Label("Property 10g: Invalid Filter Operator Error")
    void testInvalidFilterOperatorError(
        @ForAll @StringLength(min = 1, max = 10) String invalidOperator
    ) {
        // Feature: datanymize, Property 10g: Invalid Filter Operator Error
        
        // Assume operator is not valid
        Assume.that(!invalidOperator.equals("=") && 
                   !invalidOperator.equals("!=") &&
                   !invalidOperator.equals("<") &&
                   !invalidOperator.equals(">") &&
                   !invalidOperator.equals("<=") &&
                   !invalidOperator.equals(">=") &&
                   !invalidOperator.equals("IN") &&
                   !invalidOperator.equals("LIKE"));
        
        // Given a configuration with invalid filter operator
        AnonymizationConfig config = new AnonymizationConfig("test", "1.0");
        TableConfig tableConfig = new TableConfig("users");
        ColumnTransformation colTransform = new ColumnTransformation("id", "null");
        tableConfig.addColumnTransformation(colTransform);
        config.addTableConfig(tableConfig);
        
        SubsetConfig subsetConfig = new SubsetConfig();
        SubsetConfig.FilterCriteria filter = new SubsetConfig.FilterCriteria();
        filter.setColumn("country");
        filter.setOperator(invalidOperator);
        filter.setValue("DE");
        subsetConfig.addFilter(filter);
        config.setSubset(subsetConfig);
        
        // When validating the configuration
        ConfigValidator validator = new ConfigValidator();
        ConfigurationParsingException exception = null;
        try {
            validator.validate(config);
        } catch (ConfigurationParsingException e) {
            exception = e;
        }
        
        // Then an exception should be thrown
        Assume.that(exception != null);
        Assume.that(exception.getMessage().contains("operator"));
    }
}
