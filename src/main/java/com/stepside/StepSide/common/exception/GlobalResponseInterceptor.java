package com.stepside.StepSide.common.exception;

import com.stepside.StepSide.common.dto.ApiResponseDto;
import com.stepside.StepSide.common.dto.PaginationMetaDto;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Collection;

/**
 * Interceptor global de red que actúa como middleware de arquitectura.
 * Automatiza la envoltura de bloques 'data' y 'meta' bajo los más altos estándares corporativos.
 */
@RestControllerAdvice
public class GlobalResponseInterceptor implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(@NonNull MethodParameter returnType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        String className = returnType.getDeclaringClass().getName();

        // Exclusión hermética de documentación Swagger/OpenAPI para evitar corrupturas de UI
        return !className.contains("springdoc") && !className.contains("openapi");
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            @NonNull MethodParameter returnType,
            @NonNull MediaType selectedContentType,
            @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response) {

        // ESCUDO CONTRA CLASSCASTEXCEPTION: Si el motor de Spring espera un String,
        // envolverlo en un DTO romperá el pipeline de serialización. Se deja pasar crudo.
        if (selectedConverterType.isAssignableFrom(StringHttpMessageConverter.class) || body instanceof String) {
            return body;
        }

        // Blindaje perimetral: Evitamos dobles envolturas en respuestas de error o ya formateadas
        if (body instanceof ApiResponseDto || body instanceof ErrorResponseDto) {
            return body;
        }

        // Manejo estandarizado para respuestas vacías (Void / null) manteniendo la firma limpia
        if (body == null) {
            return new ApiResponseDto(null, null);
        }

        // SOPORTE ELÁSTICO NOSQL: Detección de paginación real de Spring Data MongoDB
        if (body instanceof Page<?> page) {
            PaginationMetaDto meta = new PaginationMetaDto(
                    page.getTotalElements(),
                    page.getTotalPages(),
                    page.getNumber() + 1 // Ajuste elástico de índice 0 de CPU a índice 1 de Humano
            );
            return new ApiResponseDto(page.getContent(), meta);
        }

        // Fallback controlado para colecciones masivas no paginadas (listas en memoria)
        if (body instanceof Collection<?> collection) {
            PaginationMetaDto meta = new PaginationMetaDto(collection.size(), 1, 1);
            return new ApiResponseDto(collection, meta);
        }

        // Fallback universal para recursos u objetos individuales O(1)
        return new ApiResponseDto(body, null);
    }
}
