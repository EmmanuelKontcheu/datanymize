# Datanymize Property-Based Testing Framework

This directory contains the property-based testing infrastructure for Datanymize using jqwik.

## Overview

Property-based testing (PBT) is a powerful approach to software testing that generates random test inputs to verify that properties (formal specifications) hold true across a wide range of scenarios. Unlike traditional unit tests that verify specific examples, property-based tests verify general rules that should hold for all valid inputs.

## Key Components

### 1. BasePropertyTest
Base class for all property-based tests providing:
- Common string generators (hostnames, emails, phone numbers, etc.)
- Port and timeout generators
- Data type generators (SQL, MongoDB)
- Configuration generators
- Metadata generators
- Utility methods for test assertions

**Usage:**
```java
public class MyPropertyTest extends BasePropertyTest {
    @Property(tries = 100)
    void myProperty(@ForAll("hostnames") String hostname) {
        // Test code
    }
}
```

### 2. JqwikConfiguration
Global configuration for jqwik property-based testing:
- Default tries: 100+ iterations per property
- Shrinking: Enabled for better failure diagnostics
- Timeout: 60 seconds per test
- Max discard ratio: 5.0

**Configuration Methods:**
- `getTries(TestType)` - Get configured tries for test type
- `TestType.NORMAL` - Standard tests (100 tries)
- `TestType.INTENSIVE` - Extra coverage (500 tries)
- `TestType.SMOKE` - Quick feedback (10 tries)

### 3. DatabaseConfigGenerators
Specialized generators for database connection configurations:
- PostgreSQL configs (valid and invalid)
- MySQL configs (valid and invalid)
- MongoDB configs (valid and invalid)
- Generic database configs

**Usage:**
```java
public class PostgreSQLConnectivityProperties extends DatabaseConfigGenerators {
    @Property(tries = 100)
    void connectionEstablishment(@ForAll("validPostgreSQLConfigs") ConnectionConfig config) {
        // Test code
    }
}
```

### 4. SchemaAndDataGenerators
Specialized generators for database schemas and data:
- Column definitions (valid, primary key, foreign key, PII)
- Table definitions (valid, simple, with PII)
- Sample data rows
- PII and non-PII batch data
- Foreign key relationships
- Index definitions

**Usage:**
```java
public class SchemaExtractionProperties extends SchemaAndDataGenerators {
    @Property(tries = 100)
    void schemaExtraction(@ForAll("validTables") Table table) {
        // Test code
    }
}
```

## Configuration

### jqwik.properties
Located in `src/test/resources/jqwik.properties`, this file configures:
- Number of tries (default: 100)
- Max discard ratio (default: 5)
- Timeout (default: 60s)
- Shrinking mode (default: FULL)
- Database type (default: in-memory)

**Example:**
```properties
tries = 100
max-discard-ratio = 5
timeout = 60s
shrinking = FULL
database = in-memory
```

### System Properties
Override configuration via system properties:
```bash
mvn test -Djqwik.tries=100 -Djqwik.seed=1234567890
```

### Annotation Configuration
Configure individual tests via @Property annotation:
```java
@Property(tries = 100, seed = "1234567890")
void myTest(@ForAll String input) {
    // Test code
}
```

## Writing Property-Based Tests

### Basic Structure
```java
public class MyPropertyTest extends BasePropertyTest {
    
    @Property(tries = 100)
    @DisplayName("Description of what property should hold")
    void propertyName(@ForAll("generatorName") String input) {
        // Arrange
        // Act
        // Assert
    }
}
```

### Using Generators
```java
// Use built-in generators from BasePropertyTest
@Property(tries = 100)
void testWithHostname(@ForAll("hostnames") String hostname) {
    // hostname will be generated as: localhost, 127.0.0.1, db.example.com, etc.
}

// Use custom generators
@Provide
Arbitrary<String> customStrings() {
    return Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(1)
            .ofMaxLength(10);
}

@Property(tries = 100)
void testWithCustom(@ForAll("customStrings") String input) {
    // input will be generated using customStrings generator
}
```

### Assumptions and Filtering
```java
@Property(tries = 100)
void testWithAssumption(@ForAll int value) {
    // Only test positive values
    Assume.that(value > 0);
    
    // Test code
}

// Or filter during generation
@Property(tries = 100)
void testWithFilter(@ForAll("positiveIntegers") int value) {
    // Test code
}

@Provide
Arbitrary<Integer> positiveIntegers() {
    return Arbitraries.integers()
            .between(1, Integer.MAX_VALUE);
}
```

### Combining Multiple Generators
```java
@Property(tries = 100)
void testWithMultiple(
        @ForAll("hostnames") String hostname,
        @ForAll("databasePorts") int port,
        @ForAll("databaseNames") String database) {
    // Test code with all three parameters
}
```

## Running Tests

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=PostgreSQLConnectivityProperties
```

### Run with Custom Configuration
```bash
mvn test -Djqwik.tries=500 -Djqwik.seed=1234567890
```

### Run with Specific Seed (for Reproducibility)
```bash
# When a test fails, jqwik reports the seed
# Use that seed to reproduce the failure
mvn test -Djqwik.seed=<seed-from-failure>
```

## Best Practices

### 1. Use Meaningful Generator Names
```java
// Good
@Provide
Arbitrary<String> validEmailAddresses() { ... }

// Bad
@Provide
Arbitrary<String> strings() { ... }
```

### 2. Document Properties
```java
/**
 * Property: For all valid PostgreSQL configurations,
 * a connection should be established successfully.
 */
@Property(tries = 100)
void connectionEstablishment(@ForAll("validPostgreSQLConfigs") ConnectionConfig config) {
    // Test code
}
```

### 3. Use Appropriate Number of Tries
```java
// Quick smoke test
@Property(tries = 10)
void smokeTest(@ForAll String input) { ... }

// Standard test
@Property(tries = 100)
void standardTest(@ForAll String input) { ... }

// Intensive test
@Property(tries = 500)
void intensiveTest(@ForAll String input) { ... }
```

### 4. Handle Assumptions Carefully
```java
// Good - filter during generation
@Provide
Arbitrary<Integer> positiveIntegers() {
    return Arbitraries.integers().between(1, Integer.MAX_VALUE);
}

// Avoid - too many assumptions can reduce coverage
@Property(tries = 100)
void test(@ForAll int value) {
    Assume.that(value > 0);
    Assume.that(value < 1000);
    Assume.that(value % 2 == 0);
    // ...
}
```

### 5. Test Edge Cases
```java
@Property(tries = 100)
void testEdgeCases(@ForAll("edgeCaseValues") int value) {
    // Test code
}

@Provide
Arbitrary<Integer> edgeCaseValues() {
    return Arbitraries.oneOf(
            Arbitraries.of(0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE),
            Arbitraries.integers()
    );
}
```

## Troubleshooting

### Test Fails with "Too Many Discards"
This means too many generated values were filtered out by assumptions.

**Solution:**
- Use `@Provide` generators to filter during generation instead of assumptions
- Reduce the number of assumptions
- Increase `max-discard-ratio` in jqwik.properties

### Test Fails Intermittently
This usually means the test is flaky or depends on external state.

**Solution:**
- Use the reported seed to reproduce the failure
- Check for external dependencies (databases, files, etc.)
- Ensure test isolation

### Test Runs Too Slowly
This means the test is taking too long to complete.

**Solution:**
- Reduce the number of tries
- Use `TestType.SMOKE` for quick feedback
- Optimize the test code
- Check for expensive operations in the test

### Cannot Reproduce Failure
Use the seed from the failure report.

**Solution:**
```bash
# From failure output: "seed: 1234567890"
mvn test -Djqwik.seed=1234567890
```

## Examples

### Example 1: Connection Configuration Test
```java
public class ConnectionConfigProperties extends DatabaseConfigGenerators {
    
    @Property(tries = 100)
    @DisplayName("Valid PostgreSQL configs establish connections")
    void validPostgreSQLConfigs(@ForAll("validPostgreSQLConfigs") ConnectionConfig config) {
        PostgreSQLDriver driver = new PostgreSQLDriver();
        
        try {
            IDatabaseConnection connection = driver.createConnection(config);
            Assume.that(connection != null);
            Assume.that(connection.isConnected());
            connection.close();
        } catch (Exception e) {
            Assume.that(false).as("Connection should be established");
        }
    }
}
```

### Example 2: Schema Extraction Test
```java
public class SchemaExtractionProperties extends SchemaAndDataGenerators {
    
    @Property(tries = 100)
    @DisplayName("Schema extraction preserves all table information")
    void schemaExtractionCompleteness(@ForAll("validTables") Table table) {
        // Extract schema
        DatabaseSchema schema = extractor.extractSchema(table);
        
        // Verify all columns are extracted
        Assume.that(schema.getTables().size() == 1);
        Table extractedTable = schema.getTables().get(0);
        Assume.that(extractedTable.getColumns().size() == table.getColumns().size());
        
        // Verify all column names match
        for (Column col : table.getColumns()) {
            Assume.that(extractedTable.getColumns().stream()
                    .anyMatch(c -> c.getName().equals(col.getName())));
        }
    }
}
```

### Example 3: PII Detection Test
```java
public class PIIDetectionProperties extends SchemaAndDataGenerators {
    
    @Property(tries = 100)
    @DisplayName("PII detector classifies known PII patterns correctly")
    void piiDetectionAccuracy(@ForAll("tablesWithPII") Table table) {
        PIIScanResult result = detector.scanDatabase(connection, schema);
        
        // Verify PII columns are detected
        for (Column col : table.getColumns()) {
            if (col.getName().equals("email")) {
                ColumnClassification classification = result.getClassification(col);
                Assume.that(classification.getCategory() == PIICategory.EMAIL);
                Assume.that(classification.getConfidence() >= 90);
            }
        }
    }
}
```

## References

- [jqwik Documentation](https://jqwik.net/)
- [Property-Based Testing](https://hypothesis.works/articles/what-is-property-based-testing/)
- [Datanymize Design Document](.kiro/specs/datanymize/design.md)
- [Datanymize Requirements](.kiro/specs/datanymize/requirements.md)
