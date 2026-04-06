package com.smarthire.notification_service.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    private String id;

    @Indexed
    private UUID userId;

    private String email;
    private String type;        // WELCOME, APPLICATION_SUBMITTED, STATUS_CHANGED, INTERVIEW_SCHEDULED, JOB_POSTED
    private String title;
    private String message;
    private String channel;     // EMAIL, PUSH, BOTH

    @Builder.Default
    private boolean emailSent = false;

    @Builder.Default
    private boolean pushSent = false;

    @Builder.Default
    private boolean read = false;

    private String referenceId;  // jobId, applicationId, interviewId etc.
    private String referenceType; // JOB, APPLICATION, INTERVIEW

    private String errorMessage;

    @CreatedDate
    private Instant createdAt;

    private Instant sentAt;

    public enum NotificationType {
        WELCOME,
        APPLICATION_SUBMITTED,
        APPLICATION_STATUS_CHANGED,
        INTERVIEW_SCHEDULED,
        JOB_POSTED
    }
}
