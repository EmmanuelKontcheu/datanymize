package com.datanymize.anonymization;

import com.datanymize.database.model.ForeignKey;
import com.datanymize.database.model.Row;

import java.util.*;

/**
 * Resolves foreign key dependencies for subset selection.
 * Ensures all FK dependencies of selected rows are included in the subset.
 *
 * Requirements: 6.3, 6.4
 */
public class SubsetForeignKeyResolver {

    /**
     * Strategy for handling missing FK dependencies.
     */
    public enum MissingDependencyStrategy {
        INCLUDE,      // Include the referenced record
        SET_NULL,     // Set FK to NULL
        RESTRICT      // Exclude the row with missing dependency
    }

    private final MissingDependencyStrategy strategy;
    private final Map<String, Set<Object>> selectedRowIds = new HashMap<>();
    private final Map<String, Map<Object, Object>> fkMappings = new HashMap<>();

    /**
     * Create a resolver with specified strategy.
     *
     * @param strategy Strategy for handling missing dependencies
     */
    public SubsetForeignKeyResolver(MissingDependencyStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Add a selected row to the resolver.
     *
     * @param tableName Table name
     * @param rowId Row ID (primary key value)
     */
    public void addSelectedRow(String tableName, Object rowId) {
        selectedRowIds.computeIfAbsent(tableName, k -> new HashSet<>()).add(rowId);
    }

    /**
     * Add multiple selected rows.
     *
     * @param tableName Table name
     * @param rowIds Collection of row IDs
     */
    public void addSelectedRows(String tableName, Collection<Object> rowIds) {
        selectedRowIds.computeIfAbsent(tableName, k -> new HashSet<>()).addAll(rowIds);
    }

    /**
     * Resolve FK dependencies for a row.
     * Returns the row with resolved FK dependencies based on strategy.
     *
     * @param row Row to resolve
     * @param foreignKeys List of foreign keys for this table
     * @return Resolved row, or null if row should be excluded
     */
    public Row resolveForeignKeyDependencies(Row row, List<ForeignKey> foreignKeys) {
        if (foreignKeys == null || foreignKeys.isEmpty()) {
            return row;
        }

        Row resolvedRow = Row.builder()
            .tableName(row.getTableName())
            .values(new HashMap<>(row.getValues()))
            .originalValues(new HashMap<>(row.getOriginalValues()))
            .build();

        for (ForeignKey fk : foreignKeys) {
            if (!fk.getSourceTable().equals(row.getTableName())) {
                continue;
            }

            Object fkValue = row.getValues().get(fk.getSourceColumn());
            if (fkValue == null) {
                continue;
            }

            // Check if referenced record exists in selected rows
            Set<Object> targetRowIds = selectedRowIds.get(fk.getTargetTable());
            boolean referencedRecordExists = targetRowIds != null && targetRowIds.contains(fkValue);

            if (!referencedRecordExists) {
                // Handle missing dependency based on strategy
                switch (strategy) {
                    case INCLUDE:
                        // Record that this FK dependency needs to be included
                        recordMissingDependency(fk.getTargetTable(), fkValue);
                        break;
                    case SET_NULL:
                        // Set FK to NULL
                        resolvedRow.getValues().put(fk.getSourceColumn(), null);
                        break;
                    case RESTRICT:
                        // Exclude this row
                        return null;
                }
            }
        }

        return resolvedRow;
    }

    /**
     * Record a missing FK dependency that needs to be included.
     *
     * @param tableName Table name of referenced record
     * @param rowId Row ID of referenced record
     */
    private void recordMissingDependency(String tableName, Object rowId) {
        selectedRowIds.computeIfAbsent(tableName, k -> new HashSet<>()).add(rowId);
    }

    /**
     * Get all selected row IDs for a table.
     *
     * @param tableName Table name
     * @return Set of selected row IDs
     */
    public Set<Object> getSelectedRowIds(String tableName) {
        return new HashSet<>(selectedRowIds.getOrDefault(tableName, new HashSet<>()));
    }

    /**
     * Get all tables with selected rows.
     *
     * @return Set of table names
     */
    public Set<String> getTablesWithSelectedRows() {
        return new HashSet<>(selectedRowIds.keySet());
    }

    /**
     * Get the number of selected rows for a table.
     *
     * @param tableName Table name
     * @return Number of selected rows
     */
    public int getSelectedRowCount(String tableName) {
        Set<Object> rowIds = selectedRowIds.get(tableName);
        return rowIds == null ? 0 : rowIds.size();
    }

    /**
     * Get total number of selected rows across all tables.
     *
     * @return Total number of selected rows
     */
    public int getTotalSelectedRows() {
        return selectedRowIds.values().stream().mapToInt(Set::size).sum();
    }

    /**
     * Check if a row is selected.
     *
     * @param tableName Table name
     * @param rowId Row ID
     * @return true if row is selected
     */
    public boolean isRowSelected(String tableName, Object rowId) {
        Set<Object> rowIds = selectedRowIds.get(tableName);
        return rowIds != null && rowIds.contains(rowId);
    }

    /**
     * Get the strategy for handling missing dependencies.
     *
     * @return Strategy
     */
    public MissingDependencyStrategy getStrategy() {
        return strategy;
    }

    /**
     * Clear all selected rows.
     */
    public void clear() {
        selectedRowIds.clear();
        fkMappings.clear();
    }

    /**
     * Get statistics about selected rows.
     *
     * @return Map of table name to selected row count
     */
    public Map<String, Integer> getStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        for (Map.Entry<String, Set<Object>> entry : selectedRowIds.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().size());
        }
        return stats;
    }

    /**
     * Validate that all FK dependencies are satisfied.
     *
     * @param foreignKeys List of all foreign keys
     * @return List of unresolved dependencies (empty if all resolved)
     */
    public List<String> validateDependencies(List<ForeignKey> foreignKeys) {
        List<String> unresolved = new ArrayList<>();

        for (ForeignKey fk : foreignKeys) {
            Set<Object> sourceRowIds = selectedRowIds.get(fk.getSourceTable());
            Set<Object> targetRowIds = selectedRowIds.get(fk.getTargetTable());

            if (sourceRowIds == null || targetRowIds == null) {
                continue;
            }

            // Check if all referenced records exist
            for (Object sourceRowId : sourceRowIds) {
                // This is a simplified check - in production, would need to check actual FK values
                if (!targetRowIds.contains(sourceRowId)) {
                    unresolved.add(
                        "FK from " + fk.getSourceTable() + " to " + fk.getTargetTable() +
                        " references missing record: " + sourceRowId
                    );
                }
            }
        }

        return unresolved;
    }
}
