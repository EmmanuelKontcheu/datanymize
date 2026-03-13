package com.datanymize.anonymization;

import com.datanymize.anonymization.model.AnonymizationResult;

/**
 * Listener for anonymization progress updates.
 */
public interface ProgressListener {
    /**
     * Called when progress is updated.
     * @param tableName Current table being processed
     * @param rowsProcessed Total rows processed so far
     * @param totalRows Total rows to process
     * @param percentComplete Percentage complete (0-100)
     */
    void onProgress(String tableName, long rowsProcessed, long totalRows, int percentComplete);

    /**
     * Called when an error occurs.
     * @param error Error message
     */
    void onError(String error);

    /**
     * Called when anonymization is completed.
     * @param result Anonymization result
     */
    void onCompleted(AnonymizationResult result);

    /**
     * Called when anonymization is cancelled.
     */
    void onCancelled();
}
