package com.smarthire.candidate_service.controller;

import com.smarthire.candidate_service.dto.request.ProfileUpdateRequest;
import com.smarthire.candidate_service.dto.response.ProfileResponse;
import com.smarthire.candidate_service.service.CandidateProfileService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
public class CandidateProfileController {

    private final CandidateProfileService profileService;

    // ── GET /api/candidates/me ────────────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(Authentication auth) {
        return ResponseEntity.ok(profileService.getMyProfile(extractUserId(auth)));
    }

    // ── PUT /api/candidates/me ────────────────────────────────────────────
    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateMyProfile(
            @RequestBody ProfileUpdateRequest request,
            Authentication auth) {
        return ResponseEntity.ok(profileService.updateProfile(extractUserId(auth), request));
    }

    // ── POST /api/candidates/me/resume ────────────────────────────────────
    @PostMapping("/me/resume")
    public ResponseEntity<ProfileResponse> uploadResume(
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        return ResponseEntity.ok(profileService.uploadResume(extractUserId(auth), file));
    }

    // ── GET /api/candidates — RECRUITER/ADMIN only ────────────────────────
    @GetMapping
    public ResponseEntity<List<ProfileResponse>> getAllProfiles() {
        return ResponseEntity.ok(profileService.getAllProfiles());
    }

    // ── GET /api/candidates/{id} — RECRUITER/ADMIN only ──────────────────
    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponse> getProfileById(@PathVariable String id) {
        return ResponseEntity.ok(profileService.getProfileById(id));
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private UUID extractUserId(Authentication auth) {
        return UUID.fromString((String) auth.getPrincipal());
    }

    private String extractRole(Authentication auth) {
        return (String) ((Claims) auth.getDetails()).get("role");
    }
}
