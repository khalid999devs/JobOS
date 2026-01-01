package com.jobos.shared.dto.cv;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class SectionReorderRequest {

    @NotNull(message = "Section order is required")
    private List<String> sectionIds;

    public List<String> getSectionIds() {
        return sectionIds;
    }

    public void setSectionIds(List<String> sectionIds) {
        this.sectionIds = sectionIds;
    }
}
