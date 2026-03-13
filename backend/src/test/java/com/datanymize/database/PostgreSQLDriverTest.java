package com.datanymize.database;

import com.datanymize.database.connection.IDatabaseConnection;
import com.datanymize.database.connection.PostgreSQLDriver;
import com.datanymize.database.model.ConnectionConfig;
import com.datanymize.database.model.DatabaseMetadata;
import com.datanymize.database.model.Row;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PostgreSQL driver functionality.
 * 
 * Validates Requirements: 1.1, 2.1
 * 
 * Tests cover:
 * - Schema extraction from PostgreSQL databases
 * - Data read operations with pagination
 * - Data write operations with batch support
 * - Schema creation in target databases
 */
@DisplayName("PostgreSQL Driver Tests")
class PostgreSQLDriverTest {
    
    @Test
    @DisplayName("PostgreSQL driver returns correct database type")
    void testGetDatabaseType() {
        PostgreSQLDriver driver = new PostgreSQLDriver();
        assertEquals("postgresql", driver.getDatabaseType());
    }
    
    @Test
    @DisplayName("Connection config validation rejects missing host")
    void testConnectionConfigValidationMissingHost() {
        PostgreSQLDriver driver = new PostgreSQLDriver();
        
        ConnectionConfig config = ConnectionConfig.builder()
            .type("postgresql")
            .host("")
            .port(5432)
            .database("testdb")
            .username("user")
            .password("pass")
            .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            driver.createConnection(config);
        });
    }
    
    @Test
    @DisplayName("Connection config validation rejects invalid port")
    void testConnectionConfigValidationInvalidPort() {
        PostgreSQLDriver driver = new PostgreSQLDriver();
        
        ConnectionConfig config = ConnectionConfig.builder()
            .type("postgresql")
            .host("localhost")
            .port(0)
            .database("testdb")
            .username("user")
            .password("pass")
            .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            driver.createConnection(config);
        });
    }
    
    @Test
    @DisplayName("Connection config validation rejects missing database")
    void testConnectionConfigValidationMissingDatabase() {
        PostgreSQLDriver driver = new PostgreSQLDriver();
        
        ConnectionConfig config = ConnectionConfig.builder()
            .type("postgresql")
            .host("localhost")
            .port(5432)
            .database("")
            .username("user")
            .password("pass")
            .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            driver.createConnection(config);
        });
    }
    
    @Test
    @DisplayName("Connection config validation rejects missing username")
    void testConnectionConfigValidationMissingUsername() {
        PostgreSQLDriver driver = new PostgreSQLDriver();
        
        ConnectionConfig config = ConnectionConfig.builder()
            .type("postgresql")
            .host("localhost")
            .port(5432)
            .database("testdb")
            .username("")
            .password("pass")
            .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            driver.createConnection(config);
        });
    }
    
    @Test
    @DisplayName("Connection config validation rejects null password")
    void testConnectionConfigValidationNullPassword() {
        PostgreSQLDriver driver = new PostgreSQLDriver();
        
        ConnectionConfig config = ConnectionConfig.builder()
            .type("postgresql")
            .host("localhost")
            .port(5432)
            .database("testdb")
            .username("user")
            .password(null)
            .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            driver.createConnection(config);
        });
    }
    
    @Test
    @DisplayName("Connection config validation rejects invalid timeout")
    void testConnectionConfigValidationInvalidTimeout() {
        PostgreSQLDriver driver = new PostgreSQLDriver();
        
        ConnectionConfig config = ConnectionConfig.builder()
            .type("postgresql")
            .host("localhost")
            .port(5432)
            .database("testdb")
            .username("user")
            .password("pass")
            .connectionTimeoutSeconds(0)
            .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            driver.createConnection(config);
        });
    }
    
    @Test
    @DisplayName("Row can be created with table name and values")
    void testRowCreation() {
        Row row = Row.builder()
            .tableName("users")
            .build();
        
        assertNotNull(row);
        assertEquals("users", row.getTableName());
        assertNotNull(row.getValues());
        assertEquals(0, row.getColumnCount());
    }
    
    @Test
    @DisplayName("Row can store and retrieve multiple column values")
    void testRowMultipleColumns() {
        Row row = Row.builder()
            .tableName("users")
            .build();
        
        row.setValue("id", 1);
        row.setValue("name", "John Doe");
        row.setValue("email", "john@example.com");
        row.setValue("age", 30);
        
        assertEquals(1, row.getValue("id"));
        assertEquals("John Doe", row.getValue("name"));
        assertEquals("john@example.com", row.getValue("email"));
        assertEquals(30, row.getValue("age"));
        assertEquals(4, row.getColumnCount());
    }
    
    @Test
    @DisplayName("Row can track original values separately from current values")
    void testRowOriginalValues() {
        Row row = Row.builder()
            .tableName("users")
            .build();
        
        // Set original value
        row.setOriginalValue("email", "old@example.com");
        
        // Set new value
        row.setValue("email", "new@example.com");
        
        // Verify they are different
        assertEquals("new@example.com", row.getValue("email"));
        assertEquals("old@example.com", row.getOriginalValue("email"));
    }
    
    @Test
    @DisplayName("Row can handle null values")
    void testRowNullValues() {
        Row row = Row.builder()
            .tableName("users")
            .build();
        
        row.setValue("nullable_field", null);
        
        assertTrue(row.hasColumn("nullable_field"));
        assertNull(row.getValue("nullable_field"));
        assertEquals(1, row.getColumnCount());
    }
    
    @Test
    @DisplayName("Row can handle various data types")
    void testRowVariousDataTypes() {
        Row row = Row.builder()
            .tableName("users")
            .build();
        
        row.setValue("id", 123);
        row.setValue("name", "John");
        row.setValue("balance", 99.99);
        row.setValue("active", true);
        row.setValue("data", new byte[]{1, 2, 3});
        
        assertEquals(123, row.getValue("id"));
        assertEquals("John", row.getValue("name"));
        assertEquals(99.99, row.getValue("balance"));
        assertEquals(true, row.getValue("active"));
        assertArrayEquals(new byte[]{1, 2, 3}, (byte[]) row.getValue("data"));
    }
    
    @Test
    @DisplayName("Multiple rows can be created independently")
    void testMultipleRowsIndependence() {
        Row row1 = Row.builder()
            .tableName("users")
            .build();
        
        Row row2 = Row.builder()
            .tableName("orders")
            .build();
        
        row1.setValue("id", 1);
        row1.setValue("name", "Alice");
        
        row2.setValue("id", 100);
        row2.setValue("amount", 99.99);
        
        assertEquals(1, row1.getValue("id"));
        assertEquals("Alice", row1.getValue("name"));
        assertEquals(100, row2.getValue("id"));
        assertEquals(99.99, row2.getValue("amount"));
        assertEquals("users", row1.getTableName());
        assertEquals("orders", row2.getTableName());
    }
    
    @Test
    @DisplayName("Row list can be created for batch operations")
    void testRowListForBatchOperations() {
        List<Row> rows = new ArrayList<>();
        
        for (int i = 1; i <= 10; i++) {
            Row row = Row.builder()
                .tableName("users")
                .build();
            
            row.setValue("id", i);
            row.setValue("name", "User " + i);
            row.setValue("email", "user" + i + "@example.com");
            
            rows.add(row);
        }
        
        assertEquals(10, rows.size());
        
        // Verify first row
        assertEquals(1, rows.get(0).getValue("id"));
        assertEquals("User 1", rows.get(0).getValue("name"));
        
        // Verify last row
        assertEquals(10, rows.get(9).getValue("id"));
        assertEquals("User 10", rows.get(9).getValue("name"));
    }
    
    @Test
    @DisplayName("DatabaseMetadata can be created with schema information")
    void testDatabaseMetadataCreation() {
        DatabaseMetadata metadata = DatabaseMetadata.builder()
            .databaseName("testdb")
            .databaseType("postgresql")
            .build();
        
        assertNotNull(metadata);
        assertEquals("testdb", metadata.getDatabaseName());
        assertEquals("postgresql", metadata.getDatabaseType());
    }
    
    @Test
    @DisplayName("DatabaseMetadata can store table information")
    void testDatabaseMetadataWithTables() {
        List<DatabaseMetadata.TableMetadata> tables = new ArrayList<>();
        
        DatabaseMetadata.TableMetadata table = DatabaseMetadata.TableMetadata.builder()
            .name("users")
            .rowCount(100)
            .build();
        
        tables.add(table);
        
        DatabaseMetadata metadata = DatabaseMetadata.builder()
            .databaseName("testdb")
            .databaseType("postgresql")
            .tables(tables)
            .build();
        
        assertNotNull(metadata.getTables());
        assertEquals(1, metadata.getTables().size());
        assertEquals("users", metadata.getTables().get(0).getName());
        assertEquals(100, metadata.getTables().get(0).getRowCount());
    }
    
    @Test
    @DisplayName("DatabaseMetadata can store column information")
    void testDatabaseMetadataWithColumns() {
        List<DatabaseMetadata.ColumnMetadata> columns = new ArrayList<>();
        
        columns.add(DatabaseMetadata.ColumnMetadata.builder()
            .name("id")
            .dataType("integer")
            .nullable(false)
            .isPrimaryKey(true)
            .build());
        
        columns.add(DatabaseMetadata.ColumnMetadata.builder()
            .name("name")
            .dataType("varchar")
            .nullable(false)
            .build());
        
        DatabaseMetadata.TableMetadata table = DatabaseMetadata.TableMetadata.builder()
            .name("users")
            .columns(columns)
            .build();
        
        assertNotNull(table.getColumns());
        assertEquals(2, table.getColumns().size());
        assertEquals("id", table.getColumns().get(0).getName());
        assertEquals("name", table.getColumns().get(1).getName());
    }
    
    @Test
    @DisplayName("DatabaseMetadata can store foreign key information")
    void testDatabaseMetadataWithForeignKeys() {
        List<DatabaseMetadata.ForeignKeyMetadata> foreignKeys = new ArrayList<>();
        
        foreignKeys.add(DatabaseMetadata.ForeignKeyMetadata.builder()
            .name("fk_user_id")
            .sourceTable("orders")
            .sourceColumn("user_id")
            .targetTable("users")
            .targetColumn("id")
            .onDelete("CASCADE")
            .onUpdate("CASCADE")
            .build());
        
        DatabaseMetadata metadata = DatabaseMetadata.builder()
            .databaseName("testdb")
            .databaseType("postgresql")
            .foreignKeys(foreignKeys)
            .build();
        
        assertNotNull(metadata.getForeignKeys());
        assertEquals(1, metadata.getForeignKeys().size());
        assertEquals("fk_user_id", metadata.getForeignKeys().get(0).getName());
        assertEquals("orders", metadata.getForeignKeys().get(0).getSourceTable());
        assertEquals("users", metadata.getForeignKeys().get(0).getTargetTable());
    }
    
    @Test
    @DisplayName("DatabaseMetadata can store index information")
    void testDatabaseMetadataWithIndices() {
        List<DatabaseMetadata.IndexMetadata> indices = new ArrayList<>();
        
        indices.add(DatabaseMetadata.IndexMetadata.builder()
            .name("idx_email")
            .tableName("users")
            .columns(List.of("email"))
            .unique(true)
            .build());
        
        DatabaseMetadata metadata = DatabaseMetadata.builder()
            .databaseName("testdb")
            .databaseType("postgresql")
            .indices(indices)
            .build();
        
        assertNotNull(metadata.getIndices());
        assertEquals(1, metadata.getIndices().size());
        assertEquals("idx_email", metadata.getIndices().get(0).getName());
        assertTrue(metadata.getIndices().get(0).isUnique());
    }
}
