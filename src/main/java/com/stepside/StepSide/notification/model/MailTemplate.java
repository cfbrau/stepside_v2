package com.stepside.StepSide.notification.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant;

/**
 * Entidad de infraestructura NoSQL que gobierna el cuerpo y diseño de las plantillas de correo.
 * Saneada con un método puente (Bridge) para sincronizar los contratos del servicio de email.
 */
@Document(collection = "mail_templates")
@Getter
@Setter
@NoArgsConstructor
public class MailTemplate {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("name")
    private String name;

    @Field("subject")
    private String subject;

    @Field("body")
    private String body;

    @Field("created_at")
    private Instant createdAt = Instant.now();

    @Field("updated_at")
    private Instant updatedAt = Instant.now();

    /**
     * MÉTODO PUENTE (Bridge): Resuelve la colisión de nomenclatura con el motor de Fabián.
     * Permite que 'EmailServiceImpl' llene la plantilla llamando a 'getBodyHtml()' sin romper la compilación.
     */
    public String getBodyHtml() {
        return this.body;
    }
}
