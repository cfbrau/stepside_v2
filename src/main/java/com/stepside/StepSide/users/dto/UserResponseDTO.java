package com.stepside.StepSide.users.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO inmutable de alta gama para el Backoffice de StepSide.
 */
public record UserResponseDTO(
        String id,
        String email,
        String status,
        Instant expiration,
        Instant createdAt,
        String personTtoId,
        String personCode, // <-- Transporta el DNI/Código del TTO
        String personApellido,
        String personName,
        List<Map<String, Object>> personRelations
) {}
