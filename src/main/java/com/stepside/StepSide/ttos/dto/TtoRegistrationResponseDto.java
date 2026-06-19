package com.stepside.StepSide.ttos.dto;

import java.util.Date;

/**
 * Contrato inmutable de salida corporativo que certifica el alta encolada.
 * Retorna los identificadores alfanuméricos hashes nativos de MongoDB Atlas.
 */
public record TtoRegistrationResponseDto(
        String userId,
        String email,
        String statusName,
        String personTtoId,
        String companyTtoId,
        Date registeredAt
) {}
