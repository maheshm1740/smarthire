package com.smarthire.candidate_service.kafka.event;

import lombok.Data;

// ── Consumed from ranking-engine ──────────────────────────────────────────
@Data
public class CandidateRankedEvent {
    private String applicationId;
    private Double score;
    private String feedback;
    private String status; // SHORTLISTED or REJECTED
}
