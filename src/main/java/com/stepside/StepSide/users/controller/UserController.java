package com.stepside.StepSide.users.controller;

import com.stepside.StepSide.users.dto.CompanyUsersGroupDto;
import com.stepside.StepSide.users.dto.UserApprovalRequestDTO;
import com.stepside.StepSide.users.dto.UserResponseDTO;
import com.stepside.StepSide.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Controlador de red universal para el gobierno de cuentas e identidades.
 * Centraliza las operaciones de listado elástico, workflows de aprobación y futuras mutaciones NoSQL.
 * Saneado por el Arquitecto para acoplar la agregación elástica de TTOs.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * REQUERIMIENTO 1 y 2: Recupera la nómina general de usuarios hidratada con sus TTOs.
     * Soporta filtrado opcional por identificador de estado (ObjectId) o nombre.
     * GET /api/users?status=6a3064575cffbbf108416480
     */
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getUsersWithFilter(
            @RequestParam(name = "status", required = false) String status) {

        List<UserResponseDTO> response = userService.getUsersWithFilter(status);
        return ResponseEntity.ok(response);
    }

    /**
     * REQUERIMIENTO 3: Vista jerárquica especializada para el Backoffice.
     * GET /api/users/grouped-by-company
     */
    @GetMapping("/grouped-by-company")
    public ResponseEntity<List<CompanyUsersGroupDto>> getUsersGroupedByCompany() {
        List<CompanyUsersGroupDto> response = userService.getUsersGroupedByCompany();
        return ResponseEntity.ok(response);
    }

    /**
     * WORKFLOW ADMINISTRATIVO: Ejecuta la aprobación atómica de la cuenta en el clúster NoSQL.
     * POST /api/users/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveUser(
            @PathVariable(name = "id") String userId,
            @Valid @RequestBody UserApprovalRequestDTO requestDto) {

        userService.approveUser(userId, requestDto);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // ============================================================================
    // ESPACIO RESERVADO PARA FUTUROS REQUERIMIENTOS:
    // @PutMapping("/{id}") -> Modificar datos del perfil del usuario
    // @DeleteMapping("/{id}") -> Baja lógica o suspensión de accesos
    // ============================================================================
}
