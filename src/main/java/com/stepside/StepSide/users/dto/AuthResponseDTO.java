package com.stepside.StepSide.users.dto;

/**
 * DTO de salida perimetral que transporta el Token JWT hacia el cliente.
 */
public record AuthResponseDTO(
        String token,
        String tokenType, // Habitualmente será "Bearer"
        String email
) {}
