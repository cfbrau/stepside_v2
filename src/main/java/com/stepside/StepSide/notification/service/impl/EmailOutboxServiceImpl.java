package com.stepside.StepSide.notification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stepside.StepSide.notification.dto.EmailMessageDto;
import com.stepside.StepSide.notification.model.EmailOutboxDocument;
import com.stepside.StepSide.notification.repository.EmailOutboxRepository;
import com.stepside.StepSide.notification.service.EmailOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailOutboxServiceImpl implements EmailOutboxService {

    private final EmailOutboxRepository emailOutboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void enqueue(EmailMessageDto messageDto) {
        try {
            String payload = objectMapper.writeValueAsString(messageDto);
            EmailOutboxDocument outboxDocument = new EmailOutboxDocument(
                    messageDto.to(),
                    messageDto.subject(),
                    messageDto.templateName(),
                    payload
            );

            emailOutboxRepository.save(outboxDocument);
            log.info("Correo pendiente en outbox: {}", messageDto.templateName());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo registrar el correo pendiente en outbox.", e);
        }
    }
}
