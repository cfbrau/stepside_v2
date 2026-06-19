package com.stepside.StepSide.notification.repository;

import com.stepside.StepSide.notification.model.MailTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Capa de persistencia NoSQL para el control del catálogo de plantillas de correo en MongoDB Atlas.
 * Encapsulado con alta cohesión adentro de la característica 'common/notification/repository'.
 */
@Repository
public interface MailTemplateRepository extends MongoRepository<MailTemplate, String> {

    /**
     * Localiza una plantilla dinámica por su nombre único de proceso (ej: 'NEED_APPROVAL').
     * Crítico para el motor de emails en Google Cloud Run.
     */
    Optional<MailTemplate> findByName(String name);
}
