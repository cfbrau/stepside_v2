package com.stepside.StepSide.users.repository;

import com.stepside.StepSide.users.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Capa de persistencia NoSQL para el gobierno de identidades en MongoDB Atlas.
 * Unificada milimétricamente por Fabián y extendida mediante arquitectura modular
 * para soportar consultas e integraciones de agregación avanzada.
 */
@Repository
public interface UserRepository extends MongoRepository<User, String>, UserRepositoryCustom {

    Optional<User> findByEmail(String email);

    List<User> findByStatusName(String statusName);
}
