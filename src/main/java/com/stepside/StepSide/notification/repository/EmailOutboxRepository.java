package com.stepside.StepSide.notification.repository;

import com.stepside.StepSide.notification.model.EmailOutboxDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EmailOutboxRepository extends MongoRepository<EmailOutboxDocument, String> {
    List<EmailOutboxDocument> findByStatus(String status);
}
