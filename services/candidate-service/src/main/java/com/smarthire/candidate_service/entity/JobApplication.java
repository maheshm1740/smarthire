package com.smarthire.candidate_service.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "job_applications")
@CompoundIndexes({
        @CompoundIndex(name = "idx_candidate_job", def = "{'candidateId': 1, 'jobId': 1}", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobApplication {

    @Id
    private String id;

    @Indexed
    private UUID candidateId;

    @Indexed
    private UUID jobId;

    private String jobTitle;
    private String company;

    private String coverLetter;
    private String resumeUrl;

    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    private Double rankingScore;
    private String rankingFeedback;

    private String recruiterNotes;

    @CreatedDate
    private Instant appliedAt;

    @LastModifiedDate
    private Instant updatedAt;

    private Instant reviewedAt;
    private Instant shortlistedAt;

    public enum ApplicationStatus {
        APPLIED, UNDER_REVIEW, SHORTLISTED, REJECTED, WITHDRAWN
    }
}
