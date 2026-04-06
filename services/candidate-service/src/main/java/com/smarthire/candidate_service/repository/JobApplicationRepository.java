package com.smarthire.candidate_service.repository;

import com.smarthire.candidate_service.entity.JobApplication;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobApplicationRepository extends MongoRepository<JobApplication, String> {

    List<JobApplication> findByCandidateId(UUID candidateId);

    List<JobApplication> findByJobId(UUID jobId);

    Optional<JobApplication> findByCandidateIdAndJobId(UUID candidateId, UUID jobId);

    boolean existsByCandidateIdAndJobId(UUID candidateId, UUID jobId);
}
