package com.jobos.shared.dto.job;

import java.time.LocalDateTime;

public class SavedJobResponse {

    private String id;
    private JobPostResponse job;
    private LocalDateTime savedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public JobPostResponse getJob() {
        return job;
    }

    public void setJob(JobPostResponse job) {
        this.job = job;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }
}
