package com.smarthire.notification_service.kafka.event;

import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
public class InterviewScheduledEvent {
    private String interviewId;
    private UUID candidateId;
    private UUID jobId;
    private String jobTitle;
    private String company;
    private String candidateEmail;
    private String candidateName;
    private String interviewerEmail;
    private Instant scheduledAt;
    private Integer durationMinutes;
    private String meetingLink;
    private String location;
    private String notes;
}
