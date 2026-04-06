package com.smarthire.notification_service.service;

import com.smarthire.notification_service.entity.Notification;
import com.smarthire.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final EmailService emailService;
    private final PushNotificationService pushService;
    private final EmailTemplateService templateService;

    // ── Send and persist ──────────────────────────────────────────────────

    public void sendAndSave(UUID userId, String email, String type,
                            String title, String message, String htmlBody,
                            String referenceId, String referenceType) {
        Notification notification = Notification.builder()
                .userId(userId)
                .email(email)
                .type(type)
                .title(title)
                .message(message)
                .channel("BOTH")
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();

        // Send email
        try {
            boolean emailSent = emailService.sendEmail(email, null, title, htmlBody);
            notification.setEmailSent(emailSent);
        } catch (Exception e) {
            log.error("Email failed for user {}: {}", userId, e.getMessage());
            notification.setErrorMessage("Email: " + e.getMessage());
        }

        // Send push
        try {
            boolean pushSent = pushService.sendPush(userId, title, message, referenceId);
            notification.setPushSent(pushSent);
        } catch (Exception e) {
            log.error("Push failed for user {}: {}", userId, e.getMessage());
        }

        notification.setSentAt(Instant.now());
        repository.save(notification);
        log.info("Notification saved — type={} userId={} email={}", type, userId, notification.isEmailSent());
    }

    // ── Specific notification senders ─────────────────────────────────────

    public void sendWelcome(UUID userId, String email, String firstName) {
        String html = templateService.welcome(firstName);
        sendAndSave(userId, email, "WELCOME",
            "Welcome to SmartHire, " + firstName + "!",
            "Welcome to SmartHire! Your account is ready.",
            html, null, null);
    }

    public void sendApplicationConfirmation(UUID userId, String email, String firstName,
                                             String jobTitle, String company, String applicationId) {
        String html = templateService.applicationSubmitted(firstName, jobTitle, company);
        sendAndSave(userId, email, "APPLICATION_SUBMITTED",
            "Application submitted — " + jobTitle,
            "Your application for " + jobTitle + " at " + company + " was submitted.",
            html, applicationId, "APPLICATION");
    }

    public void sendStatusChanged(UUID userId, String email, String firstName,
                                   String jobTitle, String company,
                                   String status, String applicationId) {
        String html = templateService.statusChanged(firstName, jobTitle, company, status);
        sendAndSave(userId, email, "APPLICATION_STATUS_CHANGED",
            "Application update — " + jobTitle,
            "Your application for " + jobTitle + " is now " + status,
            html, applicationId, "APPLICATION");
    }

    public void sendInterviewScheduled(UUID userId, String email, String firstName,
                                        String jobTitle, String company, String scheduledAt,
                                        Integer duration, String meetingLink,
                                        String location, String interviewId) {
        String html = templateService.interviewScheduled(firstName, jobTitle, company,
                scheduledAt, duration, meetingLink, location);
        sendAndSave(userId, email, "INTERVIEW_SCHEDULED",
            "Interview scheduled — " + jobTitle,
            "Your interview for " + jobTitle + " at " + company + " is confirmed.",
            html, interviewId, "INTERVIEW");
    }

    public void sendJobPosted(UUID userId, String email, String firstName,
                               String jobTitle, String company,
                               String location, String jobId) {
        String html = templateService.jobPosted(firstName, jobTitle, company, location, jobId);
        sendAndSave(userId, email, "JOB_POSTED",
            "New job match — " + jobTitle,
            "A new job matching your skills: " + jobTitle + " at " + company,
            html, jobId, "JOB");
    }

    public void sendGeneric(UUID userId, String email, String type,
                             String title, String message,
                             String referenceId, String referenceType) {
        sendAndSave(userId, email, type, title, message,
            "<p>" + message + "</p>", referenceId, referenceType);
    }

    // ── Read operations ───────────────────────────────────────────────────

    public List<Notification> getNotifications(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnread(UUID userId) {
        return repository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    public long countUnread(UUID userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    public void markAsRead(String notificationId) {
        repository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            repository.save(n);
        });
    }

    public void markAllAsRead(UUID userId) {
        List<Notification> unread = repository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setRead(true));
        repository.saveAll(unread);
    }
}
