package com.datanymize.pii;

import com.datanymize.pii.ai.MockAIProvider;
import com.datanymize.pii.model.PIICategory;
import com.datanymize.pii.model.PIIClassification;
import net.jqwik.api.*;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

/**
 * Property-based tests for AI-based PII detection with all factors.
 * Validates that detector considers column name, data type, and sample data.
 * 
 * **Validates: Requirements 3.2, 3.5**
 */
@DisplayName("AI-Based PII Detection with All Factors Properties")
public class AIBasedPIIDetectionProperties {
    
    private final PatternBasedPIIDetector patternDetector = new PatternBasedPIIDetector();
    private final MockAIProvider mockAIProvider = new MockAIProvider();
    private final AIBasedPIIDetector aiDetector = new AIBasedPIIDetector(mockAIProvider, patternDetector);
    
    /**
     * Property 8: PII Detection Considers All Factors
     * 
     * Column classification should consider column name, data type, and sample data.
     * The detector should use all available information to make a classification.
     */
    @Property(tries = 50)
    @DisplayName("Column name is considered in classification")
    void columnNameConsideredInClassification(
            @ForAll("knownPIIColumnNames") String columnName) {
        
        com.datanymize.database.model.DatabaseMetadata.ColumnMetadata column = 
            com.datanymize.database.model.DatabaseMetadata.ColumnMetadata.builder()
                .name(columnName)
                .dataType("varchar")
                .nullable(true)
                .build();
        
        PIIClassification classification = aiDetector.classifyColumn("test_table", column, new ArrayList<>());
        
        assert classification != null : "Classification should not be null";
        assert classification.isPII() : 
            "Column name '" + columnName + "' should result in PII classification";
    }
    
    /**
     * Property 8b: Data Type Considered in Classification
     * 
     * Data type should be considered when classifying columns.
     * For example, numeric types are less likely to be email addresses.
     */
    @Property(tries = 50)
    @DisplayName("Data type is considered in classification")
    void dataTypeConsideredInClassification(
            @ForAll("numericDataTypes") String dataType) {
        
        com.datanymize.database.model.DatabaseMetadata.ColumnMetadata column = 
            com.datanymize.database.model.DatabaseMetadata.ColumnMetadata.builder()
                .name("generic_column")
                .dataType(dataType)
                .nullable(true)
                .build();
        
        PIIClassification classification = aiDetector.classifyColumn("test_table", column, new ArrayList<>());
        
        assert classification != null : "Classification should not be null";
        // Numeric types should not be classified as email/phone/etc
        assert classification.getCategory() != PIICategory.EMAIL : 
            "Numeric column should not be classified as EMAIL";
        assert classification.getCategory() != PIICategory.PHONE : 
            "Numeric column should not be classified as PHONE";
    }
    
    /**
     * Property 8c: Sample Data Considered in Classification
     * 
     * Sample data should be analyzed and considered in the classification.
     * Columns with PII patterns in samples should be classified as PII.
     */
    @Property(tries = 50)
    @DisplayName("Sample data is considered in classification")
    void sampleDataConsideredInClassification(
            @ForAll("validEmailAddresses") String email) {
        
        List<String> samples = new ArrayList<>();
        samples.add(email);
        samples.add("another_value");
        
        com.datanymize.database.model.DatabaseMetadata.ColumnMetadata column = 
            com.datanymize.database.model.DatabaseMetadata.ColumnMetadata.builder()
                .name("generic_column")
                .dataType("varchar")
                .nullable(true)
                .build();
        
        PIIClassification classification = aiDetector.classifyColumn("test_table", column, samples);
        
        assert classification != null : "Classification should not be null";
        assert classification.getCategory() == PIICategory.EMAIL : 
            "Email pattern in sample data should be detected";
    }
    
    /**
     * Property 8d: Multiple Factors Combined
     * 
     * When multiple factors indicate PII, confidence should be high.
     */
    @Property(tries = 50)
    @DisplayName("Multiple factors increase confidence")
    void multipleFactorsIncreaseConfidence(
            @ForAll("validEmailAddresses") String email) {
        
        List<String> samples = new ArrayList<>();
        samples.add(email);
        samples.add(email);
        samples.add(email);
        
        com.datanymize.database.model.DatabaseMetadata.ColumnMetadata column = 
            com.datanymize.database.model.DatabaseMetadata.ColumnMetadata.builder()
                .name("email_address")  // Column name indicates email
                .dataType("varchar")
                .nullable(true)
                .build();
        
        PIIClassification classification = aiDetector.classifyColumn("test_table", column, samples);
        
        assert classification != null : "Classification should not be null";
        assert classification.isPII() : "Should be classified as PII";
        // With multiple factors, confidence should be high
        assert classification.getConfidence() >= 75.0 : 
            "Confidence should be high when multiple factors indicate PII";
    }
    
    /**
     * Property 8e: Fallback to Pattern Matching
     * 
     * When AI provider is unavailable, should fallback to pattern matching.
     */
    @Property(tries = 50)
    @DisplayName("Fallback to pattern matching works correctly")
    void fallbackToPatternMatchingWorks(
            @ForAll("knownPIIColumnNames") String columnName) {
        
        // Create detector with null AI provider (simulating unavailable AI)
        AIBasedPIIDetector detectorWithoutAI = new AIBasedPIIDetector(null, patternDetector);
        
        com.datanymize.database.model.DatabaseMetadata.ColumnMetadata column = 
            com.datanymize.database.model.DatabaseMetadata.ColumnMetadata.builder()
                .name(columnName)
                .dataType("varchar")
                .nullable(true)
                .build();
        
        PIIClassification classification = detectorWithoutAI.classifyColumn("test_table", column, new ArrayList<>());
        
        assert classification != null : "Classification should not be null";
        assert classification.isPII() : 
            "Pattern matching should detect PII for column: " + columnName;
    }
    
    /**
     * Property 8f: Caching Works Correctly
     * 
     * Classifications should be cached to avoid redundant processing.
     */
    @Property(tries = 50)
    @DisplayName("Classification caching works correctly")
    void classificationCachingWorks(
            @ForAll("knownPIIColumnNames") String columnName) {
        
        AIBasedPIIDetector detector = new AIBasedPIIDetector(mockAIProvider, patternDetector);
        
        com.datanymize.database.model.DatabaseMetadata.ColumnMetadata column = 
            com.datanymize.database.model.DatabaseMetadata.ColumnMetadata.builder()
                .name(columnName)
                .dataType("varchar")
                .nullable(true)
                .build();
        
        // First classification
        PIIClassification classification1 = detector.classifyColumn("test_table", column, new ArrayList<>());
        
        // Second classification (should be cached)
        PIIClassification classification2 = detector.classifyColumn("test_table", column, new ArrayList<>());
        
        assert classification1 != null : "First classification should not be null";
        assert classification2 != null : "Second classification should not be null";
        assert classification1.getCategory() == classification2.getCategory() : 
            "Cached classification should match original";
        assert classification1.getConfidence() == classification2.getConfidence() : 
            "Cached confidence should match original";
    }
    
    /**
     * Property 8g: Confidence Score Consistency
     * 
     * Confidence scores should be consistent and meaningful.
     */
    @Property(tries = 50)
    @DisplayName("Confidence scores are consistent and meaningful")
    void confidenceScoresConsistent(
            @ForAll("knownPIIColumnNames") String columnName) {
        
        com.datanymize.database.model.DatabaseMetadata.ColumnMetadata column = 
            com.datanymize.database.model.DatabaseMetadata.ColumnMetadata.builder()
                .name(columnName)
                .dataType("varchar")
                .nullable(true)
                .build();
        
        PIIClassification classification = aiDetector.classifyColumn("test_table", column, new ArrayList<>());
        
        assert classification != null : "Classification should not be null";
        assert classification.getConfidence() >= 0.0 && classification.getConfidence() <= 100.0 : 
            "Confidence should be between 0 and 100";
        
        if (classification.isPII()) {
            assert classification.getConfidence() > 0.0 : 
                "PII classification should have positive confidence";
        } else {
            assert classification.getConfidence() == 0.0 : 
                "Non-PII classification should have zero confidence";
        }
    }
    
    // ==================== Generators ====================
    
    @Provide
    Arbitrary<String> validEmailAddresses() {
        return Combinators.combine(
            Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(3)
                .ofMaxLength(10),
            Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(2)
                .ofMaxLength(8),
            Arbitraries.of("com", "org", "net", "edu")
        ).as((local, domain, tld) -> local + "@" + domain + "." + tld);
    }
    
    @Provide
    Arbitrary<String> knownPIIColumnNames() {
        return Arbitraries.of(
            "email", "email_address", "e_mail", "mail",
            "phone", "phone_number", "telephone", "mobile", "cell",
            "ssn", "social_security", "social_security_number",
            "credit_card", "cc_number", "card_number", "creditcard",
            "name", "full_name", "first_name", "last_name", "surname",
            "address", "street", "city", "state", "zip", "postal_code"
        );
    }
    
    @Provide
    Arbitrary<String> numericDataTypes() {
        return Arbitraries.of(
            "integer", "int", "bigint", "smallint", "decimal", "numeric", "float", "double"
        );
    }
}
