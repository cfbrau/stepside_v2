package com.stepside.StepSide.auth.service.impl;

import com.stepside.StepSide.auth.dto.CreateUserRequest;
import com.stepside.StepSide.auth.dto.CreateUserResponse;
import com.stepside.StepSide.auth.service.AuthService;
import com.stepside.StepSide.notification.dto.EmailMessageDto;
import com.stepside.StepSide.notification.service.EmailService;
import com.stepside.StepSide.ttos.model.Tto;
import com.stepside.StepSide.ttos.repository.TtoRepository;
import com.stepside.StepSide.users.model.User;
import com.stepside.StepSide.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación de alta gama para el control de accesos y Onboarding en MongoDB Atlas.
 * Saneada bajo estrictas normas de consistencia bidireccional de grafos y diseño polimórfico.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TtoRepository ttoRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public CreateUserResponse signUp(CreateUserRequest request) {

        // 1. ESCUDO DEFENSIVO: Validar unicidad del E-mail (Case Insensitive en memoria)
        String emailNormalizado = request.account().email().trim().toLowerCase();
        if (userRepository.findByEmail(emailNormalizado).isPresent()) {
            throw new org.springframework.dao.DuplicateKeyException(
                    "Error de Registro: El correo electrónico '" + request.account().email() + "' ya se encuentra registrado en la plataforma.");
        }

        Date timestampNow = new Date();
        String currentTimestampStr = Instant.now().toString();

        // ============================================================================
        // PASO I: CONFIGURAR / PERSISTIR EMPRESA REUTILIZABLE (NoSQL Polimórfico)
        // ============================================================================
        String taxId = request.company().attributes().get("cuit") != null
                ? request.company().attributes().get("cuit").toString().trim().toUpperCase()
                : "COMP-" + UUID.randomUUID().toString().substring(0, 8);

        Optional<Tto> existingCompany = ttoRepository.findByCode(taxId);
        Tto savedCompany;

        if (existingCompany.isPresent()) {
            savedCompany = existingCompany.get();
        } else {
            Tto companyTto = new Tto();
            companyTto.setCode(taxId);
            companyTto.setTtoName(request.company().attributes().getOrDefault("razon_social", "Empresa Registrada").toString());
            companyTto.setTtoTypeName("COMPANY");
            companyTto.setTtoStatusName("PENDING");
            companyTto.setAttributes(request.company().attributes());
            companyTto.setCreatedAt(timestampNow);
            companyTto.setUpdatedAt(timestampNow);

            savedCompany = ttoRepository.save(companyTto);
        }

        // ============================================================================
        // PASO II: PERSISTIR PERSONA CON GRAFO RELACIONAL EMBEBIDO
        // ============================================================================
        Tto personTto = new Tto();
        String dni = request.person().attributes().get("dni") != null
                ? request.person().attributes().get("dni").toString().trim()
                : "PERS-" + UUID.randomUUID().toString().substring(0, 8);

        personTto.setCode(dni.toUpperCase());
        String resolvedName = request.person().attributes().getOrDefault("nombre", "Usuario").toString() + " " +
                request.person().attributes().getOrDefault("apellido", "StepSide").toString();
        personTto.setTtoName(resolvedName.trim());
        personTto.setTtoTypeName("PERSON");
        personTto.setTtoStatusName("PENDING");
        personTto.setAttributes(request.person().attributes());
        personTto.setCreatedAt(timestampNow);
        personTto.setUpdatedAt(timestampNow);

        // Diseñamos el engrane relacional NoSQL nativo: Persona -> Pertenece a -> Empresa
        List<Map<String, Object>> personRelations = new ArrayList<>();
        Map<String, Object> toCompanyNode = new HashMap<>();
        toCompanyNode.put("relation_type_name", "BELONG_TO");
        toCompanyNode.put("related_id", savedCompany.getId());
        toCompanyNode.put("start_date", currentTimestampStr);
        toCompanyNode.put("end_date", null);
        personRelations.add(toCompanyNode);
        personTto.setRelations(personRelations);

        Tto savedPerson = ttoRepository.save(personTto);

        // ============================================================================
        // CORREGIDO: ESPEJADO MUTUO EN CALIENTE (Empresa -> Posee Empleado -> Persona)
        // Evita dejar el documento de la compañía huérfano de referencias en el cluster cloud
        // ============================================================================
        Map<String, Object> toPersonNode = new HashMap<>();
        toPersonNode.put("relation_type_name", "HAS_EMPLOYEE"); // Relación semántica inversa
        toPersonNode.put("related_id", savedPerson.getId()); // Inyectamos el ID de la persona guardada
        toPersonNode.put("start_date", currentTimestampStr);
        toPersonNode.put("end_date", null);

        if (savedCompany.getRelations() == null) {
            savedCompany.setRelations(new ArrayList<>());
        }
        savedCompany.getRelations().add(toPersonNode);
        savedCompany.setUpdatedAt(timestampNow);

        // Sincronizamos la mutación del grafo inverso directamente en Atlas
        ttoRepository.save(savedCompany);

        // ============================================================================
        // PASO III: PERSISTIR CUENTA DE USUARIO CON AUDITORÍA FÍSICA NATIVA
        // ============================================================================
        User user = new User();
        user.setEmail(emailNormalizado);
        user.setPassword(passwordEncoder.encode(request.account().password()));
        user.setStatusName("PENDING");
        user.setTtoId(savedPerson.getId());
        user.setExpiration(Date.from(Instant.now().plus(365, ChronoUnit.DAYS)));
        user.setCreatedAt(timestampNow);
        user.setUpdatedAt(timestampNow);

        User savedUser = userRepository.save(user);

        // ============================================================================
        // PASO IV: NOTIFICACIÓN ASÍNCRONA - ALERTA DE APROBACIÓN PENDIENTE
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

        return new CreateUserResponse(savedUser.getId(), savedUser.getEmail(), savedPerson.getId(), savedCompany.getId());
    }
}
