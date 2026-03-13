# Credential Manager Usage Guide

## Overview

The Credential Manager provides secure storage and management of database credentials with AES-256 encryption. Credentials are stored encrypted in memory and can be retrieved, updated, or deleted as needed.

## Quick Start

### Dependency Injection

```java
@Service
public class MyService {
    
    @Autowired
    private ICredentialManager credentialManager;
    
    // Use credentialManager in your methods
}
```

### Basic Operations

#### Store a Credential
```java
// Store a password for a connection
credentialManager.storeCredential("my-connection-id", "my-password");
```

#### Retrieve a Credential
```java
// Retrieve the password (decrypted)
String password = credentialManager.retrieveCredential("my-connection-id");
```

#### Check if Credential Exists
```java
// Check if a credential is stored
if (credentialManager.hasCredential("my-connection-id")) {
    // Credential exists
}
```

#### Delete a Credential
```java
// Delete a credential from memory
credentialManager.deleteCredential("my-connection-id");
```

#### Clear All Credentials
```java
// Clear all credentials (useful on shutdown)
credentialManager.clearAllCredentials();
```

## Common Patterns

### Connection Testing with Credential Lifecycle

```java
public ConnectionResult testConnection(String connectionId, ConnectionConfig config) {
    String password = null;
    try {
        // Retrieve credential from secure storage
        password = credentialManager.retrieveCredential(connectionId);
        
        // Use password for connection test
        config.setPassword(password);
        IDatabaseConnection conn = driver.createConnection(config);
        
        // Test connection
        boolean isValid = ConnectionValidator.validateWithTimeout(conn, 5);
        
        return ConnectionResult.builder()
            .success(isValid)
            .message("Connection test successful")
            .build();
            
    } catch (Exception e) {
        return ConnectionResult.builder()
            .success(false)
            .errorMessage("Connection test failed: " + e.getMessage())
            .build();
    } finally {
        // Password is automatically cleared from config
        // Credential remains in secure storage for future use
    }
}
```

### Storing Connection Credentials

```java
public void saveConnection(String connectionId, ConnectionConfig config) {
    try {
        // Validate configuration
        ConnectionValidator.validateConfiguration(config);
        
        // Store password securely
        credentialManager.storeCredential(connectionId, config.getPassword());
        
        // Clear password from config before storage
        config.setPassword(null);
        
        // Store config (without password)
        savedConfigs.put(connectionId, config);
        
        log.info("Connection saved: {}", connectionId);
    } catch (Exception e) {
        log.error("Failed to save connection", e);
        throw new RuntimeException("Failed to save connection", e);
    }
}
```

### Retrieving Connection with Credentials

```java
public ConnectionConfig getConnection(String connectionId) {
    ConnectionConfig config = savedConfigs.get(connectionId);
    if (config == null) {
        throw new IllegalArgumentException("Connection not found: " + connectionId);
    }
    
    try {
        // Retrieve password from secure storage
        String password = credentialManager.retrieveCredential(connectionId);
        config.setPassword(password);
        return config;
    } catch (Exception e) {
        log.error("Failed to retrieve connection credentials", e);
        throw new RuntimeException("Failed to retrieve connection", e);
    }
}
```

### Cleanup on Connection Deletion

```java
public void deleteConnection(String connectionId) {
    try {
        // Delete credential from secure storage
        credentialManager.deleteCredential(connectionId);
        
        // Delete connection config
        savedConfigs.remove(connectionId);
        
        log.info("Connection deleted: {}", connectionId);
    } catch (Exception e) {
        log.error("Failed to delete connection", e);
    }
}
```

## Error Handling

### Invalid Connection ID
```java
try {
    credentialManager.retrieveCredential(null);
} catch (IllegalArgumentException e) {
    // Handle: "Connection ID cannot be null or empty"
}
```

### Credential Not Found
```java
try {
    credentialManager.retrieveCredential("non-existent-id");
} catch (IllegalArgumentException e) {
    // Handle: "Credential not found for connection: non-existent-id"
}
```

### Encryption/Decryption Failure
```java
try {
    credentialManager.storeCredential("conn1", "password");
} catch (RuntimeException e) {
    // Handle: "Failed to store credential"
    // Check logs for underlying cause
}
```

## Security Best Practices

### 1. Never Log Passwords
```java
// ❌ WRONG
log.info("Storing password: {}", password);

// ✅ CORRECT
log.info("Storing credential for connection: {}", connectionId);
```

### 2. Clear Passwords from Memory
```java
// ✅ CORRECT - Password cleared after use
String password = credentialManager.retrieveCredential(connectionId);
try {
    // Use password
    config.setPassword(password);
} finally {
    // Password cleared from config
    config.setPassword(null);
}
```

### 3. Use Credential Manager for All Passwords
```java
// ❌ WRONG - Storing password in plain text
Map<String, String> credentials = new HashMap<>();
credentials.put("password", plainTextPassword);

// ✅ CORRECT - Using credential manager
credentialManager.storeCredential(connectionId, plainTextPassword);
```

### 4. Delete Credentials When No Longer Needed
```java
// ✅ CORRECT - Delete on connection removal
credentialManager.deleteCredential(connectionId);
```

### 5. Clear All on Shutdown
```java
@PreDestroy
public void cleanup() {
    credentialManager.clearAllCredentials();
}
```

## Testing

### Unit Tests
```java
@Test
void testCredentialStorage() {
    CredentialEncryption encryption = new CredentialEncryption();
    CredentialManager manager = new CredentialManager(encryption);
    
    // Store
    manager.storeCredential("conn1", "password123");
    
    // Verify exists
    assertTrue(manager.hasCredential("conn1"));
    
    // Retrieve
    String retrieved = manager.retrieveCredential("conn1");
    assertEquals("password123", retrieved);
    
    // Delete
    manager.deleteCredential("conn1");
    assertFalse(manager.hasCredential("conn1"));
}
```

### Property-Based Tests
See `CredentialEncryptionProperties.java` for comprehensive property-based tests covering:
- Encryption randomness
- Decryption correctness
- Credential lifecycle
- Multiple credentials
- Error handling

## Performance Considerations

### Encryption/Decryption
- AES-256 encryption is fast (microseconds per operation)
- Suitable for real-time use in connection testing
- No significant performance impact on application

### Memory Usage
- Each credential stored as encrypted string in memory
- Typical credential size: 50-100 bytes (plaintext) → 100-200 bytes (encrypted + Base64)
- For 1000 connections: ~100-200 KB memory usage

### Thread Safety
- All operations are thread-safe
- Uses ConcurrentHashMap internally
- Safe for multi-threaded Spring Boot environment

## Troubleshooting

### Credential Not Found After Storage
```
Issue: Stored credential but retrieval fails
Solution: Verify connection ID is exactly the same (case-sensitive)
```

### Decryption Fails
```
Issue: "Failed to decrypt credential"
Solution: Check that encryption key hasn't changed
         Verify encrypted data hasn't been corrupted
```

### Memory Leak
```
Issue: Credentials not cleared from memory
Solution: Ensure deleteCredential() is called when connection is deleted
         Call clearAllCredentials() on application shutdown
```

## Related Components

- **CredentialEncryption**: Low-level AES-256 encryption utility
- **ConnectionManager**: Uses CredentialManager for connection credential storage
- **IConnectionManager**: Interface defining connection management contract
- **ConnectionConfig**: Model for connection configuration (password field encrypted)

## References

- Requirements: 14.1, 14.4, 14.5
- Design Document: Section "Credential Management"
- Implementation: `backend/src/main/java/com/datanymize/security/`
- Tests: `backend/src/test/java/com/datanymize/security/CredentialEncryptionProperties.java`
