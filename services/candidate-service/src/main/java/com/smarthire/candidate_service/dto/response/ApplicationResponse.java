package com.smarthire.candidate_service.dto.response;

import com.smarthire.candidate_service.entity.JobApplication;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class ApplicationResponse {
    private String id;
    private UUID candidateId;
    private UUID jobId;
    private String jobTitle;
    private String company;
    private String coverLetter;
    private String resumeUrl;
    private JobApplication.ApplicationStatus status;
    private Double rankingScore;
    private String rankingFeedback;
    private String recruiterNotes;
    private Instant appliedAt;
    private Instant updatedAt;

    public static ApplicationResponse from(JobApplication a) {
        return ApplicationResponse.builder()
                .id(a.getId())
                .candidateId(a.getCandidateId())
                .jobId(a.getJobId())
                .jobTitle(a.getJobTitle())
                .company(a.getCompany())
                .coverLetter(a.getCoverLetter())
                .resumeUrl(a.getResumeUrl())
                .status(a.getStatus())
                .rankingScore(a.getRankingScore())
                .rankingFeedback(a.getRankingFeedback())
                .recruiterNotes(a.getRecruiterNotes())
                .appliedAt(a.getAppliedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
