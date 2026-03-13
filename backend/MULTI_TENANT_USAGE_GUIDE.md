# Multi-Tenant Infrastructure Usage Guide

## Quick Start

### 1. Creating a Tenant

```java
@Autowired
private ITenantManager tenantManager;

// Create a new tenant
TenantContext tenant = tenantManager.createTenant("Acme Corporation");
// Returns: TenantContext with tenantId, schemaName, etc.
```

### 2. Setting Current Tenant in Request

```java
// In a controller or service
@Autowired
private ITenantManager tenantManager;

@PostMapping("/api/data")
public ResponseEntity<?> createData(@RequestHeader("X-Tenant-ID") String tenantId) {
    // Tenant is automatically set by TenantIsolationFilter
    // Access current tenant
    TenantContext current = tenantManager.getCurrentTenant();
    // Use current tenant ID
    String tenantId = current.getTenantId();
}
```

### 3. Creating Tenant-Aware Services

```java
@Service
public class MyDataService extends TenantAwareService {
    
    @Autowired
    private MyDataRepository repository;
    
    public void processData(String dataId) {
        // Automatically validates tenant access
        validateTenantAccess(getCurrentTenantId());
        
        // Get current tenant schema
        String schema = getCurrentTenantSchema();
        
        // Use repository (which is also tenant-aware)
        repository.findData(dataId);
    }
}
```

### 4. Creating Tenant-Aware Repositories

```java
@Repository
public class MyDataRepository extends TenantAwareRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    public Data findData(String dataId) {
        // Get schema-qualified table name
        String tableName = getSchemaQualifiedTableName("data");
        
        // Use in query
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{dataId}, 
            new DataRowMapper());
    }
}
```

## Request Flow

### 1. HTTP Request with Tenant ID

```
POST /api/data
X-Tenant-ID: tenant_abc123def456
Content-Type: application/json

{"name": "My Data"}
```

### 2. Tenant Extraction

The `TenantIsolationFilter` automatically:
1. Extracts tenant ID from header (X-Tenant-ID)
2. Validates tenant exists and is active
3. Sets tenant context for the request
4. Clears context after response

### 3. Service Processing

Your service can access tenant context:

```java
@Service
public class DataService extends TenantAwareService {
    
    public void process() {
        // Get current tenant
        TenantContext tenant = getCurrentTenantContext();
        
        // Validate access
        validateTenantAccess(tenant.getTenantId());
        
        // Use tenant schema
        String schema = getCurrentTenantSchema();
    }
}
```

## Tenant ID Sources (Priority Order)

The filter tries to extract tenant ID from:

1. **Header**: `X-Tenant-ID` (recommended)
   ```
   X-Tenant-ID: tenant_abc123
   ```

2. **Path Variable**: `/tenants/{tenantId}/...`
   ```
   GET /api/tenants/tenant_abc123/data
   ```

3. **Query Parameter**: `?tenantId=...`
   ```
   GET /api/data?tenantId=tenant_abc123
   ```

## Error Handling

### Tenant Not Found

```java
try {
    tenantManager.setCurrentTenant(invalidTenant);
} catch (IllegalArgumentException e) {
    // Tenant doesn't exist or is inactive
    return ResponseEntity.notFound().build();
}
```

### Cross-Tenant Access Attempt

```java
try {
    tenantManager.validateTenantAccess(otherTenantId);
} catch (IllegalArgumentException e) {
    // Access denied
    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
}
```

### No Tenant Context

```java
try {
    TenantContext current = tenantManager.getCurrentTenant();
} catch (IllegalStateException e) {
    // No tenant set in current request
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
}
```

## Database Schema Isolation

Each tenant gets a unique schema:

```
Schema: schema_tenant_abc123def456
Tables:
  - schema_tenant_abc123def456.users
  - schema_tenant_abc123def456.orders
  - schema_tenant_abc123def456.products
```

Use `TenantAwareRepository.getSchemaQualifiedTableName()`:

```java
String tableName = getSchemaQualifiedTableName("users");
// Returns: "schema_tenant_abc123def456.users"
```

## Testing Tenant Isolation

### Unit Test Example

```java
@SpringBootTest
public class MyServiceTest {
    
    @Autowired
    private ITenantManager tenantManager;
    
    @Autowired
    private MyService service;
    
    @Test
    void testTenantIsolation() {
        // Create two tenants
        TenantContext tenant1 = tenantManager.createTenant("Tenant 1");
        TenantContext tenant2 = tenantManager.createTenant("Tenant 2");
        
        // Set tenant1
        tenantManager.setCurrentTenant(tenant1);
        
        // Verify tenant1 access allowed
        assertTrue(tenantManager.validateTenantAccess(tenant1.getTenantId()));
        
        // Verify tenant2 access denied
        assertFalse(tenantManager.validateTenantAccess(tenant2.getTenantId()));
    }
}
```

### Integration Test Example

```java
@SpringBootTest
@AutoConfigureMockMvc
public class TenantIsolationIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ITenantManager tenantManager;
    
    @Test
    void testCrossTenantAccessBlocked() throws Exception {
        // Create tenant
        TenantContext tenant = tenantManager.createTenant("Test Tenant");
        
        // Try to access with different tenant ID
        mockMvc.perform(get("/api/data")
            .header("X-Tenant-ID", "different_tenant_id"))
            .andExpect(status().isForbidden());
    }
}
```

## Common Patterns

### Pattern 1: Service with Tenant Validation

```java
@Service
public class OrderService extends TenantAwareService {
    
    public Order getOrder(String orderId) {
        // Validate tenant access
        validateTenantAccess(getCurrentTenantId());
        
        // Log operation
        logTenantOperation("GET_ORDER", "orderId=" + orderId);
        
        // Process
        return repository.findOrder(orderId);
    }
}
```

### Pattern 2: Repository with Schema Isolation

```java
@Repository
public class OrderRepository extends TenantAwareRepository {
    
    public List<Order> findByCustomer(String customerId) {
        String tableName = getSchemaQualifiedTableName("orders");
        String sql = "SELECT * FROM " + tableName + " WHERE customer_id = ?";
        return jdbcTemplate.query(sql, new Object[]{customerId}, 
            new OrderRowMapper());
    }
}
```

### Pattern 3: Controller with Tenant Extraction

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(
        @PathVariable String orderId,
        @RequestHeader("X-Tenant-ID") String tenantId) {
        
        // Tenant is already validated by filter
        Order order = orderService.getOrder(orderId);
        return ResponseEntity.ok(order);
    }
}
```

## Configuration

### Spring Security Configuration

The `SecurityConfiguration` class automatically:
1. Registers `TenantIsolationFilter`
2. Configures authorization rules
3. Sets up CORS and CSRF

No additional configuration needed!

### Custom Tenant Extraction

To customize tenant extraction, override `TenantIsolationFilter.extractTenantId()`:

```java
@Component
public class CustomTenantIsolationFilter extends TenantIsolationFilter {
    
    @Override
    protected String extractTenantId(HttpServletRequest request) {
        // Custom extraction logic
        return request.getHeader("X-Custom-Tenant-Header");
    }
}
```

## Troubleshooting

### Issue: "No tenant context set for current request"

**Cause**: Tenant ID not provided in request

**Solution**: Add tenant ID to request:
```
X-Tenant-ID: tenant_abc123
```

### Issue: "Access denied: Cannot access data from another tenant"

**Cause**: Trying to access different tenant's data

**Solution**: Use correct tenant ID in request header

### Issue: "Tenant not found or inactive"

**Cause**: Tenant ID doesn't exist or is inactive

**Solution**: Verify tenant ID is correct and tenant is active

### Issue: "Invalid tenant context"

**Cause**: Tenant context is null or inactive

**Solution**: Ensure tenant is created and active before setting context

## Best Practices

1. **Always use TenantAwareService/Repository**: Ensures tenant validation
2. **Use schema-qualified table names**: Prevents cross-tenant queries
3. **Validate tenant access**: Call validateTenantAccess() at service entry
4. **Log tenant operations**: Use logTenantOperation() for audit trail
5. **Handle exceptions**: Catch IllegalArgumentException and IllegalStateException
6. **Test tenant isolation**: Write tests for cross-tenant scenarios
7. **Use X-Tenant-ID header**: Standard way to pass tenant ID
8. **Clear context in finally**: Filter does this automatically

## Performance Tips

1. **Cache tenant context**: Avoid repeated lookups
2. **Use schema-qualified names**: Prevents ambiguous queries
3. **Index tenant columns**: Speed up tenant-scoped queries
4. **Monitor tenant operations**: Track performance per tenant

## Security Tips

1. **Validate tenant access**: Always call validateTenantAccess()
2. **Use schema isolation**: Prevents SQL injection attacks
3. **Log all access**: Audit trail for compliance
4. **Encrypt tenant data**: Use encryption for sensitive data
5. **Rotate tenant keys**: Periodically rotate encryption keys
6. **Monitor cross-tenant attempts**: Alert on unauthorized access

## Migration Guide

### From Single-Tenant to Multi-Tenant

1. **Add tenant ID to requests**: Include X-Tenant-ID header
2. **Extend services**: Make services extend TenantAwareService
3. **Extend repositories**: Make repositories extend TenantAwareRepository
4. **Update queries**: Use schema-qualified table names
5. **Test isolation**: Write tests for tenant isolation
6. **Deploy**: Deploy with new multi-tenant infrastructure

## References

- `TenantContext`: Tenant information model
- `ITenantManager`: Tenant management interface
- `TenantManager`: Tenant management implementation
- `TenantContextHolder`: Thread-local tenant storage
- `TenantIsolationFilter`: Spring Security filter
- `TenantAwareService`: Base class for services
- `TenantAwareRepository`: Base class for repositories
- `TenantIsolationProperties`: Property-based tests
