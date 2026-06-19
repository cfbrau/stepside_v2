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
    public void approveUser(String ttoId, UserApprovalRequestDTO request) {
        // 1. ESCUDO DEFENSIVO: Si el payload de permisos viene vacío, aborta en el acto
        if (request.permissions() == null || request.permissions().isEmpty()) {
            throw new IllegalArgumentException("Validación fallida: Se requiere la asignación de al menos una aplicación para proceder.");
        }

        // 2. CONSTRUCCIÓN DE LA CONSULTA: Localizar al usuario por ttoId o por _id primario
        Query userQuery = new Query(
                new Criteria().orOperator(
                        Criteria.where("ttoId").is(ttoId),
                        Criteria.where("_id").is(ObjectId.isValid(ttoId) ? new ObjectId(ttoId) : ttoId)
                )
        );

        Document userDoc = mongoTemplate.findOne(userQuery, Document.class, "users");
        if (userDoc == null) {
            throw new NoSuchElementException("Usuario no encontrado en el clúster NoSQL con el identificador provisto: " + ttoId);
        }
        ObjectId userId = userDoc.getObjectId("_id");

        // 3. RESOLUCIÓN DINÁMICA DE ESTADO: Localizar el _id de 'ACTIVE' en la tabla 'user_statuses'
        Query statusQuery = new Query(Criteria.where("name").is("ACTIVE"));
        Document statusDoc = mongoTemplate.findOne(statusQuery, Document.class, "user_statuses");
        if (statusDoc == null) {
            throw new IllegalStateException("Error de consistencia: No se localizó el estado 'ACTIVE' en la colección user_statuses.");
        }
        ObjectId activeStatusId = statusDoc.getObjectId("_id");

        // 4. CONTROL DE INTEGRIDAD: Validar si la cuenta ya está aprobada
        if (activeStatusId.equals(userDoc.get("status_id"))) {
            throw new IllegalArgumentException("Operación inválida: La cuenta de usuario ya se encuentra aprobada y en estado ACTIVO.");
        }

        // 5. RESOLUCIÓN MASIVA DE APLICACIONES Y ROLES
        Map<String, String> incomingPermissions = request.permissions();

        Query appsQuery = new Query(Criteria.where("name").in(incomingPermissions.keySet()));
        List<Document> fetchedApps = mongoTemplate.find(appsQuery, Document.class, "applications");

        List<Document> userApplicationsToInsert = new ArrayList<>();

        for (Document appDoc : fetchedApps) {
            ObjectId appId = appDoc.getObjectId("_id");
            String appName = appDoc.getString("name");
            String targetRoleName = incomingPermissions.get(appName);

            // Con el ID de la aplicación y el nombre del rol, buscamos el Rol correspondiente
            Query roleQuery = new Query(Criteria.where("app_id").is(appId).and("name").is(targetRoleName));
            Document roleDoc = mongoTemplate.findOne(roleQuery, Document.class, "roles");

            if (roleDoc != null) {
                ObjectId roleId = roleDoc.getObjectId("_id");

                // Construimos el documento pivote respetando milimétricamente tu JSON de producción
                Document userAppDoc = new Document()
                        .append("user_id", userId)
                        .append("appId", appId.toString())
                        .append("role_id", roleId)
                        .append("assignedAt", new Date())
                        .append("_class", "com.minorityreport.portal.auth.model.UserApplication");

                userApplicationsToInsert.add(userAppDoc);
            } else {
                log.warn("No se localizó el rol '{}' asignado para la aplicación '{}' en la base de datos.", targetRoleName, appName);
            }
        }

        // Si no se pudo resolver ninguna aplicación o rol válido, abortamos antes de romper la integridad
        if (userApplicationsToInsert.isEmpty()) {
            throw new IllegalArgumentException("Validación fallida: Ninguna de las combinaciones de Aplicación y Rol provistas es válida en los catálogos.");
        }

        // 6. PROTOCOLO DE PERSISTENCIA Y MUTACIÓN EN BOTELLA ATÓMICA
        // A) Insertamos masivamente los permisos resueltos en user_applications
        mongoTemplate.insert(userApplicationsToInsert, "user_applications");

        // B) Transicionamos el estado de la cuenta del usuario a ACTIVE
        Update update = new Update();
        update.set("status_id", activeStatusId);
        update.set("status_name", "ACTIVE");
        update.set("failed_attempts", 0);
        update.set("updated_at", new Date());

        mongoTemplate.updateFirst(userQuery, update, "users");

        // ============================================================================
        // MOTOR DE NOTIFICACIONES ASÍNCRONAS - DESPACHO DE ALERTA DE ACTIVACIÓN
        // ============================================================================
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

    @Override
    public List<CompanyUsersGroupDto> getUsersGroupedByCompany() {
        return List.of();
    }
}
