package com.smarthire.notification_service.kafka.consumer;

import com.smarthire.notification_service.kafka.event.*;
import com.smarthire.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a z").withZone(ZoneId.of("Asia/Kolkata"));

    // ── user.registered → welcome email ──────────────────────────────────

    @KafkaListener(topics = "user.registered", groupId = "notification-service")
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Processing user.registered for {}", event.getEmail());
        try {
            notificationService.sendWelcome(
                    event.getUserId(),
                    event.getEmail(),
                    event.getFirstName()
            );
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", event.getEmail(), e.getMessage());
        }
    }

    // ── application.submitted → confirmation email ────────────────────────

    @KafkaListener(topics = "application.submitted", groupId = "notification-service")
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        log.info("Processing application.submitted for application {}", event.getApplicationId());
        try {
            // Note: in production, fetch candidate name from candidate-service
            // For now use candidateId as a reference
            notificationService.sendApplicationConfirmation(
                    event.getCandidateId(),
                    null, // email resolved via candidate-service lookup in production
                    "Candidate",
                    event.getJobTitle(),
                    event.getCompany(),
                    event.getApplicationId()
            );
        } catch (Exception e) {
            log.error("Failed to send application confirmation: {}", e.getMessage());
        }
    }

    // ── interview.scheduled → calendar invite email ───────────────────────

    @KafkaListener(topics = "interview.scheduled", groupId = "notification-service")
    public void onInterviewScheduled(InterviewScheduledEvent event) {
        log.info("Processing interview.scheduled for interview {}", event.getInterviewId());
        try {
            String formattedDate = event.getScheduledAt() != null
                    ? FORMATTER.format(event.getScheduledAt()) : "TBD";

            notificationService.sendInterviewScheduled(
                    event.getCandidateId(),
                    event.getCandidateEmail(),
                    event.getCandidateName(),
                    event.getJobTitle(),
                    event.getCompany(),
                    formattedDate,
                    event.getDurationMinutes(),
                    event.getMeetingLink(),
                    event.getLocation(),
                    event.getInterviewId()
            );
        } catch (Exception e) {
            log.error("Failed to send interview notification: {}", e.getMessage());
        }
    }

    // ── notification.send → generic trigger ──────────────────────────────

    @KafkaListener(topics = "notification.send", groupId = "notification-service")
    public void onNotificationSend(NotificationSendEvent event) {
        log.info("Processing notification.send type={} userId={}", event.getType(), event.getUserId());
        try {
            notificationService.sendGeneric(
                    event.getUserId(),
                    event.getEmail(),
                    event.getType(),
                    event.getTitle(),
                    event.getMessage(),
                    event.getReferenceId(),
                    event.getReferenceType()
            );
        } catch (Exception e) {
            log.error("Failed to send generic notification: {}", e.getMessage());
        }
    }
}
