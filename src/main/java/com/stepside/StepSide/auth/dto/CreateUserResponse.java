package com.stepside.StepSide.auth.dto; // <-- SANEADO: Tu nueva ruta oficial NoSQL

/**
 * Contrato de salida (Response) inmutable que certifica el Onboarding exitoso.
 * Retorna los identificadores alfanuméricos Strings nativos de MongoDB Atlas.
 */
public record CreateUserResponse(
        String userId,
        String email,
        String personTtoId,
        String companyTtoId
) {}
