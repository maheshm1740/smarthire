package com.smarthire.candidate_service.dto.response;

import com.smarthire.candidate_service.entity.CandidateProfile;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data @Builder
public class ProfileResponse {
    private String id;
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String bio;
    private String phone;
    private String location;
    private String linkedInUrl;
    private String githubUrl;
    private String portfolioUrl;
    private Set<String> skills;
    private List<CandidateProfile.Experience> experience;
    private List<CandidateProfile.Education> education;
    private String resumeUrl;
    private String resumeFileName;
    private boolean profileComplete;
    private boolean openToWork;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProfileResponse from(CandidateProfile p) {
        return ProfileResponse.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .email(p.getEmail())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .bio(p.getBio())
                .phone(p.getPhone())
                .location(p.getLocation())
                .linkedInUrl(p.getLinkedInUrl())
                .githubUrl(p.getGithubUrl())
                .portfolioUrl(p.getPortfolioUrl())
                .skills(p.getSkills())
                .experience(p.getExperience())
                .education(p.getEducation())
                .resumeUrl(p.getResumeUrl())
                .resumeFileName(p.getResumeFileName())
                .profileComplete(p.isProfileComplete())
                .openToWork(p.isOpenToWork())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
