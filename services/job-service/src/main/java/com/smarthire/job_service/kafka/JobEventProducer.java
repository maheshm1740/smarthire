package com.smarthire.job_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobEventProducer {

    private static final String JOB_CREATED_TOPIC = "job.created";
    private static final String JOB_UPDATED_TOPIC = "job.updated";
    private static final String JOB_CLOSED_TOPIC  = "job.closed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishJobCreated(JobEvent event) { publish(JOB_CREATED_TOPIC, event); }
    public void publishJobUpdated(JobEvent event) { publish(JOB_UPDATED_TOPIC, event); }
    public void publishJobClosed(JobEvent event)  { publish(JOB_CLOSED_TOPIC,  event); }

    private void publish(String topic, JobEvent event) {
        kafkaTemplate.send(topic, event.getJobId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} for job {}: {}", topic, event.getJobId(), ex.getMessage());
                    } else {
                        log.debug("Published {} for job {} offset={}", topic, event.getJobId(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}