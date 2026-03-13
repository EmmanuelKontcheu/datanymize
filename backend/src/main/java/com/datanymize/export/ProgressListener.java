package com.datanymize.export;

/**
 * Interface for listening to export progress updates.
 * Validates: Requirements 7.1, 7.7
 */
public interface ProgressListener {
    /**
     * Called when export progress is updated.
     *
     * @param currentTable the table currently being exported
     * @param rowsProcessed total rows processed so far
     * @param totalRows estimated total rows to process
     * @param percentComplete percentage of export complete (0-100)
     */
    void onProgress(String currentTable, long rowsProcessed, long totalRows, int percentComplete);

    /**
     * Called when export is complete.
     *
     * @param result the export result with statistics
     */
    void onComplete(ExportResult result);

    /**
     * Called when an error occurs during export.
     *
     * @param error the error message
     * @param exception the exception that occurred
     */
    void onError(String error, Exception exception);

    /**
     * Called when export is cancelled.
     */
    void onCancelled();
}
