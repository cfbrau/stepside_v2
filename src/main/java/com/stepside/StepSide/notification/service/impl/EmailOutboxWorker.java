package com.stepside.StepSide.notification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stepside.StepSide.notification.dto.EmailMessageDto;
import com.stepside.StepSide.notification.model.EmailOutboxDocument;
import com.stepside.StepSide.notification.repository.EmailOutboxRepository;
import com.stepside.StepSide.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailOutboxWorker {

    private final EmailOutboxRepository emailOutboxRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @Scheduled(initialDelayString = "${stepside.email.outbox.initial-delay-ms:5000}", fixedDelayString = "${stepside.email.outbox.poll-ms:10000}")
    public void processPendingEmails() {
        List<EmailOutboxDocument> pendingEmails = emailOutboxRepository.findByStatus("PENDING");

        if (pendingEmails.isEmpty()) {
            return;
        }

        log.info("Se encontraron {} correos pendientes en outbox. Iniciando procesamiento.", pendingEmails.size());

        for (EmailOutboxDocument outboxDocument : pendingEmails) {
            try {
                EmailMessageDto messageDto = objectMapper.readValue(outboxDocument.getPayload(), EmailMessageDto.class);
                emailService.sendEmail(messageDto);

                outboxDocument.setStatus("SENT");
                outboxDocument.setUpdatedAt(new Date());
                emailOutboxRepository.save(outboxDocument);

                log.info("Correo outbox {} marcado como SENT.", outboxDocument.getId());
            } catch (Exception e) {
                log.error("No se pudo procesar el correo outbox {}: {}", outboxDocument.getId(), e.getMessage(), e);

                outboxDocument.setStatus("FAILED");
                outboxDocument.setUpdatedAt(new Date());
                emailOutboxRepository.save(outboxDocument);
            }
        }
    }
}
