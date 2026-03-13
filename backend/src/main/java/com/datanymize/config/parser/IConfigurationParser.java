package com.datanymize.config.parser;

import com.datanymize.config.model.AnonymizationConfig;

/**
 * Interface for parsing anonymization configurations.
 */
public interface IConfigurationParser {
    /**
     * Parse configuration from string content.
     * @param content Configuration content
     * @return Parsed AnonymizationConfig
     * @throws ConfigurationParsingException if parsing fails
     */
    AnonymizationConfig parse(String content) throws ConfigurationParsingException;

    /**
     * Get the format this parser handles.
     * @return Format name (e.g., "YAML", "JSON")
     */
    String getFormat();
}
