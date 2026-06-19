package com.stepside.StepSide.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Envoltorio inmutable corporativo universal para la estandarización de respuestas REST.
 * Saneado por Fabián para garantizar visibilidad global y tipado fuerte en el middleware.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponseDto(
        Object data,
        PaginationMetaDto meta
) {
    public static ApiResponseDto withMeta(Object data, PaginationMetaDto meta) {
        return new ApiResponseDto(data, meta);
    }

    public static ApiResponseDto simple(Object data) {
        return new ApiResponseDto(data, null);
    }
}
