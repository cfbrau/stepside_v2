package com.stepside.StepSide.users.controller;

import com.stepside.StepSide.users.dto.CompanyUsersGroupDto;
import com.stepside.StepSide.users.dto.UserResponseDTO;
import com.stepside.StepSide.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Gobierno de Usuarios", description = "Endpoints privados para la hidratación, reportes jerárquicos y workflows de aprobación.")
public class UserController {

    private final UserService userService;

    @GetMapping ("/getUsersWithFilter")
    public ResponseEntity<List<UserResponseDTO>> getUsersWithFilter(
            @RequestParam(name = "status", required = false) String status) {

        List<UserResponseDTO> response = userService.getUsersWithFilter(status);
        return ResponseEntity.ok(response);
    }

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
    @Operation(summary = "Aprobar cuenta de usuario", description = "Ejecuta la mutación atómica del estado del usuario y su TTO asociado a ACTIVE, y gatilla la alerta asíncrona por correo.")
    public ResponseEntity<Void> approveUser(
            @PathVariable(name = "id") String userId) {

        userService.approveUser(userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * WORKFLOW ADMINISTRATIVO: Ejecuta la baja lógica de la cuenta en el clúster NoSQL.
     * POST /api/users/{id}/deactivate
     */
    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Desactivar cuenta de usuario", description = "Ejecuta la mutación atómica del estado del usuario y su TTO asociado a DELETED.")
    public ResponseEntity<Void> deactivateUser(
            @PathVariable(name = "id") String userId) {

        userService.deactivateUser(userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
