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
 * Property-based tests for MySQL export compatibility.
 * **Validates: Requirements 7.1, 7.3**
 *
 * Property 19: Export Format Compatibility
 * - Export data and verify format is mysqldump compatible
 */
@DisplayName("MySQL Export Compatibility Properties")
public class MySQLExportCompatibilityProperties extends BasePropertyTest {
    @Mock
    private IDatabaseConnection mockConnection;

    private MySQLDumpExporter exporter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        exporter = new MySQLDumpExporter();
    }

    @Property(tries = 10)
    @DisplayName("Exported SQL contains mysqldump compatible header")
    void exportedSQLContainsMysqldumpHeader(@ForAll String databaseName) {
        // Arrange
        DatabaseSchema schema = createTestSchema(databaseName);
        String outputPath = "target/test-export-" + System.currentTimeMillis() + ".sql";

        try {
            // Act
            ExportResult result = exporter.export(mockConnection, schema, ExportFormat.MYSQL_DUMP, outputPath);

            // Assert
            assertTrue(result.isSuccess(), "Export should succeed");
            assertTrue(Files.exists(Paths.get(outputPath)), "Output file should exist");

            String content = new String(Files.readAllBytes(Paths.get(outputPath)));
            assertTrue(content.contains("MySQL database dump"), "Should contain mysqldump header");
            assertTrue(content.contains("/*!40101 SET"), "Should contain MySQL version markers");
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
    @DisplayName("Exported SQL uses proper MySQL identifier escaping")
    void exportedSQLUsesProperIdentifierEscaping(@ForAll String tableName) {
        // Arrange
        DatabaseSchema schema = createTestSchema("test_db");
        String outputPath = "target/test-export-" + System.currentTimeMillis() + ".sql";

        try {
            // Act
            ExportResult result = exporter.export(mockConnection, schema, ExportFormat.MYSQL_DUMP, outputPath);

            // Assert
            assertTrue(result.isSuccess(), "Export should succeed");

            String content = new String(Files.readAllBytes(Paths.get(outputPath)));
            // MySQL identifiers should be quoted with backticks
            assertTrue(content.contains("`"), "Should use backticks for identifiers");

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
    @DisplayName("Exported SQL contains DROP TABLE IF EXISTS")
    void exportedSQLContainsDropTableIfExists(@ForAll String tableName) {
        // Arrange
        DatabaseSchema schema = createTestSchema("test_db");
        String outputPath = "target/test-export-" + System.currentTimeMillis() + ".sql";

        try {
            // Act
            ExportResult result = exporter.export(mockConnection, schema, ExportFormat.MYSQL_DUMP, outputPath);

            // Assert
            assertTrue(result.isSuccess(), "Export should succeed");

            String content = new String(Files.readAllBytes(Paths.get(outputPath)));
            assertTrue(content.contains("DROP TABLE IF EXISTS"), "Should contain DROP TABLE IF EXISTS");

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
    @DisplayName("Exported SQL contains ENGINE and CHARSET specifications")
    void exportedSQLContainsEngineAndCharset(@ForAll String tableName) {
        // Arrange
        DatabaseSchema schema = createTestSchema("test_db");
        String outputPath = "target/test-export-" + System.currentTimeMillis() + ".sql";

        try {
            // Act
            ExportResult result = exporter.export(mockConnection, schema, ExportFormat.MYSQL_DUMP, outputPath);

            // Assert
            assertTrue(result.isSuccess(), "Export should succeed");

            String content = new String(Files.readAllBytes(Paths.get(outputPath)));
            assertTrue(content.contains("ENGINE=InnoDB"), "Should specify InnoDB engine");
            assertTrue(content.contains("CHARSET=utf8mb4"), "Should specify UTF8MB4 charset");

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
            ExportResult result = exporter.export(mockConnection, schema, ExportFormat.MYSQL_DUMP, outputPath);

            // Assert
            assertTrue(result.isSuccess(), "Export should succeed");
            assertEquals(ExportFormat.MYSQL_DUMP, result.getFormat(), "Format should be MYSQL_DUMP");
            assertTrue(result.getTablesExported() > 0, "Should export at least one table");
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

        Table table = new Table();
        table.setName("test_table");
        table.setRowCount(0);

        Column col1 = new Column();
        col1.setName("id");
        col1.setDataType("INT");
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
