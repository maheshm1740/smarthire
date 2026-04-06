package com.smarthire.candidate_service.repository;

import com.smarthire.candidate_service.entity.CandidateProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface CandidateProfileRepository extends MongoRepository<CandidateProfile, String> {

    Optional<CandidateProfile> findByUserId(UUID userId);

    Optional<CandidateProfile> findByEmail(String email);

    boolean existsByUserId(UUID userId);
}
