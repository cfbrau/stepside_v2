package com.stepside.StepSide.auth.service.impl;

import com.stepside.StepSide.auth.dto.CreateUserRequest;
import com.stepside.StepSide.auth.dto.CreateUserResponse;
import com.stepside.StepSide.auth.dto.AuthResponseDTO;
import com.stepside.StepSide.common.exception.domain.AccountLockedException;
import com.stepside.StepSide.common.exception.domain.InvalidCredentialsException;
import com.stepside.StepSide.users.domain.Email;
import com.stepside.StepSide.users.dto.LoginRequestDTO;
import com.stepside.StepSide.users.model.User;
import com.stepside.StepSide.users.repository.UserRepository;
import com.stepside.StepSide.auth.service.AuthService;
import com.stepside.StepSide.common.security.JwtProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Value("${stepside.security.application-id}")
    private String stepSideAppId;

    @Override
    public CreateUserResponse signUp(CreateUserRequest request) {
        throw new UnsupportedOperationException(
                "El flujo signUp está temporalmente deshabilitado mientras se termina el refactor de dominio y manejo global de errores."
        );
    }

    @Override
    public AuthResponseDTO login(LoginRequestDTO request) {
        Email email = new Email(request.email());
        String emailNormalizado = email.value();

        User user = userRepository.findByEmail(emailNormalizado)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getStatusName() != null && "LOCKED".equalsIgnoreCase(user.getStatusName())) {
            throw new AccountLockedException();
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        log.debug("[AUTENTICACIÓN] Identidad validada para el principal: {}", emailNormalizado);

        // CONTROL DEFENSIVO SEGURO: Verificamos la integridad estructural del ID antes del parsing a ObjectId
        String rawUserId = user.getId();
        if (rawUserId == null || rawUserId.isBlank() || !ObjectId.isValid(rawUserId)) {
            log.error("[SEGURIDAD AXIAL] Error crítico de consistencia NoSQL: El usuario {} posee un ID inválido o vacío en Atlas.", emailNormalizado);
            throw new IllegalArgumentException("Acceso denegado: Error interno de consistencia de identidad.");
        }
        ObjectId userObjectId = new ObjectId(rawUserId.trim());

        // ============================================================================
        // 🚀 PASO II: CRUCE DE ACCESOS MULTITENANT
        // La resolución de mapeo del usuario a la aplicación y la resolución del rol
        // se encapsula en el repositorio custom para mantener el servicio limpio.
        // ============================================================================
        String pureRoleName = userRepository.findRoleNameByUserAndApp(userObjectId, this.stepSideAppId.trim())
                .orElseThrow(() -> {
                    log.warn("[SEGURIDAD AXIAL] Intento ilegítimo de login. Usuario {} carece de mapeo en appId: {}", emailNormalizado, this.stepSideAppId);
                    return new IllegalArgumentException("Acceso denegado: El usuario no cuenta con privilegios en este ecosistema.");
                });

        // ============================================================================
        // 🚀 PASO II.B: EXTRACCIÓN DE EMPRESA CORPORATIVA (UNIFICADO EN CUSTOM REPO)
        // Consumimos de forma segura el método del repositorio custom mapeando el userId
        // ============================================================================
        String resolvedCompanyId = userRepository.findCompanyIdByUserId(userObjectId)
                .orElse("UNKNOWN_COMPANY");

        log.info("[AUTENTICACIÓN] Seteando pasaporte digital. Usuario: {} -> Empresa: {}", emailNormalizado, resolvedCompanyId);

        // Generamos el token utilizando la sobrecarga criptográfica con el nuevo claim de la empresa
        String token = jwtProvider.generateToken(user.getEmail(), pureRoleName.trim(), resolvedCompanyId);

        return new AuthResponseDTO(token, "Bearer", user.getEmail());
    }
}
