package com.datanymize.database.connection;

import com.datanymize.database.model.ConnectionConfig;
import com.datanymize.database.model.DatabaseMetadata;
import com.datanymize.database.model.Row;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PostgreSQL database driver implementation.
 * Handles connection creation, pooling, schema extraction, and data operations.
 * 
 * Validates Requirements: 1.1, 2.1
 */
@Slf4j
public class PostgreSQLDriver implements IDatabaseDriver {
    
    private static final String DATABASE_TYPE = "postgresql";
    private static final String JDBC_URL_FORMAT = "jdbc:postgresql://%s:%d/%s";
    private static final int DEFAULT_BATCH_SIZE = 1000;
    
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
        if (!(conn instanceof PostgreSQLConnection)) {
            throw new IllegalArgumentException("Connection must be a PostgreSQLConnection");
        }
        
        PostgreSQLConnection pgConn = (PostgreSQLConnection) conn;
        Connection underlyingConn = pgConn.getUnderlyingConnection();
        
        String databaseName = underlyingConn.getCatalog();
        
        // Extract tables and columns
        List<DatabaseMetadata.TableMetadata> tables = extractTables(underlyingConn);
        
        // Extract foreign keys
        List<DatabaseMetadata.ForeignKeyMetadata> foreignKeys = extractForeignKeys(underlyingConn);
        
        // Extract indices
        List<DatabaseMetadata.IndexMetadata> indices = extractIndices(underlyingConn);
        
        log.info("Schema extracted for database: {} with {} tables", databaseName, tables.size());
        
        return DatabaseMetadata.builder()
            .databaseName(databaseName)
            .databaseType(DATABASE_TYPE)
            .tables(tables)
            .foreignKeys(foreignKeys)
            .indices(indices)
            .build();
    }
    
    /**
     * Extract all tables and their columns from the database using information_schema.
     */
    private List<DatabaseMetadata.TableMetadata> extractTables(Connection conn) throws SQLException {
        List<DatabaseMetadata.TableMetadata> tables = new ArrayList<>();
        
        String query = "SELECT table_name FROM information_schema.tables " +
                      "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' " +
                      "ORDER BY table_name";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                String tableName = rs.getString("table_name");
                List<DatabaseMetadata.ColumnMetadata> columns = extractColumns(conn, tableName);
                List<String> primaryKeys = extractPrimaryKeys(conn, tableName);
                List<String> uniqueKeys = extractUniqueKeys(conn, tableName);
                long rowCount = getRowCount(conn, tableName);
                
                DatabaseMetadata.TableMetadata table = DatabaseMetadata.TableMetadata.builder()
                    .name(tableName)
                    .columns(columns)
                    .primaryKeys(primaryKeys)
                    .uniqueKeys(uniqueKeys)
                    .rowCount(rowCount)
                    .build();
                
                tables.add(table);
            }
        }
        
        return tables;
    }
    
    /**
     * Extract all columns for a specific table.
     */
    private List<DatabaseMetadata.ColumnMetadata> extractColumns(Connection conn, String tableName) throws SQLException {
        List<DatabaseMetadata.ColumnMetadata> columns = new ArrayList<>();
        
        String query = "SELECT column_name, data_type, is_nullable, column_default " +
                      "FROM information_schema.columns " +
                      "WHERE table_schema = 'public' AND table_name = ? " +
                      "ORDER BY ordinal_position";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String columnName = rs.getString("column_name");
                    String dataType = rs.getString("data_type");
                    boolean nullable = "YES".equals(rs.getString("is_nullable"));
                    String defaultValue = rs.getString("column_default");
                    
                    DatabaseMetadata.ColumnMetadata column = DatabaseMetadata.ColumnMetadata.builder()
                        .name(columnName)
                        .dataType(dataType)
                        .nullable(nullable)
                        .defaultValue(defaultValue)
                        .isPrimaryKey(false)  // Will be set by extractPrimaryKeys
                        .isUnique(false)      // Will be set by extractUniqueKeys
                        .build();
                    
                    columns.add(column);
                }
            }
        }
        
        return columns;
    }
    
    /**
     * Extract primary keys for a table.
     */
    private List<String> extractPrimaryKeys(Connection conn, String tableName) throws SQLException {
        List<String> primaryKeys = new ArrayList<>();
        
        String query = "SELECT a.attname " +
                      "FROM pg_index i " +
                      "JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey) " +
                      "JOIN pg_class t ON t.oid = i.indrelid " +
                      "WHERE t.relname = ? AND i.indisprimary " +
                      "ORDER BY a.attnum";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    primaryKeys.add(rs.getString("attname"));
                }
            }
        }
        
        return primaryKeys;
    }
    
    /**
     * Extract unique keys for a table.
     */
    private List<String> extractUniqueKeys(Connection conn, String tableName) throws SQLException {
        List<String> uniqueKeys = new ArrayList<>();
        
        String query = "SELECT a.attname " +
                      "FROM pg_index i " +
                      "JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey) " +
                      "JOIN pg_class t ON t.oid = i.indrelid " +
                      "WHERE t.relname = ? AND i.indisunique AND NOT i.indisprimary " +
                      "ORDER BY a.attnum";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    uniqueKeys.add(rs.getString("attname"));
                }
            }
        }
        
        return uniqueKeys;
    }
    
    /**
     * Extract foreign keys from the database.
     */
    private List<DatabaseMetadata.ForeignKeyMetadata> extractForeignKeys(Connection conn) throws SQLException {
        List<DatabaseMetadata.ForeignKeyMetadata> foreignKeys = new ArrayList<>();
        
        String query = "SELECT constraint_name, table_name, column_name, " +
                      "referenced_table_name, referenced_column_name " +
                      "FROM information_schema.referential_constraints rc " +
                      "JOIN information_schema.key_column_usage kcu " +
                      "ON rc.constraint_name = kcu.constraint_name " +
                      "WHERE kcu.table_schema = 'public'";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                String constraintName = rs.getString("constraint_name");
                String sourceTable = rs.getString("table_name");
                String sourceColumn = rs.getString("column_name");
                String targetTable = rs.getString("referenced_table_name");
                String targetColumn = rs.getString("referenced_column_name");
                
                // Get ON DELETE and ON UPDATE rules
                String onDelete = getConstraintRule(conn, constraintName, "DELETE");
                String onUpdate = getConstraintRule(conn, constraintName, "UPDATE");
                
                DatabaseMetadata.ForeignKeyMetadata fk = DatabaseMetadata.ForeignKeyMetadata.builder()
                    .name(constraintName)
                    .sourceTable(sourceTable)
                    .sourceColumn(sourceColumn)
                    .targetTable(targetTable)
                    .targetColumn(targetColumn)
                    .onDelete(onDelete)
                    .onUpdate(onUpdate)
                    .build();
                
                foreignKeys.add(fk);
            }
        }
        
        return foreignKeys;
    }
    
    /**
     * Get the ON DELETE or ON UPDATE rule for a constraint.
     */
    private String getConstraintRule(Connection conn, String constraintName, String ruleType) throws SQLException {
        String query = "SELECT " + (ruleType.equals("DELETE") ? "delete_rule" : "update_rule") + " " +
                      "FROM information_schema.referential_constraints " +
                      "WHERE constraint_name = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, constraintName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        
        return "NO ACTION";
    }
    
    /**
     * Extract indices from the database.
     */
    private List<DatabaseMetadata.IndexMetadata> extractIndices(Connection conn) throws SQLException {
        List<DatabaseMetadata.IndexMetadata> indices = new ArrayList<>();
        
        String query = "SELECT indexname, tablename, indexdef " +
                      "FROM pg_indexes " +
                      "WHERE schemaname = 'public' " +
                      "ORDER BY tablename, indexname";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                String indexName = rs.getString("indexname");
                String tableName = rs.getString("tablename");
                String indexDef = rs.getString("indexdef");
                
                // Parse columns from index definition
                List<String> columns = parseIndexColumns(indexDef);
                boolean unique = indexDef.contains("UNIQUE");
                
                DatabaseMetadata.IndexMetadata index = DatabaseMetadata.IndexMetadata.builder()
                    .name(indexName)
                    .tableName(tableName)
                    .columns(columns)
                    .unique(unique)
                    .build();
                
                indices.add(index);
            }
        }
        
        return indices;
    }
    
    /**
     * Parse column names from an index definition.
     */
    private List<String> parseIndexColumns(String indexDef) {
        List<String> columns = new ArrayList<>();
        
        // Simple parsing: extract column names between parentheses
        int startIdx = indexDef.lastIndexOf('(');
        int endIdx = indexDef.lastIndexOf(')');
        
        if (startIdx >= 0 && endIdx > startIdx) {
            String columnsPart = indexDef.substring(startIdx + 1, endIdx);
            String[] parts = columnsPart.split(",");
            
            for (String part : parts) {
                String column = part.trim().split("\\s+")[0];  // Get first word (column name)
                if (!column.isEmpty()) {
                    columns.add(column);
                }
            }
        }
        
        return columns;
    }
    
    /**
     * Get the row count for a table.
     */
    private long getRowCount(Connection conn, String tableName) throws SQLException {
        String query = "SELECT COUNT(*) as count FROM " + tableName;
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            if (rs.next()) {
                return rs.getLong("count");
            }
        }
        
        return 0;
    }
    
    @Override
    public void createSchema(IDatabaseConnection conn, DatabaseMetadata schema) throws Exception {
        if (!(conn instanceof PostgreSQLConnection)) {
            throw new IllegalArgumentException("Connection must be a PostgreSQLConnection");
        }
        
        PostgreSQLConnection pgConn = (PostgreSQLConnection) conn;
        Connection underlyingConn = pgConn.getUnderlyingConnection();
        
        try {
            pgConn.beginTransaction();
            
            // Create tables
            for (DatabaseMetadata.TableMetadata table : schema.getTables()) {
                createTable(underlyingConn, table);
            }
            
            // Create foreign keys
            if (schema.getForeignKeys() != null) {
                for (DatabaseMetadata.ForeignKeyMetadata fk : schema.getForeignKeys()) {
                    createForeignKey(underlyingConn, fk);
                }
            }
            
            // Create indices
            if (schema.getIndices() != null) {
                for (DatabaseMetadata.IndexMetadata index : schema.getIndices()) {
                    createIndex(underlyingConn, index);
                }
            }
            
            pgConn.commit();
            log.info("Schema created successfully with {} tables", schema.getTables().size());
            
        } catch (Exception e) {
            pgConn.rollback();
            log.error("Failed to create schema", e);
            throw e;
        }
    }
    
    /**
     * Create a single table.
     */
    private void createTable(Connection conn, DatabaseMetadata.TableMetadata table) throws SQLException {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sql.append(table.getName()).append(" (");
        
        List<String> columnDefs = new ArrayList<>();
        
        for (DatabaseMetadata.ColumnMetadata column : table.getColumns()) {
            StringBuilder colDef = new StringBuilder();
            colDef.append(column.getName()).append(" ").append(column.getDataType());
            
            if (!column.isNullable()) {
                colDef.append(" NOT NULL");
            }
            
            if (column.getDefaultValue() != null) {
                colDef.append(" DEFAULT ").append(column.getDefaultValue());
            }
            
            columnDefs.add(colDef.toString());
        }
        
        // Add primary key constraint
        if (table.getPrimaryKeys() != null && !table.getPrimaryKeys().isEmpty()) {
            String pkConstraint = "PRIMARY KEY (" + String.join(", ", table.getPrimaryKeys()) + ")";
            columnDefs.add(pkConstraint);
        }
        
        // Add unique constraints
        if (table.getUniqueKeys() != null && !table.getUniqueKeys().isEmpty()) {
            String ukConstraint = "UNIQUE (" + String.join(", ", table.getUniqueKeys()) + ")";
            columnDefs.add(ukConstraint);
        }
        
        sql.append(String.join(", ", columnDefs)).append(")");
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql.toString());
            log.debug("Table created: {}", table.getName());
        }
    }
    
    /**
     * Create a foreign key constraint.
     */
    private void createForeignKey(Connection conn, DatabaseMetadata.ForeignKeyMetadata fk) throws SQLException {
        String sql = String.format(
            "ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s) ON DELETE %s ON UPDATE %s",
            fk.getSourceTable(),
            fk.getName(),
            fk.getSourceColumn(),
            fk.getTargetTable(),
            fk.getTargetColumn(),
            fk.getOnDelete() != null ? fk.getOnDelete() : "NO ACTION",
            fk.getOnUpdate() != null ? fk.getOnUpdate() : "NO ACTION"
        );
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.debug("Foreign key created: {}", fk.getName());
        } catch (SQLException e) {
            // Foreign key might already exist, log and continue
            log.debug("Could not create foreign key {}: {}", fk.getName(), e.getMessage());
        }
    }
    
    /**
     * Create an index.
     */
    private void createIndex(Connection conn, DatabaseMetadata.IndexMetadata index) throws SQLException {
        String uniqueKeyword = index.isUnique() ? "UNIQUE" : "";
        String sql = String.format(
            "CREATE %s INDEX IF NOT EXISTS %s ON %s (%s)",
            uniqueKeyword,
            index.getName(),
            index.getTableName(),
            String.join(", ", index.getColumns())
        );
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.debug("Index created: {}", index.getName());
        } catch (SQLException e) {
            // Index might already exist, log and continue
            log.debug("Could not create index {}: {}", index.getName(), e.getMessage());
        }
    }
    
    @Override
    public void dropSchema(IDatabaseConnection conn, String schemaName) throws Exception {
        // This will be implemented in Phase 3 (Schema Management)
        log.info("Schema drop not yet implemented");
    }
    
    @Override
    public List<Row> readData(IDatabaseConnection conn, String tableName, int limit, int offset) throws Exception {
        if (!(conn instanceof PostgreSQLConnection)) {
            throw new IllegalArgumentException("Connection must be a PostgreSQLConnection");
        }
        
        PostgreSQLConnection pgConn = (PostgreSQLConnection) conn;
        Connection underlyingConn = pgConn.getUnderlyingConnection();
        
        List<Row> rows = new ArrayList<>();
        
        // Get column names first
        List<String> columnNames = getColumnNames(underlyingConn, tableName);
        
        // Build query with LIMIT and OFFSET
        String query = String.format(
            "SELECT * FROM %s LIMIT %d OFFSET %d",
            tableName,
            limit,
            offset
        );
        
        try (Statement stmt = underlyingConn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                Row row = Row.builder()
                    .tableName(tableName)
                    .build();
                
                for (String columnName : columnNames) {
                    Object value = rs.getObject(columnName);
                    row.setValue(columnName, value);
                }
                
                rows.add(row);
            }
        }
        
        log.debug("Read {} rows from table {}", rows.size(), tableName);
        return rows;
    }
    
    @Override
    public void writeData(IDatabaseConnection conn, String tableName, List<Row> rows) throws Exception {
        if (rows == null || rows.isEmpty()) {
            log.debug("No rows to write to table {}", tableName);
            return;
        }
        
        if (!(conn instanceof PostgreSQLConnection)) {
            throw new IllegalArgumentException("Connection must be a PostgreSQLConnection");
        }
        
        PostgreSQLConnection pgConn = (PostgreSQLConnection) conn;
        Connection underlyingConn = pgConn.getUnderlyingConnection();
        
        try {
            pgConn.beginTransaction();
            
            // Process rows in batches
            int batchSize = DEFAULT_BATCH_SIZE;
            for (int i = 0; i < rows.size(); i += batchSize) {
                int end = Math.min(i + batchSize, rows.size());
                List<Row> batch = rows.subList(i, end);
                writeBatch(underlyingConn, tableName, batch);
            }
            
            pgConn.commit();
            log.info("Wrote {} rows to table {}", rows.size(), tableName);
            
        } catch (Exception e) {
            pgConn.rollback();
            log.error("Failed to write data to table {}", tableName, e);
            throw e;
        }
    }
    
    /**
     * Write a batch of rows to a table.
     */
    private void writeBatch(Connection conn, String tableName, List<Row> rows) throws SQLException {
        if (rows.isEmpty()) {
            return;
        }
        
        // Get column names from first row
        Row firstRow = rows.get(0);
        List<String> columnNames = new ArrayList<>(firstRow.getValues().keySet());
        
        // Build INSERT statement
        String columns = String.join(", ", columnNames);
        String placeholders = String.join(", ", Collections.nCopies(columnNames.size(), "?"));
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, placeholders);
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Row row : rows) {
                int paramIndex = 1;
                for (String columnName : columnNames) {
                    Object value = row.getValue(columnName);
                    stmt.setObject(paramIndex++, value);
                }
                stmt.addBatch();
            }
            
            int[] results = stmt.executeBatch();
            log.debug("Batch write completed: {} rows", results.length);
        }
    }
    
    /**
     * Get all column names for a table.
     */
    private List<String> getColumnNames(Connection conn, String tableName) throws SQLException {
        List<String> columnNames = new ArrayList<>();
        
        String query = "SELECT column_name FROM information_schema.columns " +
                      "WHERE table_schema = 'public' AND table_name = ? " +
                      "ORDER BY ordinal_position";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    columnNames.add(rs.getString("column_name"));
                }
            }
        }
        
        return columnNames;
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
