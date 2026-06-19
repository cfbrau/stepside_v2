package com.stepside.StepSide.users.model; // <-- SANEADO: Su paquete definitivo

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.Date;

/**
 * Entidad de seguridad central adaptada para MongoDB Atlas.
 * Acomodada estrictamente bajo la feature de 'users/model'.
 */
@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserCorrect {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("email")
    private String email;

    @Field("password")
    private String password;

    @Indexed
    @Field("status_name")
    private String statusName = "PENDING_APPROVAL";

    @Indexed
    @Field("tto_id")
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
