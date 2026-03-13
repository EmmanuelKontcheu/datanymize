package com.datanymize.export;

import com.datanymize.database.connection.IDatabaseConnection;
import com.datanymize.database.model.DatabaseSchema;
import com.datanymize.export.model.ExportFormat;
import com.datanymize.export.model.ExportResult;

/**
 * Interface for exporting anonymized data in various formats.
 * Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7
 */
public interface IExporter {
    /**
     * Export data from a database connection in the specified format.
     *
     * @param connection the database connection to export from
     * @param schema the database schema
     * @param format the export format
     * @param outputPath the path where export should be written
     * @return the export result with statistics
     * @throws Exception if export fails
     */
    ExportResult export(
        IDatabaseConnection connection,
        DatabaseSchema schema,
        ExportFormat format,
        String outputPath
    ) throws Exception;

    /**
     * Set a progress listener for real-time progress updates.
     *
     * @param listener the progress listener
     */
    void setProgressListener(ProgressListener listener);

    /**
     * Cancel the current export operation.
     */
    void cancel();

    /**
     * Check if export is currently running.
     *
     * @return true if export is in progress
     */
    boolean isRunning();
}
