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
 * MySQL database driver implementation.
 * Handles connection creation, pooling, schema extraction, and data operations.
 * 
 * Validates Requirements: 1.2, 2.2
 */
@Slf4j
public class MySQLDriver implements IDatabaseDriver {
    
    private static final String DATABASE_TYPE = "mysql";
    private static final String JDBC_URL_FORMAT = "jdbc:mysql://%s:%d/%s";
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
        
        // Validation query for MySQL
        hikariConfig.setConnectionTestQuery("SELECT 1");
        
        // Pool settings
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setIdleTimeout(600000); // 10 minutes
        hikariConfig.setMaxLifetime(1800000); // 30 minutes
        
        // TLS/SSL configuration
        Properties props = new Properties();
        if (config.isUseTLS()) {
            props.setProperty("useSSL", "true");
            if (!config.isVerifyCertificate()) {
                props.setProperty("requireSSL", "true");
                props.setProperty("verifyServerCertificate", "false");
            } else {
                props.setProperty("requireSSL", "true");
                props.setProperty("verifyServerCertificate", "true");
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
        
        log.info("MySQL connection established to {}:{}/{}", 
            config.getHost(), config.getPort(), config.getDatabase());
        
        return new MySQLConnection(conn);
    }
    
    @Override
    public String getDatabaseType() {
        return DATABASE_TYPE;
    }
    
    @Override
    public boolean validateReadOnlyAccess(IDatabaseConnection conn) throws Exception {
        if (!(conn instanceof MySQLConnection)) {
            throw new IllegalArgumentException("Connection must be a MySQLConnection");
        }
        
        MySQLConnection mysqlConn = (MySQLConnection) conn;
        
        // Test SELECT is allowed
        try (Statement stmt = mysqlConn.getUnderlyingConnection().createStatement()) {
            stmt.execute("SELECT 1");
        }
        
        // Test that INSERT is blocked or fails
        try (Statement stmt = mysqlConn.getUnderlyingConnection().createStatement()) {
            stmt.execute("CREATE TEMPORARY TABLE test_write (id INT)");
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
        if (!(conn instanceof MySQLConnection)) {
            throw new IllegalArgumentException("Connection must be a MySQLConnection");
        }
        
        MySQLConnection mysqlConn = (MySQLConnection) conn;
        Connection underlyingConn = mysqlConn.getUnderlyingConnection();
        
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
        
        String query = "SELECT TABLE_NAME FROM information_schema.TABLES " +
                      "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE' " +
                      "ORDER BY TABLE_NAME";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
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
        
        String query = "SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT " +
                      "FROM information_schema.COLUMNS " +
                      "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? " +
                      "ORDER BY ORDINAL_POSITION";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String columnName = rs.getString("COLUMN_NAME");
                    String dataType = rs.getString("COLUMN_TYPE");
                    boolean nullable = "YES".equals(rs.getString("IS_NULLABLE"));
                    String defaultValue = rs.getString("COLUMN_DEFAULT");
                    
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
        
        String query = "SELECT COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                      "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CONSTRAINT_NAME = 'PRIMARY' " +
                      "ORDER BY ORDINAL_POSITION";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    primaryKeys.add(rs.getString("COLUMN_NAME"));
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
        
        String query = "SELECT COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                      "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CONSTRAINT_NAME != 'PRIMARY' " +
                      "ORDER BY ORDINAL_POSITION";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    uniqueKeys.add(rs.getString("COLUMN_NAME"));
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
        
        String query = "SELECT CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, " +
                      "REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME " +
                      "FROM information_schema.KEY_COLUMN_USAGE " +
                      "WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                String constraintName = rs.getString("CONSTRAINT_NAME");
                String sourceTable = rs.getString("TABLE_NAME");
                String sourceColumn = rs.getString("COLUMN_NAME");
                String targetTable = rs.getString("REFERENCED_TABLE_NAME");
                String targetColumn = rs.getString("REFERENCED_COLUMN_NAME");
                
                // Get ON DELETE and ON UPDATE rules
                String[] rules = getConstraintRules(conn, sourceTable, constraintName);
                String onDelete = rules[0];
                String onUpdate = rules[1];
                
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
     * Get the ON DELETE and ON UPDATE rules for a constraint.
     */
    private String[] getConstraintRules(Connection conn, String tableName, String constraintName) throws SQLException {
        String[] rules = {"NO ACTION", "NO ACTION"};
        
        String query = "SELECT UPDATE_RULE, DELETE_RULE FROM information_schema.REFERENTIAL_CONSTRAINTS " +
                      "WHERE CONSTRAINT_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CONSTRAINT_NAME = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tableName);
            stmt.setString(2, constraintName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    rules[1] = rs.getString("DELETE_RULE");  // onDelete
                    rules[0] = rs.getString("UPDATE_RULE");  // onUpdate
                }
            }
        }
        
        return rules;
    }
    
    /**
     * Extract indices from the database.
     */
    private List<DatabaseMetadata.IndexMetadata> extractIndices(Connection conn) throws SQLException {
        List<DatabaseMetadata.IndexMetadata> indices = new ArrayList<>();
        
        String query = "SELECT INDEX_NAME, TABLE_NAME, COLUMN_NAME, SEQ_IN_INDEX, NON_UNIQUE " +
                      "FROM information_schema.STATISTICS " +
                      "WHERE TABLE_SCHEMA = DATABASE() AND INDEX_NAME != 'PRIMARY' " +
                      "ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            Map<String, DatabaseMetadata.IndexMetadata> indexMap = new LinkedHashMap<>();
            
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                String tableName = rs.getString("TABLE_NAME");
                String columnName = rs.getString("COLUMN_NAME");
                boolean unique = rs.getInt("NON_UNIQUE") == 0;
                
                String key = tableName + "." + indexName;
                
                if (!indexMap.containsKey(key)) {
                    DatabaseMetadata.IndexMetadata index = DatabaseMetadata.IndexMetadata.builder()
                        .name(indexName)
                        .tableName(tableName)
                        .columns(new ArrayList<>())
                        .unique(unique)
                        .build();
                    indexMap.put(key, index);
                }
                
                indexMap.get(key).getColumns().add(columnName);
            }
            
            indices.addAll(indexMap.values());
        }
        
        return indices;
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
        if (!(conn instanceof MySQLConnection)) {
            throw new IllegalArgumentException("Connection must be a MySQLConnection");
        }
        
        MySQLConnection mysqlConn = (MySQLConnection) conn;
        Connection underlyingConn = mysqlConn.getUnderlyingConnection();
        
        try {
            mysqlConn.beginTransaction();
            
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
            
            mysqlConn.commit();
            log.info("Schema created successfully with {} tables", schema.getTables().size());
            
        } catch (Exception e) {
            mysqlConn.rollback();
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
            "CREATE %s INDEX %s ON %s (%s)",
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
        if (!(conn instanceof MySQLConnection)) {
            throw new IllegalArgumentException("Connection must be a MySQLConnection");
        }
        
        MySQLConnection mysqlConn = (MySQLConnection) conn;
        Connection underlyingConn = mysqlConn.getUnderlyingConnection();
        
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
        
        if (!(conn instanceof MySQLConnection)) {
            throw new IllegalArgumentException("Connection must be a MySQLConnection");
        }
        
        MySQLConnection mysqlConn = (MySQLConnection) conn;
        Connection underlyingConn = mysqlConn.getUnderlyingConnection();
        
        try {
            mysqlConn.beginTransaction();
            
            // Process rows in batches
            int batchSize = DEFAULT_BATCH_SIZE;
            for (int i = 0; i < rows.size(); i += batchSize) {
                int end = Math.min(i + batchSize, rows.size());
                List<Row> batch = rows.subList(i, end);
                writeBatch(underlyingConn, tableName, batch);
            }
            
            mysqlConn.commit();
            log.info("Wrote {} rows to table {}", rows.size(), tableName);
            
        } catch (Exception e) {
            mysqlConn.rollback();
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
        
        String query = "SELECT COLUMN_NAME FROM information_schema.COLUMNS " +
                      "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? " +
                      "ORDER BY ORDINAL_POSITION";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    columnNames.add(rs.getString("COLUMN_NAME"));
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
            log.info("MySQL connection pool closed");
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
