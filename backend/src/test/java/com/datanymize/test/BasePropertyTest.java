package com.datanymize.test;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for property-based tests in Datanymize.
 * 
 * Provides common generators and utilities for property-based testing across the system.
 * All property tests should extend this class to ensure consistent test configuration
 * and access to shared generators.
 * 
 * Configuration:
 * - Default tries: 100+ iterations per property
 * - Shrinking: Enabled for better failure diagnostics
 * - Seed: Can be set via system property for reproducibility
 */
@DisplayName("Base Property Test")
public abstract class BasePropertyTest {
    
    // ============ String Generators ============
    
    /**
     * Generator for valid hostnames (domain names or IP addresses).
     * Generates strings like "localhost", "db.example.com", "192.168.1.1"
     */
    protected Arbitrary<String> hostnames() {
        return Arbitraries.oneOf(
                // Localhost variants
                Arbitraries.of("localhost", "127.0.0.1", "0.0.0.0"),
                
                // Domain names
                Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .ofMinLength(1)
                        .ofMaxLength(10)
                        .map(s -> s + ".example.com"),
                
                // IP addresses
                Combinators.combine(
                        Arbitraries.integers().between(1, 255),
                        Arbitraries.integers().between(0, 255),
                        Arbitraries.integers().between(0, 255),
                        Arbitraries.integers().between(0, 255)
                ).as((a, b, c, d) -> a + "." + b + "." + c + "." + d)
        );
    }
    
    /**
     * Generator for valid database names.
     * Generates strings like "testdb", "production_db", "app_db_v2"
     */
    protected Arbitrary<String> databaseNames() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .withChars('_')
                .ofMinLength(1)
                .ofMaxLength(30)
                .filter(s -> Character.isLetter(s.charAt(0))); // Must start with letter
    }
    
    /**
     * Generator for valid usernames.
     * Generates strings like "admin", "app_user", "db_user_1"
     */
    protected Arbitrary<String> usernames() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .withChars('_')
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(s -> Character.isLetter(s.charAt(0))); // Must start with letter
    }
    
    /**
     * Generator for valid passwords.
     * Generates strings with mixed case, numbers, and special characters.
     */
    protected Arbitrary<String> passwords() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars('!', '@', '#', '$', '%', '^', '&', '*')
                .ofMinLength(8)
                .ofMaxLength(50);
    }
    
    /**
     * Generator for valid table names.
     * Generates strings like "users", "user_profiles", "orders_v2"
     */
    protected Arbitrary<String> tableNames() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .withChars('_')
                .ofMinLength(1)
                .ofMaxLength(30)
                .filter(s -> Character.isLetter(s.charAt(0))); // Must start with letter
    }
    
    /**
     * Generator for valid column names.
     * Generates strings like "id", "user_id", "created_at"
     */
    protected Arbitrary<String> columnNames() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .withChars('_')
                .ofMinLength(1)
                .ofMaxLength(30)
                .filter(s -> Character.isLetter(s.charAt(0))); // Must start with letter
    }
    
    /**
     * Generator for valid email addresses.
     * Generates strings like "user@example.com", "test.user@domain.co.uk"
     */
    protected Arbitrary<String> emailAddresses() {
        return Combinators.combine(
                Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .withCharRange('0', '9')
                        .withChars('.', '_')
                        .ofMinLength(1)
                        .ofMaxLength(20),
                Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .ofMinLength(1)
                        .ofMaxLength(10),
                Arbitraries.of("com", "org", "net", "co.uk", "de", "fr")
        ).as((local, domain, tld) -> local + "@" + domain + "." + tld);
    }
    
    /**
     * Generator for valid phone numbers.
     * Generates strings like "+1-555-123-4567", "555-123-4567"
     */
    protected Arbitrary<String> phoneNumbers() {
        return Combinators.combine(
                Arbitraries.integers().between(100, 999),
                Arbitraries.integers().between(100, 999),
                Arbitraries.integers().between(1000, 9999)
        ).as((area, exchange, line) -> area + "-" + exchange + "-" + line);
    }
    
    /**
     * Generator for valid SSN-like strings.
     * Generates strings like "123-45-6789"
     */
    protected Arbitrary<String> ssnLikeStrings() {
        return Combinators.combine(
                Arbitraries.integers().between(100, 999),
                Arbitraries.integers().between(10, 99),
                Arbitraries.integers().between(1000, 9999)
        ).as((part1, part2, part3) -> part1 + "-" + part2 + "-" + part3);
    }
    
    /**
     * Generator for valid credit card-like strings.
     * Generates strings like "4532-1234-5678-9010"
     */
    protected Arbitrary<String> creditCardLikeStrings() {
        return Combinators.combine(
                Arbitraries.integers().between(1000, 9999),
                Arbitraries.integers().between(1000, 9999),
                Arbitraries.integers().between(1000, 9999),
                Arbitraries.integers().between(1000, 9999)
        ).as((p1, p2, p3, p4) -> p1 + "-" + p2 + "-" + p3 + "-" + p4);
    }
    
    // ============ Port Generators ============
    
    /**
     * Generator for valid database ports.
     * Generates ports for PostgreSQL (5432), MySQL (3306), MongoDB (27017)
     */
    protected Arbitrary<Integer> databasePorts() {
        return Arbitraries.of(5432, 3306, 27017, 5433, 3307, 27018);
    }
    
    /**
     * Generator for valid port numbers (1024-65535).
     * Excludes well-known ports below 1024.
     */
    protected Arbitrary<Integer> validPorts() {
        return Arbitraries.integers().between(1024, 65535);
    }
    
    /**
     * Generator for invalid port numbers (0, negative, or > 65535).
     */
    protected Arbitrary<Integer> invalidPorts() {
        return Arbitraries.oneOf(
                Arbitraries.of(0, -1, -100, 65536, 70000)
        );
    }
    
    // ============ Timeout Generators ============
    
    /**
     * Generator for valid timeout values in seconds.
     * Generates values between 1 and 300 seconds (5 minutes).
     */
    protected Arbitrary<Integer> validTimeouts() {
        return Arbitraries.integers().between(1, 300);
    }
    
    /**
     * Generator for invalid timeout values.
     * Generates 0, negative, or extremely large values.
     */
    protected Arbitrary<Integer> invalidTimeouts() {
        return Arbitraries.oneOf(
                Arbitraries.of(0, -1, -100, 301, 1000, Integer.MAX_VALUE)
        );
    }
    
    // ============ Data Type Generators ============
    
    /**
     * Generator for SQL data types.
     * Generates common data types like "VARCHAR", "INT", "TIMESTAMP"
     */
    protected Arbitrary<String> sqlDataTypes() {
        return Arbitraries.of(
                "VARCHAR", "CHAR", "TEXT",
                "INT", "BIGINT", "SMALLINT", "DECIMAL", "FLOAT", "DOUBLE",
                "BOOLEAN", "DATE", "TIMESTAMP", "TIME",
                "JSON", "JSONB", "UUID", "BYTEA"
        );
    }
    
    /**
     * Generator for MongoDB data types.
     * Generates common MongoDB types like "String", "ObjectId", "Date"
     */
    protected Arbitrary<String> mongoDataTypes() {
        return Arbitraries.of(
                "String", "ObjectId", "Date", "Boolean", "Int32", "Int64",
                "Double", "Decimal128", "Array", "Object", "Null", "Binary"
        );
    }
    
    // ============ Configuration Generators ============
    
    /**
     * Generator for valid connection timeout configurations.
     * Generates realistic timeout values for database connections.
     */
    protected Arbitrary<Integer> connectionTimeouts() {
        return Arbitraries.integers().between(1, 30);
    }
    
    /**
     * Generator for batch size configurations.
     * Generates realistic batch sizes for data processing (100-10000).
     */
    protected Arbitrary<Integer> batchSizes() {
        return Arbitraries.integers().between(100, 10000);
    }
    
    /**
     * Generator for percentage values (0-100).
     * Used for subset selection and sampling.
     */
    protected Arbitrary<Integer> percentages() {
        return Arbitraries.integers().between(0, 100);
    }
    
    /**
     * Generator for seed values for deterministic transformations.
     * Generates long values suitable for random number generation.
     */
    protected Arbitrary<Long> seeds() {
        return Arbitraries.longs();
    }
    
    // ============ Metadata Generators ============
    
    /**
     * Generator for timestamps.
     * Generates LocalDateTime values for audit logging and metadata.
     */
    protected Arbitrary<LocalDateTime> timestamps() {
        return Arbitraries.longs()
                .between(0, System.currentTimeMillis())
                .map(ms -> LocalDateTime.now().minusSeconds(ms / 1000));
    }
    
    /**
     * Generator for key-value pairs (metadata).
     * Generates maps with string keys and values.
     */
    protected Arbitrary<Map<String, String>> metadata() {
        return Combinators.combine(
                Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .ofMinLength(1)
                        .ofMaxLength(10)
                        .list()
                        .ofMinSize(0)
                        .ofMaxSize(5),
                Arbitraries.strings()
                        .withCharRange('a', 'z')
                        .ofMinLength(1)
                        .ofMaxLength(20)
                        .list()
                        .ofMinSize(0)
                        .ofMaxSize(5)
        ).as((keys, values) -> {
            Map<String, String> map = new HashMap<>();
            for (int i = 0; i < Math.min(keys.size(), values.size()); i++) {
                map.put(keys.get(i), values.get(i));
            }
            return map;
        });
    }
    
    // ============ Utility Methods ============
    
    /**
     * Helper method to generate a unique ID for testing.
     * Useful for creating unique identifiers for test entities.
     */
    protected String generateTestId(String prefix) {
        return prefix + "-" + System.nanoTime();
    }
    
    /**
     * Helper method to verify that a value is within expected bounds.
     * Useful for property assertions.
     */
    protected boolean isWithinBounds(int value, int min, int max) {
        return value >= min && value <= max;
    }
    
    /**
     * Helper method to verify that a string matches a pattern.
     * Useful for validating generated strings.
     */
    protected boolean matchesPattern(String value, String pattern) {
        return value.matches(pattern);
    }
}
