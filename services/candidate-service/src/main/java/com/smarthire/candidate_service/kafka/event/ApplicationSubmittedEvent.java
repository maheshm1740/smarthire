package com.smarthire.candidate_service.kafka.event;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

// ── Published to ranking-engine and resume-parser ─────────────────────────
@Data @Builder
public class ApplicationSubmittedEvent {
    private String applicationId;
    private UUID candidateId;
    private UUID jobId;
    private String jobTitle;
    private String company;
    private String resumeUrl;
    private String coverLetter;
    private Set<String> candidateSkills;
    private Instant submittedAt;
}
