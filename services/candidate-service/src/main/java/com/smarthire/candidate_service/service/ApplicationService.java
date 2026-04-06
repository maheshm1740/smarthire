package com.smarthire.candidate_service.service;

import com.smarthire.candidate_service.dto.request.ApplyJobRequest;
import com.smarthire.candidate_service.dto.request.StatusUpdateRequest;
import com.smarthire.candidate_service.dto.response.ApplicationResponse;
import com.smarthire.candidate_service.entity.CandidateProfile;
import com.smarthire.candidate_service.entity.JobApplication;
import com.smarthire.candidate_service.exception.ApplicationNotFoundException;
import com.smarthire.candidate_service.exception.ProfileNotFoundException;
import com.smarthire.candidate_service.kafka.event.ApplicationSubmittedEvent;
import com.smarthire.candidate_service.kafka.producer.ApplicationEventProducer;
import com.smarthire.candidate_service.repository.CandidateProfileRepository;
import com.smarthire.candidate_service.repository.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final CandidateProfileRepository profileRepository;
    private final ApplicationEventProducer eventProducer;

    // ── Apply to a job ────────────────────────────────────────────────────

    public ApplicationResponse applyToJob(UUID candidateId, ApplyJobRequest req) {
        // Check candidate has a profile
        CandidateProfile profile = profileRepository.findByUserId(candidateId)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Complete your profile before applying"));

        // Prevent duplicate applications
        if (applicationRepository.existsByCandidateIdAndJobId(candidateId, req.getJobId())) {
            throw new IllegalStateException("You have already applied for this job");
        }

        JobApplication application = JobApplication.builder()
                .candidateId(candidateId)
                .jobId(req.getJobId())
                .jobTitle(req.getJobTitle())
                .company(req.getCompany())
                .coverLetter(req.getCoverLetter())
                .resumeUrl(profile.getResumeUrl())
                .status(JobApplication.ApplicationStatus.APPLIED)
                .build();

        JobApplication saved = applicationRepository.save(application);
        log.info("Candidate {} applied to job {}", candidateId, req.getJobId());

        // Publish to Kafka so resume-parser and ranking-engine can process
        eventProducer.publishApplicationSubmitted(ApplicationSubmittedEvent.builder()
                .applicationId(saved.getId())
                .candidateId(candidateId)
                .jobId(req.getJobId())
                .jobTitle(req.getJobTitle())
                .company(req.getCompany())
                .resumeUrl(profile.getResumeUrl())
                .coverLetter(req.getCoverLetter())
                .candidateSkills(profile.getSkills())
                .submittedAt(Instant.now())
                .build());

        return ApplicationResponse.from(saved);
    }

    // ── Get my applications (candidate) ───────────────────────────────────

    public List<ApplicationResponse> getMyApplications(UUID candidateId) {
        return applicationRepository.findByCandidateId(candidateId)
                .stream().map(ApplicationResponse::from).collect(Collectors.toList());
    }

    // ── Get applications by job (recruiter/admin) ─────────────────────────

    public List<ApplicationResponse> getApplicationsByJob(UUID jobId) {
        return applicationRepository.findByJobId(jobId)
                .stream().map(ApplicationResponse::from).collect(Collectors.toList());
    }

    // ── Get single application ────────────────────────────────────────────

    public ApplicationResponse getApplicationById(String applicationId) {
        return applicationRepository.findById(applicationId)
                .map(ApplicationResponse::from)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found: " + applicationId));
    }

    // ── Update status (recruiter/admin) ───────────────────────────────────

    public ApplicationResponse updateStatus(String applicationId, StatusUpdateRequest req) {
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found: " + applicationId));

        application.setStatus(req.getStatus());
        if (req.getRecruiterNotes() != null) {
            application.setRecruiterNotes(req.getRecruiterNotes());
        }

        if (req.getStatus() == JobApplication.ApplicationStatus.UNDER_REVIEW) {
            application.setReviewedAt(Instant.now());
        } else if (req.getStatus() == JobApplication.ApplicationStatus.SHORTLISTED) {
            application.setShortlistedAt(Instant.now());
        }

        JobApplication saved = applicationRepository.save(application);
        log.info("Updated application {} status to {}", applicationId, req.getStatus());
        return ApplicationResponse.from(saved);
    }

    // ── Withdraw application (candidate) ──────────────────────────────────

    public ApplicationResponse withdrawApplication(String applicationId, UUID candidateId) {
        JobApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(
                        "Application not found: " + applicationId));

        if (!application.getCandidateId().equals(candidateId)) {
            throw new com.smarthire.candidate_service.exception.AccessDeniedException(
                    "You can only withdraw your own applications");
        }

        if (application.getStatus() == JobApplication.ApplicationStatus.WITHDRAWN) {
            throw new IllegalStateException("Application already withdrawn");
        }

        application.setStatus(JobApplication.ApplicationStatus.WITHDRAWN);
        return ApplicationResponse.from(applicationRepository.save(application));
    }
}
