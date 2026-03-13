package com.datanymize.database.schema;

import com.datanymize.database.connection.IDatabaseConnection;
import com.datanymize.database.connection.MongoDBConnection;
import com.datanymize.database.model.DatabaseMetadata;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MongoDB-specific schema extractor.
 * Extracts schema metadata from MongoDB databases by querying collection metadata
 * and inferring schema from sample documents.
 * Implements caching with TTL to avoid repeated queries.
 * 
 * Validates Requirements: 2.3
 */
@Slf4j
public class MongoDBSchemaExtractor implements IDatabaseSchemaExtractor {
    
    private static final long DEFAULT_CACHE_TTL_MILLIS = 5 * 60 * 1000; // 5 minutes
    private static final int SAMPLE_SIZE = 100; // Number of documents to sample for schema inference
    
    /**
     * Cache entry for schema metadata.
     */
    private static class CacheEntry<T> {
        private final T data;
        private final long expirationTime;
        
        CacheEntry(T data, long ttlMillis) {
            this.data = data;
            this.expirationTime = System.currentTimeMillis() + ttlMillis;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
    
    private final long cacheTtlMillis;
    private final Map<String, CacheEntry<?>> cache;
    
    /**
     * Create a new MongoDB schema extractor with default cache TTL.
     */
    public MongoDBSchemaExtractor() {
        this(DEFAULT_CACHE_TTL_MILLIS);
    }
    
    /**
     * Create a new MongoDB schema extractor with custom cache TTL.
     * 
     * @param cacheTtlMillis the cache time-to-live in milliseconds
     */
    public MongoDBSchemaExtractor(long cacheTtlMillis) {
        this.cacheTtlMillis = cacheTtlMillis;
        this.cache = new ConcurrentHashMap<>();
    }
    
    @Override
    public List<DatabaseMetadata.TableMetadata> extractTables(IDatabaseConnection conn) throws Exception {
        if (!(conn instanceof MongoDBConnection)) {
            throw new IllegalArgumentException("Connection must be a MongoDBConnection");
        }
        
        MongoDBConnection mongoConn = (MongoDBConnection) conn;
        MongoDatabase database = mongoConn.getDatabase();
        
        String cacheKey = "tables_" + getCacheKeyForConnection(database);
        
        // Check cache
        CacheEntry<?> cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached tables for connection");
            return (List<DatabaseMetadata.TableMetadata>) cached.data;
        }
        
        List<DatabaseMetadata.TableMetadata> tables = new ArrayList<>();
        
        // Iterate through all collections in the database
        for (String collectionName : database.listCollectionNames()) {
            MongoCollection<Document> collection = database.getCollection(collectionName);
            
            // Extract columns from sample documents
            List<DatabaseMetadata.ColumnMetadata> columns = extractColumns(conn, collectionName);
            
            // Get row count
            long rowCount = collection.countDocuments();
            
            // MongoDB always has _id as primary key
            List<String> primaryKeys = Collections.singletonList("_id");
            
            DatabaseMetadata.TableMetadata table = DatabaseMetadata.TableMetadata.builder()
                .name(collectionName)
                .columns(columns)
                .primaryKeys(primaryKeys)
                .uniqueKeys(new ArrayList<>())
                .rowCount(rowCount)
                .build();
            
            tables.add(table);
        }
        
        // Cache the result
        cache.put(cacheKey, new CacheEntry<>(tables, cacheTtlMillis));
        log.info("Extracted {} collections from MongoDB database", tables.size());
        
        return tables;
    }
    
    @Override
    public List<DatabaseMetadata.ColumnMetadata> extractColumns(IDatabaseConnection conn, String tableName) throws Exception {
        if (!(conn instanceof MongoDBConnection)) {
            throw new IllegalArgumentException("Connection must be a MongoDBConnection");
        }
        
        MongoDBConnection mongoConn = (MongoDBConnection) conn;
        MongoDatabase database = mongoConn.getDatabase();
        
        String cacheKey = "columns_" + getCacheKeyForConnection(database) + "_" + tableName;
        
        // Check cache
        CacheEntry<?> cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached columns for collection: {}", tableName);
            return (List<DatabaseMetadata.ColumnMetadata>) cached.data;
        }
        
        List<DatabaseMetadata.ColumnMetadata> columns = new ArrayList<>();
        
        try {
            MongoCollection<Document> collection = database.getCollection(tableName);
            
            // Map to track field types across sampled documents
            Map<String, String> fieldTypes = new LinkedHashMap<>();
            
            // Sample first SAMPLE_SIZE documents to infer schema
            int count = 0;
            for (Document doc : collection.find().limit(SAMPLE_SIZE)) {
                for (String key : doc.keySet()) {
                    if (!fieldTypes.containsKey(key)) {
                        Object value = doc.get(key);
                        String type = inferBsonType(value);
                        fieldTypes.put(key, type);
                    }
                }
                count++;
            }
            
            // Convert field types to ColumnMetadata
            for (Map.Entry<String, String> entry : fieldTypes.entrySet()) {
                String fieldName = entry.getKey();
                String dataType = entry.getValue();
                
                DatabaseMetadata.ColumnMetadata column = DatabaseMetadata.ColumnMetadata.builder()
                    .name(fieldName)
                    .dataType(dataType)
                    .nullable(true)  // MongoDB fields are generally nullable
                    .defaultValue(null)
                    .isPrimaryKey("_id".equals(fieldName))
                    .isUnique("_id".equals(fieldName))
                    .build();
                
                columns.add(column);
            }
            
            // Cache the result
            cache.put(cacheKey, new CacheEntry<>(columns, cacheTtlMillis));
            log.debug("Extracted {} fields from collection: {} (sampled {} documents)", 
                columns.size(), tableName, count);
            
        } catch (Exception e) {
            log.error("Failed to extract columns from collection: {}", tableName, e);
            throw e;
        }
        
        return columns;
    }
    
    @Override
    public List<DatabaseMetadata.ForeignKeyMetadata> extractForeignKeys(IDatabaseConnection conn) throws Exception {
        if (!(conn instanceof MongoDBConnection)) {
            throw new IllegalArgumentException("Connection must be a MongoDBConnection");
        }
        
        // MongoDB doesn't have traditional foreign keys
        // Return empty list as per MongoDB design
        log.debug("MongoDB does not support traditional foreign keys");
        return new ArrayList<>();
    }
    
    @Override
    public List<DatabaseMetadata.IndexMetadata> extractIndices(IDatabaseConnection conn) throws Exception {
        if (!(conn instanceof MongoDBConnection)) {
            throw new IllegalArgumentException("Connection must be a MongoDBConnection");
        }
        
        MongoDBConnection mongoConn = (MongoDBConnection) conn;
        MongoDatabase database = mongoConn.getDatabase();
        
        String cacheKey = "indices_" + getCacheKeyForConnection(database);
        
        // Check cache
        CacheEntry<?> cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached indices for connection");
            return (List<DatabaseMetadata.IndexMetadata>) cached.data;
        }
        
        List<DatabaseMetadata.IndexMetadata> indices = new ArrayList<>();
        
        try {
            // Iterate through all collections and extract their indices
            for (String collectionName : database.listCollectionNames()) {
                MongoCollection<Document> collection = database.getCollection(collectionName);
                
                // Get all indices for this collection
                for (Document indexDoc : collection.listIndexes()) {
                    String indexName = indexDoc.getString("name");
                    Document keyDoc = (Document) indexDoc.get("key");
                    
                    if (keyDoc != null) {
                        // Extract column names from the index key document
                        List<String> columns = new ArrayList<>(keyDoc.keySet());
                        
                        // Check if index is unique
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
            
            // Cache the result
            cache.put(cacheKey, new CacheEntry<>(indices, cacheTtlMillis));
            log.info("Extracted {} indices from MongoDB database", indices.size());
            
        } catch (Exception e) {
            log.error("Failed to extract indices from MongoDB database", e);
            throw e;
        }
        
        return indices;
    }
    
    /**
     * Infer BSON type from a Java object.
     * Maps BSON types to standard data type strings.
     * 
     * @param value the BSON value to infer type from
     * @return the inferred data type as a string
     */
    private String inferBsonType(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "String";
        } else if (value instanceof Integer) {
            return "Int32";
        } else if (value instanceof Long) {
            return "Int64";
        } else if (value instanceof Double) {
            return "Double";
        } else if (value instanceof Boolean) {
            return "Boolean";
        } else if (value instanceof java.util.Date) {
            return "Date";
        } else if (value instanceof Document) {
            return "Object";
        } else if (value instanceof java.util.List) {
            return "Array";
        } else if (value instanceof org.bson.types.ObjectId) {
            return "ObjectId";
        } else if (value instanceof byte[]) {
            return "Binary";
        } else {
            return "Mixed";
        }
    }
    
    /**
     * Generate a cache key for a connection based on its database name.
     * 
     * @param database the MongoDB database
     * @return a cache key string
     */
    private String getCacheKeyForConnection(MongoDatabase database) {
        return database.getName();
    }
    
    /**
     * Clear the cache.
     */
    public void clearCache() {
        cache.clear();
        log.debug("Schema extraction cache cleared");
    }
    
    /**
     * Clear expired cache entries.
     */
    public void clearExpiredCache() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        log.debug("Expired schema extraction cache entries cleared");
    }
}
