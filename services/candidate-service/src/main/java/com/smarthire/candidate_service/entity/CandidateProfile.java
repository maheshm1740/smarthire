package com.smarthire.candidate_service.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Document(collection = "candidate_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CandidateProfile {

    @Id
    private String id;

    @Indexed(unique = true)
    private UUID userId;

    @Indexed(unique = true)
    private String email;

    private String firstName;
    private String lastName;
    private String bio;
    private String phone;
    private String location;
    private String linkedInUrl;
    private String githubUrl;
    private String portfolioUrl;

    @Builder.Default
    private Set<String> skills = new HashSet<>();

    @Builder.Default
    private List<Experience> experience = new ArrayList<>();

    @Builder.Default
    private List<Education> education = new ArrayList<>();

    private String resumeUrl;
    private String resumeFileName;

    @Builder.Default
    private boolean profileComplete = false;

    @Builder.Default
    private boolean openToWork = true;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    // ── Embedded documents ────────────────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Experience {
        private String company;
        private String title;
        private String description;
        private String startDate;
        private String endDate;
        private boolean current;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Education {
        private String institution;
        private String degree;
        private String field;
        private String startDate;
        private String endDate;
        private Double gpa;
    }
}
