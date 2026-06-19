package com.stepside.StepSide.notification.repository;

import com.stepside.StepSide.notification.model.MailDestination;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Capa de persistencia NoSQL para el control de la matriz de ruteo en MongoDB Atlas.
 * Saneada minuciosamente y encapsulada bajo el paquete oficial 'notification/repository'.
 */
@Repository
public interface MailDestinationRepository extends MongoRepository<MailDestination, String> {

    /**
     * Recupera de forma atómica la matriz de destinatarios basándose en el nombre de la plantilla.
     * Reemplaza de forma O(1) a la query relacional pesada con JOIN FETCH.
     */
    Optional<MailDestination> findByTemplateName(String templateName);
}
