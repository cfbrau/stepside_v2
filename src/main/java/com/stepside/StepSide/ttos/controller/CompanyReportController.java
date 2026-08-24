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
 * Optimizado Senior tipando el retorno síncrono del flujo analítico B2B.
 */
@RestController
@RequestMapping("/api/ttos/reports")
public class CompanyReportController {

    private final CompanyReportService companyReportService;

    public CompanyReportController(CompanyReportService companyReportService) {
        this.companyReportService = companyReportService;
    }

    @GetMapping("/companiesdash")
    public ResponseEntity<List<CompanyReportDTO>> getReporteConsolidado(
            @RequestParam(value = "fechaLimite", required = false) String fechaLimiteStr) {

        try {
            Instant fechaParametro;

            if (fechaLimiteStr != null && !fechaLimiteStr.isBlank()) {
                // 🚀 ESTÁNDAR ISO 8601: Procesa el formato internacional estricto "AAAA-MM-DD"
                LocalDate fechaLocal = LocalDate.parse(fechaLimiteStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
                fechaParametro = fechaLocal.atStartOfDay().toInstant(ZoneOffset.UTC);
            } else {
                fechaParametro = Instant.now().minus(30, ChronoUnit.DAYS);
            }

            // Consumimos el servicio que interroga a la Vista Almacenada NoSQL de Atlas
            List<CompanyReportDTO> listado = companyReportService.obtenerReporteConsolidadoEmpresas(fechaParametro);
            return ResponseEntity.ok(listado);

        } catch (Exception e) {
            // Mantenemos el bloque defensivo de Fabián para alertar al Front ante fechas malformadas
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}
