package com.datanymize.export;

import com.datanymize.database.connection.IDatabaseConnection;
import com.datanymize.database.model.Column;
import com.datanymize.database.model.DatabaseSchema;
import com.datanymize.database.model.Row;
import com.datanymize.database.model.Table;
import com.datanymize.export.model.ExportFormat;
import com.datanymize.export.model.ExportResult;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Exporter for CSV (Comma-Separated Values) format.
 * Validates: Requirements 7.1
 */
public class CSVExporter implements IExporter {
    private ProgressListener progressListener;
    private volatile boolean cancelled = false;
    private volatile boolean running = false;
    private static final String CSV_DELIMITER = ",";
    private static final String CSV_QUOTE = "\"";
    private static final String CSV_NEWLINE = "\n";

    @Override
    public ExportResult export(
        IDatabaseConnection connection,
        DatabaseSchema schema,
        ExportFormat format,
        String outputPath
    ) throws Exception {
        if (format != ExportFormat.CSV) {
            throw new IllegalArgumentException("This exporter only supports CSV format");
        }

        ExportResult result = new ExportResult();
        result.setExportId(generateExportId());
        result.setFormat(format);
        result.setOutputPath(outputPath);

        running = true;
        cancelled = false;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            // Calculate total rows
            long totalRows = 0;
            for (Table table : schema.getTables()) {
                totalRows += table.getRowCount();
            }

            long rowsProcessed = 0;

            // Export each table as a separate CSV section
            for (Table table : schema.getTables()) {
                if (cancelled) {
                    throw new InterruptedException("Export cancelled");
                }

                // Write table header comment
                writer.write("# Table: " + table.getName() + CSV_NEWLINE);

                // Write column headers
                writeHeaderRow(writer, table);

                // Read and write data rows
                List<Row> rows = readTableData(connection, table);
                for (Row row : rows) {
                    if (cancelled) {
                        throw new InterruptedException("Export cancelled");
                    }

                    writeDataRow(writer, row, table);
                    rowsProcessed++;

                    // Update progress
                    if (progressListener != null && rowsProcessed % 100 == 0) {
                        int percentComplete = (int) ((rowsProcessed * 100) / Math.max(totalRows, 1));
                        progressListener.onProgress(table.getName(), rowsProcessed, totalRows, percentComplete);
                    }
                }

                result.getTableStats().put(table.getName(), (long) rows.size());
                writer.write(CSV_NEWLINE); // Blank line between tables
            }

            result.setRowsExported(rowsProcessed);
            result.setTablesExported(schema.getTables().size());
            result.markComplete();

            if (progressListener != null) {
                progressListener.onComplete(result);
            }

        } catch (Exception e) {
            result.markFailed(e.getMessage());
            if (progressListener != null) {
                progressListener.onError("Export failed: " + e.getMessage(), e);
            }
            throw e;
        } finally {
            running = false;
        }

        return result;
    }

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

    private void writeHeaderRow(BufferedWriter writer, Table table) throws IOException {
        List<String> headers = new ArrayList<>();
        for (Column col : table.getColumns()) {
            headers.add(escapeCSVValue(col.getName()));
        }
        writer.write(String.join(CSV_DELIMITER, headers) + CSV_NEWLINE);
    }

    private void writeDataRow(BufferedWriter writer, Row row, Table table) throws IOException {
        List<String> values = new ArrayList<>();
        for (Column col : table.getColumns()) {
            Object value = row.getValues().get(col.getName());
            values.add(escapeCSVValue(value));
        }
        writer.write(String.join(CSV_DELIMITER, values) + CSV_NEWLINE);
    }

    private String escapeCSVValue(Object value) {
        if (value == null) {
            return "";
        }

        String stringValue = value.toString();

        // Check if value needs quoting
        if (stringValue.contains(CSV_DELIMITER) || stringValue.contains(CSV_QUOTE) || stringValue.contains("\n")) {
            // Escape quotes by doubling them
            stringValue = stringValue.replace(CSV_QUOTE, CSV_QUOTE + CSV_QUOTE);
            return CSV_QUOTE + stringValue + CSV_QUOTE;
        }

        return stringValue;
    }

    private List<Row> readTableData(IDatabaseConnection connection, Table table) throws SQLException {
        List<Row> rows = new ArrayList<>();
        String query = "SELECT * FROM " + table.getName();
        ResultSet rs = connection.executeQuery(query);

        while (rs.next()) {
            Row row = new Row();
            row.setTableName(table.getName());

            for (Column col : table.getColumns()) {
                Object value = rs.getObject(col.getName());
                row.getValues().put(col.getName(), value);
            }

            rows.add(row);
        }

        return rows;
    }

    private String generateExportId() {
        return "export_" + System.currentTimeMillis();
    }
}
