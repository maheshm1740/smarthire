package com.smarthire.notification_service.kafka.event;

import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
public class ApplicationSubmittedEvent {
    private String applicationId;
    private UUID candidateId;
    private UUID jobId;
    private String jobTitle;
    private String company;
    private String resumeUrl;
    private String coverLetter;
    private Instant submittedAt;
}
