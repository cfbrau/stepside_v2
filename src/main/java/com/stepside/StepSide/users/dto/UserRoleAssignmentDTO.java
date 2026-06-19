package com.stepside.StepSide.users.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO inmutable corporativo utilizado para la asignación y desvinculación individual de permisos.
 * Saneado por Fabián para el soporte de identificadores alfanuméricos en MongoDB Atlas.
 */
public record UserRoleAssignmentDTO(
        @NotNull(message = "El identificador de la aplicación (applicationId) es obligatorio.")
        String applicationId, // Saneado a String para hashes NoSQL

        @NotNull(message = "El identificador del rol (roleId) es obligatorio.")
        String roleId // Saneado a String para hashes NoSQL
) {}
