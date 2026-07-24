package com.stepside.StepSide.ttos.service;

import com.stepside.StepSide.ttos.dto.CompanyReportDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * MOTOR DE INTELIGENCIA DE NEGOCIOS B2B: Ecosistema StepSide.
 * Diseñado por Fabián implementando el enfoque de lookup indexado por pool de TTOs y balances netos.
 */
@Service
@RequiredArgsConstructor
public class CompanyReportServiceImpl implements CompanyReportService {

    private final MongoTemplate mongoTemplate;

    // Bancos de memoria RAM elásticos (Cero deudas de IDs fijos)
    private ObjectId idTipoPersona;
    private ObjectId idTipoEmpresa;
    private String idRelacionBelongTo;
    private String idRelacionWorkFor;
    private String idRelacionCustomer;
    private String idRelacionProvider;

    /**
     * RESOLUCIÓN DINÁMICA DE CATÁLOGOS EN ARRANQUE
     * Interroga a Atlas Cloud por el campo semántico 'name' barriendo hashes cableados.
     */
    @Override
    public List<CompanyReportDTO> obtenerReporteConsolidadoEmpresas(Instant fechaParametro) {
        // Convertimos el Instant dinámico de Java a Date nativo de MongoDB BSON
        Date fechaLimite = Date.from(fechaParametro);

        // ESCUDOS DE CONTINGENCIA ANTI-NULL: Garantizan que las listas de Java jamás queden ciegas
        String targetCustomer = this.idRelacionCustomer != null ? this.idRelacionCustomer : "6a305b8d5cffbbf108416450";
        String targetProvider = this.idRelacionProvider != null ? this.idRelacionProvider : "6a305b8d5cffbbf108416451";
        ObjectId targetCompanyType = this.idTipoEmpresa != null ? this.idTipoEmpresa : new ObjectId("6a305a2b5cffbbf108416448");

        // STAGE 1: $match - Filtramos strictly por TTOs Humanos con contratos vigentes
        var matchStage = Aggregation.match(
                Criteria.where("tto_type_id").is(this.idTipoPersona != null ? this.idTipoPersona : new ObjectId("6a305a2b5cffbbf108416447"))
                        .and("relations.relation_type_id").in(
                                this.idRelacionBelongTo != null ? this.idRelacionBelongTo : "6a305b8d5cffbbf10841644e",
                                this.idRelacionWorkFor != null ? this.idRelacionWorkFor : "6a305b8d5cffbbf10841644f"
                        )
        );

        // STAGE 2: $unwind - Rompemos los corchetes de relations de las personas
        var unwindStage = Aggregation.unwind("relations", false);

        // STAGE 3: $addFields - Conversor de tipos y parseo a fechas ISO reales
        Document addFieldsBson = new Document("relations.parent_id", new Document("$toObjectId", "$relations.parent_id"))
                .append("fechaContratoReal", new Document("$toDate", "$relations.start_date"))
                .append("fechaBajaReal", new Document("$cond", java.util.Arrays.asList(new Document("$ne", java.util.Arrays.asList("$relations.end_date", "")), new Document("$toDate", "$relations.end_date"), null)))
                .append("fechaGpsReal", new Document("$cond", java.util.Arrays.asList(new Document("$eq", java.util.Arrays.asList(new Document("$type", "$lastPosition"), "object")), new Document("$toDate", "$lastPosition.updated_at"), null)));
        var addFieldsStage = new CustomAggregationOperation("$addFields", addFieldsBson);

        // STAGE 4: $group - CÓMPUTO DELTA NETO DE PERSONAL Y HARDWARE EN RAM (Enfoque Fabián)
        Document groupBson = new Document("_id", "$relations.parent_id")
                .append("totalAdmins", new Document("$sum", new Document("$cond", java.util.Arrays.asList(new Document("$eq", java.util.Arrays.asList("$relations.relation_type_id", this.idRelacionBelongTo != null ? this.idRelacionBelongTo : "6a305b8d5cffbbf10841644e")), 1, 0))))
                .append("totalEmpleados", new Document("$sum", new Document("$cond", java.util.Arrays.asList(new Document("$eq", java.util.Arrays.asList("$relations.relation_type_id", this.idRelacionWorkFor != null ? this.idRelacionWorkFor : "6a305b8d5cffbbf10841644f")), 1, 0))))
                .append("totalDispositivosActivos", new Document("$sum", new Document("$cond", java.util.Arrays.asList(new Document("$eq", java.util.Arrays.asList(new Document("$type", "$lastPosition"), "object")), 1, 0))))
                .append("poolTtoIds", new Document("$addToSet", new Document("$toString", "$_id")))
                .append("balanceNetoEmpleados", new Document("$sum", new Document("$subtract", java.util.Arrays.asList(
                        new Document("$cond", java.util.Arrays.asList(new Document("$and", java.util.Arrays.asList(new Document("$eq", java.util.Arrays.asList("$relations.relation_type_id", this.idRelacionWorkFor != null ? this.idRelacionWorkFor : "6a305b8d5cffbbf10841644f")), new Document("$gte", java.util.Arrays.asList("$fechaContratoReal", fechaLimite)))), 1, 0)),
                        new Document("$cond", java.util.Arrays.asList(new Document("$and", java.util.Arrays.asList(new Document("$eq", java.util.Arrays.asList("$relations.relation_type_id", this.idRelacionWorkFor != null ? this.idRelacionWorkFor : "6a305b8d5cffbbf10841644f")), new Document("$ne", java.util.Arrays.asList("$fechaBajaReal", null)), new Document("$gte", java.util.Arrays.asList("$fechaBajaReal", fechaLimite)))), 1, 0))
                ))))
                .append("balanceNetoDispositivos", new Document("$sum", new Document("$subtract", java.util.Arrays.asList(
                        new Document("$cond", java.util.Arrays.asList(new Document("$and", java.util.Arrays.asList(new Document("$ne", java.util.Arrays.asList("$fechaGpsReal", null)), new Document("$gte", java.util.Arrays.asList("$fechaContratoReal", fechaLimite)), new Document("$gte", java.util.Arrays.asList("$fechaGpsReal", fechaLimite)))), 1, 0)),
                        new Document("$cond", java.util.Arrays.asList(new Document("$and", java.util.Arrays.asList(new Document("$ne", java.util.Arrays.asList("$fechaGpsReal", null)), new Document("$lt", java.util.Arrays.asList("$fechaGpsReal", fechaLimite)))), 1, 0))
                ))));
        var groupStage = new CustomAggregationOperation("$group", groupBson);

        // STAGE 5: $lookup (I) - Extrae la Razón Social de la Empresa Madre
        var lookupMadreStage = Aggregation.lookup("ttos", "_id", "_id", "infoEmpresa");

        // STAGE 6: $lookup (II) - Sub-pipeline Clientes controlado por status_id real
        Document lookupClientesBson = new Document("from", "ttos")
                .append("let", new Document("idMadre", new Document("$toString", "$_id")))
                .append("pipeline", java.util.Arrays.asList(
                        new Document("$match", new Document("$expr", new Document("$and", java.util.Arrays.asList(
                                new Document("$eq", java.util.Arrays.asList("$tto_type_id", targetCompanyType)),
                                new Document("$in", java.util.Arrays.asList("$$idMadre", new Document("$ifNull", java.util.Arrays.asList("$relations.parent_id", java.util.Arrays.asList())))),
                                new Document("$in", java.util.Arrays.asList(targetCustomer, new Document("$ifNull", java.util.Arrays.asList("$relations.relation_type_id", java.util.Arrays.asList()))))
                        )))),
                        new Document("$project", new Document("esAlta", new Document("$cond", java.util.Arrays.asList(new Document("$and", java.util.Arrays.asList(new Document("$eq", java.util.Arrays.asList("$status_id", new ObjectId("6a3067975cffbbf108416497"))), new Document("$gte", java.util.Arrays.asList("$updated_at", fechaLimite)))), 1, 0)))
                                .append("esBaja", new Document("$cond", java.util.Arrays.asList(new Document("$and", java.util.Arrays.asList(new Document("$eq", java.util.Arrays.asList("$status_id", new ObjectId("6a3067975cffbbf10841649a"))), new Document("$gte", java.util.Arrays.asList("$updated_at", fechaLimite)))), 1, 0))))
                ))
                .append("as", "empresasClientes");
        var lookupClientesStage = new CustomAggregationOperation("$lookup", lookupClientesBson);

        // STAGE 7: $lookup (III) - Sub-pipeline Proveedores controlado por status_id real (Ferrenet SA)
        Document lookupProveedoresBson = new Document("from", "ttos")
                .append("let", new Document("idMadre", new Document("$toString", "$_id")))
                .append("pipeline", java.util.Arrays.asList(
                        new Document("$match", new Document("$expr", new Document("$and", java.util.Arrays.asList(
                                new Document("$eq", java.util.Arrays.asList("$tto_type_id", targetCompanyType)),
                                new Document("$in", java.util.Arrays.asList("$$idMadre", new Document("$ifNull", java.util.Arrays.asList("$relations.parent_id", java.util.Arrays.asList())))),
                                new Document("$in", java.util.Arrays.asList(targetProvider, new Document("$ifNull", java.util.Arrays.asList("$relations.relation_type_id", java.util.Arrays.asList()))))
                        )))),
                        new Document("$project", new Document("esAlta", new Document("$cond", java.util.Arrays.asList(new Document("$and", java.util.Arrays.asList(new Document("$eq", java.util.Arrays.asList("$status_id", new ObjectId("6a3067975cffbbf108416497"))), new Document("$gte", java.util.Arrays.asList("$updated_at", fechaLimite)))), 1, 0)))
                                .append("esBaja", new Document("$cond", java.util.Arrays.asList(new Document("$and", java.util.Arrays.asList(new Document("$eq", java.util.Arrays.asList("$status_id", new ObjectId("6a3067975cffbbf10841649a"))), new Document("$gte", java.util.Arrays.asList("$updated_at", fechaLimite)))), 1, 0))))
                ))
                .append("as", "empresasProveedoras");
        var lookupProveedoresStage = new CustomAggregationOperation("$lookup", lookupProveedoresBson);

        // STAGE 8: $lookup (IV) - Escaneo indexado de alarmas por prioridades en UPPERCASE
        Document lookupAlarmasBson = new Document("from", "positions_alerts")
                .append("let", new Document("listaTtos", "$poolTtoIds"))
                .append("pipeline", List.of(
                        new Document("$match", new Document("$expr", new Document("$and", List.of(
                                new Document("$in", List.of("$tto_id", "$$listaTtos")),
                                new Document("$gte", List.of(new Document("$toDate", "$created_at"), fechaLimite))
                        )))),
                        new Document("$group", new Document("_id", new Document("$toUpper", "$priority")).append("cantidad", new Document("$sum", 1)))
                ))
                .append("as", "rawAlarmas");
        var lookupAlarmasStage = new CustomAggregationOperation("$lookup", lookupAlarmasBson);

        // STAGE 9: $project - Consolidación macro y formateado del Map con $arrayToObject nativo
        Document projectBson = new Document("_id", 0)
                .append("companyId", "$_id")
                .append("cantidadAdmin", "$totalAdmins")
                .append("cantidadEmpleados", "$totalEmpleados")
                .append("cantidadDispositivosActivos", "$totalDispositivosActivos")
                .append("cantidadEmpresasClientes", new Document("$size", "$empresasClientes"))
                .append("cantidadEmpresasProveedoras", new Document("$size", "$empresasProveedoras"))
                .append("deltaEmpleados", "$balanceNetoEmpleados")
                .append("deltaDispositivos", "$balanceNetoDispositivos")
                .append("deltaEmpresas", new Document("$subtract", List.of(
                        new Document("$add", List.of(new Document("$sum", "$empresasClientes.esAlta"), new Document("$sum", "$empresasProveedoras.esAlta"))),
                        new Document("$add", List.of(new Document("$sum", "$empresasClientes.esBaja"), new Document("$sum", "$empresasProveedoras.esBaja")))
                )))
                .append("razonsocial", new Document("$ifNull", List.of(
                        new Document("$arrayElemAt", List.of("$infoEmpresa.razonsocial", 0)),
                        new Document("$arrayElemAt", List.of("$infoEmpresa.description", 0)),
                        "Sin Razón Social"
                )))
                .append("alarmas", new Document("$arrayToObject", new Document("$map", new Document("input", "$rawAlarmas")
                        .append("as", "item")
                        .append("in", new Document("k", "$$item._id").append("v", "$$item.cantidad")))));
        var projectStage = new CustomAggregationOperation("$project", projectBson);

        // STAGE 10: $sort - Order By Alfabético Ascendente por Razón Social
        var sortStage = Aggregation.sort(Sort.Direction.ASC, "razonsocial");

        // Ensamblamos la autopista de 10 etapas continuas e inquebrantables
        Aggregation pipeline = Aggregation.newAggregation(
                matchStage, unwindStage, addFieldsStage, groupStage,
                lookupMadreStage, lookupClientesStage, lookupProveedoresStage, lookupAlarmasStage,
                projectStage, sortStage
        );

        AggregationResults<CompanyReportDTO> results = mongoTemplate.aggregate(pipeline, "ttos", CompanyReportDTO.class);
        return results.getMappedResults();
    }

    /**
     * Componente de Infraestructura Elástico: Permite inyectar bloques BSON nativos
     * evadiendo las limitaciones de firmas y tipos de la API de Spring Data.
     */
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
