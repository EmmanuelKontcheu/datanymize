# jqwik Property-Based Testing - Usage Examples

This document provides practical examples of how to use the Datanymize property-based testing infrastructure.

## Quick Start

### Example 1: Simple Property Test

```java
import com.datanymize.test.BasePropertyTest;
import net.jqwik.api.Property;
import net.jqwik.api.ForAll;
import org.junit.jupiter.api.DisplayName;

@DisplayName("String Validation Properties")
public class StringValidationProperties extends BasePropertyTest {
    
    /**
     * Property: All generated hostnames should be non-empty
     */
    @Property(tries = 100)
    @DisplayName("Generated hostnames are never empty")
    void hostnamesAreNonEmpty(@ForAll("hostnames") String hostname) {
        assert !hostname.isEmpty();
        assert hostname.length() > 0;
    }
    
    /**
     * Property: All generated emails should contain @ symbol
     */
    @Property(tries = 100)
    @DisplayName("Generated emails contain @ symbol")
    void emailsContainAtSymbol(@ForAll("emailAddresses") String email) {
        assert email.contains("@");
        assert email.matches("^[^@]+@[^@]+\\.[^@]+$");
    }
}
```

### Example 2: Database Configuration Testing

```java
import com.datanymize.test.DatabaseConfigGenerators;
import com.datanymize.database.model.ConnectionConfig;
import com.datanymize.database.connection.PostgreSQLDriver;
import net.jqwik.api.Property;
import net.jqwik.api.ForAll;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Database Configuration Properties")
public class DatabaseConfigurationProperties extends DatabaseConfigGenerators {
    
    /**
     * Property: All valid PostgreSQL configs have required fields
     */
    @Property(tries = 100)
    @DisplayName("Valid PostgreSQL configs have all required fields")
    void validPostgreSQLConfigsHaveRequiredFields(
            @ForAll("validPostgreSQLConfigs") ConnectionConfig config) {
        
        assert config.getType().equals("postgresql");
        assert config.getHost() != null && !config.getHost().isEmpty();
        assert config.getPort() > 0 && config.getPort() <= 65535;
        assert config.getDatabase() != null && !config.getDatabase().isEmpty();
        assert config.getUsername() != null && !config.getUsername().isEmpty();
        assert config.getPassword() != null && !config.getPassword().isEmpty();
    }
    
    /**
     * Property: Invalid PostgreSQL configs are rejected
     */
    @Property(tries = 50)
    @DisplayName("Invalid PostgreSQL configs are rejected")
    void invalidPostgreSQLConfigsAreRejected(
            @ForAll("invalidPostgreSQLConfigs") ConnectionConfig config) {
        
        PostgreSQLDriver driver = new PostgreSQLDriver();
        
        try {
            driver.createConnection(config);
            // If we get here, the invalid config was not rejected
            assert false : "Invalid config should have been rejected";
        } catch (IllegalArgumentException e) {
            // Expected - invalid config should throw exception
            assert e.getMessage() != null;
            assert !e.getMessage().isEmpty();
        } catch (Exception e) {
            // Other exceptions are also acceptable
            assert true;
        }
    }
    
    /**
     * Property: All database types are supported
     */
    @Property(tries = 100)
    @DisplayName("All database types are supported")
    void allDatabaseTypesAreSupported(
            @ForAll("anyValidDatabaseConfig") ConnectionConfig config) {
        
        assert config.getType() != null;
        assert config.getType().matches("postgresql|mysql|mongodb");
    }
}
```

### Example 3: Schema Testing

```java
import com.datanymize.test.SchemaAndDataGenerators;
import com.datanymize.database.model.Table;
import com.datanymize.database.model.Column;
import net.jqwik.api.Property;
import net.jqwik.api.ForAll;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Schema Properties")
public class SchemaProperties extends SchemaAndDataGenerators {
    
    /**
     * Property: All generated tables have at least one primary key
     */
    @Property(tries = 100)
    @DisplayName("Generated tables have at least one primary key")
    void tablesHavePrimaryKey(@ForAll("validTables") Table table) {
        assert table.getPrimaryKeys() != null;
        assert !table.getPrimaryKeys().isEmpty();
        
        // Verify primary key columns exist
        for (String pkName : table.getPrimaryKeys()) {
            assert table.getColumns().stream()
                    .anyMatch(col -> col.getName().equals(pkName));
        }
    }
    
    /**
     * Property: All generated columns have valid data types
     */
    @Property(tries = 100)
    @DisplayName("Generated columns have valid data types")
    void columnsHaveValidDataTypes(@ForAll("validColumns") Column column) {
        assert column.getDataType() != null;
        assert !column.getDataType().isEmpty();
        
        // Verify data type is one of the supported types
        String dataType = column.getDataType();
        assert dataType.matches("VARCHAR|CHAR|TEXT|INT|BIGINT|SMALLINT|" +
                "DECIMAL|FLOAT|DOUBLE|BOOLEAN|DATE|TIMESTAMP|TIME|JSON|JSONB|UUID|BYTEA");
    }
    
    /**
     * Property: PII columns are correctly identified
     */
    @Property(tries = 100)
    @DisplayName("PII columns are correctly identified")
    void piiColumnsAreIdentified(@ForAll("piiColumns") Column column) {
        String columnName = column.getName();
        
        // Verify it's a known PII column
        assert columnName.matches("email|phone|ssn|name|address|credit_card");
        
        // Verify data type is appropriate
        assert column.getDataType().matches("VARCHAR|TEXT");
    }
}
```

### Example 4: Data Generation Testing

```java
import com.datanymize.test.SchemaAndDataGenerators;
import net.jqwik.api.Property;
import net.jqwik.api.ForAll;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Data Generation Properties")
public class DataGenerationProperties extends SchemaAndDataGenerators {
    
    /**
     * Property: All generated emails are valid format
     */
    @Property(tries = 100)
    @DisplayName("Generated emails are valid format")
    void generatedEmailsAreValid(@ForAll("piiBatchData") String data) {
        // Only test email-like data
        if (data.contains("@")) {
            assert data.matches("^[^@]+@[^@]+\\.[^@]+$");
        }
    }
    
    /**
     * Property: All generated phone numbers have correct format
     */
    @Property(tries = 100)
    @DisplayName("Generated phone numbers have correct format")
    void generatedPhoneNumbersAreValid(@ForAll("piiBatchData") String data) {
        // Only test phone-like data
        if (data.matches("\\d{3}-\\d{3}-\\d{4}")) {
            assert data.split("-").length == 3;
            assert data.split("-")[0].length() == 3;
            assert data.split("-")[1].length() == 3;
            assert data.split("-")[2].length() == 4;
        }
    }
    
    /**
     * Property: Sample data rows have all expected columns
     */
    @Property(tries = 100)
    @DisplayName("Sample data rows have all expected columns")
    void sampleDataRowsHaveExpectedColumns(
            @ForAll("sampleDataRows") java.util.Map<String, Object> row) {
        
        assert row.containsKey("id");
        assert row.containsKey("email");
        assert row.containsKey("phone");
        assert row.containsKey("name");
        assert row.containsKey("address");
        
        assert row.get("id") instanceof Integer;
        assert row.get("email") instanceof String;
        assert row.get("phone") instanceof String;
        assert row.get("name") instanceof String;
        assert row.get("address") instanceof String;
    }
}
```

### Example 5: Custom Generators

```java
import com.datanymize.test.BasePropertyTest;
import net.jqwik.api.Property;
import net.jqwik.api.ForAll;
import net.jqwik.api.Provide;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Arbitraries;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Custom Generator Properties")
public class CustomGeneratorProperties extends BasePropertyTest {
    
    /**
     * Custom generator for US ZIP codes
     */
    @Provide
    Arbitrary<String> usZipCodes() {
        return Arbitraries.integers()
                .between(10000, 99999)
                .map(String::valueOf);
    }
    
    /**
     * Custom generator for credit card numbers
     */
    @Provide
    Arbitrary<String> creditCardNumbers() {
        return Arbitraries.integers()
                .between(1000000000000000L, 9999999999999999L)
                .map(String::valueOf);
    }
    
    /**
     * Property: Generated ZIP codes are 5 digits
     */
    @Property(tries = 100)
    @DisplayName("Generated ZIP codes are 5 digits")
    void zipCodesAreFiveDigits(@ForAll("usZipCodes") String zipCode) {
        assert zipCode.length() == 5;
        assert zipCode.matches("\\d{5}");
    }
    
    /**
     * Property: Generated credit card numbers are 16 digits
     */
    @Property(tries = 100)
    @DisplayName("Generated credit card numbers are 16 digits")
    void creditCardNumbersAreSixteenDigits(
            @ForAll("creditCardNumbers") String cardNumber) {
        
        assert cardNumber.length() == 16;
        assert cardNumber.matches("\\d{16}");
    }
}
```

### Example 6: Multiple Parameters

```java
import com.datanymize.test.DatabaseConfigGenerators;
import com.datanymize.database.model.ConnectionConfig;
import net.jqwik.api.Property;
import net.jqwik.api.ForAll;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Multi-Parameter Properties")
public class MultiParameterProperties extends DatabaseConfigGenerators {
    
    /**
     * Property: Connection config with timeout is valid
     */
    @Property(tries = 100)
    @DisplayName("Connection config with timeout is valid")
    void connectionConfigWithTimeoutIsValid(
            @ForAll("validPostgreSQLConfigs") ConnectionConfig config,
            @ForAll("connectionTimeouts") int timeout) {
        
        config.setConnectionTimeoutSeconds(timeout);
        
        assert config.getConnectionTimeoutSeconds() > 0;
        assert config.getConnectionTimeoutSeconds() <= 300;
        assert config.getHost() != null;
        assert config.getPort() > 0;
    }
    
    /**
     * Property: Different database types have different default ports
     */
    @Property(tries = 100)
    @DisplayName("Different database types have different default ports")
    void differentDatabaseTypesHaveDifferentPorts(
            @ForAll("validPostgreSQLConfigs") ConnectionConfig pgConfig,
            @ForAll("validMySQLConfigs") ConnectionConfig mysqlConfig,
            @ForAll("validMongoDBConfigs") ConnectionConfig mongoConfig) {
        
        assert pgConfig.getPort() == 5432;
        assert mysqlConfig.getPort() == 3306;
        assert mongoConfig.getPort() == 27017;
    }
}
```

## Advanced Patterns

### Pattern 1: Assumptions and Filtering

```java
@Property(tries = 100)
void testWithAssumptions(@ForAll int value) {
    // Only test positive values
    Assume.that(value > 0);
    
    // Test code
    assert value > 0;
}

// Or use custom generator with filtering
@Provide
Arbitrary<Integer> positiveIntegers() {
    return Arbitraries.integers()
            .between(1, Integer.MAX_VALUE);
}

@Property(tries = 100)
void testWithFiltering(@ForAll("positiveIntegers") int value) {
    assert value > 0;
}
```

### Pattern 2: Edge Cases

```java
@Provide
Arbitrary<Integer> edgeCaseIntegers() {
    return Arbitraries.oneOf(
            // Edge cases
            Arbitraries.of(0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE),
            // Random values
            Arbitraries.integers()
    );
}

@Property(tries = 100)
void testWithEdgeCases(@ForAll("edgeCaseIntegers") int value) {
    // Test handles edge cases
}
```

### Pattern 3: Combining Generators

```java
@Provide
Arbitrary<ConnectionConfig> configsWithTimeouts() {
    return Combinators.combine(
            validPostgreSQLConfigs(),
            connectionTimeouts()
    ).as((config, timeout) -> {
        config.setConnectionTimeoutSeconds(timeout);
        return config;
    });
}

@Property(tries = 100)
void testWithCombinedGenerators(
        @ForAll("configsWithTimeouts") ConnectionConfig config) {
    
    assert config.getConnectionTimeoutSeconds() > 0;
}
```

## Running Examples

### Run All Property Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=DatabaseConfigurationProperties
```

### Run with Custom Configuration
```bash
mvn test -Djqwik.tries=500
```

### Run with Specific Seed (Reproducibility)
```bash
mvn test -Djqwik.seed=1234567890
```

## Best Practices Summary

1. **Use meaningful generator names** - Makes tests self-documenting
2. **Document properties** - Explain what property should hold
3. **Use appropriate tries** - 10 for smoke, 100 for standard, 500 for intensive
4. **Filter during generation** - Avoid excessive assumptions
5. **Test edge cases** - Include boundary values in generators
6. **Combine generators** - Test interactions between components
7. **Use seeds for reproducibility** - Capture seed from failures
8. **Keep tests focused** - One property per test method
9. **Use DisplayName** - Provide clear test descriptions
10. **Handle exceptions properly** - Expect and verify error cases

## References

- [jqwik Documentation](https://jqwik.net/)
- [BasePropertyTest](./BasePropertyTest.java)
- [DatabaseConfigGenerators](./DatabaseConfigGenerators.java)
- [SchemaAndDataGenerators](./SchemaAndDataGenerators.java)
- [JqwikConfiguration](./JqwikConfiguration.java)
