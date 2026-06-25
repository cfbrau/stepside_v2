package com.stepside.StepSide.notification.service.impl;

import com.stepside.StepSide.notification.dto.EmailMessageDto;
import com.stepside.StepSide.notification.model.MailDestination;
import com.stepside.StepSide.notification.model.MailTemplate;
import com.stepside.StepSide.notification.repository.MailDestinationRepository;
import com.stepside.StepSide.notification.repository.MailTemplateRepository;
import com.stepside.StepSide.notification.service.EmailService;
import com.stepside.StepSide.notification.util.TemplateProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;


/**
 * Motor de notificaciones asíncronas de alta gama para MongoDB Atlas y Google Cloud.
 * Saneado minuciosamente bajo estrictas normas de desacoplamiento de infraestructura.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final MailDestinationRepository mailDestinationRepository;
    private final MailTemplateRepository mailTemplateRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    @Async // Mantenemos el despacho asíncrono en segundo plano para liberar la API de inmediato
    public void sendEmail(EmailMessageDto messageDto) {
        log.info("Iniciando búsqueda de ruteo dinámico NoSQL para el proceso: {}", messageDto.templateName());

        try {
            // 1. O(1) EN NUBE: Buscamos la configuración de ruteo por nombre de plantilla en Atlas
            MailDestination destinationConfig = mailDestinationRepository
                    .findByTemplateName(messageDto.templateName())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No se encontró configuración de ruteo para el proceso: " + messageDto.templateName()));

            // 2. BUSQUEDA COMPLEMENTARIA INDEPENDIENTE: Recuperamos el cuerpo HTML del catálogo
            MailTemplate templateConfig = mailTemplateRepository
                    .findByName(messageDto.templateName())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "No se encontró la plantilla de diseño en el catálogo para: " + messageDto.templateName()));

            // 3. INTERPOLACIÓN DINÁMICA: Procesamos los tokens en caliente con tu modelo NoSQL
            String finalSubject = TemplateProcessor.process(templateConfig.getSubject(), messageDto.model());
            String htmlContent = TemplateProcessor.process(templateConfig.getBodyHtml(), messageDto.model());

            // 4. DESPACHO CLOUD: Enviamos a la red SMTP encriptada usando el Preparator funcional
            mailSender.send(mimeMessage -> {
                MimeMessageHelper helper = new MimeMessageHelper(
                        mimeMessage,
                        MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                        StandardCharsets.UTF_8.name()
                );

                helper.setFrom(fromEmail);
                helper.setSubject(finalSubject);
                helper.setText(htmlContent, true); // Flag true habilita renderizado HTML nativo

                // 5. ENRUTAMIENTO MULTI-DESTINATARIO ELÁSTICO (To, Cc, Bcc)
                if (destinationConfig.getToAddresses() != null && !destinationConfig.getToAddresses().isBlank()) {
                    String processedTo = TemplateProcessor.process(destinationConfig.getToAddresses(), messageDto.model());
                    String[] toArray = processedTo.split(",");
                    helper.setTo(toArray);
                }

                if (destinationConfig.getCcAddresses() != null && !destinationConfig.getCcAddresses().isBlank()) {
                    String processedCc = TemplateProcessor.process(destinationConfig.getCcAddresses(), messageDto.model());
                    String[] ccArray = processedCc.split(",");
                    helper.setCc(ccArray);
                }

                if (destinationConfig.getBccAddresses() != null && !destinationConfig.getBccAddresses().isBlank()) {
                    String processedBcc = TemplateProcessor.process(destinationConfig.getBccAddresses(), messageDto.model());
                    String[] bccArray = processedBcc.split(",");
                    helper.setBcc(bccArray);
                }
            });

            log.info("Notificación dinámica {} despachada con éxito hacia la cola cloud SMTP.", messageDto.templateName());

        } catch (Exception e) {
            log.error("Falla crítica en el procesamiento del motor de notificaciones para {}: {}",
                    messageDto.templateName(), e.getMessage());

            throw new RuntimeException("Error en el despacho de correo para la plantilla: " + messageDto.templateName(), e);
        }

    }
}
