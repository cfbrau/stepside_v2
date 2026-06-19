package com.stepside.StepSide.users.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

/**
 * Contrato optimizado NoSQL para la aprobación administrativa de cuentas.
 * Elimina estructuras anidadas delegando el control a un mapa elástico de permisos.
 */
public record UserApprovalRequestDTO(

        @NotEmpty(message = "Debe asignar al menos una aplicación con su respectivo rol corporativo.")
        // Estructura limpia en red: {"MONITORING_API": "ADMIN", "TELEMETRY_VIEW": "OPERATOR"}
        Map<String, String> permissions
) {}
