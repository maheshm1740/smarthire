package com.smarthire.job_service.repository;

import com.smarthire.job_service.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Page<Job> findByStatus(Job.JobStatus status, Pageable pageable);

    Page<Job> findByRecruiterId(UUID recruiterId, Pageable pageable);

    @Query("""
        SELECT j FROM Job j
        WHERE (:status IS NULL OR j.status = :status)
        AND (:category IS NULL OR LOWER(j.category) = LOWER(:category))
        AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%')))
        AND (:keyword IS NULL OR
             LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:experienceLevel IS NULL OR LOWER(j.experienceLevel) = LOWER(:experienceLevel))
        AND (:remoteAllowed IS NULL OR j.remoteAllowed = :remoteAllowed)
    """)
    Page<Job> search(
            @Param("status")          Job.JobStatus status,
            @Param("category")        String category,
            @Param("location")        String location,
            @Param("keyword")         String keyword,
            @Param("experienceLevel") String experienceLevel,
            @Param("remoteAllowed")   Boolean remoteAllowed,
            Pageable pageable
    );

    List<Job> findByRecruiterId(UUID recruiterId);
}