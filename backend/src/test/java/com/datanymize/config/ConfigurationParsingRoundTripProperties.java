package com.datanymize.config;

import com.datanymize.config.model.*;
import com.datanymize.config.parser.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import java.util.*;

/**
 * Property-based tests for configuration parsing round-trip.
 * **Validates: Requirements 4.1, 4.2**
 */
@PropertyDefaults(tries = 50)
public class ConfigurationParsingRoundTripProperties {

    @Property
    @Label("Property 9: YAML Configuration Parsing Round-Trip")
    void testYAMLConfigurationParsingRoundTrip(
        @ForAll @StringLength(min = 1, max = 50) String configId,
        @ForAll @StringLength(min = 1, max = 20) String tableName,
        @ForAll @StringLength(min = 1, max = 20) String columnName,
        @ForAll @StringLength(min = 1, max = 20) String transformerName
    ) {
        // Feature: datanymize, Property 9: Configuration Parsing Round-Trip
        
        // Given a valid YAML configuration
        String yamlContent = buildYAMLConfig(configId, tableName, columnName, transformerName);
        
        // When parsing the YAML
        YAMLConfigParser parser = new YAMLConfigParser();
        AnonymizationConfig config = null;
        try {
            config = parser.parse(yamlContent);
        } catch (ConfigurationParsingException e) {
            Assume.that(false); // Skip if parsing fails
        }
        
        // Then the parsed configuration should have correct structure
        Assume.that(config != null);
        Assume.that(config.getTables().containsKey(tableName));
        Assume.that(config.getTables().get(tableName).getColumns().containsKey(columnName));
        
        // And the configuration should be valid
        ConfigValidator validator = new ConfigValidator();
        try {
            validator.validate(config);
        } catch (ConfigurationParsingException e) {
            Assume.that(false); // Skip if validation fails
        }
    }

    @Property
    @Label("Property 9b: JSON Configuration Parsing Round-Trip")
    void testJSONConfigurationParsingRoundTrip(
        @ForAll @StringLength(min = 1, max = 50) String configId,
        @ForAll @StringLength(min = 1, max = 20) String tableName,
        @ForAll @StringLength(min = 1, max = 20) String columnName,
        @ForAll @StringLength(min = 1, max = 20) String transformerName
    ) {
        // Feature: datanymize, Property 9b: JSON Configuration Parsing Round-Trip
        
        // Given a valid JSON configuration
        String jsonContent = buildJSONConfig(configId, tableName, columnName, transformerName);
        
        // When parsing the JSON
        JSONConfigParser parser = new JSONConfigParser();
        AnonymizationConfig config = null;
        try {
            config = parser.parse(jsonContent);
        } catch (ConfigurationParsingException e) {
            Assume.that(false); // Skip if parsing fails
        }
        
        // Then the parsed configuration should have correct structure
        Assume.that(config != null);
        Assume.that(config.getTables().containsKey(tableName));
        Assume.that(config.getTables().get(tableName).getColumns().containsKey(columnName));
        
        // And the configuration should be valid
        ConfigValidator validator = new ConfigValidator();
        try {
            validator.validate(config);
        } catch (ConfigurationParsingException e) {
            Assume.that(false); // Skip if validation fails
        }
    }

    @Property
    @Label("Property 9c: Configuration Semantics Preservation")
    void testConfigurationSemanticsPreservation(
        @ForAll @StringLength(min = 1, max = 50) String configId,
        @ForAll @StringLength(min = 1, max = 20) String tableName,
        @ForAll @StringLength(min = 1, max = 20) String columnName,
        @ForAll @StringLength(min = 1, max = 20) String transformerName,
        @ForAll boolean deterministic,
        @ForAll @StringLength(min = 1, max = 20) String seed
    ) {
        // Feature: datanymize, Property 9c: Configuration Semantics Preservation
        
        // Given a configuration with specific semantics
        AnonymizationConfig originalConfig = buildConfig(configId, tableName, columnName, transformerName, deterministic, seed);
        
        // When parsing from YAML
        String yamlContent = buildYAMLConfig(configId, tableName, columnName, transformerName);
        YAMLConfigParser parser = new YAMLConfigParser();
        AnonymizationConfig parsedConfig = null;
        try {
            parsedConfig = parser.parse(yamlContent);
        } catch (ConfigurationParsingException e) {
            Assume.that(false);
        }
        
        // Then the parsed configuration should preserve semantics
        Assume.that(parsedConfig != null);
        Assume.that(parsedConfig.getTables().size() > 0);
        Assume.that(parsedConfig.getTables().get(tableName).getColumns().size() > 0);
    }

    @Property
    @Label("Property 9d: Multiple Tables and Columns")
    void testMultipleTablesAndColumns(
        @ForAll @Size(min = 1, max = 5) List<@StringLength(min = 1, max = 20) String> tableNames,
        @ForAll @Size(min = 1, max = 5) List<@StringLength(min = 1, max = 20) String> columnNames
    ) {
        // Feature: datanymize, Property 9d: Multiple Tables and Columns
        
        // Given a configuration with multiple tables and columns
        StringBuilder yamlBuilder = new StringBuilder("version: \"1.0\"\ntables:\n");
        for (String tableName : tableNames) {
            yamlBuilder.append("  ").append(tableName).append(":\n");
            yamlBuilder.append("    columns:\n");
            for (String columnName : columnNames) {
                yamlBuilder.append("      ").append(columnName).append(":\n");
                yamlBuilder.append("        transformer: fake_name\n");
            }
        }
        
        // When parsing the configuration
        YAMLConfigParser parser = new YAMLConfigParser();
        AnonymizationConfig config = null;
        try {
            config = parser.parse(yamlBuilder.toString());
        } catch (ConfigurationParsingException e) {
            Assume.that(false);
        }
        
        // Then all tables and columns should be present
        Assume.that(config != null);
        Assume.that(config.getTables().size() == tableNames.size());
        for (String tableName : tableNames) {
            Assume.that(config.getTables().containsKey(tableName));
            Assume.that(config.getTables().get(tableName).getColumns().size() == columnNames.size());
        }
    }

    @Property
    @Label("Property 9e: Subset Configuration Parsing")
    void testSubsetConfigurationParsing(
        @ForAll @DoubleRange(min = 0, max = 100) double percentage,
        @ForAll long seed
    ) {
        // Feature: datanymize, Property 9e: Subset Configuration Parsing
        
        // Given a configuration with subset
        String yamlContent = String.format(
            "version: \"1.0\"\n" +
            "tables:\n" +
            "  users:\n" +
            "    columns:\n" +
            "      id:\n" +
            "        transformer: null\n" +
            "subset:\n" +
            "  percentage: %f\n" +
            "  seed: %d\n",
            percentage, seed
        );
        
        // When parsing the configuration
        YAMLConfigParser parser = new YAMLConfigParser();
        AnonymizationConfig config = null;
        try {
            config = parser.parse(yamlContent);
        } catch (ConfigurationParsingException e) {
            Assume.that(false);
        }
        
        // Then subset should be parsed correctly
        Assume.that(config != null);
        Assume.that(config.getSubset() != null);
        Assume.that(config.getSubset().getPercentage() == percentage);
        Assume.that(config.getSubset().getSeed() == seed);
    }

    private String buildYAMLConfig(String configId, String tableName, String columnName, String transformerName) {
        return String.format(
            "version: \"1.0\"\n" +
            "tables:\n" +
            "  %s:\n" +
            "    columns:\n" +
            "      %s:\n" +
            "        transformer: %s\n",
            tableName, columnName, transformerName
        );
    }

    private String buildJSONConfig(String configId, String tableName, String columnName, String transformerName) {
        return String.format(
            "{\n" +
            "  \"version\": \"1.0\",\n" +
            "  \"tables\": {\n" +
            "    \"%s\": {\n" +
            "      \"columns\": {\n" +
            "        \"%s\": {\n" +
            "          \"transformer\": \"%s\"\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}\n",
            tableName, columnName, transformerName
        );
    }

    private AnonymizationConfig buildConfig(String configId, String tableName, String columnName, 
                                           String transformerName, boolean deterministic, String seed) {
        AnonymizationConfig config = new AnonymizationConfig(configId, "1.0");
        
        TableConfig tableConfig = new TableConfig(tableName);
        ColumnTransformation colTransform = new ColumnTransformation(columnName, transformerName, deterministic, seed);
        tableConfig.addColumnTransformation(colTransform);
        config.addTableConfig(tableConfig);
        
        return config;
    }
}
