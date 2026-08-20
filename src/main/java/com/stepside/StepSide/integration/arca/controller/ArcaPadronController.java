package com.stepside.StepSide.integration.arca.controller;

import com.stepside.StepSide.integration.arca.dto.ArcaPersonaResponseDto;
import com.stepside.StepSide.integration.arca.service.ArcaPadronService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integration/arca")
@RequiredArgsConstructor
@Slf4j
public class ArcaPadronController {

    // Inyectamos la interfaz para mantener el controlador desacoplado de la implementación concreta
    private final ArcaPadronService arcaPadronService;

    /**
     * Endpoint unificado de producción: Retorna DTO procesado directo al Frontend de StepSide.
     * GET /api/integration/arca/consulta-padron?cuit=20270065628
     */
    @GetMapping(value = "/consulta-padron", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ArcaPersonaResponseDto.PersonaData> consultarPadron(@RequestParam String cuit) {
        log.info("[ARCA REST GATEWAY] Recibida consulta externa para el CUIT: {}", cuit);

        // Delegación limpia. El controlador no sabe qué es un String, un XML ni JAXB.
        ArcaPersonaResponseDto.PersonaData dataPersona = arcaPadronService.obtenerPersonaMapeada(cuit);

        return ResponseEntity.ok(dataPersona);
    }
}
