package com.stepside.StepSide.ttos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Contrato inmutable de entrada para el registro unificado del ecosistema StepSide.
 * Ataja credenciales de seguridad combinadas con esquemas elásticos NoSQL de TTO.
 */
public record TtoRegistrationRequestDto(

        // 1. Datos perimetrales de la cuenta de usuario
        @NotBlank(message = "El e-mail institucional es obligatorio.")
        @Email(message = "El formato del e-mail es inválido.")
        String email,

        @NotBlank(message = "La contraseña de acceso es obligatoria.")
        @Size(min = 8, message = "La clave debe contener al menos 8 caracteres de control.")
        String password,

        // 2. Atributos dinámicos del TTO Persona (ej: nombre, apellido, dni, legajo)
        @NotNull(message = "El mapa de atributos de la persona no puede ser nulo.")
        Map<String, Object> personAttributes,

        // 3. Atributos dinámicos del TTO Empresa asociada (ej: razón social, cuit, rubro)
        @NotNull(message = "El mapa de atributos de la organización no puede ser nulo.")
        Map<String, Object> companyAttributes
) {}
