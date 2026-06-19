package com.stepside.StepSide.auth.controller;

import com.stepside.StepSide.auth.dto.CreateUserRequest;
import com.stepside.StepSide.auth.dto.CreateUserResponse;
import com.stepside.StepSide.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador perimetral público para la gestión de Onboarding y Seguridad.
 * Saneado minuciosamente y acoplado a las cañerías del AuthService NoSQL.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * FORMULARIO PÚBLICO: Procesa el registro compuesto de la firma comercial y el usuario.
     * POST /api/auth/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<CreateUserResponse> registerEcosystemAccount(
            @Valid @RequestBody CreateUserRequest requestDto) {

        CreateUserResponse response = authService.signUp(requestDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
