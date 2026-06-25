package com.stepside.StepSide.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contrato de entrada aplanado y simplificado para el Onboarding de StepSide.
 * Elimina la complejidad de nodos anidados para agilizar el consumo desde el Frontend.
 */
public record CreateUserRequest(
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 50, message = "El nombre no puede superar los 50 caracteres.")
        String firstName,

        @NotBlank(message = "El apellido es obligatorio.")
        @Size(max = 50, message = "El apellido no puede superar los 50 caracteres.")
        String lastName,

        @NotBlank(message = "El correo electrónico es obligatorio.")
        @Email(message = "El formato del correo electrónico no es válido.")
        @Size(max = 100, message = "El email no puede superar los 100 caracteres.")
        String email,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 8, max = 255, message = "La contraseña debe tener entre 8 y 255 caracteres.")
        String password,

        @NotBlank(message = "El CUIT de la empresa es obligatorio.")
        @Size(max = 20, message = "El CUIT no puede superar los 20 caracteres.")
        String companyCuit,

        @NotBlank(message = "La razón social de la empresa es obligatoria.")
        @Size(max = 150, message = "La razón social no puede superar los 150 caracteres.")
        String companyRazonSocial,

        @NotBlank(message = "El nombre de fantasía de la empresa es obligatorio.")
        @Size(max = 100, message = "El nombre de fantasía no puede superar los 100 caracteres.")
        String companyNombreFantasia
) {}
