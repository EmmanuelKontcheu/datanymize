package com.datanymize.config.versioning;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents differences between two configuration versions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigDiff {
    private int version1;
    private int version2;
    private List<String> addedTables;
    private List<String> removedTables;
    private List<String> modifiedTables;
    private List<String> addedTransformers;
    private List<String> removedTransformers;
    private List<String> modifiedTransformers;
    private List<String> otherChanges;

    public ConfigDiff(int version1, int version2) {
        this.version1 = version1;
        this.version2 = version2;
        this.addedTables = new ArrayList<>();
        this.removedTables = new ArrayList<>();
        this.modifiedTables = new ArrayList<>();
        this.addedTransformers = new ArrayList<>();
        this.removedTransformers = new ArrayList<>();
        this.modifiedTransformers = new ArrayList<>();
        this.otherChanges = new ArrayList<>();
    }

    public boolean hasChanges() {
        return !addedTables.isEmpty() || !removedTables.isEmpty() || !modifiedTables.isEmpty() ||
               !addedTransformers.isEmpty() || !removedTransformers.isEmpty() || !modifiedTransformers.isEmpty() ||
               !otherChanges.isEmpty();
    }
}
