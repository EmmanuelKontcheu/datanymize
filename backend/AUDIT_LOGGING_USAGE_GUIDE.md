# Audit Logging Usage Guide

## Overview

The audit logging infrastructure provides comprehensive logging of all user actions for GDPR compliance. All audit logs are immutable, encrypted at rest, and automatically retained for a minimum of 1 year.

## Quick Start

### 1. Inject the AuditLogger

```java
@Service
public class MyService {
    private final IAuditLogger auditLogger;
    
    public MyService(IAuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }
}
```

### 2. Log Actions

```java
// Log a connection creation
auditLogger.logConnectionCreated("user-123", "conn-456", "postgresql");

// Log an anonymization operation
auditLogger.logAnonymization(
    "user-123",
    "production_db",
    "test_db",
    50000,
    true,
    null
);

// Log a failed operation
auditLogger.logAnonymization(
    "user-123",
    "production_db",
    "test_db",
    25000,
    false,
    "Connection timeout"
);
```

## Logging Methods

### logAction()
Generic action logging for custom events.

```java
Map<String, Object> metadata = new HashMap<>();
metadata.put("customField", "customValue");

auditLogger.logAction(
    "user-123",
    "CUSTOM_ACTION",
    "resource-id",
    true,
    null,
    metadata
);
```

**Parameters**:
- `userId`: User performing the action
- `action`: Action type (e.g., "CUSTOM_ACTION")
- `resource`: Resource being acted upon
- `success`: Whether the action succeeded
- `errorMessage`: Error message if action failed (null if successful)
- `metadata`: Additional metadata as a map

### logConnectionCreated()
Log when a database connection is created.

```java
auditLogger.logConnectionCreated("user-123", "conn-456", "postgresql");
```

**Parameters**:
- `userId`: User creating the connection
- `connectionId`: Unique connection identifier
- `connectionType`: Type of connection (postgresql, mysql, mongodb)

### logConnectionDeleted()
Log when a database connection is deleted.

```java
auditLogger.logConnectionDeleted("user-123", "conn-456");
```

**Parameters**:
- `userId`: User deleting the connection
- `connectionId`: Unique connection identifier

### logSchemaExtraction()
Log when a schema is extracted from a database.

```java
auditLogger.logSchemaExtraction("user-123", "production_db", "test_db", 150);
```

**Parameters**:
- `userId`: User performing the extraction
- `sourceDatabase`: Source database name
- `targetDatabase`: Target database name
- `rowCount`: Number of rows extracted

### logPIIScan()
Log when a PII scan is executed.

```java
auditLogger.logPIIScan("user-123", "production_db", 100000);
```

**Parameters**:
- `userId`: User performing the scan
- `database`: Database being scanned
- `rowsScanned`: Number of rows scanned

### logAnonymization()
Log when an anonymization operation is performed.

```java
// Successful anonymization
auditLogger.logAnonymization(
    "user-123",
    "production_db",
    "test_db",
    50000,
    true,
    null
);

// Failed anonymization
auditLogger.logAnonymization(
    "user-123",
    "production_db",
    "test_db",
    25000,
    false,
    "Foreign key constraint violation"
);
```

**Parameters**:
- `userId`: User performing the anonymization
- `sourceDatabase`: Source database name
- `targetDatabase`: Target database name
- `rowsProcessed`: Number of rows processed
- `success`: Whether the anonymization succeeded
- `errorMessage`: Error message if failed (null if successful)

### logDataExport()
Log when data is exported.

```java
auditLogger.logDataExport("user-123", "test_db", "SQL_DUMP", 50000);
```

**Parameters**:
- `userId`: User performing the export
- `database`: Database being exported
- `exportFormat`: Export format (SQL_DUMP, CSV, JSON, etc.)
- `rowsExported`: Number of rows exported

### getAuditLogs()
Retrieve audit logs with optional filters.

```java
LocalDateTime startDate = LocalDateTime.now().minusDays(7);
LocalDateTime endDate = LocalDateTime.now();

// Get all logs for the last 7 days
List<AuditLogEntry> allLogs = auditLogger.getAuditLogs(
    startDate,
    endDate,
    null,  // No user filter
    null   // No action filter
);

// Get logs for a specific user
List<AuditLogEntry> userLogs = auditLogger.getAuditLogs(
    startDate,
    endDate,
    "user-123",  // User filter
    null         // No action filter
);

// Get logs for a specific action
List<AuditLogEntry> actionLogs = auditLogger.getAuditLogs(
    startDate,
    endDate,
    null,              // No user filter
    "ANONYMIZATION"    // Action filter
);

// Get logs for a specific user and action
List<AuditLogEntry> specificLogs = auditLogger.getAuditLogs(
    startDate,
    endDate,
    "user-123",
    "ANONYMIZATION"
);
```

**Parameters**:
- `startDate`: Start date for the query
- `endDate`: End date for the query
- `userId`: Optional user ID filter (null to skip)
- `action`: Optional action filter (null to skip)

**Returns**: List of AuditLogEntry objects matching the criteria

## Audit Log Entry Fields

Each audit log entry contains:

```java
public class AuditLogEntry {
    String id;                          // Unique identifier (UUID)
    String tenantId;                    // Tenant ID for multi-tenant isolation
    String userId;                      // User performing the action
    String action;                      // Action type
    LocalDateTime timestamp;            // UTC timestamp
    String sourceDatabase;              // Source database (if applicable)
    String targetDatabase;              // Target database (if applicable)
    long rowsProcessed;                 // Number of rows processed
    boolean success;                    // Whether the action succeeded
    String errorMessage;                // Error message if failed
    String ipAddress;                   // IP address of the user
    String userAgent;                   // User agent string
    Map<String, Object> metadata;       // Additional metadata
    LocalDateTime createdAt;            // Creation timestamp for retention
}
```

## Action Types

Common action types logged:

- `CONNECTION_CREATED`: Database connection created
- `CONNECTION_DELETED`: Database connection deleted
- `SCHEMA_EXTRACTION`: Schema extracted from database
- `PII_SCAN`: PII scan executed
- `ANONYMIZATION`: Anonymization operation performed
- `DATA_EXPORT`: Data exported
- `CUSTOM_ACTION`: Custom action (use logAction() method)

## Retention Policy

Audit logs are automatically retained for a minimum of 1 year (365 days) as required by GDPR.

### Configuration

```yaml
audit:
  retention-days: 365        # Retention period in days
  encryption-enabled: true   # Enable encryption
  cleanup-schedule: "0 0 2 * * *"  # Daily at 2 AM UTC
```

### Automatic Cleanup

The retention policy runs automatically every day at 2 AM UTC. Logs older than the retention period are automatically deleted.

To manually trigger retention policy:

```java
@Autowired
private AuditLogRetentionService retentionService;

public void manualCleanup() {
    retentionService.executeRetentionPolicy();
}
```

## Security Features

### 1. Immutability
Audit logs are immutable (append-only). Once created, they cannot be modified or deleted (except by the retention policy).

### 2. Encryption
Sensitive fields in metadata are encrypted using AES-256 encryption at rest.

### 3. Tenant Isolation
Each audit log entry includes the tenant ID, ensuring complete data isolation in multi-tenant environments.

### 4. Automatic Tenant Context
The tenant ID is automatically captured from the TenantContextHolder, so you don't need to pass it explicitly.

## Integration Examples

### In ConnectionManager

```java
@Service
public class ConnectionManager {
    private final IAuditLogger auditLogger;
    
    public void createConnection(String userId, ConnectionConfig config) {
        // Create connection...
        auditLogger.logConnectionCreated(userId, config.getId(), config.getType());
    }
    
    public void deleteConnection(String userId, String connectionId) {
        // Delete connection...
        auditLogger.logConnectionDeleted(userId, connectionId);
    }
}
```

### In Anonymizer

```java
@Service
public class Anonymizer {
    private final IAuditLogger auditLogger;
    
    public AnonymizationResult anonymize(String userId, AnonymizationRequest request) {
        try {
            // Perform anonymization...
            long rowsProcessed = 50000;
            
            auditLogger.logAnonymization(
                userId,
                request.getSourceDatabase(),
                request.getTargetDatabase(),
                rowsProcessed,
                true,
                null
            );
            
            return new AnonymizationResult(true, rowsProcessed);
        } catch (Exception e) {
            auditLogger.logAnonymization(
                userId,
                request.getSourceDatabase(),
                request.getTargetDatabase(),
                0,
                false,
                e.getMessage()
            );
            throw e;
        }
    }
}
```

### In PII Scanner

```java
@Service
public class PIIScanner {
    private final IAuditLogger auditLogger;
    
    public PIIScanResult scan(String userId, String database) {
        // Perform PII scan...
        long rowsScanned = 100000;
        
        auditLogger.logPIIScan(userId, database, rowsScanned);
        
        return new PIIScanResult(/* ... */);
    }
}
```

## Querying Audit Logs

### Via REST API (Future Implementation)

```
GET /api/audit-logs?startDate=2024-01-01&endDate=2024-01-31&userId=user-123&action=ANONYMIZATION
```

### Via Service

```java
@Autowired
private IAuditLogger auditLogger;

public void viewAuditLogs() {
    LocalDateTime startDate = LocalDateTime.now().minusDays(30);
    LocalDateTime endDate = LocalDateTime.now();
    
    List<AuditLogEntry> logs = auditLogger.getAuditLogs(
        startDate,
        endDate,
        "user-123",
        "ANONYMIZATION"
    );
    
    for (AuditLogEntry log : logs) {
        System.out.println("Action: " + log.getAction());
        System.out.println("Timestamp: " + log.getTimestamp());
        System.out.println("Success: " + log.isSuccess());
        System.out.println("Rows Processed: " + log.getRowsProcessed());
    }
}
```

## Best Practices

1. **Always log user actions**: Log all significant user actions for audit trail
2. **Include error messages**: When logging failures, include meaningful error messages
3. **Use appropriate action types**: Use predefined action types when possible
4. **Add metadata**: Include relevant metadata for context
5. **Handle exceptions**: Catch exceptions and log them with error details
6. **Test audit logging**: Verify audit logs are created correctly in tests

## Troubleshooting

### Audit logs not being created

1. Check that TenantContextHolder has a valid tenant context set
2. Verify the database connection is working
3. Check application logs for errors
4. Ensure the audit_logs table exists in the database

### Retention policy not running

1. Check that the scheduled task is enabled
2. Verify the cron expression in application.yml
3. Check application logs for retention policy execution
4. Manually trigger retention policy for testing

### Performance issues

1. Ensure indexes are created on the audit_logs table
2. Consider archiving old audit logs to a separate database
3. Implement pagination when querying large result sets
4. Monitor database query performance

## GDPR Compliance

The audit logging infrastructure ensures GDPR compliance by:

1. **Logging all data processing activities**: All user actions are logged
2. **Maintaining audit trail**: Immutable logs provide complete audit trail
3. **Enforcing retention policies**: Automatic deletion after 1 year
4. **Encrypting sensitive data**: Metadata encrypted at rest
5. **Isolating tenant data**: Complete data isolation in multi-tenant environments
6. **Providing data access**: Audit logs can be exported for data subject requests

## References

- Requirements: 16.1, 16.2, 16.5, 16.6
- GDPR Compliance: Data Processing Agreement, Audit Trail Requirements
- Spring Boot Documentation: Scheduled Tasks, Spring Data JPA
- Encryption: AES-256 (CredentialEncryption from Task 1.5)
