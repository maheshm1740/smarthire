package com.smarthire.auth_service.kafka;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class UserRegisteredEvent {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String provider;
    private Instant registeredAt;
}