package com.datanymize.anonymization;

import com.datanymize.database.model.IDatabaseConnection;

import java.util.*;

/**
 * Manages transactions for anonymization with savepoint support.
 */
public class TransactionManager {
    private final IDatabaseConnection connection;
    private final Stack<String> savepoints;
    private boolean inTransaction;

    public TransactionManager(IDatabaseConnection connection) {
        this.connection = connection;
        this.savepoints = new Stack<>();
        this.inTransaction = false;
    }

    /**
     * Begin a transaction.
     */
    public void beginTransaction() {
        if (!inTransaction) {
            connection.beginTransaction();
            inTransaction = true;
        }
    }

    /**
     * Commit the current transaction.
     */
    public void commit() {
        if (inTransaction) {
            connection.commit();
            inTransaction = false;
            savepoints.clear();
        }
    }

    /**
     * Rollback the current transaction.
     */
    public void rollback() {
        if (inTransaction) {
            connection.rollback();
            inTransaction = false;
            savepoints.clear();
        }
    }

    /**
     * Create a savepoint.
     * @param name Savepoint name
     */
    public void createSavepoint(String name) {
        if (inTransaction) {
            savepoints.push(name);
            // Note: Actual savepoint creation would be database-specific
        }
    }

    /**
     * Rollback to a savepoint.
     * @param name Savepoint name
     */
    public void rollbackToSavepoint(String name) {
        if (inTransaction && !savepoints.isEmpty() && savepoints.peek().equals(name)) {
            savepoints.pop();
            // Note: Actual savepoint rollback would be database-specific
        }
    }

    /**
     * Check if in transaction.
     * @return true if transaction is active
     */
    public boolean isInTransaction() {
        return inTransaction;
    }

    /**
     * Get current savepoint stack.
     * @return Stack of savepoint names
     */
    public Stack<String> getSavepoints() {
        return new Stack<>(savepoints);
    }
}
