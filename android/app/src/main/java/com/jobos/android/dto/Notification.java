package com.jobos.android.dto;

public class Notification {
    private String id;
    private String title;
    private String body;
    private Long createdAt;
    private Boolean read;

    public Notification() {}

    public Notification(String id, String title, String body, Long createdAt, Boolean read) {
        this.id = id;
        this.title = title;
        this.body = body;
        this.createdAt = createdAt;
        this.read = read;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }
}
