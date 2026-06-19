package com.stepside.StepSide.notification.dto; // <-- SANEADO: Tu nueva ruta oficial por Feature

import java.util.Map;

/**
 * Contrato inmutable que encapsula los datos requeridos para despachar una notificación.
 * Diseñado por Fabián para reutilización universal en cualquier Service del backend NoSQL.
 */
public record EmailMessageDto(
        String to,              // Destinatario (Ej: "cliente@empresa.com")
        String subject,         // Asunto del correo
        String templateName,    // Nombre de la plantilla indexada en Atlas (Ej: "NEED_APPROVAL")
        Map<String, Object> model // Variables dinámicas que se inyectarán en el HTML (Nombre, link, etc)
) {
    /**
     * CONSTRUCTOR COMPACTO SENIOR: Aplica peajes rígidos de control perimetral
     * y garantiza que el mapa de variables dinámicas nunca sea nulo en la memoria.
     */
    public EmailMessageDto {
        if (to == null || to.isBlank()) throw new IllegalArgumentException("El destinatario (to) es obligatorio.");
        if (subject == null || subject.isBlank()) throw new IllegalArgumentException("El asunto (subject) es obligatorio.");
        if (templateName == null || templateName.isBlank()) throw new IllegalArgumentException("La plantilla es obligatoria.");
        if (model == null) model = Map.of();
    }
}
