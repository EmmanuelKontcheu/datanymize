package com.datanymize.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for Datanymize REST API.
 * 
 * Provides comprehensive API documentation with:
 * - API metadata and versioning
 * - Security scheme definitions (JWT Bearer tokens)
 * - Contact and license information
 * - Automatic endpoint documentation
 */
@Configuration
public class OpenApiConfiguration {
    
    @Bean
    public OpenAPI datanymizeOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Datanymize API")
                .description("Multi-database anonymization SaaS platform REST API. " +
                    "Enables developers to safely synchronize realistic test data from production " +
                    "databases into local development and test environments while maintaining GDPR compliance.")
                .version("1.0.0")
                .contact(new Contact()
                    .name("Datanymize Support")
                    .email("support@datanymize.com")
                    .url("https://datanymize.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
            .components(new Components()
                .addSecuritySchemes("bearer-jwt",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT Bearer token for API authentication")))
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
