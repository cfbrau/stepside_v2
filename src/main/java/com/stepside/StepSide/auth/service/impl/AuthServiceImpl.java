package com.stepside.StepSide.auth.service.impl;

import com.stepside.StepSide.auth.dto.CreateUserRequest;
import com.stepside.StepSide.auth.dto.CreateUserResponse;
import com.stepside.StepSide.auth.dto.AuthResponseDTO;
import com.stepside.StepSide.users.dto.LoginRequestDTO;
import com.stepside.StepSide.notification.dto.EmailMessageDto;
import com.stepside.StepSide.users.model.User;
import com.stepside.StepSide.ttos.model.Tto;
import com.stepside.StepSide.users.repository.UserRepository;
import com.stepside.StepSide.ttos.repository.TtoRepository;
import com.stepside.StepSide.notification.service.EmailService;
import com.stepside.StepSide.auth.service.AuthService;
import com.stepside.StepSide.common.security.JwtProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TtoRepository ttoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final MongoTemplate mongoTemplate;
    private final JwtProvider jwtProvider;

    // CONSTANTE DE INFRAESTRUCTURA: ID único de tu aplicación Backoffice mapeado en Atlas Cloud
    @org.springframework.beans.factory.annotation.Value("${stepside.security.application-id}")
    private String stepSideAppId;

    @Override
    @org.springframework.transaction.annotation.Transactional
    public CreateUserResponse signUp(CreateUserRequest request) {

        // 1. ESCUDO DEFENSIVO: Validar unicidad del E-mail
        String emailNormalizado = request.email().trim().toLowerCase();
        if (userRepository.findByEmail(emailNormalizado).isPresent()) {
            throw new org.springframework.dao.DuplicateKeyException(
                    "Error de Registro: El correo electrónico '" + request.email() + "' ya se encuentra registrado en la plataforma.");
        }

        Date timestampNow = new Date();
        String currentTimestampStr = java.time.Instant.now().toString();

        // 2. RESOLUCIÓN DINÁMICA DE ESTADO: Buscamos el _id del estado 'PENDING' en 'user_statuses'
        Query statusQuery = new Query(Criteria.where("name").is("PENDING"));
        statusQuery.fields().include("_id");
        Document statusDoc = mongoTemplate.findOne(statusQuery, Document.class, "user_statuses");
        ObjectId pendingStatusId = statusDoc != null ? statusDoc.getObjectId("_id") : null;

        // 3. RESOLUCIÓN DINÁMICA DEL TIPO DE RELACIÓN: Buscamos el _id de 'WORK_FOR' en 'relation_types'
        Query relTypeQuery = new Query(Criteria.where("name").is("WORK_FOR"));
        relTypeQuery.fields().include("_id");
        Document relTypeDoc = mongoTemplate.findOne(relTypeQuery, Document.class, "relation_types");
        String relationTypeIdStr = relTypeDoc != null ? relTypeDoc.get("_id").toString() : "6a305b8d5cffbbf10841644f";

        // ============================================================================
        // PASO I: CONFIGURAR / PERSISTIR EMPRESA (Garantiza nombres de campos minúsculos)
        // ============================================================================
        String taxId = request.companyCuit() != null && !request.companyCuit().trim().isEmpty()
                ? request.companyCuit().trim().toUpperCase()
                : "COMP-" + java.util.UUID.randomUUID().toString().substring(0, 8);

        java.util.Optional<Tto> existingCompany = ttoRepository.findByCode(taxId);
        Tto savedCompany;

        if (existingCompany.isPresent()) {
            savedCompany = existingCompany.get();
        } else {
            Tto companyTto = new Tto();
            companyTto.setCode(taxId);
            companyTto.setTtoName(request.companyRazonSocial().trim());
            companyTto.setTtoTypeName("COMPANY");
            companyTto.setTtoStatusName("PENDING");

            Map<String, Object> companyAttributes = new HashMap<>();
            companyAttributes.put("cuit", taxId);
            companyAttributes.put("nombre", request.companyNombreFantasia().trim());
            companyAttributes.put("razon_social", request.companyRazonSocial().trim());
            companyTto.setAttributes(companyAttributes);

            companyTto.setCreatedAt(timestampNow);
            companyTto.setUpdatedAt(timestampNow);

            savedCompany = ttoRepository.save(companyTto);
        }

        // ============================================================================
        // PASO II: PERSISTIR PERSONA CON GRAFO RELACIONAL SINCRO (parent_id y relation_type_id)
        // ============================================================================
        Tto personTto = new Tto();
        personTto.setCode(emailNormalizado);

        String resolvedName = request.firstName().trim() + " " + request.lastName().trim();
        personTto.setTtoName(resolvedName);
        personTto.setTtoTypeName("PERSON");
        personTto.setTtoStatusName("PENDING");

        Map<String, Object> personAttributes = new HashMap<>();
        personAttributes.put("nombre", request.firstName().trim());
        personAttributes.put("apellido", request.lastName().trim());
        personAttributes.put("email", emailNormalizado);
        personTto.setAttributes(personAttributes);

        personTto.setCreatedAt(timestampNow);
        personTto.setUpdatedAt(timestampNow);

        List<Map<String, Object>> personRelations = new ArrayList<>();
        Map<String, Object> toCompanyNode = new HashMap<>();
        toCompanyNode.put("relation_type_id", relationTypeIdStr);
        toCompanyNode.put("relation_type_name", "WORK_FOR");
        toCompanyNode.put("parent_id", savedCompany.getId());
        toCompanyNode.put("related_id", personTto.getCode());
        toCompanyNode.put("start_date", currentTimestampStr);
        toCompanyNode.put("end_date", null);
        personRelations.add(toCompanyNode);
        personTto.setRelations(personRelations);

        Tto savedPerson = ttoRepository.save(personTto);

        // ============================================================================
        // PASO III: MUTACIÓN INVERSA SOBRE LA COMPAÑÍA (Espejado en caliente)
        // ============================================================================
        Map<String, Object> toPersonNode = new HashMap<>();
        toPersonNode.put("relation_type_id", relationTypeIdStr);
        toPersonNode.put("relation_type_name", "HAS_EMPLOYEE");
        toPersonNode.put("parent_id", savedCompany.getId());
        toPersonNode.put("related_id", savedPerson.getId());
        toPersonNode.put("start_date", currentTimestampStr);
        toPersonNode.put("end_date", null);

        if (savedCompany.getRelations() == null) {
            savedCompany.setRelations(new ArrayList<>());
        }
        savedCompany.getRelations().add(toPersonNode);
        savedCompany.setUpdatedAt(timestampNow);
        ttoRepository.save(savedCompany);

        // ============================================================================
        // PASO IV: PERSISTIR CUENTA DE USUARIO CON ENLACE DE CATÁLOGOS LÓGICOS
        // ============================================================================
        User user = new User();
        user.setEmail(emailNormalizado);
        user.setPassword(passwordEncoder.encode(request.password()));

        user.setStatusId(pendingStatusId);
        user.setStatusName("PENDING");

        user.setTtoId(savedPerson.getId());
        user.setExpiration(Date.from(java.time.Instant.now().plus(365, java.time.temporal.ChronoUnit.DAYS)));
        user.setCreatedAt(timestampNow);
        user.setUpdatedAt(timestampNow);

        User savedUser = userRepository.save(user);

        // ============================================================================
        // PASO V: MOTOR DE NOTIFICACIONES ASÍNCRONAS
        // ============================================================================
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("mail_registered_user", savedUser.getEmail());

        EmailMessageDto alertDto = new EmailMessageDto(
                savedUser.getEmail(),
                "Aviso de Sistema - Registro Recibido",
                "NEED_APPROVAL",
                templateVariables
        );
        emailService.sendEmail(alertDto);

        return new CreateUserResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedPerson.getId(),
                savedCompany.getId(),
                "La cuenta de ecosistema ha sido registrada de forma atómica y se encuentra en espera de aprobación administrativa."
        );
    }

    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {
        // 1. Verificación de Existencia de Cuenta
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new NoSuchElementException("Credenciales inválidas: El email no se encuentra registrado."));

        // 2. Control de Candado Activo
        if (user.getStatusName() != null && "LOCKED".equalsIgnoreCase(user.getStatusName())) {
            throw new IllegalStateException("Acceso denegado: La cuenta de usuario se encuentra bloqueada.");
        }

        // 3. Verificación de Contraseña Criptográfica
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Credenciales inválidas: La contraseña provista es incorrecta.");
        }

        System.out.println("Datos de usuario");

        // ============================================================================
        // 🚀 PASO II: CRUCE DE ACCESOS MULTITENANT (Colección Física user_applications)
        // SANEADO: Apuntamos estrictamente al nombre real de tu tabla 'user_applications'
        // ============================================================================
        org.bson.types.ObjectId userObjectId = new org.bson.types.ObjectId(user.getId().toString());

        // Construimos el documento BSON con las llaves exactas de tu captura de Compass
        org.bson.Document filtroBson = new org.bson.Document()
                .append("user_id", userObjectId)
                .append("appId", this.stepSideAppId.trim()); // Mantiene la "I" mayúscula del BSON

        // Forzamos al driver a buscar strictly en la colección real de tu base
        org.bson.Document userAppDoc = mongoTemplate.getDb()
                .getCollection("user_applications") // <-- CORREGIDO: Nombre real y exacto en Atlas
                .find(filtroBson)
                .first();

        // ESCUDO DEFENSIVO OWASP: Si no posee asignación activa para este appId, se expulsa
        if (userAppDoc == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Acceso denegado: El usuario no posee privilegios asignados para el appId '" + this.stepSideAppId.trim() + "' en esta aplicación."
            );
        }

        // Recuperamos el role_id del documento BSON mapeando de forma segura
        String roleIdStr = userAppDoc.get("role_id") != null ? userAppDoc.get("role_id").toString() : null;
        if (roleIdStr == null || roleIdStr.isBlank()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Acceso denegado: Estructura de privilegios corrompida en el ecosistema."
            );
        }

        // ============================================================================
        // 🚀 PASO III: RESOLUCIÓN DEL NOMBRE DEL ROL PURO (roles)
        // ============================================================================
        org.bson.types.ObjectId roleObjectId = new org.bson.types.ObjectId(roleIdStr);
        Query queryRol = new Query(Criteria.where("_id").is(roleObjectId));
        Document roleDoc = mongoTemplate.findOne(queryRol, Document.class, "roles");

        if (roleDoc == null) {
            throw new NoSuchElementException("Error de consistencia: El rol asignado no existe en la base de datos cloud.");
        }

        String pureRoleName = roleDoc.getString("name");
        if (pureRoleName == null || pureRoleName.isBlank()) {
            throw new IllegalStateException("Error de consistencia: El rol mapeado no posee un identificador semántico válido.");
        }

        // 4. CANAL I: Generamos el token inyectándole estrictamente el rol puro ("ADMIN")
        String token = jwtProvider.generateToken(user.getEmail(), pureRoleName.trim());

        return new AuthResponseDTO(token, "Bearer", user.getEmail());
    }

}
