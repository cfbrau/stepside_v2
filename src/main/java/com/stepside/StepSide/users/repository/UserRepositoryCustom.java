package com.stepside.StepSide.users.repository;

import com.stepside.StepSide.users.dto.UserResponseDTO;
import org.bson.types.ObjectId;
import java.util.List;

public interface UserRepositoryCustom {
    List<UserResponseDTO> findAllUsersWithTto(ObjectId statusId);
}
