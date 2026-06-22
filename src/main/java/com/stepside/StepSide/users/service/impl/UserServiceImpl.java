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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final EmailService emailService;

    @Override
    public List<UserResponseDTO> getUsersWithFilter(String status) {
        ObjectId targetStatusId = null;

        if (status != null && !status.trim().isEmpty()) {
            if (ObjectId.isValid(status)) {
                targetStatusId = new ObjectId(status);
            } else {
                Query statusQuery = new Query(Criteria.where("name").is(status.trim().toUpperCase()));
                statusQuery.fields().include("_id");
                Document statusDoc = mongoTemplate.findOne(statusQuery, Document.class, "user_statuses");

                if (statusDoc != null) {
                    targetStatusId = statusDoc.getObjectId("_id");
                } else {
                    return Collections.emptyList();
                }
            }
        }

        return userRepository.findAllUsersWithTto(targetStatusId);
    }
    @Override
    @org.springframework.transaction.annotation.Transactional
    public void approveUser(String ttoId, UserApprovalRequestDTO request) {
        if (request.permissions() == null || request.permissions().isEmpty()) {
            throw new IllegalArgumentException("Validación fallida: Se requiere la asignación de al menos una aplicación para proceder.");
        }

        Query statusQuery = new Query(Criteria.where("name").is("ACTIVE"));
        statusQuery.fields().include("_id");
        Document statusDoc = mongoTemplate.findOne(statusQuery, Document.class, "user_statuses");
        if (statusDoc == null) {
            throw new IllegalStateException("Error de consistencia: No se localizó el estado 'ACTIVE' en la colección user_statuses.");
        }
        ObjectId activeStatusId = statusDoc.getObjectId("_id");

        Query userQuery = new Query(
                new Criteria().orOperator(
                        Criteria.where("ttoId").is(ttoId),
                        Criteria.where("_id").is(ObjectId.isValid(ttoId) ? new ObjectId(ttoId) : ttoId)
                )
        );
        userQuery.fields().include("_id", "email", "status_id");

        Document userDoc = mongoTemplate.findOne(userQuery, Document.class, "users");
        if (userDoc == null) {
            throw new NoSuchElementException("Usuario no encontrado en el clúster NoSQL con el identificador provisto: " + ttoId);
        }

        if (activeStatusId.equals(userDoc.get("status_id"))) {
            throw new IllegalArgumentException("Operación inválida: La cuenta de usuario ya se encuentra aprobada y en estado ACTIVO.");
        }
        ObjectId userId = userDoc.getObjectId("_id");

        Map<String, String> incomingPermissions = request.permissions();

        Query appsQuery = new Query(Criteria.where("name").in(incomingPermissions.keySet()));
        appsQuery.fields().include("_id", "name");
        List<Document> fetchedApps = mongoTemplate.find(appsQuery, Document.class, "applications");

        List<ObjectId> appIds = fetchedApps.stream().map(doc -> doc.getObjectId("_id")).collect(Collectors.toList());
        List<String> roleNames = new ArrayList<>(incomingPermissions.values());

        Query rolesQuery = new Query(Criteria.where("app_id").in(appIds).and("name").in(roleNames));
        rolesQuery.fields().include("_id", "app_id", "name");
        List<Document> fetchedRoles = mongoTemplate.find(rolesQuery, Document.class, "roles");

        Map<String, ObjectId> roleCacheMap = fetchedRoles.stream().collect(Collectors.toMap(
                role -> role.getObjectId("app_id").toString() + "_" + role.getString("name"),
                role -> role.getObjectId("_id"),
                (existing, replacement) -> existing
        ));

        List<Document> userApplicationsToInsert = new ArrayList<>(fetchedApps.size());
        Date currentDate = new Date();

        for (Document appDoc : fetchedApps) {
            String appIdStr = appDoc.getObjectId("_id").toString();
            String appName = appDoc.getString("name");
            String targetRoleName = incomingPermissions.get(appName);

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

        mongoTemplate.insert(userApplicationsToInsert, "user_applications");

        Update update = new Update();
        update.set("status_id", activeStatusId);
        update.set("status_name", "ACTIVE");
        update.set("failed_attempts", 0);
        update.set("updated_at", currentDate);
        mongoTemplate.updateFirst(userQuery, update, "users");

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
        List<UserResponseDTO> allUsers = userRepository.findAllUsersWithTto(null);

        Map<String, List<CompanyUsersGroupDto.UserExpandedNode>> groupedMap = new HashMap<>();
        Map<String, String> companyNameMap = new HashMap<>();
        Map<String, String> companyCuitMap = new HashMap<>();

        String unassignedId = "UNASSIGNED";
        groupedMap.put(unassignedId, new ArrayList<>());
        companyNameMap.put(unassignedId, "Usuarios sin Organización");
        companyCuitMap.put(unassignedId, "00-00000000-0");

        for (UserResponseDTO user : allUsers) {
            boolean hasCompany = false;

            if (user.personRelations() != null && !user.personRelations().isEmpty()) {
                for (Map<String, Object> rel : user.personRelations()) {
                    Object parentIdObj = rel.get("parent_id");
                    Object companyNameObj = rel.get("related_name");
                    Object companyCuitObj = rel.get("code");

                    if (parentIdObj != null) {
                        String compId = parentIdObj.toString();
                        String compName = companyNameObj != null ? companyNameObj.toString() : "UNKNOWN_COMPANY";
                        String compCuit = companyCuitObj != null ? companyCuitObj.toString() : "N/A";

                        CompanyUsersGroupDto.UserExpandedNode employeeNode = new CompanyUsersGroupDto.UserExpandedNode(
                                user.id(),
                                user.email(),
                                user.status(),
                                (user.personName() != null ? user.personName() : "") + " " + (user.personApellido() != null ? user.personApellido() : ""),
                                user.personCode() != null ? user.personCode() : ""
                        );

                        groupedMap.computeIfAbsent(compId, k -> new ArrayList<>()).add(employeeNode);
                        companyNameMap.putIfAbsent(compId, compName);
                        companyCuitMap.putIfAbsent(compId, compCuit);
                        hasCompany = true;
                    }
                }
            }

            if (!hasCompany) {
                CompanyUsersGroupDto.UserExpandedNode standaloneNode = new CompanyUsersGroupDto.UserExpandedNode(
                        user.id(),
                        user.email(),
                        user.status(),
                        (user.personName() != null ? user.personName() : "") + " " + (user.personApellido() != null ? user.personApellido() : ""),
                        user.personCode() != null ? user.personCode() : ""
                );
                groupedMap.get(unassignedId).add(standaloneNode);
            }
        }

        if (groupedMap.get(unassignedId).isEmpty()) {
            groupedMap.remove(unassignedId);
            companyNameMap.remove(unassignedId);
            companyCuitMap.remove(unassignedId);
        }

        return groupedMap.entrySet().stream()
                .map(entry -> new CompanyUsersGroupDto(
                        entry.getKey(),
                        companyCuitMap.get(entry.getKey()),
                        companyNameMap.get(entry.getKey()),
                        entry.getValue()
                ))
                .collect(Collectors.toList());
    }
}
