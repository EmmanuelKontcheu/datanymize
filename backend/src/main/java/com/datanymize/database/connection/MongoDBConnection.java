package com.datanymize.database.connection;

import com.datanymize.database.model.DatabaseMetadata;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;

/**
 * MongoDB database connection implementation.
 * Wraps MongoDB Java Driver client and database connections.
 * 
 * Validates Requirements: 1.3, 2.3
 */
@Slf4j
public class MongoDBConnection implements IDatabaseConnection {
    
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private boolean inTransaction = false;
    
    /**
     * Create a MongoDB connection.
     * 
     * @param mongoClient the MongoDB client
     * @param database the MongoDB database
     */
    public MongoDBConnection(MongoClient mongoClient, MongoDatabase database) {
        this.mongoClient = mongoClient;
        this.database = database;
    }
    
    /**
     * Get the underlying MongoDB database.
     */
    public MongoDatabase getDatabase() {
        return database;
    }
    
    /**
     * Get the underlying MongoDB client.
     */
    public MongoClient getMongoClient() {
        return mongoClient;
    }
    
    @Override
    public java.sql.ResultSet executeQuery(String query) throws SQLException {
        throw new SQLException("MongoDB does not support SQL queries. Use MongoDB query API instead.");
    }
    
    @Override
    public int executeUpdate(String query) throws SQLException {
        throw new SQLException("MongoDB does not support SQL updates. Use MongoDB update API instead.");
    }
    
    @Override
    public void beginTransaction() throws SQLException {
        // MongoDB transactions are handled at the session level
        // For now, we'll just set a flag
        inTransaction = true;
        log.debug("Transaction started");
    }
    
    @Override
    public void commit() throws SQLException {
        // MongoDB auto-commits in single-threaded context
        inTransaction = false;
        log.debug("Transaction committed");
    }
    
    @Override
    public void rollback() throws SQLException {
        // MongoDB doesn't support rollback in the same way as SQL databases
        // This is a limitation of MongoDB's transaction model
        inTransaction = false;
        log.debug("Transaction rolled back (note: MongoDB has limited rollback support)");
    }
    
    @Override
    public void close() throws SQLException {
        if (mongoClient != null) {
            mongoClient.close();
            log.info("MongoDB connection closed");
        }
    }
    
    @Override
    public boolean isConnected() throws SQLException {
        try {
            // Try to execute a simple command to verify connection
            database.runCommand(new org.bson.Document("ping", 1));
            return true;
        } catch (Exception e) {
            log.debug("Connection check failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean validate() throws SQLException {
        return isConnected();
    }
    
    @Override
    public DatabaseMetadata getMetadata() throws SQLException {
        throw new SQLException("Use MongoDBDriver.extractSchema() to get metadata");
    }
}
