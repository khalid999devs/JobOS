package com.jobos.android.data.model.cv;

public class UpdateCVRequest {

    private String title;
    private String templateId;
    private String visibility;

    public UpdateCVRequest() {
    }

    public UpdateCVRequest(String title, String templateId, String visibility) {
        this.title = title;
        this.templateId = templateId;
        this.visibility = visibility;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }
}
