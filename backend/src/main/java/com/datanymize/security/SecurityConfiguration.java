package com.datanymize.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for Datanymize.
 * 
 * Configures:
 * - Tenant isolation filter for multi-tenant support
 * - CORS and CSRF settings
 * - Basic security rules
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {
    
    private final TenantIsolationFilter tenantIsolationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Add tenant isolation filter before authentication
            .addFilterBefore(tenantIsolationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // Configure authorization
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/health").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            
            // Disable CSRF for now (enable in production with proper token handling)
            .csrf(csrf -> csrf.disable())
            
            // Configure CORS
            .cors(cors -> {});
        
        return http.build();
    }
}
