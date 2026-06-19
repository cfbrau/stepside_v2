package com.stepside.StepSide.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO inmutable de salida para la unificación de errores en la plataforma.
 * Saneado por Fabián para responder con un estándar enterprise hacia el Frontend.
 */
public record ErrorResponseDto(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,

        @JsonInclude(JsonInclude.Include.NON_NULL) // Oculta el casillero en la red si no hay errores de bindings
        Map<String, String> validations
) {}
