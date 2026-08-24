package com.stepside.StepSide.ttos.dto;

import java.util.List;
import java.util.Map;

/**
 * CONTRATO DE ANALÍTICA AVANZADA B2B: Dominio TTOs - StepSide.
 * Saneado y sincronizado milimétricamente con el Stored Procedure de MongoDB Atlas.
 */
public record CompanyReportDTO(
        String companyId,
        String razonsocial,
        int cantidadAdmin,
        boolean tieneAdminsPendientes, // <-- 🎯 CONFIGURADO: Flag booleano puro que desacopla la estética del Front
        List<AdminUserDetailDTO> usuariosAdministradores, // <-- CONFIGURADO: Detalle atómico de identidades administradoras
        int cantidadEmpleados,
        int cantidadDispositivosActivos,
        int cantidadEmpresasClientes,
        int cantidadEmpresasProveedoras,
        int deltaEmpleados,
        int deltaDispositivos,
        int deltaEmpresas,
        Map<String, Integer> alarmas
) {

    /**
     * SUB-CONTRATO DE INFRAESTRUCTURA DE SEGURIDAD
     * Representa las identidades atómicas de los administradores mapeadas desde la colección users.
     */
    public record AdminUserDetailDTO(
            String userId,
            String email,
            String nombre,
            String apellido,
            String status_id,
            String status_name
    ) {}
}
