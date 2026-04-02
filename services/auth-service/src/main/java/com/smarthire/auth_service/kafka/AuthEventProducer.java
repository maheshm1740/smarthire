package com.smarthire.auth_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventProducer {

    private static final String USER_REGISTERED_TOPIC = "user.registered";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserRegistered(UserRegisteredEvent event) {
        kafkaTemplate.send(USER_REGISTERED_TOPIC, event.getUserId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish user.registered for {}: {}", event.getEmail(), ex.getMessage());
                    } else {
                        log.debug("Published user.registered for {} offset={}", event.getEmail(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}