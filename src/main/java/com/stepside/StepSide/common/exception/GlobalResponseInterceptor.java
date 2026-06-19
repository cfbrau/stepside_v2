package com.stepside.StepSide.common.exception;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import java.util.Collection;

/**
 * Interceptor global de red que actúa como middleware de arquitectura.
 * Automatiza la envoltura de bloques 'data' y 'meta' bajo los más altos estándares NoSQL.
 */
@RestControllerAdvice
public class GlobalResponseInterceptor implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        String className = returnType.getDeclaringClass().getName();
        // Deja pasar de forma limpia los bytes analíticos de Swagger y OpenAPI
        return !className.contains("springdoc") && !className.contains("openapi");
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        if (body == null) {
            return null;
        }

        // Blindaje perimetral NoSQL: Evitamos dobles envolturas en respuestas de error o contratos globales
        if (body instanceof ApiResponseDto || body instanceof ErrorResponseDto) {
            return body;
        }

        // Dejamos pasar cadenas planas de geolocalización o direcciones de Atlas
        if (body instanceof String) {
            return body;
        }

        // Aplicamos el estándar elástico para colecciones masivas
        if (body instanceof Collection<?> collection) {
            PaginationMetaDto meta = new PaginationMetaDto(collection.size(), 1, 1);
            return new ApiResponseDto(collection, meta);
        }

        // Fallback universal para recursos individuales O(1) de altas y búsquedas
        return new ApiResponseDto(body, null);
    }
}

// ============================================================================
// CONTRATOS COMPACTOS DE RED: Declarados al final del archivo para fijar el classpath
// ============================================================================
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
record ApiResponseDto(Object data, PaginationMetaDto meta) {}

record PaginationMetaDto(int totalElements, int totalPages, int currentPage) {}
