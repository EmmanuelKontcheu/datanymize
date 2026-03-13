package com.datanymize.database.schema;

import com.datanymize.database.connection.IDatabaseConnection;
import com.datanymize.database.connection.IDatabaseDriver;
import com.datanymize.database.model.DatabaseMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of schema synchronization.
 * Creates target database schemas from source schemas while respecting foreign key constraints.
 * 
 * Validates Requirements: 2.5
 */
@Slf4j
@Component
public class SchemaSynchronizer implements ISchemaSynchronizer {
    
    private final Map<String, IDatabaseDriver> driverRegistry;
    
    public SchemaSynchronizer(Map<String, IDatabaseDriver> driverRegistry) {
        this.driverRegistry = driverRegistry != null ? driverRegistry : new HashMap<>();
    }
    
    @Override
    public void syncSchema(IDatabaseConnection sourceConn, IDatabaseConnection targetConn, DatabaseMetadata sourceSchema) throws Exception {
        log.info("Starting schema synchronization from {} to {}", sourceSchema.getDatabaseName(), targetConn.getMetadata().getDatabaseName());
        
        try {
            // Create schema in target database
            createSchema(targetConn, sourceSchema);
            log.info("Schema synchronization completed successfully");
        } catch (Exception e) {
            log.error("Schema synchronization failed", e);
            throw new Exception("Failed to synchronize schema: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void createSchema(IDatabaseConnection targetConn, DatabaseMetadata schema) throws Exception {
        log.info("Creating schema in target database: {}", schema.getDatabaseName());
        
        if (schema == null || schema.getTables() == null || schema.getTables().isEmpty()) {
            log.warn("Schema is empty, nothing to create");
            return;
        }
        
        try {
            // Get the appropriate driver for the target database
            String databaseType = schema.getDatabaseType();
            IDatabaseDriver driver = driverRegistry.get(databaseType);
            
            if (driver == null) {
                throw new Exception("No driver found for database type: " + databaseType);
            }
            
            // Use driver to create schema
            driver.createSchema(targetConn, schema);
            
            log.info("Schema created successfully in target database");
        } catch (Exception e) {
            log.error("Failed to create schema", e);
            throw new Exception("Failed to create schema: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void dropSchema(IDatabaseConnection targetConn, String schemaName) throws Exception {
        log.info("Dropping schema from target database: {}", schemaName);
        
        if (schemaName == null || schemaName.trim().isEmpty()) {
            throw new Exception("Schema name cannot be null or empty");
        }
        
        try {
            // Get database type from connection metadata
            String databaseType = targetConn.getMetadata().getDatabaseType();
            IDatabaseDriver driver = driverRegistry.get(databaseType);
            
            if (driver == null) {
                throw new Exception("No driver found for database type: " + databaseType);
            }
            
            // Use driver to drop schema
            driver.dropSchema(targetConn, schemaName);
            
            log.info("Schema dropped successfully from target database");
        } catch (Exception e) {
            log.error("Failed to drop schema", e);
            throw new Exception("Failed to drop schema: " + e.getMessage(), e);
        }
    }
}
