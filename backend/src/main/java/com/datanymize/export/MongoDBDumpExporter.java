package com.datanymize.export;

import com.datanymize.database.connection.IDatabaseConnection;
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
import org.json.JSONObject;

/**
 * Exporter for MongoDB BSON dump format (mongodump compatible).
 * Validates: Requirements 7.1, 7.4
 */
public class MongoDBDumpExporter implements IExporter {
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
        if (format != ExportFormat.MONGODB_DUMP) {
            throw new IllegalArgumentException("This exporter only supports MONGODB_DUMP format");
        }

        ExportResult result = new ExportResult();
        result.setExportId(generateExportId());
        result.setFormat(format);
        result.setOutputPath(outputPath);

        running = true;
        cancelled = false;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            // Write header
            writeHeader(writer, schema);

            // Calculate total rows
            long totalRows = 0;
            for (Table table : schema.getTables()) {
                totalRows += table.getRowCount();
            }

            // Export collections
            long rowsProcessed = 0;
            for (Table collection : schema.getTables()) {
                if (cancelled) {
                    throw new InterruptedException("Export cancelled");
                }

                // Write collection metadata
                writeCollectionMetadata(writer, collection);

                // Read and write documents
                List<Row> documents = readCollectionData(connection, collection);
                for (Row document : documents) {
                    if (cancelled) {
                        throw new InterruptedException("Export cancelled");
                    }

                    writeDocument(writer, document);
                    rowsProcessed++;

                    // Update progress
                    if (progressListener != null && rowsProcessed % 100 == 0) {
                        int percentComplete = (int) ((rowsProcessed * 100) / Math.max(totalRows, 1));
                        progressListener.onProgress(collection.getName(), rowsProcessed, totalRows, percentComplete);
                    }
                }

                result.getTableStats().put(collection.getName(), (long) documents.size());
            }

            // Write footer
            writeFooter(writer);

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

    private void writeHeader(BufferedWriter writer, DatabaseSchema schema) throws IOException {
        writer.write("{\n");
        writer.write("  \"_metadata\": {\n");
        writer.write("    \"type\": \"mongodump\",\n");
        writer.write("    \"generator\": \"Datanymize\",\n");
        writer.write("    \"timestamp\": \"" + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME) + "\",\n");
        writer.write("    \"database\": \"" + schema.getDatabaseName() + "\",\n");
        writer.write("    \"collections\": " + schema.getTables().size() + "\n");
        writer.write("  },\n");
        writer.write("  \"collections\": [\n");
    }

    private void writeCollectionMetadata(BufferedWriter writer, Table collection) throws IOException {
        writer.write("    {\n");
        writer.write("      \"name\": \"" + collection.getName() + "\",\n");
        writer.write("      \"documents\": " + collection.getRowCount() + ",\n");
        writer.write("      \"data\": [\n");
    }

    private void writeDocument(BufferedWriter writer, Row document) throws IOException {
        JSONObject json = new JSONObject();

        for (String key : document.getValues().keySet()) {
            Object value = document.getValues().get(key);
            if (value == null) {
                json.put(key, JSONObject.NULL);
            } else {
                json.put(key, value);
            }
        }

        writer.write("        " + json.toString() + ",\n");
    }

    private void writeFooter(BufferedWriter writer) throws IOException {
        // Remove trailing comma from last document
        writer.write("      ]\n");
        writer.write("    }\n");
        writer.write("  ]\n");
        writer.write("}\n");
    }

    private List<Row> readCollectionData(IDatabaseConnection connection, Table collection) throws SQLException {
        List<Row> documents = new ArrayList<>();
        String query = "SELECT * FROM " + collection.getName();
        ResultSet rs = connection.executeQuery(query);

        while (rs.next()) {
            Row document = new Row();
            document.setTableName(collection.getName());

            // Read all columns as document fields
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                String columnName = rs.getMetaData().getColumnName(i);
                Object value = rs.getObject(i);
                document.getValues().put(columnName, value);
            }

            documents.add(document);
        }

        return documents;
    }

    private String generateExportId() {
        return "export_" + System.currentTimeMillis();
    }
}
