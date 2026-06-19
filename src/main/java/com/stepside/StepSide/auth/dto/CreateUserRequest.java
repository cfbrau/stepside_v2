package com.stepside.StepSide.auth.dto; // <-- SANEADO: Tu nueva ruta oficial NoSQL

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Contrato de entrada (Request) jerárquico inmutable para el Onboarding en MongoDB Atlas.
 * Flexibiliza las descripciones volviéndolas opcionales para permitir el autocompletado en el backend.
 */
public record CreateUserRequest(
        @NotNull(message = "Los datos de la cuenta de usuario son obligatorios.")
        @Valid
        AccountNode account,

        @NotNull(message = "Los datos de la persona física son obligatorios.")
        @Valid
        PersonNode person,

        @NotNull(message = "Los datos de la organización o empresa son obligatorios.")
        @Valid
        CompanyNode company
) {
    public record AccountNode(
            @NotBlank(message = "El correo electrónico es obligatorio.")
            @Email(message = "El formato del correo electrónico ingresado no es válido.")
            @Size(max = 100, message = "El email no puede superar los 100 caracteres.")
            String email,

            @NotBlank(message = "La contraseña es obligatoria.")
            @Size(min = 8, max = 255, message = "La contraseña debe tener entre 8 y 255 caracteres.")
            String password
    ) {}

    public record PersonNode(
            String description,

            @NotNull(message = "Los atributos variables de la persona son obligatorios.")
            Map<String, Object> attributes
    ) {}

    public record CompanyNode(
            String description,

            @NotNull(message = "Los atributos dinámicos de la empresa son obligatorios.")
            Map<String, Object> attributes
    ) {}
}
