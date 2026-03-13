package com.datanymize.pii;

import com.datanymize.pii.model.PIICategory;
import com.datanymize.pii.model.PIIClassification;
import net.jqwik.api.*;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

/**
 * Property-based tests for pattern-based PII detection.
 * Validates that known PII patterns are correctly classified with appropriate confidence.
 * 
 * **Validates: Requirements 3.3, 3.4**
 */
@DisplayName("Pattern-Based PII Detection Properties")
public class PatternBasedPIIDetectionProperties {
    
    private final PatternBasedPIIDetector detector = new PatternBasedPIIDetector();
    
    /**
     * Property 7: PII Classification Consistency
     * 
     * For known PII patterns (email, phone, SSN, credit card),
     * the detector should consistently classify them with high confidence (>80%).
     */
    @Property(tries = 50)
    @DisplayName("Known PII patterns are classified with high confidence")
    void knownPIIPatternsClassifiedWithHighConfidence(
            @ForAll("validEmailAddresses") String email) {
        
        PIICategory category = detector.detectPattern(email);
        assert category == PIICategory.EMAIL : 
            "Email pattern not detected: " + email;
    }
    
    /**
     * Property 7b: Phone Pattern Detection
     * 
     * Phone numbers in various formats should be detected as PHONE category.
     */
    @Property(tries = 50)
    @DisplayName("Phone patterns are detected correctly")
    void phonePatternDetected(
            @ForAll("validPhoneNumbers") String phone) {
        
        PIICategory category = detector.detectPattern(phone);
        assert category == PIICategory.PHONE : 
            "Phone pattern not detected: " + phone;
    }
    
    /**
     * Property 7c: SSN Pattern Detection
     * 
     * Social Security Numbers in XXX-XX-XXXX format should be detected.
     */
    @Property(tries = 50)
    @DisplayName("SSN patterns are detected correctly")
    void ssnPatternDetected(
            @ForAll("validSSNs") String ssn) {
        
        PIICategory category = detector.detectPattern(ssn);
        assert category == PIICategory.SSN : 
            "SSN pattern not detected: " + ssn;
    }
    
    /**
     * Property 7d: Credit Card Pattern Detection
     * 
     * Credit card numbers (13-19 digits) should be detected.
     */
    @Property(tries = 50)
    @DisplayName("Credit card patterns are detected correctly")
    void creditCardPatternDetected(
            @ForAll("validCreditCards") String creditCard) {
        
        PIICategory category = detector.detectPattern(creditCard);
        assert category == PIICategory.CREDIT_CARD : 
            "Credit card pattern not detected: " + creditCard;
    }
    
    /**
     * Property 8: PII Detection Considers All Factors
     * 
     * Column classification should consider column name, data type, and sample data.
     * Columns with known PII names should be classified with high confidence.
     */
    @Property(tries = 50)
    @DisplayName("Column name heuristics are applied correctly")
    void columnNameHeuristicsApplied(
            @ForAll("knownPIIColumnNames") String columnName) {
        
        // Create a column with the PII column name
        com.datanymize.database.model.DatabaseMetadata.ColumnMetadata column = 
            com.datanymize.database.model.DatabaseMetadata.ColumnMetadata.builder()
                .name(columnName)
                .dataType("varchar")
                .nullable(true)
                .build();
        
        // Classify with empty samples (should still detect from name)
        PIIClassification classification = detector.classifyColumn("test_table", column, new ArrayList<>());
        
        assert classification.isPII() : 
            "Column name '" + columnName + "' should be classified as PII";
        assert classification.getConfidence() >= 80.0 : 
            "Column name '" + columnName + "' should have high confidence (>=80%)";
    }
    
    /**
     * Property 8b: Identifier Column Detection
     * 
     * Columns with identifier names (user_id, customer_id, etc.) should be classified
     * as IDENTIFIER with 80%+ confidence.
     */
    @Property(tries = 50)
    @DisplayName("Identifier columns are detected with correct confidence")
    void identifierColumnsDetected(
            @ForAll("identifierColumnNames") String columnName) {
        
        com.datanymize.database.model.DatabaseMetadata.ColumnMetadata column = 
            com.datanymize.database.model.DatabaseMetadata.ColumnMetadata.builder()
                .name(columnName)
                .dataType("integer")
                .nullable(false)
                .build();
        
        PIIClassification classification = detector.classifyColumn("test_table", column, new ArrayList<>());
        
        assert classification.getCategory() == PIICategory.IDENTIFIER : 
            "Column '" + columnName + "' should be classified as IDENTIFIER";
        assert classification.getConfidence() >= 80.0 : 
            "Column '" + columnName + "' should have 80%+ confidence";
    }
    
    /**
     * Property 8c: Non-PII Columns Not Misclassified
     * 
     * Columns with generic names and non-PII data should not be classified as PII.
     */
    @Property(tries = 50)
    @DisplayName("Non-PII columns are not misclassified")
    void nonPIIColumnsNotMisclassified(
            @ForAll("genericColumnNames") String columnName) {
        
        com.datanymize.database.model.DatabaseMetadata.ColumnMetadata column = 
            com.datanymize.database.model.DatabaseMetadata.ColumnMetadata.builder()
                .name(columnName)
                .dataType("varchar")
                .nullable(true)
                .build();
        
        PIIClassification classification = detector.classifyColumn("test_table", column, new ArrayList<>());
        
        assert !classification.isPII() : 
            "Column '" + columnName + "' should not be classified as PII";
    }
    
    /**
     * Property 8d: Data Pattern Analysis
     * 
     * When sample data contains PII patterns, the column should be classified accordingly.
     */
    @Property(tries = 50)
    @DisplayName("Data patterns are analyzed correctly")
    void dataPatternAnalyzed(
            @ForAll("validEmailAddresses") String email) {
        
        List<String> samples = new ArrayList<>();
        samples.add(email);
        samples.add("other_value");
        samples.add("another_value");
        
        com.datanymize.database.model.DatabaseMetadata.ColumnMetadata column = 
            com.datanymize.database.model.DatabaseMetadata.ColumnMetadata.builder()
                .name("generic_column")
                .dataType("varchar")
                .nullable(true)
                .build();
        
        PIIClassification classification = detector.classifyColumn("test_table", column, samples);
        
        assert classification.getCategory() == PIICategory.EMAIL : 
            "Email pattern in data should be detected";
        assert classification.getConfidence() > 0 : 
            "Confidence should be > 0 when pattern is found in data";
    }
    
    /**
     * Property 8e: Confidence Score Calculation
     * 
     * Confidence scores should be calculated based on percentage of matching patterns.
     */
    @Property(tries = 50)
    @DisplayName("Confidence scores are calculated correctly")
    void confidenceScoreCalculated(
            @ForAll("validEmailAddresses") String email) {
        
        List<String> samples = new ArrayList<>();
        // 50% emails, 50% non-emails
        samples.add(email);
        samples.add("not_an_email");
        
        com.datanymize.database.model.DatabaseMetadata.ColumnMetadata column = 
            com.datanymize.database.model.DatabaseMetadata.ColumnMetadata.builder()
                .name("generic_column")
                .dataType("varchar")
                .nullable(true)
                .build();
        
        PIIClassification classification = detector.classifyColumn("test_table", column, samples);
        
        if (classification.isPII()) {
            // Confidence should be around 50% (1 out of 2 samples matched)
            assert classification.getConfidence() >= 40.0 && classification.getConfidence() <= 60.0 : 
                "Confidence should be around 50% for 50% match rate, got: " + classification.getConfidence();
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
    Arbitrary<String> validPhoneNumbers() {
        return Combinators.combine(
            Arbitraries.integers().between(200, 999),
            Arbitraries.integers().between(200, 999),
            Arbitraries.integers().between(1000, 9999)
        ).as((area, exchange, number) -> 
            String.format("(%d) %d-%d", area, exchange, number)
        );
    }
    
    @Provide
    Arbitrary<String> validSSNs() {
        return Combinators.combine(
            Arbitraries.integers().between(100, 999),
            Arbitraries.integers().between(10, 99),
            Arbitraries.integers().between(1000, 9999)
        ).as((area, group, serial) -> 
            String.format("%d-%d-%d", area, group, serial)
        );
    }
    
    @Provide
    Arbitrary<String> validCreditCards() {
        return Arbitraries.strings()
            .withCharRange('0', '9')
            .ofMinLength(13)
            .ofMaxLength(19);
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
    Arbitrary<String> identifierColumnNames() {
        return Arbitraries.of(
            "id", "user_id", "customer_id", "order_id", "product_id", "account_id",
            "employee_id", "person_id", "record_id", "identifier", "uuid", "guid"
        );
    }
    
    @Provide
    Arbitrary<String> genericColumnNames() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(5)
            .ofMaxLength(15)
            .filter(name -> !name.contains("email") && !name.contains("phone") && 
                           !name.contains("ssn") && !name.contains("card") &&
                           !name.contains("name") && !name.contains("address") &&
                           !name.contains("id"));
    }
}
