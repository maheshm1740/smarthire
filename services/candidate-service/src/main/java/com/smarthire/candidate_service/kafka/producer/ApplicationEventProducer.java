package com.smarthire.candidate_service.kafka.producer;

import com.smarthire.candidate_service.kafka.event.ApplicationSubmittedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationEventProducer {

    private static final String APPLICATION_SUBMITTED_TOPIC = "application.submitted";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishApplicationSubmitted(ApplicationSubmittedEvent event) {
        kafkaTemplate.send(APPLICATION_SUBMITTED_TOPIC, event.getApplicationId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish application.submitted for {}: {}",
                                event.getApplicationId(), ex.getMessage());
                    } else {
                        log.debug("Published application.submitted for application {}",
                                event.getApplicationId());
                    }
                });
    }
}
