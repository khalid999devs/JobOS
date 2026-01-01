package com.jobos.shared.dto.application;

import jakarta.validation.constraints.NotNull;

public class ApplicationStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private String status;

    private String notes;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
