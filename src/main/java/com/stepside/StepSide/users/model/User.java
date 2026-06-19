package com.stepside.StepSide.users.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.util.Date;

/**
 * Entidad de seguridad central adaptada para MongoDB Atlas.
 * Saneada por Fabián con el mapeo explícito de la llave de enlace lógico camelCase.
 */
@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id // Convierte de forma inquebrantable el _id binario de Atlas a String en memoria
    @Field(value = "_id", targetType = FieldType.OBJECT_ID)
    private String id;

    @Indexed(unique = true)
    @Field("email")
    private String email;

    @Field("password")
    private String password;

    @Field("status_name")
    private String statusName;

    @Field("status_id")
    private ObjectId statusId;

    // SOLUCIÓN: Forzamos el amarre con la clave exacta BSON de tu clúster cloud
    @Field("ttoId")
    private String ttoId;

    @Field("failed_attempts")
    private Integer failedAttempts = 0;

    @Field("locked_until")
    private Date lockedUntil;

    @Field("expiration")
    private Date expiration;

    @Field("created_at")
    private Date createdAt = new Date();

    @Field("updated_at")
    private Date updatedAt = new Date();
}
