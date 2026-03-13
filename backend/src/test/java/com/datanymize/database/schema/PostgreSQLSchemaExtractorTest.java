package com.datanymize.database.schema;

import com.datanymize.database.connection.IDatabaseConnection;
import com.datanymize.database.connection.PostgreSQLConnection;
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
 * Unit tests for PostgreSQLSchemaExtractor.
 * 
 * Tests schema extraction functionality including:
 * - Table extraction
 * - Column extraction
 * - Foreign key extraction
 * - Index extraction
 * - Caching behavior
 */
@DisplayName("PostgreSQL Schema Extractor Tests")
class PostgreSQLSchemaExtractorTest {
    
    private PostgreSQLSchemaExtractor extractor;
    
    @Mock
    private PostgreSQLConnection mockConnection;
    
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
        extractor = new PostgreSQLSchemaExtractor(5000); // 5 second cache TTL
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
        when(mockResultSet.getString("table_name")).thenReturn("users");
        
        // Mock columns extraction
        when(mockUnderlyingConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        
        // Mock columns result set
        when(mockResultSet.next())
            .thenReturn(true)   // First column
            .thenReturn(false); // No more columns
        when(mockResultSet.getString("column_name")).thenReturn("id");
        when(mockResultSet.getString("data_type")).thenReturn("integer");
        when(mockResultSet.getString("is_nullable")).thenReturn("NO");
        when(mockResultSet.getString("column_default")).thenReturn(null);
        
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
        when(mockResultSet.getString("column_name"))
            .thenReturn("id")
            .thenReturn("name");
        when(mockResultSet.getString("data_type"))
            .thenReturn("integer")
            .thenReturn("varchar");
        when(mockResultSet.getString("is_nullable"))
            .thenReturn("NO")
            .thenReturn("YES");
        when(mockResultSet.getString("column_default"))
            .thenReturn(null)
            .thenReturn(null);
        
        // Execute
        List<DatabaseMetadata.ColumnMetadata> columns = extractor.extractColumns(mockConnection, "users");
        
        // Verify
        assertNotNull(columns);
        assertEquals(2, columns.size());
        assertEquals("id", columns.get(0).getName());
        assertEquals("integer", columns.get(0).getDataType());
        assertEquals("name", columns.get(1).getName());
        assertEquals("varchar", columns.get(1).getDataType());
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
        when(mockResultSet.getString("constraint_name")).thenReturn("fk_user_id");
        when(mockResultSet.getString("table_name")).thenReturn("orders");
        when(mockResultSet.getString("column_name")).thenReturn("user_id");
        when(mockResultSet.getString("referenced_table_name")).thenReturn("users");
        when(mockResultSet.getString("referenced_column_name")).thenReturn("id");
        
        // Mock constraint rule extraction
        when(mockUnderlyingConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true).thenReturn(false);
        when(mockResultSet.getString(1)).thenReturn("CASCADE");
        
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
        when(mockResultSet.getString("indexname")).thenReturn("idx_users_email");
        when(mockResultSet.getString("tablename")).thenReturn("users");
        when(mockResultSet.getString("indexdef")).thenReturn("CREATE UNIQUE INDEX idx_users_email ON users (email)");
        
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
    @DisplayName("Should throw exception for non-PostgreSQL connection")
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
        when(mockUnderlyingConnection.getSchema()).thenReturn("public");
        
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
        when(mockUnderlyingConnection.getSchema()).thenReturn("public");
        
        // Execute first extraction
        extractor.extractTables(mockConnection);
        
        // Clear cache
        extractor.clearCache();
        
        // Execute second extraction (should not use cache)
        extractor.extractTables(mockConnection);
        
        // Verify that createStatement was called twice (cache was cleared)
        verify(mockUnderlyingConnection, times(2)).createStatement();
    }
}
