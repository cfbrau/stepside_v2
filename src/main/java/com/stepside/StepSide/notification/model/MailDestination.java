package com.stepside.StepSide.notification.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import java.time.Instant; // SINCRO: API moderna de tiempo tolerante a strings ISO

/**
 * Entidad de infraestructura NoSQL que gobierna las reglas de ruteo y destinatarios de mensajería.
 * Saneada por el Arquitecto utilizando tipos de tiempo elásticos para eludir fallos de conversión de Atlas.
 */
@Document(collection = "mail_destinations")
@Getter
@Setter
@NoArgsConstructor
public class MailDestination {

    @Id
    private String id;

    @Indexed(unique = true)
    @Field("template_name")
    private String templateName;

    @Field("to_addresses")
    private String toAddresses;

    @Field("cc_addresses")
    private String ccAddresses;

    @Field("bcc_addresses")
    private String bccAddresses;

    @Field("created_at")
    private Instant createdAt = Instant.now(); // Inicialización elástica adaptada

    @Field("updated_at")
    private Instant updatedAt = Instant.now(); // Inicialización elástica adaptada
}
