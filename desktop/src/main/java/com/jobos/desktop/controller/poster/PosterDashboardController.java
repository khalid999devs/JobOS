package com.jobos.desktop.controller.poster;

import com.jobos.desktop.core.session.SessionManager;
import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.ui.LoadingOverlay;
import com.jobos.desktop.service.ApiClient;
import com.jobos.shared.dto.common.ApiResponse;
import com.jobos.shared.dto.profile.ProfileResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ResourceBundle;

public class PosterDashboardController implements Initializable {
    
    @FXML private Label welcomeLabel;
    @FXML private Label totalJobsLabel;
    @FXML private Label activeJobsLabel;
    @FXML private Label totalApplicationsLabel;
    @FXML private Label creditsLabel;
    @FXML private Label jobsPostedMetric;
    @FXML private Label viewsMetric;
    @FXML private Label applicationsMetric;
    @FXML private Label hiredMetric;
    @FXML private VBox recentApplicationsList;
    @FXML private VBox myJobsContainer;
    @FXML private HBox statsContainer;
    
    private final ApiClient apiClient = ApiClient.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final Router router = Router.getInstance();
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupWelcome();
        loadDashboardData();
    }
    
    private void setupWelcome() {
        ProfileResponse profile = sessionManager.getProfile();
        if (profile != null) {
            String name = profile.getFirstName() != null ? profile.getFirstName() : "Employer";
            welcomeLabel.setText("Welcome back, " + name + "!");
        } else {
            welcomeLabel.setText("Welcome back!");
        }
    }
    
    private void loadDashboardData() {
        LoadingOverlay.show("Loading dashboard...");
        
        apiClient.get("/api/jobs/my", ApiResponse.class)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    @SuppressWarnings("unchecked")
                    ApiResponse<Object> apiResp = (ApiResponse<Object>) response;
                    Object result = apiResp.getResult();
                    
                    if (result instanceof List<?> jobs) {
                        updateJobStats(jobs);
                        updateMyJobs(jobs);
                    }
                    
                    loadRecentApplicants();
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    LoadingOverlay.hide();
                    setDefaultStats();
                });
                return null;
            });
    }
    
    private void loadRecentApplicants() {
        apiClient.get("/api/applications/received?limit=5", ApiResponse.class)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    LoadingOverlay.hide();
                    
                    @SuppressWarnings("unchecked")
                    ApiResponse<Object> apiResp = (ApiResponse<Object>) response;
                    Object result = apiResp.getResult();
                    
                    if (result instanceof List<?> applications) {
                        updateTotalApplications(applications.size());
                        updateRecentApplicants(applications);
                    }
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    LoadingOverlay.hide();
                });
                return null;
            });
    }
    
    private void updateJobStats(List<?> jobs) {
        int total = jobs.size();
        int active = 0;
        
        for (Object job : jobs) {
            if (job instanceof LinkedHashMap<?, ?> map) {
                String status = getString(map, "status");
                if ("OPEN".equals(status) || "ACTIVE".equals(status)) {
                    active++;
                }
            }
        }
        
        totalJobsLabel.setText(String.valueOf(total));
        activeJobsLabel.setText(String.valueOf(active));
        creditsLabel.setText("0");
    }
    
    private void setDefaultStats() {
        totalJobsLabel.setText("0");
        activeJobsLabel.setText("0");
        totalApplicationsLabel.setText("0");
        creditsLabel.setText("0");
    }
    
    private void updateTotalApplications(int count) {
        totalApplicationsLabel.setText(String.valueOf(count));
    }
    
    private void updateMyJobs(List<?> jobs) {
        myJobsContainer.getChildren().clear();
        
        int displayCount = Math.min(jobs.size(), 4);
        for (int i = 0; i < displayCount; i++) {
            Object job = jobs.get(i);
            if (job instanceof LinkedHashMap<?, ?> map) {
                myJobsContainer.getChildren().add(createJobCard(map));
            }
        }
        
        if (jobs.isEmpty()) {
            Label emptyLabel = new Label("No job posts yet. Create your first job!");
            emptyLabel.getStyleClass().add("empty-state-text");
            myJobsContainer.getChildren().add(emptyLabel);
        }
    }
    
    private VBox createJobCard(LinkedHashMap<?, ?> job) {
        VBox card = new VBox(8);
        card.getStyleClass().add("job-card");
        card.setPrefWidth(280);
        
        String title = getString(job, "title");
        String status = getString(job, "status");
        String location = getString(job, "location");
        
        Label titleLabel = new Label(title != null ? title : "Unknown");
        titleLabel.getStyleClass().add("job-card-title");
        
        Label statusLabel = new Label(status != null ? formatStatus(status) : "Unknown");
        statusLabel.getStyleClass().addAll("badge", getStatusBadgeClass(status));
        
        HBox titleRow = new HBox(8);
        titleRow.getChildren().addAll(titleLabel, statusLabel);
        
        HBox meta = new HBox(12);
        if (location != null) {
            Label locLabel = new Label("📍 " + location);
            locLabel.getStyleClass().add("job-card-meta");
            meta.getChildren().add(locLabel);
        }
        
        Object applicantCount = job.get("applicationCount");
        Label applicantsLabel = new Label("👥 " + (applicantCount != null ? applicantCount : "0") + " applicants");
        applicantsLabel.getStyleClass().add("job-card-meta");
        meta.getChildren().add(applicantsLabel);
        
        card.getChildren().addAll(titleRow, meta);
        
        card.setOnMouseClicked(e -> {
            Object idObj = job.get("id");
            if (idObj != null) {
                router.navigate(Route.POSTER_APPLICANTS, java.util.Map.of("jobId", idObj.toString()));
            }
        });
        
        return card;
    }
    
    private void updateRecentApplicants(List<?> applications) {
        recentApplicationsList.getChildren().clear();
        
        for (Object app : applications) {
            if (app instanceof LinkedHashMap<?, ?> map) {
                recentApplicationsList.getChildren().add(createApplicantItem(map));
            }
        }
        
        if (applications.isEmpty()) {
            Label emptyLabel = new Label("No applications received yet");
            emptyLabel.getStyleClass().add("empty-state-text");
            recentApplicationsList.getChildren().add(emptyLabel);
        }
    }
    
    private VBox createApplicantItem(LinkedHashMap<?, ?> app) {
        VBox item = new VBox(4);
        item.getStyleClass().add("list-item");
        
        String applicantName = "Unknown Applicant";
        Object applicantObj = app.get("applicant");
        if (applicantObj instanceof LinkedHashMap<?, ?> applicant) {
            String firstName = getString(applicant, "firstName");
            String lastName = getString(applicant, "lastName");
            if (firstName != null) {
                applicantName = firstName;
                if (lastName != null) applicantName += " " + lastName;
            }
        }
        
        String jobTitle = "Unknown Position";
        Object jobObj = app.get("job");
        if (jobObj instanceof LinkedHashMap<?, ?> job) {
            String title = getString(job, "title");
            if (title != null) jobTitle = title;
        }
        
        Label nameLabel = new Label(applicantName);
        nameLabel.getStyleClass().add("list-item-title");
        
        Label jobLabel = new Label("Applied for: " + jobTitle);
        jobLabel.getStyleClass().add("list-item-subtitle");
        
        String status = getString(app, "status");
        Label statusLabel = new Label(status != null ? formatStatus(status) : "Unknown");
        statusLabel.getStyleClass().addAll("badge", getBadgeClass(status));
        
        HBox row = new HBox(8);
        row.getChildren().addAll(nameLabel, statusLabel);
        
        item.getChildren().addAll(row, jobLabel);
        
        item.setOnMouseClicked(e -> {
            Object idObj = app.get("id");
            if (idObj != null) {
                router.navigate(Route.POSTER_APPLICATION_DETAIL, java.util.Map.of("applicationId", idObj.toString()));
            }
        });
        
        return item;
    }
    
    @FXML
    private void onCreateJob() {
        router.navigate(Route.POSTER_JOB_FORM);
    }
    
    @FXML
    private void onViewJobs() {
        router.navigate(Route.POSTER_JOB_POSTS);
    }
    
    @FXML
    private void onViewApplicants() {
        router.navigate(Route.POSTER_APPLICANTS);
    }
    
    @FXML
    private void onBuyCredits() {
        router.navigate(Route.CREDITS);
    }
    
    private String getString(LinkedHashMap<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
    
    private String formatStatus(String status) {
        if (status == null) return "Unknown";
        String formatted = status.replace("_", " ");
        return formatted.charAt(0) + formatted.substring(1).toLowerCase();
    }
    
    private String getStatusBadgeClass(String status) {
        if (status == null) return "badge-default";
        return switch (status) {
            case "OPEN", "ACTIVE" -> "badge-success";
            case "PAUSED" -> "badge-warning";
            case "CLOSED", "EXPIRED" -> "badge-danger";
            default -> "badge-default";
        };
    }
    
    private String getBadgeClass(String status) {
        if (status == null) return "badge-default";
        return switch (status) {
            case "PENDING", "SUBMITTED" -> "badge-warning";
            case "INTERVIEW", "INTERVIEW_SCHEDULED" -> "badge-info";
            case "ACCEPTED", "HIRED" -> "badge-success";
            case "REJECTED" -> "badge-danger";
            default -> "badge-default";
        };
    }
}
