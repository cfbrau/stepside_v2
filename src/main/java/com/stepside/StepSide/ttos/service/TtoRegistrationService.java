package com.stepside.StepSide.ttos.service;

import com.stepside.StepSide.ttos.dto.TtoRegistrationRequestDto;
import com.stepside.StepSide.ttos.dto.TtoRegistrationResponseDto;

/**
 * Contrato de negocio central para gobernar el onboarding elástico en StepSide.
 * Orquesta la creación atómica de credenciales y el espejado de grafos NoSQL en MongoDB Atlas.
 */
public interface TtoRegistrationService {

    /**
     * Ejecuta el flujo secuencial de registro: valida unicidad, extrae códigos naturales,
     * persiste los documentos TTO (Persona y Empresa) y da de alta la cuenta de seguridad.
     */
    TtoRegistrationResponseDto registerEcosystemUser(TtoRegistrationRequestDto requestDto);
}
