package com.stepside.StepSide.users.service.impl;

import com.stepside.StepSide.users.dto.CompanyUsersGroupDto;
import com.stepside.StepSide.users.dto.UserApprovalRequestDTO;
import com.stepside.StepSide.users.dto.UserResponseDTO;
import com.stepside.StepSide.users.repository.UserRepository;
import com.stepside.StepSide.users.service.UserService;
import com.stepside.StepSide.notification.dto.EmailMessageDto;
import com.stepside.StepSide.notification.service.EmailService;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger; // SINCRO: Import nativo de la interfaz de SLF4J
import org.slf4j.LoggerFactory; // SINCRO: Import de la fábrica de Loggers nativa
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Orquestador de lógica de negocio para usuarios y workflows de aprobación NoSQL.
 * Centraliza las transiciones de estado y la persistencia relacional de aplicaciones asignadas.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    // Instanciación explícita del Logger perimetral para eludir fallos del build de Lombok
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final EmailService emailService;

    @Override
    public List<UserResponseDTO> getUsersWithFilter(String status) {
        ObjectId targetStatusId = null;

        if (status != null && !status.trim().isEmpty() && ObjectId.isValid(status)) {
            targetStatusId = new ObjectId(status);
        }

        return userRepository.findAllUsersWithTto(targetStatusId);
    }

    @Override
    public List<CompanyUsersGroupDto> getUsersGroupedByCompany() {
        return List.of();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional // Garantiza que si algo falla, se aplique un rollback absoluto
    public void approveUser(String ttoId, UserApprovalRequestDTO request) {
        // 1. ESCUDO DEFENSIVO: Validación perimetral temprana
        if (request.permissions() == null || request.permissions().isEmpty()) {
            throw new IllegalArgumentException("Validación fallida: Se requiere la asignación de al menos una aplicación para proceder.");
        }

        // 2. RESOLUCIÓN DINÁMICA DE ESTADO PREVIA: Evita procesamiento si el catálogo está corrupto
        Query statusQuery = new Query(Criteria.where("name").is("ACTIVE"));
        statusQuery.fields().include("_id"); // Proyección limpia: solo requerimos el ID
        Document statusDoc = mongoTemplate.findOne(statusQuery, Document.class, "user_statuses");
        if (statusDoc == null) {
            throw new IllegalStateException("Error de consistencia: No se localizó el estado 'ACTIVE' en la colección user_statuses.");
        }
        ObjectId activeStatusId = statusDoc.getObjectId("_id");

        // 3. CONSTRUCCIÓN DE LA CONSULTA Y CONTROL DE INTEGRIDAD: Localizar usuario de forma segura
        Query userQuery = new Query(
                new Criteria().orOperator(
                        Criteria.where("ttoId").is(ttoId),
                        Criteria.where("_id").is(ObjectId.isValid(ttoId) ? new ObjectId(ttoId) : ttoId)
                )
        );
        userQuery.fields().include("_id", "email", "status_id"); // Proyección eficiente de RAM

        Document userDoc = mongoTemplate.findOne(userQuery, Document.class, "users");
        if (userDoc == null) {
            throw new NoSuchElementException("Usuario no encontrado en el clúster NoSQL con el identificador provisto: " + ttoId);
        }

        if (activeStatusId.equals(userDoc.get("status_id"))) {
            throw new IllegalArgumentException("Operación inválida: La cuenta de usuario ya se encuentra aprobada y en estado ACTIVO.");
        }
        ObjectId userId = userDoc.getObjectId("_id");

        // 4. RESOLUCIÓN MASIVA DE UN SOLO VIAJE (Eliminación total del problema N+1)
        Map<String, String> incomingPermissions = request.permissions();

        // A) Traemos todas las aplicaciones del payload en un lote masivo
        Query appsQuery = new Query(Criteria.where("name").in(incomingPermissions.keySet()));
        appsQuery.fields().include("_id", "name");
        List<Document> fetchedApps = mongoTemplate.find(appsQuery, Document.class, "applications");

        // Mapeamos los IDs de las aplicaciones encontradas para armar la query masiva de roles
        List<ObjectId> appIds = fetchedApps.stream().map(doc -> doc.getObjectId("_id")).collect(Collectors.toList());
        List<String> roleNames = new ArrayList<>(incomingPermissions.values());

        // B) OPTIMIZACIÓN SUPREMA: Traemos todos los roles coincidentes de un solo viaje usando $and y $in
        Query rolesQuery = new Query(Criteria.where("app_id").in(appIds).and("name").in(roleNames));
        rolesQuery.fields().include("_id", "app_id", "name");
        List<Document> fetchedRoles = mongoTemplate.find(rolesQuery, Document.class, "roles");

        // Indexamos los roles en un mapa de memoria O(1) usando una clave compuesta "appId_roleName"
        Map<String, ObjectId> roleCacheMap = fetchedRoles.stream().collect(Collectors.toMap(
                role -> role.getObjectId("app_id").toString() + "_" + role.getString("name"),
                role -> role.getObjectId("_id"),
                (existing, replacement) -> existing
        ));

        // 5. CONSTRUCCIÓN ASOCIATIVA EN MEMORIA
        List<Document> userApplicationsToInsert = new ArrayList<>(fetchedApps.size());
        Date currentDate = new Date();

        for (Document appDoc : fetchedApps) {
            String appIdStr = appDoc.getObjectId("_id").toString();
            String appName = appDoc.getString("name");
            String targetRoleName = incomingPermissions.get(appName);

            // Buscamos el rol de forma instantánea en la cache local sin golpear la base de datos
            String cacheKey = appIdStr + "_" + targetRoleName;

            if (roleCacheMap.containsKey(cacheKey)) {
                ObjectId roleId = roleCacheMap.get(cacheKey);

                Document userAppDoc = new Document()
                        .append("user_id", userId)
                        .append("appId", appIdStr)
                        .append("role_id", roleId)
                        .append("assignedAt", currentDate)
                        .append("_class", "com.minorityreport.portal.auth.model.UserApplication");

                userApplicationsToInsert.add(userAppDoc);
            } else {
                log.warn("Omisión defensiva: No existe la combinación del rol '{}' para la aplicación '{}' en los catálogos.", targetRoleName, appName);
            }
        }

        if (userApplicationsToInsert.isEmpty()) {
            throw new IllegalArgumentException("Validación fallida: Ninguna de las combinaciones de Aplicación y Rol provistas es válida en el sistema.");
        }

        // 6. PERSISTENCIA ATÓMICA TRANSACCIONAL
        mongoTemplate.insert(userApplicationsToInsert, "user_applications");

        Update update = new Update();
        update.set("status_id", activeStatusId);
        update.set("status_name", "ACTIVE");
        update.set("failed_attempts", 0);
        update.set("updated_at", currentDate);
        mongoTemplate.updateFirst(userQuery, update, "users");

        // 7. DESPACHO ASÍNCRONO DE ALERTA (Fuera del riesgo transaccional)
        String userEmail = userDoc.getString("email");
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("mail_registered_user", userEmail);

        EmailMessageDto emailDto = new EmailMessageDto(
                userEmail,
                "Aviso de Sistema - Cuenta Activada",
                "ACCEPT_APPROVAL",
                templateVariables
        );
        emailService.sendEmail(emailDto);
    }

}
