package com.stepside.StepSide.notification.service;

import com.stepside.StepSide.notification.dto.EmailMessageDto;

/**
 * Contrato de negocio transversal encargado de gobernar el motor de notificaciones de StepSide.
 * Define la abstracción perimetral para el despacho asíncrono sobre infraestructuras cloud.
 */
public interface EmailService {

    /**
     * Procesa y despacha un correo electrónico utilizando plantillas dinámicas BSON.
     * Mapea las variables inyectadas en el modelo y resuelve el ruteo de destinatarios en caliente.
     *
     * @param messageDto Contrato inmutable validado con los metadatos de red del correo.
     */
    void sendEmail(EmailMessageDto messageDto);
}
