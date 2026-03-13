package com.datanymize.database;

import com.datanymize.database.model.Row;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Row model.
 * 
 * Validates Requirements: 1.1, 1.2, 1.3
 */
@DisplayName("Row Model Tests")
class RowModelTest {
    
    @Test
    @DisplayName("Row can be created with table name and values")
    void testRowCreation() {
        Row row = Row.builder()
                .tableName("users")
                .build();
        
        assertNotNull(row);
        assertEquals("users", row.getTableName());
        assertNotNull(row.getValues());
        assertNotNull(row.getOriginalValues());
        assertEquals(0, row.getColumnCount());
    }
    
    @Test
    @DisplayName("Row can store and retrieve column values")
    void testSetAndGetValue() {
        Row row = Row.builder()
                .tableName("users")
                .build();
        
        row.setValue("id", 1);
        row.setValue("name", "John Doe");
        row.setValue("email", "john@example.com");
        
        assertEquals(1, row.getValue("id"));
        assertEquals("John Doe", row.getValue("name"));
        assertEquals("john@example.com", row.getValue("email"));
        assertEquals(3, row.getColumnCount());
    }
    
    @Test
    @DisplayName("Row can store and retrieve original values")
    void testSetAndGetOriginalValue() {
        Row row = Row.builder()
                .tableName("users")
                .build();
        
        row.setValue("email", "john.doe@example.com");
        row.setOriginalValue("email", "john@example.com");
        
        assertEquals("john.doe@example.com", row.getValue("email"));
        assertEquals("john@example.com", row.getOriginalValue("email"));
    }
    
    @Test
    @DisplayName("Row can check if column exists")
    void testHasColumn() {
        Row row = Row.builder()
                .tableName("users")
                .build();
        
        row.setValue("id", 1);
        row.setValue("name", "John");
        
        assertTrue(row.hasColumn("id"));
        assertTrue(row.hasColumn("name"));
        assertFalse(row.hasColumn("email"));
    }
    
    @Test
    @DisplayName("Row returns null for non-existent columns")
    void testGetNonExistentColumn() {
        Row row = Row.builder()
                .tableName("users")
                .build();
        
        row.setValue("id", 1);
        
        assertNull(row.getValue("nonexistent"));
        assertNull(row.getOriginalValue("nonexistent"));
    }
    
    @Test
    @DisplayName("Row can be created with initial values")
    void testRowCreationWithValues() {
        Row row = Row.builder()
                .tableName("users")
                .build();
        
        row.setValue("id", 1);
        row.setValue("name", "Alice");
        row.setValue("age", 30);
        
        assertEquals(3, row.getColumnCount());
        assertEquals(1, row.getValue("id"));
        assertEquals("Alice", row.getValue("name"));
        assertEquals(30, row.getValue("age"));
    }
    
    @Test
    @DisplayName("Row can update existing column values")
    void testUpdateColumnValue() {
        Row row = Row.builder()
                .tableName("users")
                .build();
        
        row.setValue("status", "active");
        assertEquals("active", row.getValue("status"));
        
        row.setValue("status", "inactive");
        assertEquals("inactive", row.getValue("status"));
    }
    
    @Test
    @DisplayName("Row maintains separate values and original values")
    void testSeparateValuesAndOriginals() {
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
        
        // Verify column count only counts values, not originals
        assertEquals(1, row.getColumnCount());
    }
    
    @Test
    @DisplayName("Row can handle null values")
    void testNullValues() {
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
    void testVariousDataTypes() {
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
    @DisplayName("Row builder creates independent instances")
    void testBuilderIndependence() {
        Row row1 = Row.builder()
                .tableName("users")
                .build();
        
        Row row2 = Row.builder()
                .tableName("orders")
                .build();
        
        row1.setValue("id", 1);
        row2.setValue("id", 2);
        
        assertEquals(1, row1.getValue("id"));
        assertEquals(2, row2.getValue("id"));
        assertEquals("users", row1.getTableName());
        assertEquals("orders", row2.getTableName());
    }
}
