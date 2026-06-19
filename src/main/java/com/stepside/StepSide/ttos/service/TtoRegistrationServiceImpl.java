package com.stepside.StepSide.ttos.service;

import com.stepside.StepSide.ttos.dto.TtoRegistrationRequestDto;
import com.stepside.StepSide.ttos.dto.TtoRegistrationResponseDto;
import com.stepside.StepSide.ttos.model.Tto;
import com.stepside.StepSide.ttos.repository.TtoRepository;
import com.stepside.StepSide.users.model.User;
import com.stepside.StepSide.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementación transaccional para la orquestación del Onboarding en MongoDB Atlas.
 * Aplica diseño polimórfico reutilizable y vinculación elástica por referencias de String.
 */
@Service
@RequiredArgsConstructor
public class TtoRegistrationServiceImpl implements TtoRegistrationService {

    private final UserRepository userRepository;
    private final TtoRepository ttoRepository;
    private final PasswordEncoder passwordEncoder; // Inyecta el BCrypt configurado en tu seguridad

    @Override
    public TtoRegistrationResponseDto registerEcosystemUser(TtoRegistrationRequestDto requestDto) {

        // 1. REGLA DE PROTECCIÓN PERIMETRAL: Validar unicidad del E-mail
        if (userRepository.findByEmail(requestDto.email()).isPresent()) {
            throw new IllegalArgumentException("Conflicto de seguridad: El e-mail '" + requestDto.email() + "' ya posee una cuenta registrada.");
        }

        // 2. EXTRACCIÓN DINÁMICA DE CÓDIGOS NATURALES DESDE LOS MAPAS NOSQL
        String rawCuit = (String) requestDto.companyAttributes().get("cuit");
        String rawDni = (String) requestDto.personAttributes().get("dni");

        if (rawCuit == null || rawCuit.isBlank()) {
            throw new IllegalArgumentException("Validación fallida: El atributo 'cuit' de la organización es mandatorio.");
        }
        if (rawDni == null || rawDni.isBlank()) {
            throw new IllegalArgumentException("Validación fallida: El atributo 'dni' del personal es mandatorio.");
        }

        String companyCode = rawCuit.trim().toUpperCase();
        String personCode = rawDni.trim().toUpperCase();

        // 3. SEGUNDA BARRERA DEFENSIVA: Validar que no existan duplicados físicos en MongoDB Atlas
        if (ttoRepository.findByCode(companyCode).isPresent()) {
            throw new IllegalArgumentException("Conflicto comercial: Ya existe una Empresa dada de alta con el CUIT '" + companyCode + "'.");
        }
        if (ttoRepository.findByCode(personCode).isPresent()) {
            throw new IllegalArgumentException("Conflicto operacional: Ya existe un TTO Persona registrado con el DNI '" + personCode + "'.");
        }

        Date timestampNow = new Date();

        // ============================================================================
        // 4. PERSISTENCIA PASO A PASO: CREACIÓN DE LA EMPRESA (COMPANY)
        // ============================================================================
        Tto companyTto = new Tto();
        companyTto.setCode(companyCode);
        companyTto.setTtoName((String) requestDto.companyAttributes().getOrDefault("razon_social", "Empresa Registrada"));
        companyTto.setTtoTypeName("COMPANY");
        companyTto.setTtoStatusName("ACTIVE"); // Las organizaciones nacen operativas
        companyTto.setAttributes(requestDto.companyAttributes());
        companyTto.setCreatedAt(timestampNow);
        companyTto.setUpdatedAt(timestampNow);

        Tto savedCompany = ttoRepository.save(companyTto);

        // ============================================================================
        // 5. PERSISTENCIA PASO A PASO: CREACIÓN DE LA PERSONA (PERSON) Y VÍNCULO B2B
        // ============================================================================
        Tto personTto = new Tto();
        personTto.setCode(personCode);

        String resolvedName = requestDto.personAttributes().getOrDefault("nombre", "Usuario").toString() + " " +
                requestDto.personAttributes().getOrDefault("apellido", "StepSide").toString();
        personTto.setTtoName(resolvedName.trim());
        personTto.setTtoTypeName("PERSON");
        personTto.setTtoStatusName("ACTIVE");
        personTto.setAttributes(requestDto.personAttributes());
        personTto.setCreatedAt(timestampNow);
        personTto.setUpdatedAt(timestampNow);

        // Diseñamos el lazo relacional NoSQL elástico por String referencial
        Map<String, Object> workRelation = new HashMap<>();
        workRelation.put("relation_type_name", "WORK_FOR");
        workRelation.put("related_tto_id", savedCompany.getId()); // Inyectamos el Hash ID de la empresa arriba guardada
        workRelation.put("start_date", timestampNow.toString());
        workRelation.put("end_date", null);

        personTto.getRelations().add(workRelation);
        Tto savedPerson = ttoRepository.save(personTto);

        // ============================================================================
        // 6. PERSISTENCIA PASO A PASO: CUENTA DE USUARIO CANDADO (PENDING_APPROVAL)
        // ============================================================================
        User newUser = new User();
        newUser.setEmail(requestDto.email().trim().toLowerCase());
        newUser.setPassword(passwordEncoder.encode(requestDto.password())); // Cifrado simétrico BCrypt
        newUser.setStatusName("PENDING"); // Clavamos el peaje administrativo solicitado
        newUser.setTtoId(savedPerson.getId()); // Amarramos la cuenta al String ID de la persona
        newUser.setCreatedAt(timestampNow);
        newUser.setUpdatedAt(timestampNow);

        User savedUser = userRepository.save(newUser);

        // 7. RESPUESTA INMUTABLE DE ALTA CALIDAD PARA EL FRONTEND
        return new TtoRegistrationResponseDto(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getStatusName(),
                savedPerson.getId(),
                savedCompany.getId(),
                timestampNow
        );
    }
}
