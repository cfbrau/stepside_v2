package com.stepside.StepSide.ttos.dto;

import java.util.Map;

/**
 * CONTRATO DE ANALÍTICA AVANZADA B2B: Dominio TTOs - StepSide.
 * Diseñado por Fabián para transportar contadores, deltas y el mapa de alarmas normalizado.
 */
public record CompanyReportDTO(
        String companyId,
        String razonsocial,
        int cantidadAdmin,
        int cantidadEmpleados,
        int cantidadDispositivosActivos,
        int cantidadEmpresasClientes,
        int cantidadEmpresasProveedoras,
        int deltaEmpleados,
        int deltaDispositivos,
        int deltaEmpresas,
        Map<String, Integer> alarmas // <-- Estructura dinámica de prioridades ("HIGH": X, "LOW": Y)
) {}
