package com.smarthire.candidate_service.controller;

import com.smarthire.candidate_service.dto.request.ApplyJobRequest;
import com.smarthire.candidate_service.dto.request.StatusUpdateRequest;
import com.smarthire.candidate_service.dto.response.ApplicationResponse;
import com.smarthire.candidate_service.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    // ── POST /api/applications — candidate applies to a job ───────────────
    @PostMapping
    public ResponseEntity<ApplicationResponse> apply(
            @Valid @RequestBody ApplyJobRequest request,
            Authentication auth) {
        UUID candidateId = extractUserId(auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.applyToJob(candidateId, request));
    }

    // ── GET /api/applications/my — candidate's own applications ──────────
    @GetMapping("/my")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications(Authentication auth) {
        return ResponseEntity.ok(applicationService.getMyApplications(extractUserId(auth)));
    }

    // ── GET /api/applications/job/{jobId} — recruiter views applicants ────
    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<ApplicationResponse>> getByJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(applicationService.getApplicationsByJob(jobId));
    }

    // ── GET /api/applications/{id} ────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(applicationService.getApplicationById(id));
    }

    // ── PATCH /api/applications/{id}/status — recruiter updates status ────
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApplicationResponse> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(applicationService.updateStatus(id, request));
    }

    // ── PATCH /api/applications/{id}/withdraw — candidate withdraws ───────
    @PatchMapping("/{id}/withdraw")
    public ResponseEntity<ApplicationResponse> withdraw(
            @PathVariable String id,
            Authentication auth) {
        return ResponseEntity.ok(
                applicationService.withdrawApplication(id, extractUserId(auth)));
    }

    private UUID extractUserId(Authentication auth) {
        return UUID.fromString((String) auth.getPrincipal());
    }
}
