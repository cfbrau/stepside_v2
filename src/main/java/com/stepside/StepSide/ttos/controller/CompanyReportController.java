package com.stepside.StepSide.ttos.controller;

import com.stepside.StepSide.ttos.dto.CompanyReportDTO;
import com.stepside.StepSide.ttos.service.CompanyReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * APIS DE REPORTE Y DASHBOARD DE FLOTAS: Backend Transaccional 8080.
 * Saneado por Fabián bajo el estándar internacional estricto ISO 8601 (AAAA-MM-DD).
 */
@RestController
@RequestMapping("/api/ttos/reports")
public class CompanyReportController {

    private final CompanyReportService companyReportService;

    public CompanyReportController(CompanyReportService companyReportService) {
        this.companyReportService = companyReportService;
    }

    @GetMapping("/companiesdash")
    public ResponseEntity<?> getReporteConsolidado(
            @RequestParam(value = "fechaLimite", required = false) String fechaLimiteStr) {

        try {
            Instant fechaParametro;

            if (fechaLimiteStr != null && !fechaLimiteStr.isBlank()) {
                // 🚀 ESTÁNDAR ISO 8601: Muerde el formato internacional "2026-06-15" sin devaluar la CPU
                LocalDate fechaLocal = LocalDate.parse(fechaLimiteStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
                fechaParametro = fechaLocal.atStartOfDay().toInstant(ZoneOffset.UTC);
            } else {
                fechaParametro = Instant.now().minus(30, ChronoUnit.DAYS);
            }

            List<CompanyReportDTO> listado = companyReportService.obtenerReporteConsolidadoEmpresas(fechaParametro);
            return ResponseEntity.ok(listado);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"Internal Server Error\",\"message\":\"Error. Se requiere formato internacional ISO 8601 (AAAA-MM-DD). Detalle: " + e.getMessage() + "\"}");
        }
    }
}
