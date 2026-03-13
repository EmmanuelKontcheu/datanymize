package com.datanymize.security;

import com.datanymize.tenant.ITenantManager;
import com.datanymize.tenant.model.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Spring Security filter for tenant isolation.
 * 
 * Enforces tenant boundaries by:
 * 1. Extracting tenant ID from request headers or path
 * 2. Validating tenant access
 * 3. Setting tenant context for the request
 * 4. Clearing tenant context after request completion
 * 
 * Validates Requirements 17.2, 17.3:
 * - Ensures only own tenant data is visible
 * - Prevents cross-tenant access
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantIsolationFilter extends OncePerRequestFilter {
    
    private final ITenantManager tenantManager;
    
    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String TENANT_PATH_VARIABLE = "tenantId";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            // Extract tenant ID from request
            String tenantId = extractTenantId(request);
            
            if (tenantId != null && !tenantId.isEmpty()) {
                // Validate tenant access
                if (!tenantManager.validateTenantAccess(tenantId)) {
                    log.warn("Unauthorized tenant access attempt: {}", tenantId);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, 
                        "Access to tenant data denied");
                    return;
                }
                
                // Get tenant context and set it
                var tenantContext = tenantManager.getTenant(tenantId);
                if (tenantContext.isPresent()) {
                    tenantManager.setCurrentTenant(tenantContext.get());
                    log.debug("Set tenant context for request: {}", tenantId);
                } else {
                    log.warn("Tenant not found: {}", tenantId);
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, 
                        "Tenant not found");
                    return;
                }
            }
            
            // Continue with the filter chain
            filterChain.doFilter(request, response);
            
        } finally {
            // Always clear tenant context after request
            tenantManager.clearCurrentTenant();
        }
    }
    
    /**
     * Extracts tenant ID from request.
     * Tries multiple sources in order:
     * 1. X-Tenant-ID header
     * 2. tenantId path variable
     * 3. tenant query parameter
     * 
     * @param request The HTTP request
     * @return The tenant ID, or null if not found
     */
    private String extractTenantId(HttpServletRequest request) {
        // Try header first
        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId != null && !tenantId.isEmpty()) {
            return tenantId;
        }
        
        // Try path variable
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.contains("/tenants/")) {
            String[] parts = pathInfo.split("/");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("tenants".equals(parts[i]) && i + 1 < parts.length) {
                    return parts[i + 1];
                }
            }
        }
        
        // Try query parameter
        tenantId = request.getParameter("tenantId");
        if (tenantId != null && !tenantId.isEmpty()) {
            return tenantId;
        }
        
        return null;
    }
}
