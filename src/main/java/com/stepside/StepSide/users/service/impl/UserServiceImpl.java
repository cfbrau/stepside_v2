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
import java.util.*;
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

        // PASO 1: Viaje masivo a la base de datos para traer TODOS los usuarios hidratados.
        // Se le pasa null para que el repositorio ignore filtros de estado y traiga la nómina completa.
        List<UserResponseDTO> allUsers = userRepository.findAllUsersWithTto(null);

        // Diccionarios Hash en memoria de Java para armar el árbol jerárquico final.
        Map<String, List<CompanyUsersGroupDto.UserExpandedNode>> groupedMap = new HashMap<>();
        Map<String, String> companyNameMap = new HashMap<>();
        Map<String, String> companyCuitMap = new HashMap<>();

        // PASO 2: "Operación Escudo" - Recolección de IDs de las empresas.
        // Recorremos todos los usuarios para juntar en un 'Set' los IDs de las organizaciones de sus relaciones.
        // Al usar un Set, evitamos duplicados en memoria (si 20 usuarios son de la misma empresa, el ID se guarda una sola vez).
        Set<ObjectId> parentCompanyIds = new HashSet<>();
        for (UserResponseDTO user : allUsers) {
            if (user.personRelations() != null) {
                for (Map<String, Object> rel : user.personRelations()) {
                    Object parentIdObj = rel.get("parent_id");
                    // Validamos que el ID tenga el formato hexadecimal correcto de 24 caracteres de MongoDB
                    if (parentIdObj != null && org.bson.types.ObjectId.isValid(parentIdObj.toString())) {
                        parentCompanyIds.add(new org.bson.types.ObjectId(parentIdObj.toString()));
                    }
                }
            }
        }

        // Mapas auxiliares de respaldo que van a guardar los datos puros leídos directo de la base de datos
        Map<String, String> dbCompanyCuitMap = new HashMap<>();
        Map<String, String> dbCompanyNameMap = new HashMap<>();

        // PASO 3: "Query de Respaldo Defensiva" - Consulta Batch a la tabla de TTOs (Empresas).
        // Si encontramos al menos una empresa asociada en el paso anterior, vamos a Atlas a buscar sus datos.
        if (!parentCompanyIds.isEmpty()) {
            // Creamos la query usando el operador $in pasándole toda la lista de IDs recolectados
            org.springframework.data.mongodb.core.query.Query companyQuery =
                    new org.springframework.data.mongodb.core.query.Query(org.springframework.data.mongodb.core.query.Criteria.where("_id").in(parentCompanyIds));

            // Optimización de RAM: Solicitamos únicamente las columnas estrictamente necesarias de la empresa
            companyQuery.fields().include("_id", "code", "description", "attributes.cuit", "attributes.nombre", "attributes.razon_social");

            // Ejecutamos la búsqueda como Documentos crudos BSON en la colección plural "ttos"
            List<Document> companiesRaw = mongoTemplate.find(companyQuery, Document.class, "ttos");

            // Procesamos el JSON crudo retornado por MongoDB Atlas
            for (Document doc : companiesRaw) {
                String compIdStr = doc.get("_id").toString();

                // Extracción Jerárquica del CUIT corporativo:
                String compCuit = "N/A";
                Document attrs = (Document) doc.get("attributes");
                // Intento 1: Buscamos en el subobjeto attributes.cuit
                if (attrs != null && attrs.getString("cuit") != null) {
                    compCuit = attrs.getString("cuit");
                }
                // Intento 2 (Fallback): Si no estaba ahí, buscamos en el campo 'code' de la raíz
                else if (doc.getString("code") != null) {
                    compCuit = doc.getString("code");
                }
                dbCompanyCuitMap.put(compIdStr, compCuit); // Guardamos en el diccionario de CUITs

                // Extracción Jerárquica del Nombre comercial de la Empresa:
                String compName = "EMPRESA_SIN_NOMBRE";
                if (attrs != null && attrs.getString("nombre") != null) compName = attrs.getString("nombre");
                else if (attrs != null && attrs.getString("razon_social") != null) compName = attrs.getString("razon_social");
                else if (doc.getString("description") != null) compName = doc.getString("description");

                dbCompanyNameMap.put(compIdStr, compName); // Guardamos en el diccionario de Nombres
            }
        }

        // PASO 4: Inicialización del Comodín "Usuarios sin Organización".
        // Preparamos la bolsa común para alojar a aquellos usuarios independientes que no tienen un parent_id en sus datos.
        String unassignedId = "UNASSIGNED";
        groupedMap.put(unassignedId, new ArrayList<>());
        companyNameMap.put(unassignedId, "Usuarios sin Organización");
        companyCuitMap.put(unassignedId, "00-00000000-0");

        // PASO 5: "El Verdadero Join en Memoria O(N)" - Clasificación y Acoplamiento de Empleados.
        // Recorremos nuevamente la lista general de usuarios para meterlos adentro del casillero de su respectiva empresa.
        for (UserResponseDTO user : allUsers) {
            boolean hasCompany = false;

            // Si el usuario registra relaciones con organizaciones en sus TTOs
            if (user.personRelations() != null && !user.personRelations().isEmpty()) {
                for (Map<String, Object> rel : user.personRelations()) {
                    Object parentIdObj = rel.get("parent_id");

                    if (parentIdObj != null) {
                        String compId = parentIdObj.toString();

                        // Cruzamos los datos de la empresa buscándolos de forma instantánea O(1) en los mapas de respaldo
                        String compName = dbCompanyNameMap.getOrDefault(compId, "UNKNOWN_COMPANY");
                        String compCuit = dbCompanyCuitMap.getOrDefault(compId, "N/A");

                        // Salvavidas: Si la query Batch falló pero el repositorio conservaba algún dato viejo, lo rescatamos
                        if (rel.get("related_name") != null && "UNKNOWN_COMPANY".equals(compName)) {
                            compName = rel.get("related_name").toString();
                        }
                        if (rel.get("company_cuit") != null && "N/A".equals(compCuit)) {
                            compCuit = rel.get("company_cuit").toString();
                        }

                        // Instanciamos el sub-record compacto 'UserExpandedNode' diseñado por Fabián para la grilla de la UI
                        CompanyUsersGroupDto.UserExpandedNode employeeNode = new CompanyUsersGroupDto.UserExpandedNode(
                                user.id(),
                                user.email(),
                                user.status(),
                                (user.personName() != null ? user.personName() : "") + " " + (user.personApellido() != null ? user.personApellido() : ""),
                                user.personCode() != null ? user.personCode() : "" // Extraemos el DNI real mapeado de la tabla tto
                        );

                        // Metemos al empleado adentro de la lista correspondiente a su ID de Empresa
                        groupedMap.computeIfAbsent(compId, k -> new ArrayList<>()).add(employeeNode);
                        companyNameMap.put(compId, compName);
                        companyCuitMap.put(compId, compCuit); // Seteamos el CUIT verídico salvado
                        hasCompany = true;
                    }
                }
            }

            // Si el bucle terminó y el usuario demostró no tener ninguna empresa asociada, va a la bolsa de independientes
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

        // Control de Higiene Visual: Si ningún usuario terminó en la bolsa de "Sin Organización", la borramos para limpiar el JSON
        if (groupedMap.get(unassignedId).isEmpty()) {
            groupedMap.remove(unassignedId);
            companyNameMap.remove(unassignedId);
            companyCuitMap.remove(unassignedId);
        }

        // PASO 6: Transformación y Despacho.
        // Convertimos nuestro mapa de grupos de Java utilizando la API de Streams para retornar la lista inmutable de CompanyUsersGroupDto.
        return groupedMap.entrySet().stream()
                .map(entry -> new CompanyUsersGroupDto(
                        entry.getKey(),                      // companyId
                        companyCuitMap.get(entry.getKey()), // companyCuit (Mapeado sin pérdidas de N/A)
                        companyNameMap.get(entry.getKey()), // companyName
                        entry.getValue()                     // employees (Lista elástica de sub-records)
                ))
                .collect(Collectors.toList());
    }

}
