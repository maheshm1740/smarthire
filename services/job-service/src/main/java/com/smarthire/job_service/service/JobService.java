package com.smarthire.job_service.service;

import com.smarthire.job_service.dto.request.JobRequest;
import com.smarthire.job_service.dto.request.JobSearchRequest;
import com.smarthire.job_service.dto.response.JobResponse;
import com.smarthire.job_service.entity.Job;
import com.smarthire.job_service.exception.AccessDeniedException;
import com.smarthire.job_service.exception.JobNotFoundException;
import com.smarthire.job_service.kafka.JobEvent;
import com.smarthire.job_service.kafka.JobEventProducer;
import com.smarthire.job_service.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobEventProducer eventProducer;

    @Transactional
    public JobResponse createJob(JobRequest req, UUID recruiterId) {
        Job job = Job.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .company(req.getCompany())
                .location(req.getLocation())
                .remoteAllowed(req.isRemoteAllowed())
                .category(req.getCategory())
                .experienceLevel(req.getExperienceLevel())
                .salaryMin(req.getSalaryMin())
                .salaryMax(req.getSalaryMax())
                .salaryCurrency(req.getSalaryCurrency() != null ? req.getSalaryCurrency() : "USD")
                .skills(req.getSkills())
                .status(Job.JobStatus.DRAFT)
                .recruiterId(recruiterId)
                .deadlineAt(req.getDeadlineAt())
                .build();

        Job saved = jobRepository.save(job);
        log.info("Created job {} by recruiter {}", saved.getId(), recruiterId);
        return JobResponse.from(saved);
    }

    public JobResponse getJobById(UUID id) {
        return JobResponse.from(findJobOrThrow(id));
    }

    public Page<JobResponse> searchJobs(JobSearchRequest req) {
        Sort sort = req.getSortDir().equalsIgnoreCase("asc")
                ? Sort.by(req.getSortBy()).ascending()
                : Sort.by(req.getSortBy()).descending();
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), sort);
        return jobRepository.search(
                Job.JobStatus.PUBLISHED,
                req.getCategory(), req.getLocation(), req.getKeyword(),
                req.getExperienceLevel(), req.getRemoteAllowed(), pageable
        ).map(JobResponse::from);
    }

    public List<JobResponse> getMyJobs(UUID recruiterId) {
        return jobRepository.findByRecruiterId(recruiterId)
                .stream().map(JobResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public JobResponse updateJob(UUID id, JobRequest req, UUID requesterId, String role) {
        Job job = findJobOrThrow(id);
        assertCanModify(job, requesterId, role);
        if (job.getStatus() == Job.JobStatus.CLOSED) {
            throw new IllegalStateException("Cannot update a closed job");
        }
        job.setTitle(req.getTitle());
        job.setDescription(req.getDescription());
        job.setCompany(req.getCompany());
        job.setLocation(req.getLocation());
        job.setRemoteAllowed(req.isRemoteAllowed());
        job.setCategory(req.getCategory());
        job.setExperienceLevel(req.getExperienceLevel());
        job.setSalaryMin(req.getSalaryMin());
        job.setSalaryMax(req.getSalaryMax());
        if (req.getSalaryCurrency() != null) job.setSalaryCurrency(req.getSalaryCurrency());
        job.setSkills(req.getSkills());
        job.setDeadlineAt(req.getDeadlineAt());
        Job saved = jobRepository.save(job);
        if (saved.getStatus() == Job.JobStatus.PUBLISHED) {
            eventProducer.publishJobUpdated(toEvent(saved, "JOB_UPDATED"));
        }
        return JobResponse.from(saved);
    }

    @Transactional
    public JobResponse publishJob(UUID id, UUID requesterId, String role) {
        Job job = findJobOrThrow(id);
        assertCanModify(job, requesterId, role);
        if (job.getStatus() != Job.JobStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT jobs can be published");
        }
        job.setStatus(Job.JobStatus.PUBLISHED);
        job.setPublishedAt(Instant.now());
        Job saved = jobRepository.save(job);
        eventProducer.publishJobCreated(toEvent(saved, "JOB_CREATED"));
        return JobResponse.from(saved);
    }

    @Transactional
    public JobResponse closeJob(UUID id, UUID requesterId, String role) {
        Job job = findJobOrThrow(id);
        assertCanModify(job, requesterId, role);
        if (job.getStatus() == Job.JobStatus.CLOSED) {
            throw new IllegalStateException("Job is already closed");
        }
        job.setStatus(Job.JobStatus.CLOSED);
        job.setClosedAt(Instant.now());
        Job saved = jobRepository.save(job);
        eventProducer.publishJobClosed(toEvent(saved, "JOB_CLOSED"));
        return JobResponse.from(saved);
    }

    @Transactional
    public void deleteJob(UUID id, UUID requesterId, String role) {
        Job job = findJobOrThrow(id);
        assertCanModify(job, requesterId, role);
        if (job.getStatus() == Job.JobStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot delete a published job. Close it first.");
        }
        jobRepository.delete(job);
    }

    private Job findJobOrThrow(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException("Job not found: " + id));
    }

    private void assertCanModify(Job job, UUID requesterId, String role) {
        if (!"ADMIN".equals(role) && !job.getRecruiterId().equals(requesterId)) {
            throw new AccessDeniedException("You don't have permission to modify this job");
        }
    }

    private JobEvent toEvent(Job job, String eventType) {
        return JobEvent.builder()
                .eventType(eventType)
                .jobId(job.getId())
                .title(job.getTitle())
                .company(job.getCompany())
                .location(job.getLocation())
                .category(job.getCategory())
                .experienceLevel(job.getExperienceLevel())
                .skills(job.getSkills())
                .recruiterId(job.getRecruiterId())
                .status(job.getStatus().name())
                .occurredAt(Instant.now())
                .build();
    }
}