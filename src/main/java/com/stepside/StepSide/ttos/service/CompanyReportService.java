package com.stepside.StepSide.ttos.service;

import com.stepside.StepSide.ttos.dto.CompanyReportDTO;
import java.time.Instant;
import java.util.List;

public interface CompanyReportService {
    List<CompanyReportDTO> obtenerReporteConsolidadoEmpresas(Instant fechaParametro);
}
