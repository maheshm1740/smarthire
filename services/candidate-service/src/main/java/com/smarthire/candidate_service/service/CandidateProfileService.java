package com.smarthire.candidate_service.service;

import com.smarthire.candidate_service.dto.request.ProfileUpdateRequest;
import com.smarthire.candidate_service.dto.response.ProfileResponse;
import com.smarthire.candidate_service.entity.CandidateProfile;
import com.smarthire.candidate_service.exception.AccessDeniedException;
import com.smarthire.candidate_service.exception.ProfileNotFoundException;
import com.smarthire.candidate_service.repository.CandidateProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateProfileService {

    private final CandidateProfileRepository profileRepository;
    private final ResumeService resumeService;

    // ── Get my profile ────────────────────────────────────────────────────

    public ProfileResponse getMyProfile(UUID userId) {
        CandidateProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + userId));
        return ProfileResponse.from(profile);
    }

    // ── Get profile by ID (recruiter/admin) ───────────────────────────────

    public ProfileResponse getProfileById(String profileId) {
        CandidateProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found: " + profileId));
        return ProfileResponse.from(profile);
    }

    // ── Get all profiles (recruiter/admin) ────────────────────────────────

    public List<ProfileResponse> getAllProfiles() {
        return profileRepository.findAll()
                .stream()
                .map(ProfileResponse::from)
                .collect(Collectors.toList());
    }

    // ── Update profile ────────────────────────────────────────────────────

    public ProfileResponse updateProfile(UUID userId, ProfileUpdateRequest req) {
        CandidateProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + userId));

        profile.setBio(req.getBio());
        profile.setPhone(req.getPhone());
        profile.setLocation(req.getLocation());
        profile.setLinkedInUrl(req.getLinkedInUrl());
        profile.setGithubUrl(req.getGithubUrl());
        profile.setPortfolioUrl(req.getPortfolioUrl());
        profile.setSkills(req.getSkills());
        profile.setOpenToWork(req.isOpenToWork());

        if (req.getExperience() != null) {
            profile.setExperience(req.getExperience().stream()
                    .map(e -> CandidateProfile.Experience.builder()
                            .company(e.getCompany()).title(e.getTitle())
                            .description(e.getDescription()).startDate(e.getStartDate())
                            .endDate(e.getEndDate()).current(e.isCurrent())
                            .build())
                    .collect(Collectors.toList()));
        }

        if (req.getEducation() != null) {
            profile.setEducation(req.getEducation().stream()
                    .map(e -> CandidateProfile.Education.builder()
                            .institution(e.getInstitution()).degree(e.getDegree())
                            .field(e.getField()).startDate(e.getStartDate())
                            .endDate(e.getEndDate()).gpa(e.getGpa())
                            .build())
                    .collect(Collectors.toList()));
        }

        profile.setProfileComplete(isProfileComplete(profile));
        CandidateProfile saved = profileRepository.save(profile);
        log.info("Updated profile for user {}", userId);
        return ProfileResponse.from(saved);
    }

    // ── Upload resume ─────────────────────────────────────────────────────

    public ProfileResponse uploadResume(UUID userId, MultipartFile file) {
        CandidateProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + userId));

        // Delete old resume if exists
        if (profile.getResumeUrl() != null) {
            resumeService.deleteResume(profile.getResumeUrl());
        }

        String objectName = resumeService.uploadResume(file, userId);
        profile.setResumeUrl(objectName);
        profile.setResumeFileName(file.getOriginalFilename());
        profile.setProfileComplete(isProfileComplete(profile));

        CandidateProfile saved = profileRepository.save(profile);
        log.info("Resume uploaded for user {}: {}", userId, objectName);
        return ProfileResponse.from(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private boolean isProfileComplete(CandidateProfile p) {
        return p.getBio() != null && !p.getBio().isBlank()
                && p.getSkills() != null && !p.getSkills().isEmpty()
                && p.getResumeUrl() != null;
    }

    public void assertOwnership(UUID profileUserId, UUID requesterId, String role) {
        if (!"ADMIN".equals(role) && !profileUserId.equals(requesterId)) {
            throw new AccessDeniedException("You can only modify your own profile");
        }
    }
}
