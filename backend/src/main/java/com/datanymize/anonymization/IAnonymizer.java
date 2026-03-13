package com.datanymize.anonymization;

import com.datanymize.config.model.AnonymizationConfig;
import com.datanymize.database.connection.IDatabaseConnection;
import com.datanymize.anonymization.model.AnonymizationResult;

/**
 * Interface for anonymization engine.
 */
public interface IAnonymizer {
    /**
     * Execute anonymization.
     * @param sourceConn Source database connection
     * @param targetConn Target database connection
     * @param config Anonymization configuration
     * @param listener Progress listener
     * @return Anonymization result
     */
    AnonymizationResult anonymize(
        IDatabaseConnection sourceConn,
        IDatabaseConnection targetConn,
        AnonymizationConfig config,
        ProgressListener listener
    );

    /**
     * Cancel ongoing anonymization.
     */
    void cancel();

    /**
     * Check if anonymization is running.
     * @return true if anonymization is in progress
     */
    boolean isRunning();
}
