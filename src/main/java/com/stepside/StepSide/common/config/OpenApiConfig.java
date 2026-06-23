package com.stepside.StepSide.common.config;

import io.jsonwebtoken.lang.Collections;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gobierno centralizado de la documentación OpenAPI / Swagger de StepSide.
 * Inyecta los contratos de seguridad JWT requeridos para pruebas perimetrales en Cloud Run.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "BearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("StepSide Identity & Governance API")
                        .version("v2.0.0")
                        .description("Arquitectura central NoSQL para el Backoffice y el control perimetral de StepSide."))
                // Activamos el requerimiento de seguridad global en la interfaz web
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingrese estrictamente el Token JWT Bearer obtenido del endpoint /api/auth/login para desbloquear las consultas privadas.")));
    }
}
