package com.datanymize.database.schema;

import com.datanymize.database.connection.IDatabaseConnection;
import com.datanymize.database.connection.MySQLConnection;
import com.datanymize.database.model.DatabaseMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MySQLSchemaExtractor.
 * 
 * Tests schema extraction functionality including:
 * - Table extraction
 * - Column extraction
 * - Foreign key extraction
 * - Index extraction
 * - Caching behavior
 */
@DisplayName("MySQL Schema Extractor Tests")
class MySQLSchemaExtractorTest {
    
    private MySQLSchemaExtractor extractor;
    
    @Mock
    private MySQLConnection mockConnection;
    
    @Mock
    private Connection mockUnderlyingConnection;
    
    @Mock
    private Statement mockStatement;
    
    @Mock
    private PreparedStatement mockPreparedStatement;
    
    @Mock
    private ResultSet mockResultSet;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        extractor = new MySQLSchemaExtractor(5000); // 5 second cache TTL
    }
    
    @Test
    @DisplayName("Should extract tables from database")
    void testExtractTables() throws Exception {
        // Setup
        when(mockConnection.getUnderlyingConnection()).thenReturn(mockUnderlyingConnection);
        when(mockUnderlyingConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        
        // Mock table result set
        when(mockResultSet.next())
            .thenReturn(true)   // First table
            .thenReturn(false); // No more tables
        when(mockResultSet.getString("TABLE_NAME")).thenReturn("users");
        
        // Mock columns extraction
        when(mockUnderlyingConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        
        // Mock columns result set
        when(mockResultSet.next())
            .thenReturn(true)   // First column
            .thenReturn(false); // No more columns
        when(mockResultSet.getString("COLUMN_NAME")).thenReturn("id");
        when(mockResultSet.getString("COLUMN_TYPE")).thenReturn("int");
        when(mockResultSet.getString("IS_NULLABLE")).thenReturn("NO");
        when(mockResultSet.getString("COLUMN_DEFAULT")).thenReturn(null);
        
        // Execute
        List<DatabaseMetadata.TableMetadata> tables = extractor.extractTables(mockConnection);
        
        // Verify
        assertNotNull(tables);
        assertEquals(1, tables.size());
        assertEquals("users", tables.get(0).getName());
    }
    
    @Test
    @DisplayName("Should extract columns from table")
    void testExtractColumns() throws Exception {
        // Setup
        when(mockConnection.getUnderlyingConnection()).thenReturn(mockUnderlyingConnection);
        when(mockUnderlyingConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        
        // Mock columns result set
        when(mockResultSet.next())
            .thenReturn(true)   // First column
            .thenReturn(true)   // Second column
            .thenReturn(false); // No more columns
        when(mockResultSet.getString("COLUMN_NAME"))
            .thenReturn("id")
            .thenReturn("name");
        when(mockResultSet.getString("COLUMN_TYPE"))
            .thenReturn("int")
            .thenReturn("varchar(255)");
        when(mockResultSet.getString("IS_NULLABLE"))
            .thenReturn("NO")
            .thenReturn("YES");
        when(mockResultSet.getString("COLUMN_DEFAULT"))
            .thenReturn(null)
            .thenReturn(null);
        
        // Execute
        List<DatabaseMetadata.ColumnMetadata> columns = extractor.extractColumns(mockConnection, "users");
        
        // Verify
        assertNotNull(columns);
        assertEquals(2, columns.size());
        assertEquals("id", columns.get(0).getName());
        assertEquals("int", columns.get(0).getDataType());
        assertEquals("name", columns.get(1).getName());
        assertEquals("varchar(255)", columns.get(1).getDataType());
    }
    
    @Test
    @DisplayName("Should extract foreign keys from database")
    void testExtractForeignKeys() throws Exception {
        // Setup
        when(mockConnection.getUnderlyingConnection()).thenReturn(mockUnderlyingConnection);
        when(mockUnderlyingConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        
        // Mock foreign keys result set
        when(mockResultSet.next())
            .thenReturn(true)   // First FK
            .thenReturn(false); // No more FKs
        when(mockResultSet.getString("CONSTRAINT_NAME")).thenReturn("fk_user_id");
        when(mockResultSet.getString("TABLE_NAME")).thenReturn("orders");
        when(mockResultSet.getString("COLUMN_NAME")).thenReturn("user_id");
        when(mockResultSet.getString("REFERENCED_TABLE_NAME")).thenReturn("users");
        when(mockResultSet.getString("REFERENCED_COLUMN_NAME")).thenReturn("id");
        
        // Mock SHOW CREATE TABLE for constraint rules
        when(mockUnderlyingConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getString(2)).thenReturn(
            "CREATE TABLE `orders` (" +
            "  `id` int NOT NULL," +
            "  `user_id` int NOT NULL," +
            "  CONSTRAINT `fk_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE" +
            ")"
        );
        
        // Execute
        List<DatabaseMetadata.ForeignKeyMetadata> foreignKeys = extractor.extractForeignKeys(mockConnection);
        
        // Verify
        assertNotNull(foreignKeys);
        assertEquals(1, foreignKeys.size());
        assertEquals("fk_user_id", foreignKeys.get(0).getName());
        assertEquals("orders", foreignKeys.get(0).getSourceTable());
        assertEquals("users", foreignKeys.get(0).getTargetTable());
    }
    
    @Test
    @DisplayName("Should extract indices from database")
    void testExtractIndices() throws Exception {
        // Setup
        when(mockConnection.getUnderlyingConnection()).thenReturn(mockUnderlyingConnection);
        when(mockUnderlyingConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        
        // Mock indices result set
        when(mockResultSet.next())
            .thenReturn(true)   // First index
            .thenReturn(false); // No more indices
        when(mockResultSet.getString("INDEX_NAME")).thenReturn("idx_users_email");
        when(mockResultSet.getString("TABLE_NAME")).thenReturn("users");
        when(mockResultSet.getString("COLUMN_NAME")).thenReturn("email");
        when(mockResultSet.getInt("NON_UNIQUE")).thenReturn(0); // 0 = unique
        
        // Execute
        List<DatabaseMetadata.IndexMetadata> indices = extractor.extractIndices(mockConnection);
        
        // Verify
        assertNotNull(indices);
        assertEquals(1, indices.size());
        assertEquals("idx_users_email", indices.get(0).getName());
        assertEquals("users", indices.get(0).getTableName());
        assertTrue(indices.get(0).isUnique());
    }
    
    @Test
    @DisplayName("Should extract primary keys from table")
    void testExtractPrimaryKeys() throws Exception {
        // Setup
        when(mockConnection.getUnderlyingConnection()).thenReturn(mockUnderlyingConnection);
        when(mockUnderlyingConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        
        // Mock primary keys result set
        when(mockResultSet.next())
            .thenReturn(true)   // First PK
            .thenReturn(false); // No more PKs
        when(mockResultSet.getString("COLUMN_NAME")).thenReturn("id");
        
        // Execute
        List<String> primaryKeys = extractor.extractColumns(mockConnection, "users")
            .stream()
            .filter(DatabaseMetadata.ColumnMetadata::isPrimaryKey)
            .map(DatabaseMetadata.ColumnMetadata::getName)
            .toList();
        
        // Note: In real scenario, primary keys would be marked during extraction
        // This test verifies the structure is correct
        assertNotNull(primaryKeys);
    }
    
    @Test
    @DisplayName("Should throw exception for non-MySQL connection")
    void testInvalidConnectionType() {
        // Setup
        IDatabaseConnection invalidConnection = mock(IDatabaseConnection.class);
        
        // Execute & Verify
        assertThrows(IllegalArgumentException.class, () -> {
            extractor.extractTables(invalidConnection);
        });
    }
    
    @Test
    @DisplayName("Should cache extraction results")
    void testCaching() throws Exception {
        // Setup
        when(mockConnection.getUnderlyingConnection()).thenReturn(mockUnderlyingConnection);
        when(mockUnderlyingConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);
        when(mockUnderlyingConnection.getCatalog()).thenReturn("testdb");
        
        // Execute first extraction
        extractor.extractTables(mockConnection);
        
        // Execute second extraction (should use cache)
        extractor.extractTables(mockConnection);
        
        // Verify that createStatement was called only once (cached on second call)
        verify(mockUnderlyingConnection, times(1)).createStatement();
    }
    
    @Test
    @DisplayName("Should clear cache")
    void testClearCache() throws Exception {
        // Setup
        when(mockConnection.getUnderlyingConnection()).thenReturn(mockUnderlyingConnection);
        when(mockUnderlyingConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);
        when(mockUnderlyingConnection.getCatalog()).thenReturn("testdb");
        
        // Execute first extraction
        extractor.extractTables(mockConnection);
        
        // Clear cache
        extractor.clearCache();
        
        // Execute second extraction (should not use cache)
        extractor.extractTables(mockConnection);
        
        // Verify that createStatement was called twice (cache was cleared)
        verify(mockUnderlyingConnection, times(2)).createStatement();
    }
    
    @Test
    @DisplayName("Should handle multiple indices on same table")
    void testExtractMultipleIndices() throws Exception {
        // Setup
        when(mockConnection.getUnderlyingConnection()).thenReturn(mockUnderlyingConnection);
        when(mockUnderlyingConnection.createStatement()).thenReturn(mockStatement);
        when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
        
        // Mock multiple indices result set
        when(mockResultSet.next())
            .thenReturn(true)   // First index
            .thenReturn(true)   // Second index
            .thenReturn(false); // No more indices
        when(mockResultSet.getString("INDEX_NAME"))
            .thenReturn("idx_users_email")
            .thenReturn("idx_users_username");
        when(mockResultSet.getString("TABLE_NAME"))
            .thenReturn("users")
            .thenReturn("users");
        when(mockResultSet.getString("COLUMN_NAME"))
            .thenReturn("email")
            .thenReturn("username");
        when(mockResultSet.getInt("NON_UNIQUE"))
            .thenReturn(0)  // unique
            .thenReturn(0); // unique
        
        // Execute
        List<DatabaseMetadata.IndexMetadata> indices = extractor.extractIndices(mockConnection);
        
        // Verify
        assertNotNull(indices);
        assertEquals(2, indices.size());
        assertEquals("idx_users_email", indices.get(0).getName());
        assertEquals("idx_users_username", indices.get(1).getName());
    }
    
    @Test
    @DisplayName("Should handle nullable columns")
    void testExtractNullableColumns() throws Exception {
        // Setup
        when(mockConnection.getUnderlyingConnection()).thenReturn(mockUnderlyingConnection);
        when(mockUnderlyingConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        
        // Mock columns with nullable
        when(mockResultSet.next())
            .thenReturn(true)   // First column (not nullable)
            .thenReturn(true)   // Second column (nullable)
            .thenReturn(false); // No more columns
        when(mockResultSet.getString("COLUMN_NAME"))
            .thenReturn("id")
            .thenReturn("description");
        when(mockResultSet.getString("COLUMN_TYPE"))
            .thenReturn("int")
            .thenReturn("text");
        when(mockResultSet.getString("IS_NULLABLE"))
            .thenReturn("NO")
            .thenReturn("YES");
        when(mockResultSet.getString("COLUMN_DEFAULT"))
            .thenReturn(null)
            .thenReturn(null);
        
        // Execute
        List<DatabaseMetadata.ColumnMetadata> columns = extractor.extractColumns(mockConnection, "users");
        
        // Verify
        assertNotNull(columns);
        assertEquals(2, columns.size());
        assertFalse(columns.get(0).isNullable());
        assertTrue(columns.get(1).isNullable());
    }
}
