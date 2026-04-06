package com.smarthire.job_service.kafka;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data @Builder
public class JobEvent {
    private String eventType;
    private UUID jobId;
    private String title;
    private String company;
    private String location;
    private String category;
    private String experienceLevel;
    private Set<String> skills;
    private UUID recruiterId;
    private String status;
    private Instant occurredAt;
}