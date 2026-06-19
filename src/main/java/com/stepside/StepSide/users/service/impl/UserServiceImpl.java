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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Orquestador de lógica de negocio para usuarios y workflows de aprobación NoSQL.
 * Saneado por el Arquitecto para inyectar correctamente las dependencias de infraestructura.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    // Inyecciones atómicas obligatorias gestionadas por @RequiredArgsConstructor de Lombok
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
        // 1. ESCUDO DEFENSIVO: Si el mapa de permisos viene vacío, aborta en el acto
        if (request.permissions() == null || request.permissions().isEmpty()) {
            throw new IllegalArgumentException("Validación fallida: Se requiere la asignación de al menos una aplicación para proceder.");
        }

        // 2. CONSTRUCCIÓN DE LA CONSULTA: Buscamos al usuario por su 'ttoId' lógico (String en base de datos)
        org.springframework.data.mongodb.core.query.Query userQuery = new org.springframework.data.mongodb.core.query.Query(
                new org.springframework.data.mongodb.core.query.Criteria().orOperator(
                        org.springframework.data.mongodb.core.query.Criteria.where("ttoId").is(ttoId),
                        org.springframework.data.mongodb.core.query.Criteria.where("_id").is(
                                org.bson.types.ObjectId.isValid(ttoId) ? new org.bson.types.ObjectId(ttoId) : ttoId
                        )
                )
        );

        org.bson.Document userDoc = mongoTemplate.findOne(userQuery, org.bson.Document.class, "users");
        if (userDoc == null) {
            throw new java.util.NoSuchElementException("Usuario no encontrado en el clúster NoSQL con el identificador provisto: " + ttoId);
        }

        // 3. RESOLUCIÓN DINÁMICA DE ESTADO: Buscamos el _id real de 'ACTIVE' en la tabla 'user_statuses'
        Query statusQuery = new Query(Criteria.where("name").is("ACTIVE"));
        Document statusDoc = mongoTemplate.findOne(statusQuery, Document.class, "user_statuses");
        if (statusDoc == null) {
            throw new IllegalStateException("Error de consistencia: No se localizó el estado 'ACTIVE' en la colección user_status.");
        }
        ObjectId activeStatusId = statusDoc.getObjectId("_id");

        // 4. CONTROL DE INTEGRIDAD: Validamos si la cuenta ya posee asignado el ID del estado activo
        if (activeStatusId.equals(userDoc.get("status_id"))) {
            throw new IllegalArgumentException("Operación inválida: La cuenta de usuario ya se encuentra aprobada y en estado ACTIVO.");
        }

        // 5. PROTOCOLO DE MUTACIÓN ATÓMICA DIRECTA sobre la colección de usuarios
        Update update = new Update();
        update.set("status_id", activeStatusId);      // Relación indexada lógica
        update.set("status_name", "ACTIVE");          // Redundancia controlada para retrocompatibilidad
        update.set("failed_attempts", 0);             // Limpieza higiénica de bloqueos previos
        update.set("updated_at", new Date());         // Estampa temporal de auditoría

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

        // Despacho asincrónico directo
        emailService.sendEmail(emailDto);
    }

    @Override
    public List<CompanyUsersGroupDto> getUsersGroupedByCompany() {
        // Espacio reservado para el requerimiento 3 a futuro
        return List.of();
    }
}
