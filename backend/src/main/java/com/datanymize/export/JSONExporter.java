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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Exporter for JSON format.
 * Validates: Requirements 7.1
 */
public class JSONExporter implements IExporter {
    private ProgressListener progressListener;
    private volatile boolean cancelled = false;
    private volatile boolean running = false;

    @Override
    public ExportResult export(
        IDatabaseConnection connection,
        DatabaseSchema schema,
        ExportFormat format,
        String outputPath
    ) throws Exception {
        if (format != ExportFormat.JSON) {
            throw new IllegalArgumentException("This exporter only supports JSON format");
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

            // Create root JSON object
            JSONObject root = new JSONObject();

            // Add metadata
            JSONObject metadata = new JSONObject();
            metadata.put("generator", "Datanymize");
            metadata.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            metadata.put("database", schema.getDatabaseName());
            metadata.put("tables", schema.getTables().size());
            root.put("_metadata", metadata);

            // Add tables
            JSONObject tables = new JSONObject();
            long rowsProcessed = 0;

            for (Table table : schema.getTables()) {
                if (cancelled) {
                    throw new InterruptedException("Export cancelled");
                }

                JSONArray tableData = new JSONArray();

                // Read and convert data
                List<Row> rows = readTableData(connection, table);
                for (Row row : rows) {
                    if (cancelled) {
                        throw new InterruptedException("Export cancelled");
                    }

                    JSONObject rowJson = new JSONObject();
                    for (String key : row.getValues().keySet()) {
                        Object value = row.getValues().get(key);
                        if (value == null) {
                            rowJson.put(key, JSONObject.NULL);
                        } else {
                            rowJson.put(key, value);
                        }
                    }
                    tableData.put(rowJson);
                    rowsProcessed++;

                    // Update progress
                    if (progressListener != null && rowsProcessed % 100 == 0) {
                        int percentComplete = (int) ((rowsProcessed * 100) / Math.max(totalRows, 1));
                        progressListener.onProgress(table.getName(), rowsProcessed, totalRows, percentComplete);
                    }
                }

                tables.put(table.getName(), tableData);
                result.getTableStats().put(table.getName(), (long) rows.size());
            }

            root.put("tables", tables);

            // Write JSON to file with pretty printing
            writer.write(root.toString(2));

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
