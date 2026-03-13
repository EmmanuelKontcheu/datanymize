package com.datanymize.database.connection;

import com.datanymize.database.model.DatabaseMetadata;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * PostgreSQL-specific database connection implementation.
 */
public class PostgreSQLConnection implements IDatabaseConnection {
    
    private final Connection connection;
    
    public PostgreSQLConnection(Connection connection) {
        this.connection = connection;
    }
    
    @Override
    public ResultSet executeQuery(String query) throws SQLException {
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(query);
    }
    
    @Override
    public int executeUpdate(String query) throws SQLException {
        Statement stmt = connection.createStatement();
        return stmt.executeUpdate(query);
    }
    
    @Override
    public void beginTransaction() throws SQLException {
        connection.setAutoCommit(false);
    }
    
    @Override
    public void commit() throws SQLException {
        connection.commit();
        connection.setAutoCommit(true);
    }
    
    @Override
    public void rollback() throws SQLException {
        connection.rollback();
        connection.setAutoCommit(true);
    }
    
    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
    
    @Override
    public boolean isConnected() throws SQLException {
        return connection != null && !connection.isClosed();
    }
    
    @Override
    public boolean validate() throws SQLException {
        if (!isConnected()) {
            return false;
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SELECT 1");
            return true;
        }
    }
    
    @Override
    public DatabaseMetadata getMetadata() throws SQLException {
        // This will be implemented in a future task (Schema Extraction)
        // For now, return a basic structure
        return DatabaseMetadata.builder()
            .databaseName(connection.getCatalog())
            .databaseType("postgresql")
            .build();
    }
    
    /**
     * Get the underlying JDBC connection.
     */
    public Connection getUnderlyingConnection() {
        return connection;
    }
}
