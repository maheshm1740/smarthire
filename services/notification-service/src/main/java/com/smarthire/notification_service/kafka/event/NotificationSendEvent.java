package com.smarthire.notification_service.kafka.event;

import lombok.Data;
import java.util.Map;
import java.util.UUID;

@Data
public class NotificationSendEvent {
    private UUID userId;
    private String email;
    private String type;
    private String title;
    private String message;
    private String channel;        // EMAIL, PUSH, BOTH
    private String referenceId;
    private String referenceType;
    private Map<String, String> metadata;
}
