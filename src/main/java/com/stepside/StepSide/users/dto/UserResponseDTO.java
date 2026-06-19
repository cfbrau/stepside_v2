package com.stepside.StepSide.users.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO inmutable de alta gama para el Backoffice de StepSide.
 * Proporciona una visión global unificada del usuario y su TTO en una estructura plana.
 */
public record UserResponseDTO(
        String id,
        String email,
        String status, // Nombre resuelto de la tabla user_status
        Instant expiration,
        Instant createdAt,

        // Datos del TTO Persona integrados en la raíz
        String personTtoId,
        String personApellido, // tto.attributes.apellido
        String personName,     // tto.attributes.nombre
        List<Map<String, Object>> personRelations // tto.relations
) {}
