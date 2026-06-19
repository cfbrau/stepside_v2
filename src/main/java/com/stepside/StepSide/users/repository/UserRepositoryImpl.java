package com.stepside.StepSide.users.repository;

import com.stepside.StepSide.users.dto.UserResponseDTO;
import com.stepside.StepSide.users.model.User;
import com.stepside.StepSide.ttos.model.Tto;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación de infraestructura híbrida optimizada para el Backoffice.
 * Resuelve las relaciones jerárquicas en memoria de Java para garantizar
 * la máxima estabilidad y velocidad de procesamiento.
 */
@Repository
public class UserRepositoryImpl implements UserRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @SuppressWarnings("unchecked")
    @Override
    public List<UserResponseDTO> findAllUsersWithTto(ObjectId statusId) {

        // 1. Obtener los usuarios de la base aplicando el filtro
        Query userQuery = new Query();
        if (statusId != null) {
            userQuery.addCriteria(Criteria.where("status_id").is(statusId));
        }
        List<User> users = mongoTemplate.find(userQuery, User.class, "users");

        if (users.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Extraer los IDs únicos de TTO primarios y Estados
        Set<ObjectId> primaryTtoIds = users.stream()
                .map(User::getTtoId)
                .filter(id -> id != null && !id.trim().isEmpty() && ObjectId.isValid(id))
                .map(ObjectId::new)
                .collect(Collectors.toSet());

        Set<ObjectId> statusIds = users.stream()
                .map(User::getStatusId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        // 3. Consultar la tabla de TTOs primarios (Personas)
        String ttoCollection = mongoTemplate.getCollectionName(Tto.class);
        Query primaryTtoQuery = new Query(Criteria.where("_id").in(primaryTtoIds));
        primaryTtoQuery.fields().include("_id", "attributes", "relations");
        List<Document> primaryTtosRaw = mongoTemplate.find(primaryTtoQuery, Document.class, ttoCollection);

        Map<String, Document> primaryTtoMap = primaryTtosRaw.stream()
                .collect(Collectors.toMap(doc -> doc.get("_id").toString(), doc -> doc, (e, r) -> e));

        // 4. Recolectar de forma masiva los IDs de segundo nivel (Relaciones anidadas)
        Set<String> relationTypeIds = new HashSet<>();
        Set<ObjectId> parentCompanyIds = new HashSet<>();

        for (Document ttoDoc : primaryTtosRaw) {
            List<Document> relations = (List<Document>) ttoDoc.get("relations");
            if (relations != null) {
                for (Document rel : relations) {
                    Object typeIdObj = rel.get("relation_type_id");
                    if (typeIdObj != null && !typeIdObj.toString().trim().isEmpty()) {
                        relationTypeIds.add(typeIdObj.toString());
                    }
                    Object parentIdObj = rel.get("parent_id");
                    if (parentIdObj != null && ObjectId.isValid(parentIdObj.toString())) {
                        parentCompanyIds.add(new ObjectId(parentIdObj.toString()));
                    }
                }
            }
        }

        // 5. Carga en Batch de catálogos relacionales ($in)
        // Resolver nombres de Estados de usuario
        Query statusQuery = new Query(Criteria.where("_id").in(statusIds));
        statusQuery.fields().include("_id", "name");
        List<Document> statusRaw = mongoTemplate.find(statusQuery, Document.class, "user_statuses");
        Map<String, String> statusMap = statusRaw.stream()
                .collect(Collectors.toMap(doc -> doc.get("_id").toString(), doc -> doc.getString("name"), (e, r) -> e));

        // Resolver descripciones de la colección 'relations_types'
        Map<String, String> relationTypeMap = new HashMap<>();
        if (!relationTypeIds.isEmpty()) {
            Query relTypeQuery = new Query(Criteria.where("_id").in(relationTypeIds));
            List<Document> relTypesRaw = mongoTemplate.find(relTypeQuery, Document.class, "relation_types");
            relationTypeMap = relTypesRaw.stream().collect(Collectors.toMap(
                    doc -> doc.get("_id").toString(),
                    doc -> doc.getString("name") != null ? doc.getString("name") : doc.getString("description"),
                    (e, r) -> e
            ));
        }

        // Resolver nombres de empresas asociadas (usando parent_id de la tabla tto)
        Map<String, String> companyMap = new HashMap<>();
        if (!parentCompanyIds.isEmpty()) {
            Query companyQuery = new Query(Criteria.where("_id").in(parentCompanyIds));
            companyQuery.fields().include("_id", "description", "attributes.nombre", "attributes.razon_social");
            List<Document> companiesRaw = mongoTemplate.find(companyQuery, Document.class, ttoCollection);
            companyMap = companiesRaw.stream().collect(Collectors.toMap(
                    doc -> doc.get("_id").toString(),
                    doc -> {
                        Document attrs = (Document) doc.get("attributes");
                        if (attrs != null && attrs.getString("nombre") != null) return attrs.getString("nombre");
                        if (attrs != null && attrs.getString("razon_social") != null) return attrs.getString("razon_social");
                        return doc.getString("description") != null ? doc.getString("description") : "EMPRESA_SIN_NOMBRE";
                    },
                    (e, r) -> e
            ));
        }

        // 6. Ensamble final de DTOs en memoria de Java
        List<UserResponseDTO> finalResults = new ArrayList<>(users.size());

        for (User user : users) {
            String personTtoId = null;
            String personApellido = null;
            String personName = null;
            List<Map<String, Object>> enrichedRelations = null;

            if (user.getTtoId() != null && primaryTtoMap.containsKey(user.getTtoId())) {
                Document ttoDoc = primaryTtoMap.get(user.getTtoId());
                personTtoId = ttoDoc.get("_id").toString();

                Document attributes = (Document) ttoDoc.get("attributes");
                if (attributes != null) {
                    personApellido = attributes.getString("apellido");
                    personName = attributes.getString("nombre");
                }

                List<Document> rawRelations = (List<Document>) ttoDoc.get("relations");
                if (rawRelations != null) {
                    enrichedRelations = new ArrayList<>(rawRelations.size());
                    for (Document rel : rawRelations) {
                        Map<String, Object> relMap = new HashMap<>(rel);

                        // Mapeo del Tipo de Relación
                        Object typeId = rel.get("relation_type_id");
                        String typeName = (typeId != null) ? relationTypeMap.getOrDefault(typeId.toString(), "UNKNOWN_TYPE") : "UNKNOWN_TYPE";
                        relMap.put("relation_type_name", typeName);

                        // Mapeo del Nombre de la Empresa utilizando parent_id
                        Object parentId = rel.get("parent_id");
                        String companyName = (parentId != null) ? companyMap.getOrDefault(parentId.toString(), "UNKNOWN_COMPANY") : "UNKNOWN_COMPANY";
                        relMap.put("related_name", companyName);

                        enrichedRelations.add(relMap);
                    }
                }
            }

            // Formateo de fechas y estados
            Instant expInstant = user.getExpiration() != null ? user.getExpiration().toInstant() : null;
            Instant createdInstant = user.getCreatedAt() != null ? user.getCreatedAt().toInstant() : null;

            String resolvedStatus = "UNKNOWN";
            if (user.getStatusId() != null && statusMap.containsKey(user.getStatusId().toString())) {
                resolvedStatus = statusMap.get(user.getStatusId().toString());
            } else if (user.getStatusName() != null) {
                resolvedStatus = user.getStatusName();
            }

            finalResults.add(new UserResponseDTO(
                    user.getId(),
                    user.getEmail(),
                    resolvedStatus,
                    expInstant,
                    createdInstant,
                    personTtoId,
                    personApellido,
                    personName,
                    enrichedRelations
            ));
        }

        return finalResults;
    }
}
