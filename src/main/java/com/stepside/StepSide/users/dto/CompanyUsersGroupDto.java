package com.stepside.StepSide.users.dto;

import java.util.List;

/**
 * Contrato inmutable especializado para la vista jerárquica del Backoffice.
 * Agrupa las identidades y sus estados de aprobación bajo el nodo gobernante de cada Empresa.
 */
public record CompanyUsersGroupDto(
        String companyId,
        String companyCuit,
        String companyName,

        // Nómina elástica de empleados asociados y vigentes en el cluster cloud
        List<UserExpandedNode> employees
) {
    public record UserExpandedNode(
            String userId,
            String email,
            String status,
            String personName,
            String personDni
    ) {}
}
