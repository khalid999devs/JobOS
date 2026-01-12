package com.jobos.android.data.model.application;

public class CreateApplicationRequest {

    private String jobId;
    private String cvId;
    private String cvFileUrl;
    private String coverLetter;
    private String answers;

    public CreateApplicationRequest() {
    }

    public CreateApplicationRequest(String jobId, String cvId, String cvFileUrl, String coverLetter, String answers) {
        this.jobId = jobId;
        this.cvId = cvId;
        this.cvFileUrl = cvFileUrl;
        this.coverLetter = coverLetter;
        this.answers = answers;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getCvId() {
        return cvId;
    }

    public void setCvId(String cvId) {
        this.cvId = cvId;
    }

    public String getCvFileUrl() {
        return cvFileUrl;
    }

    public void setCvFileUrl(String cvFileUrl) {
        this.cvFileUrl = cvFileUrl;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public String getAnswers() {
        return answers;
    }

    public void setAnswers(String answers) {
        this.answers = answers;
    }
}
