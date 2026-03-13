package com.datanymize.export;

import com.datanymize.export.model.ExportResult;

/**
 * Base class for all exporters with common progress tracking functionality.
 * Validates: Requirements 7.7
 */
public abstract class BaseExporter implements IExporter {
    protected ProgressListener progressListener;
    protected volatile boolean cancelled = false;
    protected volatile boolean running = false;

    @Override
    public void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Report progress to the listener.
     *
     * @param currentTable the table currently being processed
     * @param rowsProcessed total rows processed so far
     * @param totalRows estimated total rows to process
     * @param percentComplete percentage of export complete (0-100)
     */
    protected void reportProgress(String currentTable, long rowsProcessed, long totalRows, int percentComplete) {
        if (progressListener != null) {
            progressListener.onProgress(currentTable, rowsProcessed, totalRows, percentComplete);
        }
    }

    /**
     * Report completion to the listener.
     *
     * @param result the export result
     */
    protected void reportComplete(ExportResult result) {
        if (progressListener != null) {
            progressListener.onComplete(result);
        }
    }

    /**
     * Report error to the listener.
     *
     * @param error the error message
     * @param exception the exception that occurred
     */
    protected void reportError(String error, Exception exception) {
        if (progressListener != null) {
            progressListener.onError(error, exception);
        }
    }

    /**
     * Report cancellation to the listener.
     */
    protected void reportCancelled() {
        if (progressListener != null) {
            progressListener.onCancelled();
        }
    }

    /**
     * Check if export has been cancelled.
     *
     * @return true if export should be cancelled
     * @throws InterruptedException if export is cancelled
     */
    protected void checkCancellation() throws InterruptedException {
        if (cancelled) {
            reportCancelled();
            throw new InterruptedException("Export cancelled");
        }
    }

    /**
     * Generate a unique export ID.
     *
     * @return the export ID
     */
    protected String generateExportId() {
        return "export_" + System.currentTimeMillis() + "_" + System.nanoTime();
    }
}
