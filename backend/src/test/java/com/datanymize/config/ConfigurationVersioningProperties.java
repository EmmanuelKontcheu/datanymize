package com.datanymize.config;

import com.datanymize.config.model.*;
import com.datanymize.config.versioning.ConfigDiff;
import com.datanymize.config.versioning.ConfigVersion;
import com.datanymize.config.versioning.ConfigVersionManager;
import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for configuration versioning.
 * **Validates: Requirements 4.7, 20.1, 20.3**
 *
 * Property 13: Configuration Versioning
 * For any configuration modification, the system should create a new version and preserve the ability to restore previous versions.
 */
@PropertyDefaults(tries = 50)
class ConfigurationVersioningProperties {

    /**
     * Property 13a: Creating versions increments version number
     * Each new version should have an incrementing version number.
     */
    @Property
    @Label("Creating versions increments version number")
    void testVersionNumberIncrement(
        @ForAll @StringLength(min = 1, max = 50) String configId,
        @ForAll @StringLength(min = 1, max = 50) String userId
    ) {
        ConfigVersionManager manager = new ConfigVersionManager();

        // Create multiple versions
        AnonymizationConfig config1 = createTestConfig("table1");
        ConfigVersion v1 = manager.createVersion(configId, config1, userId);

        AnonymizationConfig config2 = createTestConfig("table2");
        ConfigVersion v2 = manager.createVersion(configId, config2, userId);

        AnonymizationConfig config3 = createTestConfig("table3");
        ConfigVersion v3 = manager.createVersion(configId, config3, userId);

        // Version numbers should increment
        assertEquals(1, v1.getVersionNumber(), "First version should be 1");
        assertEquals(2, v2.getVersionNumber(), "Second version should be 2");
        assertEquals(3, v3.getVersionNumber(), "Third version should be 3");
    }

    /**
     * Property 13b: Versions are retrievable by version number
     * Each created version should be retrievable by its version number.
     */
    @Property
    @Label("Versions are retrievable by version number")
    void testVersionRetrieval(
        @ForAll @StringLength(min = 1, max = 50) String configId,
        @ForAll @StringLength(min = 1, max = 50) String userId
    ) {
        ConfigVersionManager manager = new ConfigVersionManager();

        AnonymizationConfig config1 = createTestConfig("table1");
        ConfigVersion v1 = manager.createVersion(configId, config1, userId);

        AnonymizationConfig config2 = createTestConfig("table2");
        ConfigVersion v2 = manager.createVersion(configId, config2, userId);

        // Retrieve versions
        ConfigVersion retrieved1 = manager.getVersion(configId, 1);
        ConfigVersion retrieved2 = manager.getVersion(configId, 2);

        assertNotNull(retrieved1, "Version 1 should be retrievable");
        assertNotNull(retrieved2, "Version 2 should be retrievable");
        assertEquals(1, retrieved1.getVersionNumber(), "Retrieved version 1 should have correct number");
        assertEquals(2, retrieved2.getVersionNumber(), "Retrieved version 2 should have correct number");
    }

    /**
     * Property 13c: Version history contains all created versions
     * The version history should contain all versions created for a configuration.
     */
    @Property
    @Label("Version history contains all created versions")
    void testVersionHistory(
        @ForAll @StringLength(min = 1, max = 50) String configId,
        @ForAll @StringLength(min = 1, max = 50) String userId
    ) {
        ConfigVersionManager manager = new ConfigVersionManager();

        // Create 5 versions
        for (int i = 1; i <= 5; i++) {
            AnonymizationConfig config = createTestConfig("table" + i);
            manager.createVersion(configId, config, userId);
        }

        // Get history
        List<ConfigVersion> history = manager.getVersionHistory(configId);

        assertEquals(5, history.size(), "History should contain 5 versions");
        for (int i = 0; i < 5; i++) {
            assertEquals(i + 1, history.get(i).getVersionNumber(), "Version numbers should be sequential");
        }
    }

    /**
     * Property 13d: Latest version is correctly identified
     * The latest version should be the most recently created version.
     */
    @Property
    @Label("Latest version is correctly identified")
    void testLatestVersion(
        @ForAll @StringLength(min = 1, max = 50) String configId,
        @ForAll @StringLength(min = 1, max = 50) String userId
    ) {
        ConfigVersionManager manager = new ConfigVersionManager();

        // Create multiple versions
        for (int i = 1; i <= 3; i++) {
            AnonymizationConfig config = createTestConfig("table" + i);
            manager.createVersion(configId, config, userId);
        }

        // Get latest
        ConfigVersion latest = manager.getLatestVersion(configId);

        assertNotNull(latest, "Latest version should exist");
        assertEquals(3, latest.getVersionNumber(), "Latest version should be version 3");
    }

    /**
     * Property 13e: Configuration data is preserved in versions
     * The configuration data stored in a version should match the original configuration.
     */
    @Property
    @Label("Configuration data is preserved in versions")
    void testConfigurationPreservation(
        @ForAll @StringLength(min = 1, max = 50) String configId,
        @ForAll @StringLength(min = 1, max = 50) String userId,
        @ForAll @StringLength(min = 1, max = 50) String tableName
    ) {
        ConfigVersionManager manager = new ConfigVersionManager();

        AnonymizationConfig originalConfig = createTestConfig(tableName);
        ConfigVersion version = manager.createVersion(configId, originalConfig, userId);

        // Verify configuration is preserved
        AnonymizationConfig retrievedConfig = version.getConfig();
        assertNotNull(retrievedConfig, "Configuration should be preserved");
        assertTrue(
            retrievedConfig.getTables().containsKey(tableName),
            "Configuration should contain the table"
        );
    }

    /**
     * Property 13f: Version comparison detects added tables
     * Comparing versions should detect tables added in the newer version.
     */
    @Property
    @Label("Version comparison detects added tables")
    void testVersionComparisonAddedTables(
        @ForAll @StringLength(min = 1, max = 50) String configId,
        @ForAll @StringLength(min = 1, max = 50) String userId
    ) {
        ConfigVersionManager manager = new ConfigVersionManager();

        // Version 1: table1
        AnonymizationConfig config1 = createTestConfig("table1");
        manager.createVersion(configId, config1, userId);

        // Version 2: table1 + table2
        AnonymizationConfig config2 = createTestConfig("table1");
        config2.getTables().put("table2", createTableConfig("table2"));
        manager.createVersion(configId, config2, userId);

        // Compare versions
        ConfigDiff diff = manager.compareVersions(configId, 1, 2);

        assertTrue(
            diff.getAddedTables().contains("table2"),
            "Diff should show table2 as added"
        );
    }

    /**
     * Property 13g: Version comparison detects removed tables
     * Comparing versions should detect tables removed in the newer version.
     */
    @Property
    @Label("Version comparison detects removed tables")
    void testVersionComparisonRemovedTables(
        @ForAll @StringLength(min = 1, max = 50) String configId,
        @ForAll @StringLength(min = 1, max = 50) String userId
    ) {
        ConfigVersionManager manager = new ConfigVersionManager();

        // Version 1: table1 + table2
        AnonymizationConfig config1 = createTestConfig("table1");
        config1.getTables().put("table2", createTableConfig("table2"));
        manager.createVersion(configId, config1, userId);

        // Version 2: table1 only
        AnonymizationConfig config2 = createTestConfig("table1");
        manager.createVersion(configId, config2, userId);

        // Compare versions
        ConfigDiff diff = manager.compareVersions(configId, 1, 2);

        assertTrue(
            diff.getRemovedTables().contains("table2"),
            "Diff should show table2 as removed"
        );
    }

    /**
     * Property 13h: Version restoration retrieves correct configuration
     * Restoring to a previous version should retrieve the exact configuration from that version.
     */
    @Property
    @Label("Version restoration retrieves correct configuration")
    void testVersionRestoration(
        @ForAll @StringLength(min = 1, max = 50) String configId,
        @ForAll @StringLength(min = 1, max = 50) String userId
    ) {
        ConfigVersionManager manager = new ConfigVersionManager();

        // Create version 1
        AnonymizationConfig config1 = createTestConfig("table1");
        manager.createVersion(configId, config1, userId);

        // Create version 2
        AnonymizationConfig config2 = createTestConfig("table2");
        manager.createVersion(configId, config2, userId);

        // Create version 3
        AnonymizationConfig config3 = createTestConfig("table3");
        manager.createVersion(configId, config3, userId);

        // Restore to version 1
        ConfigVersion restoredVersion = manager.getVersion(configId, 1);
        assertNotNull(restoredVersion, "Version 1 should be retrievable");
        assertTrue(
            restoredVersion.getConfig().getTables().containsKey("table1"),
            "Restored version should have table1"
        );
        assertFalse(
            restoredVersion.getConfig().getTables().containsKey("table2"),
            "Restored version should not have table2"
        );
    }

    /**
     * Property 13i: Multiple configurations have independent version histories
     * Version histories for different configurations should be independent.
     */
    @Property
    @Label("Multiple configurations have independent version histories")
    void testIndependentVersionHistories(
        @ForAll @StringLength(min = 1, max = 50) String configId1,
        @ForAll @StringLength(min = 1, max = 50) String configId2,
        @ForAll @StringLength(min = 1, max = 50) String userId
    ) {
        Assume.that(!configId1.equals(configId2));

        ConfigVersionManager manager = new ConfigVersionManager();

        // Create versions for config 1
        for (int i = 1; i <= 3; i++) {
            AnonymizationConfig config = createTestConfig("table" + i);
            manager.createVersion(configId1, config, userId);
        }

        // Create versions for config 2
        for (int i = 1; i <= 2; i++) {
            AnonymizationConfig config = createTestConfig("table" + i);
            manager.createVersion(configId2, config, userId);
        }

        // Check version counts
        assertEquals(3, manager.getVersionCount(configId1), "Config 1 should have 3 versions");
        assertEquals(2, manager.getVersionCount(configId2), "Config 2 should have 2 versions");
    }

    /**
     * Property 13j: Version metadata is preserved
     * Version metadata (timestamp, creator) should be preserved.
     */
    @Property
    @Label("Version metadata is preserved")
    void testVersionMetadata(
        @ForAll @StringLength(min = 1, max = 50) String configId,
        @ForAll @StringLength(min = 1, max = 50) String userId
    ) {
        ConfigVersionManager manager = new ConfigVersionManager();

        AnonymizationConfig config = createTestConfig("table1");
        ConfigVersion version = manager.createVersion(configId, config, userId);

        // Check metadata
        assertNotNull(version.getCreatedAt(), "Timestamp should be set");
        assertEquals(userId, version.getCreatedBy(), "Creator should be preserved");
        assertEquals(configId, version.getConfigId(), "Config ID should be preserved");
    }

    // Helper methods

    private AnonymizationConfig createTestConfig(String tableName) {
        AnonymizationConfig config = new AnonymizationConfig();
        config.setTables(new HashMap<>());
        config.setTransformers(new HashMap<>());
        config.getTables().put(tableName, createTableConfig(tableName));
        return config;
    }

    private TableConfig createTableConfig(String tableName) {
        TableConfig tableConfig = new TableConfig();
        tableConfig.setTableName(tableName);
        tableConfig.setColumns(new HashMap<>());
        tableConfig.setPrimaryKeys(new ArrayList<>());
        return tableConfig;
    }
}
