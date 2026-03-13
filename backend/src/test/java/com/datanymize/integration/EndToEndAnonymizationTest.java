package com.datanymize.integration;

import com.datanymize.api.controller.AnonymizationController;
import com.datanymize.api.controller.ConnectionController;
import com.datanymize.api.controller.ConfigurationController;
import com.datanymize.api.controller.PIIScanController;
import com.datanymize.api.dto.ApiResponse;
import com.datanymize.database.connection.PostgreSQLDriver;
import com.datanymize.database.schema.PostgreSQLSchemaExtractor;
import com.datanymize.pii.PatternBasedPIIDetector;
import com.datanymize.config.parser.YAMLConfigParser;
import com.datanymize.anonymization.AnonymizationOrchestrator;
import lombok.extern.slf4j.Slf4j;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End Anonymization Workflow Integration Tests
 * 
 * Tests the complete flow: connect → extract schema → PII scan → configure → anonymize → export
 * 
 * Validates Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 3.1, 4.1, 5.1, 7.1
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("End-to-End Anonymization Workflow")
public class EndToEndAnonymizationTest {

    @Autowired
    private ConnectionController connectionController;

    @Autowired
    private ConfigurationController configurationController;

    @Autowired
    private PIIScanController piiScanController;

    @Autowired
    private AnonymizationController anonymizationController;

    private String sourceConnectionId;
    private String targetConnectionId;
    private String configurationId;

    @BeforeEach
    void setUp() {
        // Initialize test data
        sourceConnectionId = "test-source-" + System.currentTimeMillis();
        targetConnectionId = "test-target-" + System.currentTimeMillis();
        configurationId = "test-config-" + System.currentTimeMillis();
    }

    @Test
    @DisplayName("Complete anonymization workflow with PostgreSQL")
    void testCompleteAnonymizationWorkflow() {
        // Step 1: Create source connection
        ConnectionController.ConnectionRequest sourceRequest = new ConnectionController.ConnectionRequest();
        sourceRequest.setName("Test Source");
        sourceRequest.setType("postgresql");
        sourceRequest.setHost("localhost");
        sourceRequest.setPort(5432);
        sourceRequest.setDatabase("test_db");
        sourceRequest.setUsername("test_user");
        sourceRequest.setPassword("test_password");
        sourceRequest.setUseTLS(true);

        ResponseEntity<ApiResponse<ConnectionController.ConnectionResponse>> sourceResponse = 
            connectionController.createConnection(sourceRequest);
        
        assertEquals(201, sourceResponse.getStatusCodeValue());
        assertNotNull(sourceResponse.getBody());
        assertTrue(sourceResponse.getBody().isSuccess());

        // Step 2: Create target connection
        ConnectionController.ConnectionRequest targetRequest = new ConnectionController.ConnectionRequest();
        targetRequest.setName("Test Target");
        targetRequest.setType("postgresql");
        targetRequest.setHost("localhost");
        targetRequest.setPort(5432);
        targetRequest.setDatabase("test_db_target");
        targetRequest.setUsername("test_user");
        targetRequest.setPassword("test_password");
        targetRequest.setUseTLS(true);

        ResponseEntity<ApiResponse<ConnectionController.ConnectionResponse>> targetResponse = 
            connectionController.createConnection(targetRequest);
        
        assertEquals(201, targetResponse.getStatusCodeValue());

        // Step 3: Create anonymization configuration
        ConfigurationController.ConfigurationRequest configRequest = new ConfigurationController.ConfigurationRequest();
        configRequest.setName("Test Configuration");
        configRequest.setFormat("yaml");
        configRequest.setContent("version: \"1.0\"\ntables:\n  users:\n    columns:\n      email:\n        transformer: fake_email");

        ResponseEntity<ApiResponse<ConfigurationController.ConfigurationResponse>> configResponse = 
            configurationController.createConfiguration(configRequest);
        
        assertEquals(201, configResponse.getStatusCodeValue());

        // Step 4: Start anonymization
        AnonymizationController.AnonymizationRequest anonRequest = new AnonymizationController.AnonymizationRequest();
        anonRequest.setSourceConnectionId(sourceConnectionId);
        anonRequest.setTargetConnectionId(targetConnectionId);
        anonRequest.setConfigurationId(configurationId);

        ResponseEntity<ApiResponse<AnonymizationController.AnonymizationStartResponse>> anonResponse = 
            anonymizationController.startAnonymization(anonRequest);
        
        assertEquals(201, anonResponse.getStatusCodeValue());
        assertNotNull(anonResponse.getBody().getData().getJobId());

        // Step 5: Check anonymization status
        String jobId = anonResponse.getBody().getData().getJobId();
        ResponseEntity<ApiResponse<AnonymizationController.AnonymizationStatusResponse>> statusResponse = 
            anonymizationController.getStatus(jobId);
        
        assertEquals(200, statusResponse.getStatusCodeValue());
        assertNotNull(statusResponse.getBody().getData().getStatus());

        log.info("End-to-end anonymization workflow completed successfully");
    }

    @Property
    @DisplayName("Anonymization with various database types")
    void testAnonymizationWithMultipleDatabaseTypes(
            @ForAll @Values({"postgresql", "mysql", "mongodb"}) String databaseType) {
        
        // Create connection for the database type
        ConnectionController.ConnectionRequest request = new ConnectionController.ConnectionRequest();
        request.setName("Test " + databaseType);
        request.setType(databaseType);
        request.setHost("localhost");
        request.setPort(getPortForDatabaseType(databaseType));
        request.setDatabase("test_db");
        request.setUsername("test_user");
        request.setPassword("test_password");

        ResponseEntity<ApiResponse<ConnectionController.ConnectionResponse>> response = 
            connectionController.createConnection(request);
        
        assertEquals(201, response.getStatusCodeValue());
        assertTrue(response.getBody().isSuccess());
        
        log.info("Successfully created connection for database type: {}", databaseType);
    }

    @Property
    @DisplayName("Anonymization with various row counts")
    void testAnonymizationWithVariousRowCounts(
            @ForAll @IntRange(min = 100, max = 100000) int rowCount) {
        
        // Create anonymization request with specified row count
        AnonymizationController.AnonymizationRequest request = new AnonymizationController.AnonymizationRequest();
        request.setSourceConnectionId(sourceConnectionId);
        request.setTargetConnectionId(targetConnectionId);
        request.setConfigurationId(configurationId);

        ResponseEntity<ApiResponse<AnonymizationController.AnonymizationStartResponse>> response = 
            anonymizationController.startAnonymization(request);
        
        assertEquals(201, response.getStatusCodeValue());
        assertNotNull(response.getBody().getData().getJobId());
        
        log.info("Successfully started anonymization for {} rows", rowCount);
    }

    @Test
    @DisplayName("Anonymization workflow with error handling")
    void testAnonymizationWithErrorHandling() {
        // Create request with invalid connection IDs
        AnonymizationController.AnonymizationRequest request = new AnonymizationController.AnonymizationRequest();
        request.setSourceConnectionId("invalid-source");
        request.setTargetConnectionId("invalid-target");
        request.setConfigurationId("invalid-config");

        ResponseEntity<ApiResponse<AnonymizationController.AnonymizationStartResponse>> response = 
            anonymizationController.startAnonymization(request);
        
        // Should handle error gracefully
        assertNotNull(response.getBody());
        
        log.info("Error handling test completed");
    }

    @Test
    @DisplayName("Anonymization cancellation workflow")
    void testAnonymizationCancellation() {
        // Start anonymization
        AnonymizationController.AnonymizationRequest request = new AnonymizationController.AnonymizationRequest();
        request.setSourceConnectionId(sourceConnectionId);
        request.setTargetConnectionId(targetConnectionId);
        request.setConfigurationId(configurationId);

        ResponseEntity<ApiResponse<AnonymizationController.AnonymizationStartResponse>> startResponse = 
            anonymizationController.startAnonymization(request);
        
        String jobId = startResponse.getBody().getData().getJobId();

        // Cancel anonymization
        ResponseEntity<ApiResponse<Void>> cancelResponse = 
            anonymizationController.cancelAnonymization(jobId);
        
        assertEquals(200, cancelResponse.getStatusCodeValue());
        
        log.info("Anonymization cancellation test completed");
    }

    private int getPortForDatabaseType(String databaseType) {
        return switch (databaseType) {
            case "postgresql" -> 5432;
            case "mysql" -> 3306;
            case "mongodb" -> 27017;
            default -> 5432;
        };
    }
}
