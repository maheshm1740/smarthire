package com.smarthire.job_service.dto.request;

import lombok.Data;

@Data
public class JobSearchRequest {
    private String keyword;
    private String category;
    private String location;
    private String experienceLevel;
    private Boolean remoteAllowed;
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdAt";
    private String sortDir = "desc";
}