package com.datanymize.config.versioning;

import com.datanymize.config.model.AnonymizationConfig;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Manages configuration versioning.
 * Tracks configuration changes and allows restoration to previous versions.
 */
@Service
public class ConfigVersionManager {
    private final Map<String, List<ConfigVersion>> configVersions = new HashMap<>();
    private final Map<String, Integer> versionCounters = new HashMap<>();

    /**
     * Create a new version of a configuration.
     * @param configId Configuration ID
     * @param config Configuration to version
     * @param createdBy User who created the version
     * @return Created ConfigVersion
     */
    public ConfigVersion createVersion(String configId, AnonymizationConfig config, String createdBy) {
        int versionNumber = versionCounters.getOrDefault(configId, 0) + 1;
        versionCounters.put(configId, versionNumber);

        ConfigVersion version = new ConfigVersion(configId, versionNumber, config, createdBy);
        configVersions.computeIfAbsent(configId, k -> new ArrayList<>()).add(version);

        return version;
    }

    /**
     * Get a specific version of a configuration.
     * @param configId Configuration ID
     * @param versionNumber Version number
     * @return ConfigVersion or null if not found
     */
    public ConfigVersion getVersion(String configId, int versionNumber) {
        List<ConfigVersion> versions = configVersions.get(configId);
        if (versions == null) {
            return null;
        }

        for (ConfigVersion version : versions) {
            if (version.getVersionNumber() == versionNumber) {
                return version;
            }
        }

        return null;
    }

    /**
     * Get all versions of a configuration.
     * @param configId Configuration ID
     * @return List of ConfigVersions
     */
    public List<ConfigVersion> getVersionHistory(String configId) {
        return new ArrayList<>(configVersions.getOrDefault(configId, new ArrayList<>()));
    }

    /**
     * Get the latest version of a configuration.
     * @param configId Configuration ID
     * @return Latest ConfigVersion or null if no versions exist
     */
    public ConfigVersion getLatestVersion(String configId) {
        List<ConfigVersion> versions = configVersions.get(configId);
        if (versions == null || versions.isEmpty()) {
            return null;
        }

        return versions.get(versions.size() - 1);
    }

    /**
     * Compare two versions of a configuration.
     * @param configId Configuration ID
     * @param version1 First version number
     * @param version2 Second version number
     * @return ConfigDiff
     */
    public ConfigDiff compareVersions(String configId, int version1, int version2) {
        ConfigVersion v1 = getVersion(configId, version1);
        ConfigVersion v2 = getVersion(configId, version2);

        if (v1 == null || v2 == null) {
            throw new IllegalArgumentException("One or both versions not found");
        }

        ConfigDiff diff = new ConfigDiff(version1, version2);

        // Compare tables
        Set<String> tables1 = v1.getConfig().getTables().keySet();
        Set<String> tables2 = v2.getConfig().getTables().keySet();

        for (String table : tables2) {
            if (!tables1.contains(table)) {
                diff.getAddedTables().add(table);
            }
        }

        for (String table : tables1) {
            if (!tables2.contains(table)) {
                diff.getRemovedTables().add(table);
            }
        }

        for (String table : tables1) {
            if (tables2.contains(table) && !v1.getConfig().getTables().get(table).equals(v2.getConfig().getTables().get(table))) {
                diff.getModifiedTables().add(table);
            }
        }

        // Compare transformers
        Set<String> transformers1 = v1.getConfig().getTransformers().keySet();
        Set<String> transformers2 = v2.getConfig().getTransformers().keySet();

        for (String transformer : transformers2) {
            if (!transformers1.contains(transformer)) {
                diff.getAddedTransformers().add(transformer);
            }
        }

        for (String transformer : transformers1) {
            if (!transformers2.contains(transformer)) {
                diff.getRemovedTransformers().add(transformer);
            }
        }

        for (String transformer : transformers1) {
            if (transformers2.contains(transformer) && !v1.getConfig().getTransformers().get(transformer).equals(v2.getConfig().getTransformers().get(transformer))) {
                diff.getModifiedTransformers().add(transformer);
            }
        }

        return diff;
    }

    /**
     * Delete all versions of a configuration.
     * @param configId Configuration ID
     */
    public void deleteConfigVersions(String configId) {
        configVersions.remove(configId);
        versionCounters.remove(configId);
    }

    /**
     * Get version count for a configuration.
     * @param configId Configuration ID
     * @return Number of versions
     */
    public int getVersionCount(String configId) {
        List<ConfigVersion> versions = configVersions.get(configId);
        return versions == null ? 0 : versions.size();
    }

    /**
     * Save a configuration (creates a new version).
     * @param configId Configuration ID
     * @param config Configuration to save
     */
    public void saveConfiguration(String configId, AnonymizationConfig config) {
        createVersion(configId, config, "system");
    }

    /**
     * Get the latest configuration.
     * @param configId Configuration ID
     * @return Latest AnonymizationConfig or null if not found
     */
    public AnonymizationConfig getLatestConfiguration(String configId) {
        ConfigVersion latest = getLatestVersion(configId);
        return latest != null ? latest.getConfig() : null;
    }

    /**
     * Get the latest version number.
     * @param configId Configuration ID
     * @return Latest version number or 0 if no versions exist
     */
    public int getLatestVersionNumber(String configId) {
        ConfigVersion latest = getLatestVersion(configId);
        return latest != null ? latest.getVersionNumber() : 0;
    }

    /**
     * Get configuration history (alias for getVersionHistory).
     * @param configId Configuration ID
     * @return List of ConfigVersions
     */
    public List<ConfigVersion> getConfigurationHistory(String configId) {
        return getVersionHistory(configId);
    }

    /**
     * Restore a configuration to a previous version.
     * @param configId Configuration ID
     * @param versionNumber Version number to restore
     * @return Restored AnonymizationConfig or null if version not found
     */
    public AnonymizationConfig restoreVersion(String configId, int versionNumber) {
        ConfigVersion version = getVersion(configId, versionNumber);
        if (version == null) {
            return null;
        }
        
        // Create a new version with the restored config
        AnonymizationConfig restoredConfig = new AnonymizationConfig();
        restoredConfig.setId(version.getConfig().getId());
        restoredConfig.setTables(new HashMap<>(version.getConfig().getTables()));
        restoredConfig.setTransformers(new HashMap<>(version.getConfig().getTransformers()));
        restoredConfig.setSubset(version.getConfig().getSubset());
        restoredConfig.setReferentialIntegrity(version.getConfig().getReferentialIntegrity());
        
        createVersion(configId, restoredConfig, "system");
        return restoredConfig;
    }
}
