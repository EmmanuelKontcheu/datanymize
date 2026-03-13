package com.datanymize.security;

import com.datanymize.audit.IAuditLogger;
import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for read-only access enforcement.
 * 
 * **Validates: Requirements 13.1, 13.4**
 */
@PropertyDefaults(tries = 100)
public class ReadOnlyAccessEnforcementProperties {
    
    private static final String TEST_CONNECTION_ID = "test-connection";
    private static final String TEST_TENANT_ID = "test-tenant";
    
    /**
     * Property 20: Read-Only Access Enforcement
     * 
     * Verifies that write operations (INSERT, UPDATE, DELETE, DROP, ALTER) are blocked
     * while SELECT operations are allowed.
     */
    @Property
    void readOnlyAccessEnforcementProperty(
            @ForAll("selectQueries") String selectQuery,
            @ForAll("writeQueries") String writeQuery) {
        
        IAuditLogger mockAuditLogger = Mockito.mock(IAuditLogger.class);
        ReadOnlyValidator validator = new ReadOnlyValidator(mockAuditLogger, TEST_CONNECTION_ID, TEST_TENANT_ID);
        
        // SELECT queries should be allowed
        assertDoesNotThrow(() -> validator.validateQuery(selectQuery),
            "SELECT query should be allowed: " + selectQuery);
        
        // Write queries should be blocked
        assertThrows(ReadOnlyValidator.ReadOnlyAccessViolationException.class,
            () -> validator.validateQuery(writeQuery),
            "Write query should be blocked: " + writeQuery);
    }
    
    /**
     * Property: Write operations are consistently blocked
     * 
     * Verifies that the same write operation is always blocked.
     */
    @Property
    void writeOperationsConsistentlyBlockedProperty(
            @ForAll("writeQueries") String writeQuery) {
        
        IAuditLogger mockAuditLogger = Mockito.mock(IAuditLogger.class);
        ReadOnlyValidator validator = new ReadOnlyValidator(mockAuditLogger, TEST_CONNECTION_ID, TEST_TENANT_ID);
        
        // First attempt should be blocked
        assertThrows(ReadOnlyValidator.ReadOnlyAccessViolationException.class,
            () -> validator.validateQuery(writeQuery));
        
        // Second attempt should also be blocked (consistency)
        assertThrows(ReadOnlyValidator.ReadOnlyAccessViolationException.class,
            () -> validator.validateQuery(writeQuery));
    }
    
    /**
     * Property: Error messages are meaningful
     * 
     * Verifies that error messages contain helpful information.
     */
    @Property
    void errorMessagesAreMeaningfulProperty(
            @ForAll("writeQueries") String writeQuery) {
        
        IAuditLogger mockAuditLogger = Mockito.mock(IAuditLogger.class);
        ReadOnlyValidator validator = new ReadOnlyValidator(mockAuditLogger, TEST_CONNECTION_ID, TEST_TENANT_ID);
        
        ReadOnlyValidator.ReadOnlyAccessViolationException exception = 
            assertThrows(ReadOnlyValidator.ReadOnlyAccessViolationException.class,
                () -> validator.validateQuery(writeQuery));
        
        String message = exception.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("Read-only") || message.contains("violation"),
            "Error message should mention read-only access: " + message);
        assertTrue(message.length() > 20,
            "Error message should be descriptive: " + message);
    }
    
    /**
     * Property: Case-insensitive operation detection
     * 
     * Verifies that operations are detected regardless of case.
     */
    @Property
    void caseInsensitiveOperationDetectionProperty() {
        IAuditLogger mockAuditLogger = Mockito.mock(IAuditLogger.class);
        ReadOnlyValidator validator = new ReadOnlyValidator(mockAuditLogger, TEST_CONNECTION_ID, TEST_TENANT_ID);
        
        String[] insertVariants = {
            "INSERT INTO table VALUES (1)",
            "insert into table values (1)",
            "InSeRt InTo table values (1)",
            "  INSERT  INTO table values (1)"
        };
        
        for (String query : insertVariants) {
            assertThrows(ReadOnlyValidator.ReadOnlyAccessViolationException.class,
                () -> validator.validateQuery(query),
                "Should block INSERT regardless of case: " + query);
        }
    }
    
    /**
     * Property: SELECT queries with various formats are allowed
     * 
     * Verifies that different SELECT query formats are allowed.
     */
    @Property
    void selectQueriesAllowedProperty(
            @ForAll("selectQueries") String selectQuery) {
        
        IAuditLogger mockAuditLogger = Mockito.mock(IAuditLogger.class);
        ReadOnlyValidator validator = new ReadOnlyValidator(mockAuditLogger, TEST_CONNECTION_ID, TEST_TENANT_ID);
        
        assertDoesNotThrow(() -> validator.validateQuery(selectQuery),
            "SELECT query should be allowed: " + selectQuery);
    }
    
    // Providers for test data
    
    @Provide
    Arbitrary<String> selectQueries() {
        return Arbitraries.of(
            "SELECT * FROM users",
            "select id, name from users where id = 1",
            "SELECT COUNT(*) FROM orders",
            "  SELECT * FROM products  ",
            "SELECT u.id, u.name FROM users u JOIN orders o ON u.id = o.user_id",
            "select * from users where name like '%test%'",
            "SELECT DISTINCT category FROM products",
            "SELECT * FROM users LIMIT 10",
            "SELECT * FROM users ORDER BY name ASC",
            "SELECT * FROM users WHERE age > 18 AND status = 'active'"
        );
    }
    
    @Provide
    Arbitrary<String> writeQueries() {
        return Arbitraries.of(
            "INSERT INTO users (name, email) VALUES ('John', 'john@example.com')",
            "UPDATE users SET name = 'Jane' WHERE id = 1",
            "DELETE FROM users WHERE id = 1",
            "DROP TABLE users",
            "ALTER TABLE users ADD COLUMN age INT",
            "CREATE TABLE new_table (id INT)",
            "TRUNCATE TABLE users",
            "GRANT SELECT ON users TO user1",
            "REVOKE SELECT ON users FROM user1",
            "insert into users values (1, 'test')",
            "update users set status = 'inactive'",
            "delete from orders where user_id = 1",
            "drop database test_db",
            "alter table users modify column name varchar(255)",
            "create index idx_users_email on users(email)"
        );
    }
}
