package com.stepside.StepSide.common.exception;

import com.stepside.StepSide.common.exception.domain.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * CONTROLADOR PERIMETRAL GLOBAL DE EXCEPCIONES: Ecosistema StepSide.
 * Saneado bajo especificaciones de seguridad OWASP, inmutabilidad y tipado estricto.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Captura errores de validación de esquemas JSON (@Valid).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        // Uso de API de Streams optimizada para colectar mapas de validaciones
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Campo inválido",
                        (existente, nuevo) -> existente // Mitiga colisiones de campos duplicados
                ));

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Los datos enviados no son válidos.",
                request,
                errors
        );
    }

    /**
     * Captura cuerpos HTTP con JSON mal formado o ilegible.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_JSON",
                "El cuerpo de la solicitud no es un JSON válido.",
                request,
                null
        );
    }

    /**
     * Captura la ausencia de recursos basada en excepciones de la JVM.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponseDto> handleNoSuchElementException(
            NoSuchElementException ex, HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "Recurso no encontrado.",
                request,
                null
        );
    }

    /**
     * Captura las excepciones de dominio con código HTTP explícito y formato estándar.
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponseDto> handleDomainException(
            DomainException ex, HttpServletRequest request) {

        return buildErrorResponse(
                ex.getStatus(),
                ex.getCode(),
                ex.getMessage(),
                request,
                null
        );
    }

    /**
     * Captura credenciales inválidas en endpoints de autenticación.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS",
                "Credenciales inválidas.",
                request,
                null
        );
    }

    /**
     * Captura accesos restringidos que alcanzaron la capa del controlador de Spring.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED",
                "Acceso denegado.",
                request,
                null
        );
    }

    /**
     * Captura violaciones de unicidad o restricciones de persistencia NoSQL.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.error("[PERSISTENCIA CLOUD ERROR] Infracción de restricciones en la base de datos: {}", ex.getMessage());

        String rootMessage = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        String userMessage = "Conflicto de unicidad de datos en la persistencia. El registro ya existe.";

        // Saneado seguro: Parsing limitado exclusivamente para orientar al cliente sin revelar la traza interna
        if (rootMessage != null && rootMessage.contains("e11000")) {
            userMessage = "Error de Registro: Los identificadores o credenciales ya se encuentran asociados a una entidad activa.";
        }

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "DATA_CONFLICT",
                "Conflicto de datos.",
                request,
                null
        );
    }

    /**
     * Captura rutas HTTP que no mapean a ningún controlador activo de la API.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "ROUTE_NOT_FOUND",
                "Ruta inexistente.",
                request,
                null
        );
    }

    /**
     * Manejo limpio de argumentos inválidos genéricos.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.error("Error procesando la solicitud", ex);        
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                "Solicitud incorrecta.",
                request,
                null
        );
    }

    /**
     * ESCUDO DE CONTENCIÓN FINAL (Zero-Knowledge): Previene fugas de infraestructura hacia el cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleAllUncaughtExceptions(
            Exception ex, HttpServletRequest request) {

        // Registro asíncrono forense detallado EXCLUSIVAMENTE en la consola del servidor (Seguridad)
        log.error("[FALLO NO CONTROLADO] Incidente crítico perimetral detectado en el path: {} | Detalle: ",
                request.getRequestURI(), ex);

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "Error interno del servidor.",
                request,
                null
        );
    }

    private ResponseEntity<ErrorResponseDto> buildErrorResponse(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request,
            Map<String, String> validations) {

        ErrorResponseDto errorDto = new ErrorResponseDto(
                LocalDateTime.now(),
                status.value(),
                error,
                message,
                request.getRequestURI(),
                validations
        );
        return ResponseEntity.status(status).body(errorDto);
    }
}
