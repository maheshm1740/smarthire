package com.smarthire.notification_service.kafka.event;

import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
public class UserRegisteredEvent {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private Instant registeredAt;
}
