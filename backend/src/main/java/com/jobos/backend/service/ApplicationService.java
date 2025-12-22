package com.jobos.backend.service;

import com.jobos.backend.domain.application.Application;
import com.jobos.backend.domain.application.ApplicationStatus;
import com.jobos.backend.domain.application.ApplicationStatusHistory;
import com.jobos.backend.domain.job.JobPost;
import com.jobos.backend.domain.job.JobStatus;
import com.jobos.backend.domain.user.User;
import com.jobos.backend.domain.user.UserRole;
import com.jobos.backend.repository.ApplicationRepository;
import com.jobos.backend.repository.ApplicationStatusHistoryRepository;
import com.jobos.backend.repository.JobPostRepository;
import com.jobos.backend.repository.UserRepository;
import com.jobos.shared.dto.application.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final JobPostRepository jobPostRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            ApplicationStatusHistoryRepository statusHistoryRepository,
            JobPostRepository jobPostRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this.applicationRepository = applicationRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.jobPostRepository = jobPostRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ApplicationResponse apply(UUID seekerId, ApplicationRequest request) {
        User seeker = userRepository.findById(seekerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (seeker.getRole() != UserRole.SEEKER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only seekers can apply to jobs");
        }

        JobPost jobPost = jobPostRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        if (jobPost.getStatus() != JobStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot apply to closed job");
        }

        if (applicationRepository.existsBySeekerAndJobPost(seeker, jobPost)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already applied to this job");
        }

        Application application = new Application();
        application.setSeeker(seeker);
        application.setJobPost(jobPost);
        application.setCvFileUrl(request.getCvFileUrl());
        application.setCoverLetter(request.getCoverLetter());
        application.setAnswers(request.getAnswers());
        application.setStatus(ApplicationStatus.PENDING);

        application = applicationRepository.saveAndFlush(application);

        jobPost.setApplicationCount(jobPost.getApplicationCount() + 1);
        jobPostRepository.save(jobPost);

        notificationService.publishUserNotification(
                jobPost.getPoster().getId().toString(),
                "New Application for " + jobPost.getTitle(),
                seeker.getFirstName() + " " + seeker.getLastName() + " applied to your job"
        );

        return mapToApplicationResponse(application);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationListResponse> getMyApplications(UUID seekerId, Pageable pageable) {
        User seeker = userRepository.findById(seekerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Page<Application> applications = applicationRepository.findBySeeker(seeker, pageable);
        return applications.map(this::mapToApplicationListResponse);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(UUID applicationId, UUID userId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        if (!application.getSeeker().getId().equals(userId) && 
            !application.getJobPost().getPoster().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return mapToApplicationResponse(application);
    }

    @Transactional(readOnly = true)
    public Page<ApplicantResponse> getJobApplicants(UUID jobId, UUID posterId, String statusFilter, Pageable pageable) {
        User poster = userRepository.findById(posterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (poster.getRole() != UserRole.POSTER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only posters can view applicants");
        }

        JobPost jobPost = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        if (!jobPost.getPoster().getId().equals(posterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view applicants for your own jobs");
        }

        Page<Application> applications;
        if (statusFilter != null && !statusFilter.isEmpty()) {
            try {
                ApplicationStatus status = ApplicationStatus.valueOf(statusFilter.toUpperCase());
                applications = applicationRepository.findByJobPostAndStatus(jobPost, status, pageable);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status filter");
            }
        } else {
            applications = applicationRepository.findByJobPost(jobPost, pageable);
        }

        return applications.map(this::mapToApplicantResponse);
    }

    @Transactional
    public ApplicationResponse updateApplicationStatus(UUID applicationId, UUID posterId, ApplicationStatusUpdateRequest request) {
        User poster = userRepository.findById(posterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (poster.getRole() != UserRole.POSTER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only posters can update application status");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        if (!application.getJobPost().getPoster().getId().equals(posterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update applications for your own jobs");
        }

        ApplicationStatus oldStatus = application.getStatus();
        ApplicationStatus newStatus;
        try {
            newStatus = ApplicationStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + request.getStatus());
        }

        if (oldStatus == newStatus) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application is already in " + newStatus + " status");
        }

        application.setStatus(newStatus);
        application = applicationRepository.saveAndFlush(application);

        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setApplication(application);
        history.setFromStatus(oldStatus);
        history.setToStatus(newStatus);
        history.setChangedBy(posterId);
        history.setNotes(request.getNotes());
        statusHistoryRepository.save(history);

        notificationService.publishUserNotification(
                application.getSeeker().getId().toString(),
                "Application Status Updated",
                "Your application for " + application.getJobPost().getTitle() + " is now " + newStatus
        );

        return mapToApplicationResponse(application);
    }

    private ApplicationResponse mapToApplicationResponse(Application application) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(application.getId().toString());
        response.setJobId(application.getJobPost().getId().toString());
        response.setJobTitle(application.getJobPost().getTitle());
        response.setCompany(application.getJobPost().getCompany());
        response.setLocation(application.getJobPost().getLocation());
        response.setStatus(application.getStatus().name());
        response.setCvFileUrl(application.getCvFileUrl());
        response.setCoverLetter(application.getCoverLetter());
        response.setAnswers(application.getAnswers());
        response.setAppliedAt(application.getAppliedAt());
        response.setUpdatedAt(application.getUpdatedAt());
        return response;
    }

    private ApplicationListResponse mapToApplicationListResponse(Application application) {
        ApplicationListResponse response = new ApplicationListResponse();
        response.setId(application.getId().toString());
        response.setJobId(application.getJobPost().getId().toString());
        response.setJobTitle(application.getJobPost().getTitle());
        response.setCompany(application.getJobPost().getCompany());
        response.setLocation(application.getJobPost().getLocation());
        response.setStatus(application.getStatus().name());
        response.setAppliedAt(application.getAppliedAt());
        response.setUpdatedAt(application.getUpdatedAt());
        return response;
    }

    private ApplicantResponse mapToApplicantResponse(Application application) {
        ApplicantResponse response = new ApplicantResponse();
        response.setId(application.getId().toString());
        response.setApplicationId(application.getId().toString());
        response.setSeekerId(application.getSeeker().getId().toString());
        response.setSeekerName(application.getSeeker().getFirstName() + " " + application.getSeeker().getLastName());
        response.setSeekerEmail(application.getSeeker().getEmail());
        response.setStatus(application.getStatus().name());
        response.setCvFileUrl(application.getCvFileUrl());
        response.setCoverLetter(application.getCoverLetter());
        response.setAnswers(application.getAnswers());
        response.setAppliedAt(application.getAppliedAt());
        response.setUpdatedAt(application.getUpdatedAt());
        return response;
    }
}
