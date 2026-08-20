package com.stepside.StepSide.users.repository;

import com.stepside.StepSide.users.dto.UserResponseDTO;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryCustom {
    List<UserResponseDTO> findAllUsersWithTto(ObjectId statusId);

    Optional<String> findRoleNameByUserAndApp(ObjectId userId, String appId);

    Optional<String> findCompanyIdByUserId(org.bson.types.ObjectId userId);

}
