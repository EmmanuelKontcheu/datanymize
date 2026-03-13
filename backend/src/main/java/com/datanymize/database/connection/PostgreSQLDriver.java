package com.datanymize.database.connection;

import com.datanymize.database.model.ConnectionConfig;
import com.datanymize.database.model.DatabaseMetadata;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * PostgreSQL database driver implementation.
 * Handles connection creation, pooling, and validation.
 */
@Slf4j
public class PostgreSQLDriver implements IDatabaseDriver {
    
    private static final String DATABASE_TYPE = "postgresql";
    private static final String JDBC_URL_FORMAT = "jdbc:postgresql://%s:%d/%s";
    
    private HikariDataSource dataSource;
    
    @Override
    public IDatabaseConnection createConnection(ConnectionConfig config) throws Exception {
        validateConfig(config);
        
        // Create HikariCP connection pool
        HikariConfig hikariConfig = new HikariConfig();
        
        // Build JDBC URL
        String jdbcUrl = String.format(
            JDBC_URL_FORMAT,
            config.getHost(),
            config.getPort(),
            config.getDatabase()
        );
        
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());
        
        // Connection timeout in milliseconds
        hikariConfig.setConnectionTimeout(config.getConnectionTimeoutSeconds() * 1000L);
        
        // Validation query
        hikariConfig.setConnectionTestQuery("SELECT 1");
        
        // Pool settings
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setIdleTimeout(600000); // 10 minutes
        hikariConfig.setMaxLifetime(1800000); // 30 minutes
        
        // TLS/SSL configuration
        Properties props = new Properties();
        if (config.isUseTLS()) {
            props.setProperty("ssl", "true");
            if (!config.isVerifyCertificate()) {
                props.setProperty("sslmode", "require");
            } else {
                props.setProperty("sslmode", "verify-full");
            }
        }
        
        // Add any additional parameters
        if (config.getAdditionalParams() != null) {
            config.getAdditionalParams().forEach(props::setProperty);
        }
        
        hikariConfig.setDataSourceProperties(props);
        
        // Create data source
        this.dataSource = new HikariDataSource(hikariConfig);
        
        // Get a connection to verify it works
        Connection conn = dataSource.getConnection();
        
        log.info("PostgreSQL connection established to {}:{}/{}", 
            config.getHost(), config.getPort(), config.getDatabase());
        
        return new PostgreSQLConnection(conn);
    }
    
    @Override
    public String getDatabaseType() {
        return DATABASE_TYPE;
    }
    
    @Override
    public boolean validateReadOnlyAccess(IDatabaseConnection conn) throws Exception {
        if (!(conn instanceof PostgreSQLConnection)) {
            throw new IllegalArgumentException("Connection must be a PostgreSQLConnection");
        }
        
        PostgreSQLConnection pgConn = (PostgreSQLConnection) conn;
        
        // Test SELECT is allowed
        try (Statement stmt = pgConn.getUnderlyingConnection().createStatement()) {
            stmt.execute("SELECT 1");
        }
        
        // Test that INSERT is blocked or fails
        try (Statement stmt = pgConn.getUnderlyingConnection().createStatement()) {
            stmt.execute("CREATE TEMP TABLE test_write (id INT)");
            stmt.execute("INSERT INTO test_write VALUES (1)");
            // If we get here, write access is allowed - this is a problem
            stmt.execute("DROP TABLE test_write");
            return false; // Write access detected - not read-only
        } catch (SQLException e) {
            // Expected - write should fail
            log.debug("Write operation blocked as expected: {}", e.getMessage());
            return true; // Read-only access confirmed
        }
    }
    
    @Override
    public DatabaseMetadata extractSchema(IDatabaseConnection conn) throws Exception {
        // This will be implemented in Phase 3 (Schema Management)
        // For now, return a basic structure
        return DatabaseMetadata.builder()
            .databaseType(DATABASE_TYPE)
            .build();
    }
    
    @Override
    public void createSchema(IDatabaseConnection conn, DatabaseMetadata schema) throws Exception {
        // This will be implemented in Phase 3 (Schema Management)
        log.info("Schema creation not yet implemented");
    }
    
    @Override
    public void dropSchema(IDatabaseConnection conn, String schemaName) throws Exception {
        // This will be implemented in Phase 3 (Schema Management)
        log.info("Schema drop not yet implemented");
    }
    
    /**
     * Close the connection pool.
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("PostgreSQL connection pool closed");
        }
    }
    
    /**
     * Validate the connection configuration.
     */
    private void validateConfig(ConnectionConfig config) throws IllegalArgumentException {
        if (config.getHost() == null || config.getHost().isEmpty()) {
            throw new IllegalArgumentException("Host is required");
        }
        if (config.getPort() <= 0 || config.getPort() > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        if (config.getDatabase() == null || config.getDatabase().isEmpty()) {
            throw new IllegalArgumentException("Database name is required");
        }
        if (config.getUsername() == null || config.getUsername().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (config.getPassword() == null) {
            throw new IllegalArgumentException("Password is required");
        }
        if (config.getConnectionTimeoutSeconds() <= 0) {
            throw new IllegalArgumentException("Connection timeout must be positive");
        }
    }
}
