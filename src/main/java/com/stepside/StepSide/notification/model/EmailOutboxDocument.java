package com.stepside.StepSide.notification.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Document(collection = "email_outbox")
public class EmailOutboxDocument {

    @Id
    private String id;

    @Field("to")
    private String to;

    @Field("subject")
    private String subject;

    @Field("template_name")
    private String templateName;

    @Field("payload")
    private String payload;

    @Field("status")
    @Indexed
    private String status = "PENDING";

    @Field("created_at")
    private Date createdAt = new Date();

    @Field("updated_at")
    private Date updatedAt = new Date();

    public EmailOutboxDocument() {
    }

    public EmailOutboxDocument(String to, String subject, String templateName, String payload) {
        this.to = to;
        this.subject = subject;
        this.templateName = templateName;
        this.payload = payload;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
