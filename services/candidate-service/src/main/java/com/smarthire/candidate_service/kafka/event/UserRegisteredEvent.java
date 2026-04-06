package com.smarthire.candidate_service.kafka.event;

import lombok.Data;
import java.time.Instant;
import java.util.UUID;

// ── Consumed from auth-service ────────────────────────────────────────────
@Data
public class UserRegisteredEvent {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String provider;
    private Instant registeredAt;
}
