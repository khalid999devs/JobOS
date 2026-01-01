package com.jobos.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobos.backend.domain.job.*;
import com.jobos.backend.domain.user.User;
import com.jobos.backend.repository.JobPostRepository;
import com.jobos.backend.repository.SavedJobRepository;
import com.jobos.backend.repository.UserRepository;
import com.jobos.shared.dto.job.*;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JobSearchService {

    private static final Logger logger = LoggerFactory.getLogger(JobSearchService.class);

    private final JobPostRepository jobPostRepository;
    private final SavedJobRepository savedJobRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public JobSearchService(JobPostRepository jobPostRepository,
                           SavedJobRepository savedJobRepository,
                           UserRepository userRepository,
                           ObjectMapper objectMapper) {
        this.jobPostRepository = jobPostRepository;
        this.savedJobRepository = savedJobRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public JobSearchResponse searchJobs(JobSearchRequest searchRequest, UUID userId) {
        Specification<JobPost> spec = buildJobSearchSpecification(searchRequest);

        Sort sort = buildSort(searchRequest);
        Pageable pageable = PageRequest.of(
                searchRequest.getPage(),
                Math.min(searchRequest.getSize(), 100),
                sort
        );

        Page<JobPost> jobPage = jobPostRepository.findAll(spec, pageable);

        final User finalUser;
        if (userId != null) {
            finalUser = userRepository.findById(userId).orElse(null);
        } else {
            finalUser = null;
        }

        List<JobListResponse> jobListResponses = jobPage.getContent().stream()
                .map(job -> mapToJobListResponse(job, finalUser))
                .collect(Collectors.toList());

        JobSearchResponse response = new JobSearchResponse();
        response.setJobs(jobListResponses);
        response.setCurrentPage(jobPage.getNumber());
        response.setTotalPages(jobPage.getTotalPages());
        response.setTotalElements(jobPage.getTotalElements());
        response.setPageSize(jobPage.getSize());
        response.setHasNext(jobPage.hasNext());
        response.setHasPrevious(jobPage.hasPrevious());

        return response;
    }

    @Transactional
    public void saveJob(UUID userId, UUID jobId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        JobPost jobPost = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        // Check if already saved - make it idempotent
        if (savedJobRepository.existsByUserAndJobPost(user, jobPost)) {
            return; // Already saved, no action needed
        }

        SavedJob savedJob = new SavedJob();
        savedJob.setUser(user);
        savedJob.setJobPost(jobPost);
        savedJobRepository.save(savedJob);
    }

    @Transactional
    public void unsaveJob(UUID userId, UUID jobId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        JobPost jobPost = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        SavedJob savedJob = savedJobRepository.findByUserAndJobPost(user, jobPost)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job is not bookmarked"));

        savedJobRepository.delete(savedJob);
    }

    @Transactional(readOnly = true)
    public Page<JobListResponse> getSavedJobs(UUID userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Page<SavedJob> savedJobs = savedJobRepository.findByUser(user, pageable);

        return savedJobs.map(savedJob -> mapSavedJobToJobListResponse(savedJob, user));
    }

    private Specification<JobPost> buildJobSearchSpecification(JobSearchRequest searchRequest) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("status"), JobStatus.ACTIVE));

            if (searchRequest.getKeywords() != null && !searchRequest.getKeywords().isEmpty()) {
                String keyword = "%" + searchRequest.getKeywords().toLowerCase() + "%";
                Predicate titleMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), keyword);
                Predicate descriptionMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), keyword);
                Predicate companyMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("company")), keyword);
                predicates.add(criteriaBuilder.or(titleMatch, descriptionMatch, companyMatch));
            }

            if (searchRequest.getLocation() != null && !searchRequest.getLocation().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("location")),
                        "%" + searchRequest.getLocation().toLowerCase() + "%"
                ));
            }

            if (searchRequest.getIsRemote() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isRemote"), searchRequest.getIsRemote()));
            }

            if (searchRequest.getJobTypes() != null && !searchRequest.getJobTypes().isEmpty()) {
                List<JobType> jobTypes = searchRequest.getJobTypes().stream()
                        .map(type -> {
                            try {
                                return JobType.valueOf(type.toUpperCase());
                            } catch (IllegalArgumentException e) {
                                return null;
                            }
                        })
                        .filter(type -> type != null)
                        .collect(Collectors.toList());

                if (!jobTypes.isEmpty()) {
                    predicates.add(root.get("jobType").in(jobTypes));
                }
            }

            if (searchRequest.getExperienceLevels() != null && !searchRequest.getExperienceLevels().isEmpty()) {
                List<ExperienceLevel> experienceLevels = searchRequest.getExperienceLevels().stream()
                        .map(level -> {
                            try {
                                return ExperienceLevel.valueOf(level.toUpperCase());
                            } catch (IllegalArgumentException e) {
                                return null;
                            }
                        })
                        .filter(level -> level != null)
                        .collect(Collectors.toList());

                if (!experienceLevels.isEmpty()) {
                    predicates.add(root.get("experienceLevel").in(experienceLevels));
                }
            }

            if (searchRequest.getSalaryMin() != null) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("salaryMax")),
                        criteriaBuilder.greaterThanOrEqualTo(root.get("salaryMax"), searchRequest.getSalaryMin())
                ));
            }

            if (searchRequest.getSalaryMax() != null) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("salaryMin")),
                        criteriaBuilder.lessThanOrEqualTo(root.get("salaryMin"), searchRequest.getSalaryMax())
                ));
            }

            if (searchRequest.getSkills() != null && !searchRequest.getSkills().isEmpty()) {
                for (String skill : searchRequest.getSkills()) {
                    predicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("skills")),
                            "%" + skill.toLowerCase() + "%"
                    ));
                }
            }

            if (searchRequest.getPostedWithin() != null && !searchRequest.getPostedWithin().isEmpty()) {
                LocalDateTime sinceDate = calculatePostedWithinDate(searchRequest.getPostedWithin());
                if (sinceDate != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), sinceDate));
                }
            }

            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("applicationDeadline")),
                    criteriaBuilder.greaterThanOrEqualTo(root.get("applicationDeadline"), LocalDate.now())
            ));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort buildSort(JobSearchRequest searchRequest) {
        String sortBy = searchRequest.getSortBy() != null ? searchRequest.getSortBy() : "createdAt";
        String direction = searchRequest.getSortDirection() != null ? searchRequest.getSortDirection() : "DESC";

        Sort.Direction sortDirection = direction.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;

        switch (sortBy) {
            case "salaryMax":
                return Sort.by(sortDirection, "salaryMax").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "salaryMin":
                return Sort.by(sortDirection, "salaryMin").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "applicationDeadline":
                return Sort.by(sortDirection, "applicationDeadline").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "createdAt":
            default:
                return Sort.by(sortDirection, "createdAt");
        }
    }

    private LocalDateTime calculatePostedWithinDate(String postedWithin) {
        LocalDateTime now = LocalDateTime.now();
        switch (postedWithin.toLowerCase()) {
            case "24h":
            case "day":
                return now.minusDays(1);
            case "3days":
                return now.minusDays(3);
            case "week":
            case "7days":
                return now.minusDays(7);
            case "month":
            case "30days":
                return now.minusDays(30);
            default:
                return null;
        }
    }

    private JobListResponse mapToJobListResponse(JobPost jobPost, User user) {
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
        response.setDescription(jobPost.getDescription());

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

        Boolean isSaved = false;
        if (user != null) {
            isSaved = savedJobRepository.existsByUserAndJobPost(user, jobPost);
        }
        response.setIsSaved(isSaved);

        return response;
    }

    private JobListResponse mapSavedJobToJobListResponse(SavedJob savedJob, User user) {
        JobPost jobPost = savedJob.getJobPost();
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
        response.setDescription(jobPost.getDescription());

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
        response.setSavedAt(savedJob.getSavedAt());
        response.setIsSaved(true);

        return response;
    }
}
