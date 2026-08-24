package com.stepside.StepSide.ttos.service;

import com.stepside.StepSide.ttos.dto.CompanyReportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyReportServiceImpl implements CompanyReportService {

    private final MongoTemplate mongoTemplate;

    @Override
    public List<CompanyReportDTO> obtenerReporteConsolidadoEmpresas(Instant fechaParametro) {
        Date fechaLimite = Date.from(fechaParametro);

        log.info("[BI-VIEW-ENGINE] Interrogando al Stored Procedure analítico para el Dashboard.");

        // Construimos la proyección final adaptada milimétricamente al nuevo formato de la vista
        Document projectBson = new Document("_id", 0)
                .append("companyId", "$companyId")
                .append("razonsocial", "$razonsocial")
                .append("cantidadAdmin", "$cantidadAdmin")

                // MEJORA: Sincronizado con el booleano puro que definiste en Atlas
                .append("tieneAdminsPendientes", "$tieneAdminsPendientes")
                .append("usuariosAdministradores", "$usuariosAdministradores")

                .append("cantidadEmpleados", "$cantidadEmpleados")
                .append("cantidadDispositivosActivos", "$cantidadDispositivosActivos")
                .append("cantidadEmpresasClientes", "$cantidadEmpresasClientes")
                .append("cantidadEmpresasProveedoras", "$cantidadEmpresasProveedoras")

                // Filtramos dinámicamente los deltas en la RAM de Java según el parámetro del Front
                .append("deltaEmpleados", new Document("$subtract", Arrays.asList(
                        new Document("$sum", new Document("$filter", new Document("input", "$poolEmpleados").append("as", "e").append("cond", new Document("$gte", Arrays.asList(new Document("$toDate", "$$e.created_at"), fechaLimite))))),
                        new Document("$sum", new Document("$filter", new Document("input", "$poolEmpleados").append("as", "e").append("cond", new Document("$eq", Arrays.asList("$$e.status_id", "6a3067975cffbbf10841649a")))))
                )))
                .append("deltaDispositivos", "$cantidadDispositivosActivos")

                // Mapeo adaptado del delta de relaciones corporativas
                .append("deltaEmpresas", "$deltaEmpresas")
                .append("alarmas", "$alarmas");

        // MEJOR PRÁCTICA: Encapsulamos la etapa usando tu componente formal CustomAggregationOperation
        var projectStage = new CustomAggregationOperation("$project", projectBson);
        var sortStage = Aggregation.sort(Sort.Direction.ASC, "razonsocial");

        // Ensamblamos la autopista analítica apuntando directo a la vista almacenada
        Aggregation pipeline = Aggregation.newAggregation(projectStage, sortStage);

        // ESCUDO DE CONTROL DE AUDITORÍA CONTRA EL ERROR 500
        try {
            AggregationResults<CompanyReportDTO> results = mongoTemplate.aggregate(
                    pipeline,
                    "vw_company_analytics_report", // Nuestra vista unificada en Atlas
                    CompanyReportDTO.class
            );
            return results.getMappedResults();
        } catch (Exception e) {
            log.error("[BI-ENGINE CRITICAL] Error al ejecutar o mapear el pipeline NoSQL: {}", e.getMessage(), e);
            throw e;
        }
    }

    // COMPONENTE FORMAL DE INFRAESTRUCTURA DE ESCAPE (Diseño Fabián)
    private static class CustomAggregationOperation implements org.springframework.data.mongodb.core.aggregation.AggregationOperation {
        private final String operator;
        private final Document document;

        public CustomAggregationOperation(String operator, Document document) {
            this.operator = operator;
            this.document = document;
        }

        @Override
        public Document toDocument(org.springframework.data.mongodb.core.aggregation.AggregationOperationContext context) {
            return new Document(operator, document);
        }
    }
}
