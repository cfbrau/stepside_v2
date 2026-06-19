package com.stepside.StepSide.ttos.repository;

import com.stepside.StepSide.ttos.model.Tto;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio central NoSQL para la persistencia de documentos TTO en MongoDB Atlas.
 * Sincronizado milimétricamente con el nuevo esquema de paquetes por Feature.
 */
@Repository
public interface TtoRepository extends MongoRepository<Tto, String> {

    /**
     * Localiza un objeto de seguimiento por su código natural (CUIT, DNI, Patente).
     */
    Optional<Tto> findByCode(String code);

    /**
     * Recupera el catálogo de objetos filtrando estrictamente por su tipo (COMPANY, PERSON, etc).
     */
    List<Tto> findByTtoTypeName(String ttoTypeName);
}
