package com.stepside.StepSide.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO inmutable para capturar el intento de inicio de sesión en StepSide.
 */
public record LoginRequestDTO(
        @NotBlank(message = "El email no puede estar vacío.")
        @Email(message = "El formato del email es inválido.")
        String email,

        @NotBlank(message = "La contraseña no puede estar vacía.")
        String password
) {}
