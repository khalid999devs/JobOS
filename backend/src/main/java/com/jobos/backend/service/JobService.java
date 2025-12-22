package com.jobos.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobos.backend.domain.job.*;
import com.jobos.backend.domain.user.PosterProfile;
import com.jobos.backend.domain.user.User;
import com.jobos.backend.domain.user.UserRole;
import com.jobos.backend.repository.JobPostRepository;
import com.jobos.backend.repository.PosterProfileRepository;
import com.jobos.backend.repository.SavedJobRepository;
import com.jobos.backend.repository.UserRepository;
import com.jobos.shared.dto.job.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobService {

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PosterProfileRepository posterProfileRepository;

    @Autowired
    private SavedJobRepository savedJobRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public JobPostResponse createJob(UUID posterId, UserRole userRole, JobPostRequest request) {
        if (userRole != UserRole.POSTER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: POSTER role required");
        }

        User poster = userRepository.findById(posterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!poster.getProfileCompleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile must be completed before posting jobs");
        }

        Optional<PosterProfile> posterProfileOpt = posterProfileRepository.findByUser(poster);
        if (posterProfileOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Poster profile not found");
        }

        validateJobPostRequest(request);

        JobPost jobPost = new JobPost();
        jobPost.setPoster(poster);
        jobPost.setTitle(request.getTitle());
        jobPost.setCompany(request.getCompany());
        jobPost.setLocation(request.getLocation());
        jobPost.setIsRemote(request.getIsRemote() != null ? request.getIsRemote() : false);

        try {
            jobPost.setJobType(JobType.valueOf(request.getJobType().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid job type: " + request.getJobType());
        }

        if (request.getExperienceLevel() != null) {
            try {
                jobPost.setExperienceLevel(ExperienceLevel.valueOf(request.getExperienceLevel().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid experience level: " + request.getExperienceLevel());
            }
        }

        jobPost.setSalaryMin(request.getSalaryMin());
        jobPost.setSalaryMax(request.getSalaryMax());
        jobPost.setSalaryCurrency(request.getSalaryCurrency() != null ? request.getSalaryCurrency() : "USD");

        try {
            jobPost.setSkills(objectMapper.writeValueAsString(request.getSkills()));
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process skills");
        }

        jobPost.setDescription(request.getDescription());
        jobPost.setResponsibilities(request.getResponsibilities());
        jobPost.setRequirements(request.getRequirements());
        jobPost.setBenefits(request.getBenefits());
        jobPost.setApplicationDeadline(request.getApplicationDeadline());

        JobStatus status = JobStatus.DRAFT;
        if (request.getStatus() != null) {
            try {
                status = JobStatus.valueOf(request.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + request.getStatus());
            }
        }
        jobPost.setStatus(status);

        jobPost.setViewCount(0);
        jobPost.setApplicationCount(0);

        JobPost saved = jobPostRepository.save(jobPost);
        return mapToJobPostResponse(saved, null);
    }

    @Transactional(readOnly = true)
    public Page<JobListResponse> getMyJobs(UUID posterId, UserRole userRole, String status, Pageable pageable) {
        if (userRole != UserRole.POSTER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: POSTER role required");
        }

        User poster = userRepository.findById(posterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Page<JobPost> jobPosts;
        if (status != null && !status.isEmpty()) {
            try {
                JobStatus jobStatus = JobStatus.valueOf(status.toUpperCase());
                jobPosts = jobPostRepository.findByPosterAndStatus(poster, jobStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + status);
            }
        } else {
            jobPosts = jobPostRepository.findByPoster(poster, pageable);
        }

        return jobPosts.map(this::mapToJobListResponse);
    }

    @Transactional
    public JobPostResponse getJobById(UUID jobId, UUID userId) {
        JobPost jobPost = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        jobPost.incrementViewCount();
        jobPostRepository.save(jobPost);

        Boolean isSaved = null;
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                isSaved = savedJobRepository.existsByUserAndJobPost(user, jobPost);
            }
        }

        return mapToJobPostResponse(jobPost, isSaved);
    }

    @Transactional
    public JobPostResponse updateJob(UUID jobId, UUID posterId, UserRole userRole, JobPostUpdateRequest request) {
        if (userRole != UserRole.POSTER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: POSTER role required");
        }

        JobPost jobPost = jobPostRepository.findByIdAndPoster(jobId, userRepository.findById(posterId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found or unauthorized"));

        validateJobPostUpdateRequest(request);

        if (request.getTitle() != null) jobPost.setTitle(request.getTitle());
        if (request.getCompany() != null) jobPost.setCompany(request.getCompany());
        if (request.getLocation() != null) jobPost.setLocation(request.getLocation());
        if (request.getIsRemote() != null) jobPost.setIsRemote(request.getIsRemote());

        if (request.getJobType() != null) {
            try {
                jobPost.setJobType(JobType.valueOf(request.getJobType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid job type: " + request.getJobType());
            }
        }

        if (request.getExperienceLevel() != null) {
            try {
                jobPost.setExperienceLevel(ExperienceLevel.valueOf(request.getExperienceLevel().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid experience level: " + request.getExperienceLevel());
            }
        }

        if (request.getSalaryMin() != null) jobPost.setSalaryMin(request.getSalaryMin());
        if (request.getSalaryMax() != null) jobPost.setSalaryMax(request.getSalaryMax());
        if (request.getSalaryCurrency() != null) jobPost.setSalaryCurrency(request.getSalaryCurrency());

        if (request.getSkills() != null) {
            try {
                jobPost.setSkills(objectMapper.writeValueAsString(request.getSkills()));
            } catch (JsonProcessingException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process skills");
            }
        }

        if (request.getDescription() != null) jobPost.setDescription(request.getDescription());
        if (request.getResponsibilities() != null) jobPost.setResponsibilities(request.getResponsibilities());
        if (request.getRequirements() != null) jobPost.setRequirements(request.getRequirements());
        if (request.getBenefits() != null) jobPost.setBenefits(request.getBenefits());
        if (request.getApplicationDeadline() != null) jobPost.setApplicationDeadline(request.getApplicationDeadline());

        if (request.getStatus() != null) {
            try {
                JobStatus newStatus = JobStatus.valueOf(request.getStatus().toUpperCase());
                jobPost.setStatus(newStatus);
                if (newStatus == JobStatus.CLOSED) {
                    jobPost.setClosedAt(LocalDateTime.now());
                } else if (newStatus == JobStatus.ACTIVE && jobPost.getClosedAt() != null) {
                    jobPost.setClosedAt(null);
                }
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + request.getStatus());
            }
        }

        JobPost updated = jobPostRepository.save(jobPost);
        return mapToJobPostResponse(updated, null);
    }

    @Transactional
    public void closeJob(UUID jobId, UUID posterId, UserRole userRole) {
        if (userRole != UserRole.POSTER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: POSTER role required");
        }

        JobPost jobPost = jobPostRepository.findByIdAndPoster(jobId, userRepository.findById(posterId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found or unauthorized"));

        jobPost.setStatus(JobStatus.CLOSED);
        jobPost.setClosedAt(LocalDateTime.now());
        jobPostRepository.save(jobPost);
    }

    @Transactional
    public void reopenJob(UUID jobId, UUID posterId, UserRole userRole) {
        if (userRole != UserRole.POSTER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: POSTER role required");
        }

        JobPost jobPost = jobPostRepository.findByIdAndPoster(jobId, userRepository.findById(posterId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found or unauthorized"));

        if (jobPost.getApplicationDeadline() != null && jobPost.getApplicationDeadline().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot reopen job with past deadline");
        }

        jobPost.setStatus(JobStatus.ACTIVE);
        jobPost.setClosedAt(null);
        jobPostRepository.save(jobPost);
    }

    private void validateJobPostRequest(JobPostRequest request) {
        if (request.getSalaryMin() != null && request.getSalaryMax() != null) {
            if (request.getSalaryMin() > request.getSalaryMax()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum salary cannot exceed maximum salary");
            }
        }

        if (request.getApplicationDeadline() != null && request.getApplicationDeadline().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application deadline must be in the future");
        }

        if (request.getSkills() != null && (request.getSkills().size() < 1 || request.getSkills().size() > 20)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Skills must contain 1-20 items");
        }
    }

    private void validateJobPostUpdateRequest(JobPostUpdateRequest request) {
        if (request.getSalaryMin() != null && request.getSalaryMax() != null) {
            if (request.getSalaryMin() > request.getSalaryMax()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum salary cannot exceed maximum salary");
            }
        }

        if (request.getApplicationDeadline() != null && request.getApplicationDeadline().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application deadline must be in the future");
        }

        if (request.getSkills() != null && (request.getSkills().size() < 1 || request.getSkills().size() > 20)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Skills must contain 1-20 items");
        }
    }

    private JobListResponse mapToJobListResponse(JobPost jobPost) {
        JobListResponse response = new JobListResponse();
        response.setId(jobPost.getId().toString());
        response.setTitle(jobPost.getTitle());
        response.setCompany(jobPost.getCompany());
        response.setLocation(jobPost.getLocation());
        response.setIsRemote(jobPost.getIsRemote());
        response.setJobType(jobPost.getJobType().name());
        response.setExperienceLevel(jobPost.getExperienceLevel() != null ? jobPost.getExperienceLevel().name() : null);
        response.setSalaryMin(jobPost.getSalaryMin());
        response.setSalaryMax(jobPost.getSalaryMax());
        response.setSalaryCurrency(jobPost.getSalaryCurrency());

        try {
            List<String> skills = objectMapper.readValue(jobPost.getSkills(), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            response.setSkills(skills);
        } catch (JsonProcessingException e) {
            response.setSkills(List.of());
        }

        response.setApplicationDeadline(jobPost.getApplicationDeadline());
        response.setStatus(jobPost.getStatus().name());
        response.setApplicationCount(jobPost.getApplicationCount());
        response.setCreatedAt(jobPost.getCreatedAt());
        response.setIsSaved(null);

        return response;
    }

    private JobPostResponse mapToJobPostResponse(JobPost jobPost, Boolean isSaved) {
        JobPostResponse response = new JobPostResponse();
        response.setId(jobPost.getId().toString());
        response.setPosterId(jobPost.getPoster().getId().toString());
        String posterName = jobPost.getPoster().getFirstName() + " " + jobPost.getPoster().getLastName();
        response.setPosterName(posterName);
        response.setPosterEmail(jobPost.getPoster().getEmail());
        response.setTitle(jobPost.getTitle());
        response.setCompany(jobPost.getCompany());
        response.setLocation(jobPost.getLocation());
        response.setIsRemote(jobPost.getIsRemote());
        response.setJobType(jobPost.getJobType().name());
        response.setExperienceLevel(jobPost.getExperienceLevel() != null ? jobPost.getExperienceLevel().name() : null);
        response.setSalaryMin(jobPost.getSalaryMin());
        response.setSalaryMax(jobPost.getSalaryMax());
        response.setSalaryCurrency(jobPost.getSalaryCurrency());

        try {
            List<String> skills = objectMapper.readValue(jobPost.getSkills(), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            response.setSkills(skills);
        } catch (JsonProcessingException e) {
            response.setSkills(List.of());
        }

        response.setDescription(jobPost.getDescription());
        response.setResponsibilities(jobPost.getResponsibilities());
        response.setRequirements(jobPost.getRequirements());
        response.setBenefits(jobPost.getBenefits());
        response.setApplicationDeadline(jobPost.getApplicationDeadline());
        response.setStatus(jobPost.getStatus().name());
        response.setViewCount(jobPost.getViewCount());
        response.setApplicationCount(jobPost.getApplicationCount());
        response.setCreatedAt(jobPost.getCreatedAt());
        response.setUpdatedAt(jobPost.getUpdatedAt());
        response.setClosedAt(jobPost.getClosedAt());
        response.setIsSaved(isSaved);

        return response;
    }
}
