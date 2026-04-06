package com.smarthire.job_service.controller;

import com.smarthire.job_service.dto.request.JobRequest;
import com.smarthire.job_service.dto.request.JobSearchRequest;
import com.smarthire.job_service.dto.response.JobResponse;
import com.smarthire.job_service.service.JobService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<JobResponse> createJob(
            @Valid @RequestBody JobRequest request,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobService.createJob(request, extractUserId(auth)));
    }

    @GetMapping
    public ResponseEntity<Page<JobResponse>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String experienceLevel,
            @RequestParam(required = false) Boolean remoteAllowed,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        JobSearchRequest req = new JobSearchRequest();
        req.setKeyword(keyword); req.setCategory(category);
        req.setLocation(location); req.setExperienceLevel(experienceLevel);
        req.setRemoteAllowed(remoteAllowed); req.setPage(page);
        req.setSize(size); req.setSortBy(sortBy); req.setSortDir(sortDir);
        return ResponseEntity.ok(jobService.searchJobs(req));
    }

    @GetMapping("/my")
    public ResponseEntity<List<JobResponse>> getMyJobs(Authentication auth) {
        return ResponseEntity.ok(jobService.getMyJobs(extractUserId(auth)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(@PathVariable UUID id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobResponse> updateJob(
            @PathVariable UUID id,
            @Valid @RequestBody JobRequest request,
            Authentication auth) {
        return ResponseEntity.ok(
                jobService.updateJob(id, request, extractUserId(auth), extractRole(auth)));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<JobResponse> publishJob(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(jobService.publishJob(id, extractUserId(auth), extractRole(auth)));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<JobResponse> closeJob(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(jobService.closeJob(id, extractUserId(auth), extractRole(auth)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID id, Authentication auth) {
        jobService.deleteJob(id, extractUserId(auth), extractRole(auth));
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(Authentication auth) {
        return UUID.fromString((String) auth.getPrincipal());
    }

    private String extractRole(Authentication auth) {
        return (String) ((Claims) auth.getDetails()).get("role");
    }
}