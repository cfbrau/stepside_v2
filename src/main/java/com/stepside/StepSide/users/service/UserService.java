package com.stepside.StepSide.users.service;

import com.stepside.StepSide.users.dto.CompanyUsersGroupDto;
import com.stepside.StepSide.users.dto.UserApprovalRequestDTO;
import com.stepside.StepSide.users.dto.UserResponseDTO;
import java.util.List;

/**
 * Contrato de negocio universal para la gestión y gobernanza de identidades.
 * Define las operaciones requeridas por la capa de exposición REST.
 */
public interface UserService {

    /**
     * Recupera la nómina general de usuarios hidratada con sus TTOs y filtrada por estado.
     */
    List<UserResponseDTO> getUsersWithFilter(String status);

    /**
     * Obtiene la vista jerárquica especializada para el Backoffice agrupada por empresa.
     */
    List<CompanyUsersGroupDto> getUsersGroupedByCompany();

    /**
     * Ejecuta la aprobación atómica de una cuenta de usuario.
     */
    void approveUser(String userId, UserApprovalRequestDTO requestDto);
}
