package com.smarthire.candidate_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class ApplyJobRequest {

    @NotNull(message = "Job ID is required")
    private UUID jobId;

    private String jobTitle;
    private String company;
    private String coverLetter;
}
