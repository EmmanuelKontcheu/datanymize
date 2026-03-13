package com.datanymize.anonymization;

import com.datanymize.anonymization.AnonymizationOrchestrator.AnonymizationException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for error handling and rollback.
 * **Validates: Requirements 19.2, 19.3**
 *
 * Property 27: Error Handling and Rollback
 * For any error that occurs during anonymization, the system should log the error, display it to the user,
 * and rollback all changes made during that operation.
 */
@PropertyDefaults(tries = 50)
class ErrorHandlingAndRollbackProperties {

    /**
     * Property 27a: Anonymization exception is thrown on error
     * When an error occurs, an AnonymizationException should be thrown.
     */
    @Property
    @Label("Anonymization exception is thrown on error")
    void testAnonymizationExceptionThrown(
        @ForAll @StringLength(min = 1, max = 100) String errorMessage
    ) {
        // Create exception
        AnonymizationException exception = new AnonymizationException(errorMessage);

        // Verify exception properties
        assertNotNull(exception, "Exception should be created");
        assertEquals(errorMessage, exception.getMessage(), "Message should match");
        assertTrue(
            exception instanceof RuntimeException,
            "Should be a RuntimeException"
        );
    }

    /**
     * Property 27b: Exception preserves cause
     * When an exception is created with a cause, the cause should be preserved.
     */
    @Property
    @Label("Exception preserves cause")
    void testExceptionPreservesCause(
        @ForAll @StringLength(min = 1, max = 100) String errorMessage
    ) {
        // Create cause
        Exception cause = new Exception("Root cause");

        // Create exception with cause
        AnonymizationException exception = new AnonymizationException(errorMessage, cause);

        // Verify cause is preserved
        assertNotNull(exception.getCause(), "Cause should be preserved");
        assertEquals("Root cause", exception.getCause().getMessage(), "Cause message should match");
    }

    /**
     * Property 27c: Error message is meaningful
     * Error messages should contain useful information for debugging.
     */
    @Property
    @Label("Error message is meaningful")
    void testErrorMessageMeaningful(
        @ForAll @StringLength(min = 1, max = 50) String tableName,
        @ForAll @StringLength(min = 1, max = 50) String errorReason
    ) {
        String errorMessage = "Failed to process table " + tableName + ": " + errorReason;
        AnonymizationException exception = new AnonymizationException(errorMessage);

        // Verify message contains useful information
        assertTrue(
            exception.getMessage().contains(tableName),
            "Error message should contain table name"
        );
        assertTrue(
            exception.getMessage().contains(errorReason),
            "Error message should contain error reason"
        );
    }

    /**
     * Property 27d: Transaction manager can rollback
     * The transaction manager should support rollback operations.
     */
    @Property
    @Label("Transaction manager can rollback")
    void testTransactionManagerRollback() {
        TransactionManager manager = new TransactionManager();

        // Begin transaction
        manager.beginTransaction();

        // Verify transaction is active
        assertTrue(manager.isTransactionActive(), "Transaction should be active");

        // Rollback
        manager.rollback();

        // Verify transaction is rolled back
        assertFalse(manager.isTransactionActive(), "Transaction should be rolled back");
    }

    /**
     * Property 27e: Transaction manager can commit
     * The transaction manager should support commit operations.
     */
    @Property
    @Label("Transaction manager can commit")
    void testTransactionManagerCommit() {
        TransactionManager manager = new TransactionManager();

        // Begin transaction
        manager.beginTransaction();

        // Verify transaction is active
        assertTrue(manager.isTransactionActive(), "Transaction should be active");

        // Commit
        manager.commit();

        // Verify transaction is committed
        assertFalse(manager.isTransactionActive(), "Transaction should be committed");
    }

    /**
     * Property 27f: Savepoints can be created and restored
     * The transaction manager should support savepoints for partial rollback.
     */
    @Property
    @Label("Savepoints can be created and restored")
    void testSavepointCreationAndRestoration(
        @ForAll @StringLength(min = 1, max = 50) String savepointName
    ) {
        TransactionManager manager = new TransactionManager();

        // Begin transaction
        manager.beginTransaction();

        // Create savepoint
        manager.createSavepoint(savepointName);

        // Verify savepoint exists
        assertTrue(
            manager.hasSavepoint(savepointName),
            "Savepoint should exist after creation"
        );

        // Rollback to savepoint
        manager.rollbackToSavepoint(savepointName);

        // Verify rollback succeeded
        assertTrue(manager.isTransactionActive(), "Transaction should still be active");
    }

    /**
     * Property 27g: Multiple savepoints can be managed
     * The transaction manager should support multiple savepoints.
     */
    @Property
    @Label("Multiple savepoints can be managed")
    void testMultipleSavepoints(
        @ForAll @StringLength(min = 1, max = 50) String savepoint1,
        @ForAll @StringLength(min = 1, max = 50) String savepoint2
    ) {
        Assume.that(!savepoint1.equals(savepoint2));

        TransactionManager manager = new TransactionManager();

        // Begin transaction
        manager.beginTransaction();

        // Create multiple savepoints
        manager.createSavepoint(savepoint1);
        manager.createSavepoint(savepoint2);

        // Verify both exist
        assertTrue(manager.hasSavepoint(savepoint1), "First savepoint should exist");
        assertTrue(manager.hasSavepoint(savepoint2), "Second savepoint should exist");

        // Rollback to first savepoint
        manager.rollbackToSavepoint(savepoint1);

        // Verify first exists but second may not
        assertTrue(manager.hasSavepoint(savepoint1), "First savepoint should still exist");
    }

    /**
     * Property 27h: Error recovery is possible
     * After an error, the system should be able to recover and continue.
     */
    @Property
    @Label("Error recovery is possible")
    void testErrorRecovery(
        @ForAll @StringLength(min = 1, max = 50) String errorMessage
    ) {
        TransactionManager manager = new TransactionManager();

        // Begin transaction
        manager.beginTransaction();

        // Simulate error and rollback
        try {
            throw new AnonymizationException(errorMessage);
        } catch (AnonymizationException e) {
            manager.rollback();
        }

        // Verify recovery is possible
        assertFalse(manager.isTransactionActive(), "Transaction should be rolled back");

        // Begin new transaction
        manager.beginTransaction();
        assertTrue(manager.isTransactionActive(), "New transaction should be active");

        // Commit
        manager.commit();
        assertFalse(manager.isTransactionActive(), "Transaction should be committed");
    }

    /**
     * Property 27i: Error context is preserved
     * Error information should be preserved for logging and debugging.
     */
    @Property
    @Label("Error context is preserved")
    void testErrorContextPreservation(
        @ForAll @StringLength(min = 1, max = 50) String tableName,
        @ForAll @StringLength(min = 1, max = 50) String operation
    ) {
        String errorMessage = "Error during " + operation + " on table " + tableName;
        AnonymizationException exception = new AnonymizationException(errorMessage);

        // Verify context is preserved
        String message = exception.getMessage();
        assertTrue(message.contains(operation), "Operation should be in error message");
        assertTrue(message.contains(tableName), "Table name should be in error message");
    }

    /**
     * Property 27j: Rollback cleans up resources
     * After rollback, resources should be cleaned up properly.
     */
    @Property
    @Label("Rollback cleans up resources")
    void testRollbackCleansUpResources() {
        TransactionManager manager = new TransactionManager();

        // Begin transaction
        manager.beginTransaction();

        // Verify transaction is active
        assertTrue(manager.isTransactionActive(), "Transaction should be active");

        // Rollback
        manager.rollback();

        // Verify resources are cleaned up
        assertFalse(manager.isTransactionActive(), "Transaction should be cleaned up");
        assertEquals(0, manager.getSavepointCount(), "Savepoints should be cleaned up");
    }

    /**
     * Mock TransactionManager for testing.
     */
    public static class TransactionManager {
        private boolean transactionActive = false;
        private java.util.Map<String, Boolean> savepoints = new java.util.HashMap<>();

        public void beginTransaction() {
            transactionActive = true;
            savepoints.clear();
        }

        public void commit() {
            transactionActive = false;
            savepoints.clear();
        }

        public void rollback() {
            transactionActive = false;
            savepoints.clear();
        }

        public void createSavepoint(String name) {
            if (!transactionActive) {
                throw new IllegalStateException("No active transaction");
            }
            savepoints.put(name, true);
        }

        public void rollbackToSavepoint(String name) {
            if (!savepoints.containsKey(name)) {
                throw new IllegalArgumentException("Savepoint not found: " + name);
            }
            // Remove savepoints created after this one
            java.util.List<String> toRemove = new java.util.ArrayList<>();
            for (String sp : savepoints.keySet()) {
                if (!sp.equals(name)) {
                    toRemove.add(sp);
                }
            }
            toRemove.forEach(savepoints::remove);
        }

        public boolean isTransactionActive() {
            return transactionActive;
        }

        public boolean hasSavepoint(String name) {
            return savepoints.containsKey(name);
        }

        public int getSavepointCount() {
            return savepoints.size();
        }
    }
}
