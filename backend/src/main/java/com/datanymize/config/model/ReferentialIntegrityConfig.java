package com.datanymize.config.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for referential integrity handling during anonymization.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferentialIntegrityConfig {
    private String missingForeignKeyStrategy;  // INCLUDE, SET_NULL, RESTRICT
    private boolean validateAfterAnonymization; // Validate FK integrity after anonymization
    private boolean preserveConstraints;        // Preserve FK constraints

    public ReferentialIntegrityConfig() {
        this.missingForeignKeyStrategy = "INCLUDE";
        this.validateAfterAnonymization = true;
        this.preserveConstraints = true;
    }
}
