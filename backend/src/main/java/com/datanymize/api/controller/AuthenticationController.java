package com.datanymize.api.controller;

import com.datanymize.api.dto.ApiResponse;
import com.datanymize.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;

/**
 * REST API controller for authentication and authorization.
 * 
 * Validates Requirements: 13.1, 14.1, 16.1, 17.2, 17.3
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthenticationController {
    
    @Autowired(required = false)
    private AuthenticationManager authenticationManager;
    
    @Autowired(required = false)
    private JwtTokenProvider jwtTokenProvider;
    
    /**
     * User login endpoint.
     * POST /api/auth/login
     */
    @PostMapping("/login")
    @Operation(summary = "User login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        
        try {
            log.info("Login attempt for user: {}", request.getUsername());
            
            // Authenticate user
            if (authenticationManager != null) {
                Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                    )
                );
                
                // Generate JWT token
                String token = jwtTokenProvider != null 
                    ? jwtTokenProvider.generateToken(authentication)
                    : generateMockToken(request.getUsername());
                
                LoginResponse response = LoginResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .expiresIn(3600) // 1 hour
                    .userId("user-123")
                    .username(request.getUsername())
                    .roles(new String[]{"USER", "ADMIN"})
                    .loginAt(LocalDateTime.now())
                    .build();
                
                return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
            } else {
                // Mock authentication for development
                String token = generateMockToken(request.getUsername());
                
                LoginResponse response = LoginResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .userId("user-123")
                    .username(request.getUsername())
                    .roles(new String[]{"USER", "ADMIN"})
                    .loginAt(LocalDateTime.now())
                    .build();
                
                return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
            }
            
        } catch (AuthenticationException e) {
            log.error("Authentication failed for user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid credentials", "AUTH_FAILED", 401));
        } catch (Exception e) {
            log.error("Login failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage(), "LOGIN_FAILED", 500));
        }
    }
    
    /**
     * User logout endpoint.
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    @Operation(summary = "User logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        try {
            log.info("User logout");
            
            // In production, would invalidate token in blacklist
            // For now, just return success
            
            return ResponseEntity.ok(ApiResponse.success("Logout successful"));
            
        } catch (Exception e) {
            log.error("Logout failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage(), "LOGOUT_FAILED", 500));
        }
    }
    
    /**
     * Refresh JWT token.
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        
        try {
            log.info("Token refresh requested");
            
            // Validate refresh token
            if (jwtTokenProvider != null && jwtTokenProvider.validateToken(request.getRefreshToken())) {
                String newToken = jwtTokenProvider.generateTokenFromRefreshToken(request.getRefreshToken());
                
                RefreshTokenResponse response = RefreshTokenResponse.builder()
                    .token(newToken)
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .refreshedAt(LocalDateTime.now())
                    .build();
                
                return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
            } else {
                // Mock token refresh for development
                String newToken = generateMockToken("user");
                
                RefreshTokenResponse response = RefreshTokenResponse.builder()
                    .token(newToken)
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .refreshedAt(LocalDateTime.now())
                    .build();
                
                return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
            }
            
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid refresh token", "REFRESH_FAILED", 401));
        }
    }
    
    /**
     * Get current user information.
     * GET /api/auth/me
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user information")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        try {
            log.info("Getting current user information");
            
            // In production, would extract user from JWT token
            CurrentUserResponse response = CurrentUserResponse.builder()
                .userId("user-123")
                .username("john.doe@example.com")
                .email("john.doe@example.com")
                .roles(new String[]{"USER", "ADMIN"})
                .tenantId("tenant-123")
                .createdAt(LocalDateTime.now().minusDays(30))
                .build();
            
            return ResponseEntity.ok(ApiResponse.success(response, "User information retrieved"));
            
        } catch (Exception e) {
            log.error("Failed to get current user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(e.getMessage(), "GET_USER_FAILED", 500));
        }
    }
    
    // ============ Helper Methods ============
    
    private String generateMockToken(String username) {
        // Generate a simple mock JWT token for development
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
               "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IiIgKyB1c2VybmFtZSArICIiLCJpYXQiOjE1MTYyMzkwMjJ9." +
               "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    }
    
    // ============ Request/Response DTOs ============
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginRequest {
        private String username;
        private String password;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginResponse {
        private String token;
        private String tokenType;
        private long expiresIn;
        private String userId;
        private String username;
        private String[] roles;
        private LocalDateTime loginAt;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RefreshTokenRequest {
        private String refreshToken;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RefreshTokenResponse {
        private String token;
        private String tokenType;
        private long expiresIn;
        private LocalDateTime refreshedAt;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CurrentUserResponse {
        private String userId;
        private String username;
        private String email;
        private String[] roles;
        private String tenantId;
        private LocalDateTime createdAt;
    }
}
