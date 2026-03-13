package com.datanymize.export;

import static org.junit.jupiter.api.Assertions.*;

import com.datanymize.database.connection.IDatabaseConnection;
import com.datanymize.database.model.Column;
import com.datanymize.database.model.DatabaseSchema;
import com.datanymize.database.model.Table;
import com.datanymize.export.model.ExportFormat;
import com.datanymize.export.model.ExportResult;
import com.datanymize.test.BasePropertyTest;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Property-based tests for MongoDB export compatibility.
 * **Validates: Requirements 7.1, 7.4**
 *
 * Property 19: Export Format Compatibility
 * - Export data and verify format is mongodump compatible
 */
@DisplayName("MongoDB Export Compatibility Properties")
public class MongoDBExportCompatibilityProperties extends BasePropertyTest {
    @Mock
    private IDatabaseConnection mockConnection;

    private MongoDBDumpExporter exporter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        exporter = new MongoDBDumpExporter();
    }

    @Property(tries = 10)
    @DisplayName("Exported BSON contains mongodump compatible metadata")
    void exportedBSONContainsMongodumpMetadata(@ForAll String databaseName) {
        // Arrange
        DatabaseSchema schema = createTestSchema(databaseName);
        String outputPath = "target/test-export-" + System.currentTimeMillis() + ".json";

        try {
            // Act
            ExportResult result = exporter.export(mockConnection, schema, ExportFormat.MONGODB_DUMP, outputPath);

            // Assert
            assertTrue(result.isSuccess(), "Export should succeed");
            assertTrue(Files.exists(Paths.get(outputPath)), "Output file should exist");

            String content = new String(Files.readAllBytes(Paths.get(outputPath)));
            assertTrue(content.contains("_metadata"), "Should contain metadata");
            assertTrue(content.contains("mongodump"), "Should contain mongodump type");
            assertTrue(content.contains("collections"), "Should contain collections");

        } catch (Exception e) {
            fail("Export should not throw exception: " + e.getMessage());
        } finally {
            try {
                Files.deleteIfExists(Paths.get(outputPath));
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Property(tries = 10)
    @DisplayName("Exported BSON is valid JSON format")
    void exportedBSONIsValidJSON(@ForAll String databaseName) {
        // Arrange
        DatabaseSchema schema = createTestSchema(databaseName);
        String outputPath = "target/test-export-" + System.currentTimeMillis() + ".json";

        try {
            // Act
            ExportResult result = exporter.export(mockConnection, schema, ExportFormat.MONGODB_DUMP, outputPath);

            // Assert
            assertTrue(result.isSuccess(), "Export should succeed");

            String content = new String(Files.readAllBytes(Paths.get(outputPath)));
            // Basic JSON validation
            assertTrue(content.startsWith("{"), "Should start with {");
            assertTrue(content.endsWith("}"), "Should end with }");
            assertTrue(content.contains("\""), "Should contain JSON quotes");

        } catch (Exception e) {
            fail("Export should not throw exception: " + e.getMessage());
        } finally {
            try {
                Files.deleteIfExists(Paths.get(outputPath));
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Property(tries = 10)
    @DisplayName("Exported BSON contains collection data")
    void exportedBSONContainsCollectionData(@ForAll String collectionName) {
        // Arrange
        DatabaseSchema schema = createTestSchema("test_db");
        String outputPath = "target/test-export-" + System.currentTimeMillis() + ".json";

        try {
            // Act
            ExportResult result = exporter.export(mockConnection, schema, ExportFormat.MONGODB_DUMP, outputPath);

            // Assert
            assertTrue(result.isSuccess(), "Export should succeed");

            String content = new String(Files.readAllBytes(Paths.get(outputPath)));
            assertTrue(content.contains("\"data\""), "Should contain data array");

        } catch (Exception e) {
            fail("Export should not throw exception: " + e.getMessage());
        } finally {
            try {
                Files.deleteIfExists(Paths.get(outputPath));
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Property(tries = 10)
    @DisplayName("Exported BSON contains collection metadata")
    void exportedBSONContainsCollectionMetadata(@ForAll String collectionName) {
        // Arrange
        DatabaseSchema schema = createTestSchema("test_db");
        String outputPath = "target/test-export-" + System.currentTimeMillis() + ".json";

        try {
            // Act
            ExportResult result = exporter.export(mockConnection, schema, ExportFormat.MONGODB_DUMP, outputPath);

            // Assert
            assertTrue(result.isSuccess(), "Export should succeed");

            String content = new String(Files.readAllBytes(Paths.get(outputPath)));
            assertTrue(content.contains("\"name\""), "Should contain collection name");
            assertTrue(content.contains("\"documents\""), "Should contain document count");

        } catch (Exception e) {
            fail("Export should not throw exception: " + e.getMessage());
        } finally {
            try {
                Files.deleteIfExists(Paths.get(outputPath));
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Example
    @DisplayName("Export result contains correct statistics")
    void exportResultContainsCorrectStatistics() {
        // Arrange
        DatabaseSchema schema = createTestSchema("test_db");
        String outputPath = "target/test-export-" + System.currentTimeMillis() + ".json";

        try {
            // Act
            ExportResult result = exporter.export(mockConnection, schema, ExportFormat.MONGODB_DUMP, outputPath);

            // Assert
            assertTrue(result.isSuccess(), "Export should succeed");
            assertEquals(ExportFormat.MONGODB_DUMP, result.getFormat(), "Format should be MONGODB_DUMP");
            assertTrue(result.getTablesExported() > 0, "Should export at least one collection");
            assertTrue(result.getDuration() >= 0, "Duration should be non-negative");

        } catch (Exception e) {
            fail("Export should not throw exception: " + e.getMessage());
        } finally {
            try {
                Files.deleteIfExists(Paths.get(outputPath));
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    private DatabaseSchema createTestSchema(String databaseName) {
        DatabaseSchema schema = new DatabaseSchema();
        schema.setDatabaseName(databaseName);

        Table collection = new Table();
        collection.setName("test_collection");
        collection.setRowCount(0);

        Column col1 = new Column();
        col1.setName("_id");
        col1.setDataType("ObjectId");
        col1.setNullable(false);

        Column col2 = new Column();
        col2.setName("name");
        col2.setDataType("String");
        col2.setNullable(true);

        collection.getColumns().add(col1);
        collection.getColumns().add(col2);

        schema.getTables().add(collection);

        return schema;
    }
}
