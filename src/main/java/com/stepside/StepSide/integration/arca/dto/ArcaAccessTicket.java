package com.stepside.StepSide.integration.arca.dto;

import java.time.LocalDateTime;

/**
 * Contrato inmutable que gobierna las credenciales dinámicas otorgadas por el WSAA.
 * Saneado para mitigar penalizaciones de red mediante resguardo de expiración elástico.
 */
public record ArcaAccessTicket(
        String token,
        String sign,
        LocalDateTime expiration
) {
    public boolean isExpired() {
        // Retorna true si ya expiró o si faltan menos de 5 minutos para que expire (Margen de seguridad)
        return LocalDateTime.now().isAfter(this.expiration.minusMinutes(5));
    }
}
