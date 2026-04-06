package com.smarthire.job_service.dto.response;

import com.smarthire.job_service.entity.Job;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data @Builder
public class JobResponse {
    private UUID id;
    private String title;
    private String description;
    private String company;
    private String location;
    private boolean remoteAllowed;
    private String category;
    private String experienceLevel;
    private Long salaryMin;
    private Long salaryMax;
    private String salaryCurrency;
    private Set<String> skills;
    private Job.JobStatus status;
    private UUID recruiterId;
    private Instant deadlineAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant publishedAt;
    private Instant closedAt;

    public static JobResponse from(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .description(job.getDescription())
                .company(job.getCompany())
                .location(job.getLocation())
                .remoteAllowed(job.isRemoteAllowed())
                .category(job.getCategory())
                .experienceLevel(job.getExperienceLevel())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .salaryCurrency(job.getSalaryCurrency())
                .skills(job.getSkills())
                .status(job.getStatus())
                .recruiterId(job.getRecruiterId())
                .deadlineAt(job.getDeadlineAt())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .publishedAt(job.getPublishedAt())
                .closedAt(job.getClosedAt())
                .build();
    }
}