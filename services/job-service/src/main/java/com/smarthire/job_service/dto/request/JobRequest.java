package com.smarthire.job_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
public class JobRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Company is required")
    private String company;

    @NotBlank(message = "Location is required")
    private String location;

    private boolean remoteAllowed = false;
    private String category;
    private String experienceLevel;
    private Long salaryMin;
    private Long salaryMax;
    private String salaryCurrency = "USD";
    private Set<String> skills = new HashSet<>();
    private Instant deadlineAt;
}