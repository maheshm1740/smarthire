package com.smarthire.candidate_service.dto.request;

import lombok.Data;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class ProfileUpdateRequest {
    private String bio;
    private String phone;
    private String location;
    private String linkedInUrl;
    private String githubUrl;
    private String portfolioUrl;
    private Set<String> skills = new HashSet<>();
    private List<ExperienceRequest> experience = new ArrayList<>();
    private List<EducationRequest> education = new ArrayList<>();
    private boolean openToWork = true;

    @Data
    public static class ExperienceRequest {
        private String company;
        private String title;
        private String description;
        private String startDate;
        private String endDate;
        private boolean current;
    }

    @Data
    public static class EducationRequest {
        private String institution;
        private String degree;
        private String field;
        private String startDate;
        private String endDate;
        private Double gpa;
    }
}
