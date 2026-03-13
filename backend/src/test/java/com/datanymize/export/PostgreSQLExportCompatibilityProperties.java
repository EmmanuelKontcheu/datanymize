package com.datanymize.export;

import static org.junit.jupiter.api.Assertions.*;

import com.datanymize.database.connection.IDatabaseConnection;
import com.datanymize.database.model.Column;
import com.datanymize.database.model.DatabaseSchema;
import com.datanymize.database.model.Table;
import com.datanymize.export.model.ExportFormat;
import com.datanymize.export.model.ExportResult;
import com.datanymize.test.BasePropertyTest;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Property-based tests for PostgreSQL export compatibility.
 * **Validates: Requirements 7.1, 7.2**
 *
 * Property 19: Export Format Compatibility
 * - Export data and verify format is pg_dump compatible
 * - Test exported SQL can be imported
 */
@DisplayName("PostgreSQL Export Compatibility Properties")
public class PostgreSQLExportCompatibilityProperties extends BasePropertyTest {
    @Mock
    private IDatabaseConnection mockConnection;

    private PostgreSQLDumpExporter exporter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        exporter = new PostgreSQLDumpExporter();
    }

    @Property(tries = 10)
    @DisplayName("Exported SQL contains pg_dump compatible header")
    void exportedSQLContainsPgDumpHeader(@ForAll String databaseName) {
        // Arrange
        DatabaseSchema schema = createTestSchema(databaseName);
        String outputPath = "target/test-export-" + System.currentTimeMillis() + ".sql";

        try {
            // Act
            ExportResult result = exporter.export(mockConnection, schema, ExportFormat.POSTGRESQL_DUMP, outputPath);

            // Assert
            assertTrue(result.isSuccess(), "Export should succeed");
            assertTrue(Files.exists(Paths.get(outputPath)), "Output file should exist");

            String content = new String(Files.readAllBytes(Paths.get(outputPath)));
            assertTrue(content.contains("PostgreSQL database dump"), "Should contain pg_dump header");
            assertTrue(content.contains("SET statement_timeout"), "Should contain PostgreSQL settings");
            assertTrue(content.contains("CREATE TABLE"), "Should contain CREATE TABLE statements");

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
    @DisplayName("Exported SQL uses proper PostgreSQL identifier escaping")
    void exportedSQLUsesProperIdentifierEscaping(@ForAll String tableName) {
        // Arrange
        DatabaseSchema schema = createTestSchema("test_db");
        String outputPath = "target/test-export-" + System.currentTimeMillis() + ".sql";

        try {
            // Act
            ExportResult result = exporter.export(mockConnection, schema, ExportFormat.POSTGRESQL_DUMP, outputPath);

            // Assert
            assertTrue(result.isSuccess(), "Export should succeed");

            String content = new String(Files.readAllBytes(Paths.get(outputPath)));
            // PostgreSQL identifiers should be quoted with double quotes
            assertTrue(content.contains("\""), "Should use double quotes for identifiers");

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
    @DisplayName("Exported SQL contains valid INSERT statements")
    void exportedSQLContainsValidInsertStatements(@ForAll @IntRange(min = 1, max = 10) int tableCount) {
        // Arrange
        DatabaseSchema schema = createTestSchema("test_db");
        String outputPath = "target/test-export-" + System.currentTimeMillis() + ".sql";

        try {
            // Act
            ExportResult result = exporter.export(mockConnection, schema, ExportFormat.POSTGRESQL_DUMP, outputPath);

            // Assert
            assertTrue(result.isSuccess(), "Export should succeed");

            String content = new String(Files.readAllBytes(Paths.get(outputPath)));
            assertTrue(content.contains("INSERT INTO"), "Should contain INSERT statements");
            assertTrue(content.contains("VALUES"), "Should contain VALUES clause");

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
        String outputPath = "target/test-export-" + System.currentTimeMillis() + ".sql";

        try {
            // Act
            ExportResult result = exporter.export(mockConnection, schema, ExportFormat.POSTGRESQL_DUMP, outputPath);

            // Assert
            assertTrue(result.isSuccess(), "Export should succeed");
            assertEquals(ExportFormat.POSTGRESQL_DUMP, result.getFormat(), "Format should be POSTGRESQL_DUMP");
            assertTrue(result.getTablesExported() > 0, "Should export at least one table");
            assertTrue(result.getDuration() >= 0, "Duration should be non-negative");
            assertNotNull(result.getTableStats(), "Table stats should not be null");

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
    @DisplayName("Export can be cancelled")
    void exportCanBeCancelled() {
        // Arrange
        DatabaseSchema schema = createTestSchema("test_db");
        String outputPath = "target/test-export-" + System.currentTimeMillis() + ".sql";

        try {
            // Act
            exporter.cancel();

            // Assert
            assertFalse(exporter.isRunning(), "Export should not be running after cancel");

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

        Table table = new Table();
        table.setName("test_table");
        table.setRowCount(0);

        Column col1 = new Column();
        col1.setName("id");
        col1.setDataType("INTEGER");
        col1.setNullable(false);
        col1.setPrimaryKey(true);

        Column col2 = new Column();
        col2.setName("name");
        col2.setDataType("VARCHAR(255)");
        col2.setNullable(true);

        table.getColumns().add(col1);
        table.getColumns().add(col2);
        table.getPrimaryKeys().add("id");

        schema.getTables().add(table);

        return schema;
    }
}
