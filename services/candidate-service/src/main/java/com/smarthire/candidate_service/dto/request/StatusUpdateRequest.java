package com.smarthire.candidate_service.dto.request;

import com.smarthire.candidate_service.entity.JobApplication;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateRequest {

    @NotNull
    private JobApplication.ApplicationStatus status;

    private String recruiterNotes;
}
