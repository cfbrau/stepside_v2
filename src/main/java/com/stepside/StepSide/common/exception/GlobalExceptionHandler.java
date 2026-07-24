package com.stepside.StepSide.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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

        ErrorResponseDto errorDto = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Error de Validación",
                "El JSON enviado no cumple con las restricciones de la plataforma.",
                request.getRequestURI(),
                errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    /**
     * Captura la ausencia de recursos basada en excepciones de la JVM.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponseDto> handleNoSuchElementException(
            NoSuchElementException ex, HttpServletRequest request) {

        ErrorResponseDto errorDto = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Recurso No Encontrado",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDto);
    }

    /**
     * Captura accesos restringidos que alcanzaron la capa del controlador de Spring.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        ErrorResponseDto errorDto = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "Acceso Denegado",
                "Su jerarquía de acceso no cuenta con los privilegios requeridos para este endpoint.",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorDto);
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

        ErrorResponseDto errorDto = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Conflicto de Datos",
                userMessage,
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorDto);
    }

    /**
     * Captura rutas HTTP que no mapean a ningún controlador activo de la API.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpServletRequest request) {

        ErrorResponseDto errorDto = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Ruta Inexistente",
                "El endpoint al que intenta acceder no existe en la plataforma.",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDto);
    }

    /**
     * Manejo limpio de argumentos inválidos genéricos.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        ErrorResponseDto errorDto = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Solicitud Incorrecta",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
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

        ErrorResponseDto errorDto = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Error Interno del Servidor",
                "Ocurrió un error imprevisto en la plataforma. Por favor, contacte soporte técnico utilizando el timestamp de telemetría.",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
    }
}
