package com.smarthire.candidate_service.kafka.consumer;

import com.smarthire.candidate_service.entity.CandidateProfile;
import com.smarthire.candidate_service.entity.JobApplication;
import com.smarthire.candidate_service.kafka.event.CandidateRankedEvent;
import com.smarthire.candidate_service.kafka.event.UserRegisteredEvent;
import com.smarthire.candidate_service.repository.CandidateProfileRepository;
import com.smarthire.candidate_service.repository.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CandidateEventConsumer {

    private final CandidateProfileRepository profileRepository;
    private final JobApplicationRepository applicationRepository;

    // ── Auto-create profile when user registers ───────────────────────────

    @KafkaListener(topics = "user.registered", groupId = "candidate-service")
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Received user.registered for {} ({})", event.getEmail(), event.getRole());

        // Only create profiles for CANDIDATE role
        if (!"CANDIDATE".equals(event.getRole())) return;

        if (profileRepository.existsByUserId(event.getUserId())) {
            log.debug("Profile already exists for user {}", event.getUserId());
            return;
        }

        CandidateProfile profile = CandidateProfile.builder()
                .userId(event.getUserId())
                .email(event.getEmail())
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .build();

        profileRepository.save(profile);
        log.info("Auto-created candidate profile for {}", event.getEmail());
    }

    // ── Update application when ranking-engine finishes ───────────────────

    @KafkaListener(topics = "candidate.ranked", groupId = "candidate-service")
    public void onCandidateRanked(CandidateRankedEvent event) {
        log.info("Received candidate.ranked for application {}", event.getApplicationId());

        applicationRepository.findById(event.getApplicationId()).ifPresent(application -> {
            application.setRankingScore(event.getScore());
            application.setRankingFeedback(event.getFeedback());

            if ("SHORTLISTED".equals(event.getStatus())) {
                application.setStatus(JobApplication.ApplicationStatus.SHORTLISTED);
            } else if ("REJECTED".equals(event.getStatus())) {
                application.setStatus(JobApplication.ApplicationStatus.REJECTED);
            }

            applicationRepository.save(application);
            log.info("Updated application {} status to {}", event.getApplicationId(), event.getStatus());
        });
    }
}
