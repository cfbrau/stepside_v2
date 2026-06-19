package com.stepside.StepSide.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Interceptor global de excepciones corporativo de StepSide.
 * Saneado por Fabián bajo estrictas normas NoSQL para el soporte de MongoDB Atlas.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Captura errores de lógica de negocio o argumentos inválidos.
     * Transforma un feo error 500 en un prolijo HTTP 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        String message = ex.getMessage() != null ? ex.getMessage() : "";

        if (message.toLowerCase().contains("no existe") || message.toLowerCase().contains("no se encontró")) {
            ErrorResponseDto error = new ErrorResponseDto(
                    LocalDateTime.now(),
                    HttpStatus.NOT_FOUND.value(),
                    "Recurso No Encontrado",
                    ex.getMessage(),
                    request.getRequestURI(),
                    null
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Solicitud Incorrecta",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Intercepta los fallos de validaciones de Spring (@Valid / @NotBlank / @Size).
     * Devuelve una lista detallada campo por campo con un HTTP 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Error de Validación",
                "El JSON enviado no cumple con las restricciones requeridas.",
                request.getRequestURI(),
                errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * MIGRACIÓN NOSQL: Captura fallos de ausencia de datos en colecciones BSON (MongoDB).
     * Transforma la excepción nativa de Java en un prolijo HTTP 404 Not Found.
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponseDto> handleNoSuchElementException(NoSuchElementException ex, HttpServletRequest request) {
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Recurso No Encontrado",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * MIGRACIÓN NOSQL: Captura violaciones de integridad de datos en el cluster de MongoDB Atlas.
     * Saneado por Fabián para interpretar el error E11000 de duplicados.
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleDataIntegrityViolationException(
            org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {

        String errorMessage = "";
        String userMessage = "Error de consistencia de datos en la persistencia cloud.";

        if (ex.getRootCause() != null) {
            errorMessage = ex.getRootCause().getMessage().toLowerCase();
        } else if (ex.getMessage() != null) {
            errorMessage = ex.getMessage().toLowerCase();
        }

        if (!errorMessage.isEmpty()) {
            if (errorMessage.contains("unique") || errorMessage.contains("duplicada") ||
                    errorMessage.contains("ya existe") || errorMessage.contains("registrado") || errorMessage.contains("e11000")) {

                if (errorMessage.contains("email") || errorMessage.contains("correo") || errorMessage.contains("account")) {
                    userMessage = "Error de Registro: El correo electrónico ingresado ya se encuentra asociado a una cuenta activa.";
                } else if (errorMessage.contains("code") || errorMessage.contains("cuit") || errorMessage.contains("dni")) {
                    userMessage = "Error de Identidad: El código de negocio o documento ya existe registrado en el sistema.";
                } else {
                    userMessage = "Error de duplicidad NoSQL: " + (ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage());
                }
            }
        }

        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Conflicto de Datos",
                userMessage,
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Captura fallos de credenciales inválidas, cuentas pendientes o bloqueadas.
     * Transforma el error en un prolijo HTTP 401 Unauthorized corporativo.
     */
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthenticationException(
            org.springframework.security.core.AuthenticationException ex, HttpServletRequest request) {

        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "No Autorizado",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Captura errores de rutas inexistentes o recursos estáticos no encontrados en Spring Boot 3.
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoResourceFoundException(
            org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpServletRequest request) {

        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Ruta Inexistente",
                "El endpoint o recurso al que intenta acceder no existe en la plataforma. Verifique la sintaxis de la URL.",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Línea de defensa final. Captura cualquier excepción no controlada en el sistema.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleAllUncaughtExceptions(Exception ex, HttpServletRequest request) {
        ErrorResponseDto error = new ErrorResponseDto(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Error Interno del Servidor",
                "Ocurrió un error imprevisto en la plataforma. Por favor, contacte al administrador. Detalle: " + ex.getMessage(),
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
