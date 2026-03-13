package com.datanymize.database.connection;

import com.datanymize.database.model.ConnectionConfig;
import com.datanymize.database.model.DatabaseMetadata;
import com.datanymize.database.model.Row;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertManyOptions;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * MongoDB database driver implementation.
 * Handles connection creation, schema extraction, and data operations for MongoDB.
 * 
 * Validates Requirements: 1.3, 2.3
 */
@Slf4j
public class MongoDBDriver implements IDatabaseDriver {
    
    private static final String DATABASE_TYPE = "mongodb";
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int DEFAULT_PORT = 27017;
    
    private MongoClient mongoClient;
    
    @Override
    public IDatabaseConnection createConnection(ConnectionConfig config) throws Exception {
        validateConfig(config);
        
        // Build MongoDB connection string
        MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder();
        
        // Set server address
        ServerAddress serverAddress = new ServerAddress(config.getHost(), config.getPort());
        settingsBuilder.applyToClusterSettings(builder -> 
            builder.hosts(Collections.singletonList(serverAddress))
        );
        
        // Set credentials if provided
        if (config.getUsername() != null && !config.getUsername().isEmpty()) {
            MongoCredential credential = MongoCredential.createScramSha256Credential(
                config.getUsername(),
                config.getDatabase(),
                config.getPassword().toCharArray()
            );
            settingsBuilder.credential(credential);
        }
        
        // Set connection timeout
        int timeoutMs = (int) (config.getConnectionTimeoutSeconds() * 1000L);
        settingsBuilder.applyToSocketSettings(builder ->
            builder.connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                   .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        );
        
        // Set TLS/SSL configuration
        if (config.isUseTLS()) {
            settingsBuilder.applyToSslSettings(builder -> {
                builder.enabled(true);
                if (!config.isVerifyCertificate()) {
                    builder.invalidHostNameAllowed(true);
                }
            });
        }
        
        // Create MongoDB client
        this.mongoClient = MongoClients.create(settingsBuilder.build());
        
        // Get database
        MongoDatabase database = mongoClient.getDatabase(config.getDatabase());
        
        // Verify connection by running a ping command
        try {
            database.runCommand(new Document("ping", 1));
            log.info("MongoDB connection established to {}:{}/{}", 
                config.getHost(), config.getPort(), config.getDatabase());
        } catch (Exception e) {
            mongoClient.close();
            throw new Exception("Failed to connect to MongoDB: " + e.getMessage(), e);
        }
        
        return new MongoDBConnection(mongoClient, database);
    }
    
    @Override
    public String getDatabaseType() {
        return DATABASE_TYPE;
    }
    
    @Override
    public boolean validateReadOnlyAccess(IDatabaseConnection conn) throws Exception {
        if (!(conn instanceof MongoDBConnection)) {
            throw new IllegalArgumentException("Connection must be a MongoDBConnection");
        }
        
        MongoDBConnection mongoConn = (MongoDBConnection) conn;
        MongoDatabase database = mongoConn.getDatabase();
        
        // Test read access
        try {
            database.listCollectionNames().first();
            log.debug("Read access verified");
        } catch (Exception e) {
            log.error("Read access test failed: {}", e.getMessage());
            return false;
        }
        
        // MongoDB doesn't have a simple way to test write permissions without actually writing
        // We'll assume read-only if the user has read permissions
        // In a real scenario, you might check user roles via admin database
        return true;
    }
    
    @Override
    public DatabaseMetadata extractSchema(IDatabaseConnection conn) throws Exception {
        if (!(conn instanceof MongoDBConnection)) {
            throw new IllegalArgumentException("Connection must be a MongoDBConnection");
        }
        
        MongoDBConnection mongoConn = (MongoDBConnection) conn;
        MongoDatabase database = mongoConn.getDatabase();
        
        String databaseName = database.getName();
        
        // Extract collections and their metadata
        List<DatabaseMetadata.TableMetadata> tables = extractCollections(database);
        
        // Extract indices
        List<DatabaseMetadata.IndexMetadata> indices = extractIndices(database);
        
        log.info("Schema extracted for MongoDB database: {} with {} collections", 
            databaseName, tables.size());
        
        return DatabaseMetadata.builder()
            .databaseName(databaseName)
            .databaseType(DATABASE_TYPE)
            .tables(tables)
            .foreignKeys(new ArrayList<>())  // MongoDB doesn't have traditional FKs
            .indices(indices)
            .build();
    }
    
    /**
     * Extract all collections and their metadata from the database.
     */
    private List<DatabaseMetadata.TableMetadata> extractCollections(MongoDatabase database) {
        List<DatabaseMetadata.TableMetadata> tables = new ArrayList<>();
        
        for (String collectionName : database.listCollectionNames()) {
            MongoCollection<Document> collection = database.getCollection(collectionName);
            
            // Extract columns from sample documents
            List<DatabaseMetadata.ColumnMetadata> columns = extractColumnsFromCollection(collection);
            
            // Get row count
            long rowCount = collection.countDocuments();
            
            DatabaseMetadata.TableMetadata table = DatabaseMetadata.TableMetadata.builder()
                .name(collectionName)
                .columns(columns)
                .primaryKeys(Collections.singletonList("_id"))  // MongoDB always has _id
                .uniqueKeys(new ArrayList<>())
                .rowCount(rowCount)
                .build();
            
            tables.add(table);
        }
        
        return tables;
    }
    
    /**
     * Extract column metadata from sample documents in a collection.
     */
    private List<DatabaseMetadata.ColumnMetadata> extractColumnsFromCollection(
            MongoCollection<Document> collection) {
        
        Map<String, String> columnTypes = new LinkedHashMap<>();
        
        // Sample first 100 documents to infer schema
        int sampleSize = 100;
        int count = 0;
        
        for (Document doc : collection.find().limit(sampleSize)) {
            for (String key : doc.keySet()) {
                if (!columnTypes.containsKey(key)) {
                    Object value = doc.get(key);
                    String type = inferBsonType(value);
                    columnTypes.put(key, type);
                }
            }
            count++;
            if (count >= sampleSize) {
                break;
            }
        }
        
        // Convert to ColumnMetadata
        List<DatabaseMetadata.ColumnMetadata> columns = new ArrayList<>();
        for (Map.Entry<String, String> entry : columnTypes.entrySet()) {
            DatabaseMetadata.ColumnMetadata column = DatabaseMetadata.ColumnMetadata.builder()
                .name(entry.getKey())
                .dataType(entry.getValue())
                .nullable(true)  // MongoDB fields are generally nullable
                .defaultValue(null)
                .isPrimaryKey("_id".equals(entry.getKey()))
                .isUnique("_id".equals(entry.getKey()))
                .build();
            
            columns.add(column);
        }
        
        return columns;
    }
    
    /**
     * Infer BSON type from a Java object.
     */
    private String inferBsonType(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "string";
        } else if (value instanceof Integer) {
            return "int32";
        } else if (value instanceof Long) {
            return "int64";
        } else if (value instanceof Double) {
            return "double";
        } else if (value instanceof Boolean) {
            return "bool";
        } else if (value instanceof java.util.Date) {
            return "date";
        } else if (value instanceof Document) {
            return "object";
        } else if (value instanceof java.util.List) {
            return "array";
        } else if (value instanceof org.bson.types.ObjectId) {
            return "objectId";
        } else {
            return "mixed";
        }
    }
    
    /**
     * Extract indices from all collections in the database.
     */
    private List<DatabaseMetadata.IndexMetadata> extractIndices(MongoDatabase database) {
        List<DatabaseMetadata.IndexMetadata> indices = new ArrayList<>();
        
        for (String collectionName : database.listCollectionNames()) {
            MongoCollection<Document> collection = database.getCollection(collectionName);
            
            // Get all indices for this collection
            for (Document indexDoc : collection.listIndexes()) {
                String indexName = indexDoc.getString("name");
                Document keyDoc = (Document) indexDoc.get("key");
                
                if (keyDoc != null) {
                    List<String> columns = new ArrayList<>(keyDoc.keySet());
                    boolean unique = indexDoc.getBoolean("unique", false);
                    
                    DatabaseMetadata.IndexMetadata index = DatabaseMetadata.IndexMetadata.builder()
                        .name(indexName)
                        .tableName(collectionName)
                        .columns(columns)
                        .unique(unique)
                        .build();
                    
                    indices.add(index);
                }
            }
        }
        
        return indices;
    }
    
    @Override
    public void createSchema(IDatabaseConnection conn, DatabaseMetadata schema) throws Exception {
        if (!(conn instanceof MongoDBConnection)) {
            throw new IllegalArgumentException("Connection must be a MongoDBConnection");
        }
        
        MongoDBConnection mongoConn = (MongoDBConnection) conn;
        MongoDatabase database = mongoConn.getDatabase();
        
        try {
            // Create collections
            for (DatabaseMetadata.TableMetadata table : schema.getTables()) {
                createCollection(database, table);
            }
            
            // Create indices
            if (schema.getIndices() != null) {
                for (DatabaseMetadata.IndexMetadata index : schema.getIndices()) {
                    createIndex(database, index);
                }
            }
            
            log.info("Schema created successfully with {} collections", schema.getTables().size());
            
        } catch (Exception e) {
            log.error("Failed to create schema", e);
            throw e;
        }
    }
    
    /**
     * Create a collection.
     */
    private void createCollection(MongoDatabase database, DatabaseMetadata.TableMetadata table) {
        try {
            database.createCollection(table.getName());
            log.debug("Collection created: {}", table.getName());
        } catch (Exception e) {
            // Collection might already exist
            log.debug("Could not create collection {}: {}", table.getName(), e.getMessage());
        }
    }
    
    /**
     * Create an index on a collection.
     */
    private void createIndex(MongoDatabase database, DatabaseMetadata.IndexMetadata index) {
        try {
            MongoCollection<Document> collection = database.getCollection(index.getTableName());
            
            // Build index key document
            Document indexKey = new Document();
            for (String column : index.getColumns()) {
                indexKey.append(column, 1);  // 1 for ascending order
            }
            
            // Create index with options
            com.mongodb.client.model.IndexOptions options = new com.mongodb.client.model.IndexOptions();
            if (index.isUnique()) {
                options.unique(true);
            }
            options.name(index.getName());
            
            collection.createIndex(indexKey, options);
            log.debug("Index created: {}", index.getName());
        } catch (Exception e) {
            // Index might already exist
            log.debug("Could not create index {}: {}", index.getName(), e.getMessage());
        }
    }
    
    @Override
    public void dropSchema(IDatabaseConnection conn, String schemaName) throws Exception {
        if (!(conn instanceof MongoDBConnection)) {
            throw new IllegalArgumentException("Connection must be a MongoDBConnection");
        }
        
        MongoDBConnection mongoConn = (MongoDBConnection) conn;
        MongoDatabase database = mongoConn.getDatabase();
        
        try {
            database.drop();
            log.info("Database dropped: {}", schemaName);
        } catch (Exception e) {
            log.error("Failed to drop database {}", schemaName, e);
            throw e;
        }
    }
    
    @Override
    public List<Row> readData(IDatabaseConnection conn, String tableName, int limit, int offset) 
            throws Exception {
        
        if (!(conn instanceof MongoDBConnection)) {
            throw new IllegalArgumentException("Connection must be a MongoDBConnection");
        }
        
        MongoDBConnection mongoConn = (MongoDBConnection) conn;
        MongoDatabase database = mongoConn.getDatabase();
        MongoCollection<Document> collection = database.getCollection(tableName);
        
        List<Row> rows = new ArrayList<>();
        
        // Query with skip and limit
        for (Document doc : collection.find().skip(offset).limit(limit)) {
            Row row = Row.builder()
                .tableName(tableName)
                .build();
            
            // Convert BSON document to Row
            for (String key : doc.keySet()) {
                Object value = doc.get(key);
                row.setValue(key, convertBsonValue(value));
            }
            
            rows.add(row);
        }
        
        log.debug("Read {} documents from collection {}", rows.size(), tableName);
        return rows;
    }
    
    /**
     * Convert BSON values to Java objects suitable for storage.
     */
    private Object convertBsonValue(Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Document) {
            // Convert nested documents to JSON string for storage
            return ((Document) value).toJson();
        } else if (value instanceof java.util.List) {
            // Convert lists to JSON string for storage
            return value.toString();
        } else {
            return value;
        }
    }
    
    @Override
    public void writeData(IDatabaseConnection conn, String tableName, List<Row> rows) 
            throws Exception {
        
        if (rows == null || rows.isEmpty()) {
            log.debug("No rows to write to collection {}", tableName);
            return;
        }
        
        if (!(conn instanceof MongoDBConnection)) {
            throw new IllegalArgumentException("Connection must be a MongoDBConnection");
        }
        
        MongoDBConnection mongoConn = (MongoDBConnection) conn;
        MongoDatabase database = mongoConn.getDatabase();
        MongoCollection<Document> collection = database.getCollection(tableName);
        
        try {
            // Process rows in batches
            int batchSize = DEFAULT_BATCH_SIZE;
            for (int i = 0; i < rows.size(); i += batchSize) {
                int end = Math.min(i + batchSize, rows.size());
                List<Row> batch = rows.subList(i, end);
                writeBatch(collection, batch);
            }
            
            log.info("Wrote {} documents to collection {}", rows.size(), tableName);
            
        } catch (Exception e) {
            log.error("Failed to write data to collection {}", tableName, e);
            throw e;
        }
    }
    
    /**
     * Write a batch of rows to a collection.
     */
    private void writeBatch(MongoCollection<Document> collection, List<Row> rows) {
        List<Document> documents = new ArrayList<>();
        
        for (Row row : rows) {
            Document doc = new Document();
            
            // Convert Row values to BSON Document
            for (Map.Entry<String, Object> entry : row.getValues().entrySet()) {
                doc.append(entry.getKey(), entry.getValue());
            }
            
            documents.add(doc);
        }
        
        // Insert all documents in batch
        InsertManyOptions options = new InsertManyOptions().ordered(false);
        collection.insertMany(documents, options);
        
        log.debug("Batch write completed: {} documents", documents.size());
    }
    
    /**
     * Close the MongoDB client.
     */
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            log.info("MongoDB client closed");
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
        if (config.getConnectionTimeoutSeconds() <= 0) {
            throw new IllegalArgumentException("Connection timeout must be positive");
        }
    }
}
