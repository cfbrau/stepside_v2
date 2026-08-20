package com.stepside.StepSide.notification.service;

import com.stepside.StepSide.notification.dto.EmailMessageDto;

public interface EmailOutboxService {
    void enqueue(EmailMessageDto messageDto);
}
