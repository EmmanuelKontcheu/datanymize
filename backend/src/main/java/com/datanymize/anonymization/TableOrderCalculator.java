package com.datanymize.anonymization;

import com.datanymize.database.model.DatabaseSchema;
import com.datanymize.database.model.ForeignKey;
import com.datanymize.database.model.Table;

import java.util.*;

/**
 * Calculates the correct order to process tables during anonymization.
 * Respects foreign key constraints using topological sort.
 */
public class TableOrderCalculator {

    /**
     * Calculate table processing order.
     * @param schema Database schema
     * @return List of table names in processing order
     * @throws IllegalArgumentException if circular dependencies detected
     */
    public List<String> calculateTableOrder(DatabaseSchema schema) throws IllegalArgumentException {
        // Build dependency graph
        Map<String, Set<String>> dependencies = new HashMap<>();
        Map<String, Table> tableMap = new HashMap<>();

        // Initialize all tables
        for (Table table : schema.getTables()) {
            dependencies.put(table.getName(), new HashSet<>());
            tableMap.put(table.getName(), table);
        }

        // Add foreign key dependencies
        for (ForeignKey fk : schema.getForeignKeys()) {
            String sourceTable = fk.getSourceTable();
            String targetTable = fk.getTargetTable();

            if (dependencies.containsKey(sourceTable) && dependencies.containsKey(targetTable)) {
                // Source table depends on target table (target must be processed first)
                dependencies.get(sourceTable).add(targetTable);
            }
        }

        // Perform topological sort
        return topologicalSort(dependencies);
    }

    /**
     * Perform topological sort on dependency graph.
     * @param dependencies Map of table name to set of dependencies
     * @return Sorted list of table names
     * @throws IllegalArgumentException if circular dependencies detected
     */
    private List<String> topologicalSort(Map<String, Set<String>> dependencies) throws IllegalArgumentException {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, Set<String>> graph = new HashMap<>();

        // Initialize in-degree and graph
        for (String table : dependencies.keySet()) {
            inDegree.put(table, 0);
            graph.put(table, new HashSet<>(dependencies.get(table)));
        }

        // Calculate in-degrees
        for (String table : dependencies.keySet()) {
            for (String dependency : dependencies.get(table)) {
                inDegree.put(dependency, inDegree.getOrDefault(dependency, 0) + 1);
            }
        }

        // Find all nodes with in-degree 0
        Queue<String> queue = new LinkedList<>();
        for (String table : inDegree.keySet()) {
            if (inDegree.get(table) == 0) {
                queue.add(table);
            }
        }

        List<String> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String table = queue.poll();
            result.add(table);

            // Process all tables that depend on this table
            for (String dependent : dependencies.keySet()) {
                if (dependencies.get(dependent).contains(table)) {
                    inDegree.put(dependent, inDegree.get(dependent) - 1);
                    if (inDegree.get(dependent) == 0) {
                        queue.add(dependent);
                    }
                }
            }
        }

        // Check for cycles
        if (result.size() != dependencies.size()) {
            throw new IllegalArgumentException("Circular foreign key dependencies detected");
        }

        return result;
    }

    /**
     * Check if there are circular dependencies.
     * @param schema Database schema
     * @return true if circular dependencies exist
     */
    public boolean hasCircularDependencies(DatabaseSchema schema) {
        try {
            calculateTableOrder(schema);
            return false;
        } catch (IllegalArgumentException e) {
            return e.getMessage().contains("Circular");
        }
    }
}
