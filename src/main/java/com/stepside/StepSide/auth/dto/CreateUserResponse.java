package com.stepside.StepSide.auth.dto;

/**
 * DTO de salida unificado que confirma la creación atómica de la cuenta,
 * inyectando los identificadores de persistencia NoSQL generados.
 */
public record CreateUserResponse(
        String userId,
        String email,
        String personTtoId,
        String companyTtoId,
        String message
) {}
