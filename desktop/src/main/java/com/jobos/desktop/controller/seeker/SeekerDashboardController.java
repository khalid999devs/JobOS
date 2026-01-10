package com.jobos.desktop.controller.seeker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jobos.desktop.core.session.SessionManager;
import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.ui.SkeletonLoader;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.ApiClient;
import com.jobos.shared.dto.profile.ProfileResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class SeekerDashboardController implements Initializable {
    
    @FXML private Label welcomeLabel;
    @FXML private Label totalApplicationsLabel;
    @FXML private Label pendingLabel;
    @FXML private Label interviewsLabel;
    @FXML private Label savedJobsLabel;
    @FXML private VBox recentApplicationsList;
    @FXML private VBox recommendedJobsContainer;
    @FXML private HBox statsContainer;
    @FXML private Label availableJobsMetric;
    @FXML private Label cvsMetric;
    @FXML private Label profileViewsMetric;
    @FXML private Label responsesMetric;
    
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
        if (profile != null && profile.getFirstName() != null) {
            String name = profile.getFirstName();
            if (profile.getLastName() != null) {
                name += " " + profile.getLastName();
            }
            welcomeLabel.setText("Welcome back, " + name + "!");
        } else {
            apiClient.get("/api/users/me", new TypeReference<ProfileResponse>() {})
                .thenAccept(p -> {
                    if (p != null) {
                        sessionManager.updateProfile(p);
                        Platform.runLater(() -> {
                            String name = p.getFirstName() != null ? p.getFirstName() : "User";
                            if (p.getLastName() != null) {
                                name += " " + p.getLastName();
                            }
                            welcomeLabel.setText("Welcome back, " + name + "!");
                        });
                    }
                })
                .exceptionally(e -> null);
        }
    }
    
    private void loadDashboardData() {
        recentApplicationsList.getChildren().setAll(SkeletonLoader.createSkeletonApplicantsList(3));
        recommendedJobsContainer.getChildren().setAll(SkeletonLoader.createSkeletonJobList(4));
        
        apiClient.get("/api/applications?page=0&size=20", new TypeReference<Map<String, Object>>() {})
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    if (response != null) {
                        Object applications = response.get("applications");
                        if (applications instanceof List<?> appList) {
                            updateStats(appList);
                            updateRecentApplications(appList);
                        } else {
                            setDefaultStats();
                            updateRecentApplications(new java.util.ArrayList<>());
                        }
                    } else {
                        setDefaultStats();
                        updateRecentApplications(new java.util.ArrayList<>());
                    }
                    loadAdditionalData();
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    setDefaultStats();
                    updateRecentApplications(new java.util.ArrayList<>());
                    loadAdditionalData();
                });
                return null;
            });
    }
    
    private void loadAdditionalData() {
        apiClient.get("/api/cvs?page=0&size=100", new TypeReference<Map<String, Object>>() {})
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    if (response != null) {
                        Object content = response.get("content");
                        if (content instanceof List<?> cvs) {
                            cvsMetric.setText(String.valueOf(cvs.size()));
                        }
                    }
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> cvsMetric.setText("0"));
                return null;
            });
        
        apiClient.get("/api/jobs/saved?page=0&size=1", new TypeReference<Map<String, Object>>() {})
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    if (response != null && response.get("totalElements") != null) {
                        savedJobsLabel.setText(String.valueOf(response.get("totalElements")));
                    }
                });
            })
            .exceptionally(e -> null);
        
        loadRecommendedJobs();
    }
    
    private void loadRecommendedJobs() {
        com.jobos.shared.dto.job.JobSearchRequest request = new com.jobos.shared.dto.job.JobSearchRequest();
        request.setPage(0);
        request.setSize(4);
        
        apiClient.post("/api/jobs/search", request, com.jobos.shared.dto.job.JobSearchResponse.class)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    if (response != null && response.getJobs() != null) {
                        updateRecommendedJobs(response.getJobs());
                        if (response.getTotalElements() != null) {
                            availableJobsMetric.setText(response.getTotalElements() > 1000 ? "1000+" : String.valueOf(response.getTotalElements()));
                        }
                    }
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> recommendedJobsContainer.getChildren().clear());
                return null;
            });
    }
    
    private void updateStats(List<?> applications) {
        int total = applications.size();
        int pending = 0;
        int interviews = 0;
        
        for (Object app : applications) {
            if (app instanceof LinkedHashMap<?, ?> map) {
                String status = getString(map, "status");
                if ("PENDING".equals(status) || "SUBMITTED".equals(status)) {
                    pending++;
                } else if ("INTERVIEW".equals(status) || "INTERVIEW_SCHEDULED".equals(status)) {
                    interviews++;
                }
            }
        }
        
        totalApplicationsLabel.setText(String.valueOf(total));
        pendingLabel.setText(String.valueOf(pending));
        interviewsLabel.setText(String.valueOf(interviews));
        savedJobsLabel.setText("0");
    }
    
    private void setDefaultStats() {
        totalApplicationsLabel.setText("0");
        pendingLabel.setText("0");
        interviewsLabel.setText("0");
        savedJobsLabel.setText("0");
    }
    
    private void updateRecentApplications(List<?> applications) {
        recentApplicationsList.getChildren().clear();
        
        int displayCount = Math.min(applications.size(), 5);
        for (int i = 0; i < displayCount; i++) {
            Object app = applications.get(i);
            if (app instanceof LinkedHashMap<?, ?> map) {
                recentApplicationsList.getChildren().add(createApplicationItem(map));
            }
        }
        
        if (applications.isEmpty()) {
            Label emptyLabel = new Label("No applications yet. Start browsing jobs!");
            emptyLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px; -fx-padding: 16 0;");
            recentApplicationsList.getChildren().add(emptyLabel);
        }
    }
    
    private HBox createApplicationItem(LinkedHashMap<?, ?> app) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12, 16, 12, 16));
        item.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 8; -fx-cursor: hand;");
        
        // Get job title - it might be directly on app or nested in job object
        String jobTitle = getString(app, "jobTitle");
        if (jobTitle == null) {
            Object jobObj = app.get("job");
            if (jobObj instanceof LinkedHashMap<?, ?> job) {
                jobTitle = getString(job, "title");
            }
        }
        if (jobTitle == null) jobTitle = "Unknown Position";
        
        // Get company
        String company = getString(app, "company");
        if (company == null) {
            Object jobObj = app.get("job");
            if (jobObj instanceof LinkedHashMap<?, ?> job) {
                company = getString(job, "companyName");
            }
        }
        
        VBox textBox = new VBox(2);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        
        Label titleLabel = new Label(jobTitle);
        titleLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 14px; -fx-font-weight: 600;");
        
        if (company != null && !company.isEmpty()) {
            Label companyLabel = new Label(company);
            companyLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");
            textBox.getChildren().addAll(titleLabel, companyLabel);
        } else {
            textBox.getChildren().add(titleLabel);
        }
        
        // Status badge
        String status = getString(app, "status");
        Label statusLabel = createStatusLabel(status);
        
        item.getChildren().addAll(textBox, statusLabel);
        
        // Click handler
        item.setOnMouseClicked(e -> {
            Object idObj = app.get("id");
            if (idObj != null) {
                router.navigate(Route.SEEKER_APPLICATION_DETAIL, java.util.Map.of("id", idObj.toString()));
            }
        });
        
        item.setOnMouseEntered(e -> item.setStyle("-fx-background-color: #F0FDFA; -fx-background-radius: 8; -fx-cursor: hand;"));
        item.setOnMouseExited(e -> item.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 8; -fx-cursor: hand;"));
        
        return item;
    }
    
    private Label createStatusLabel(String status) {
        Label label = new Label(formatStatus(status != null ? status : "PENDING"));
        
        String bgColor, textColor;
        switch (status != null ? status.toUpperCase() : "") {
            case "PENDING", "SUBMITTED" -> { bgColor = "#FEF3C7"; textColor = "#D97706"; }
            case "REVIEWING", "REVIEWED" -> { bgColor = "#E0F2FE"; textColor = "#0284C7"; }
            case "SHORTLISTED" -> { bgColor = "#F3E8FF"; textColor = "#9333EA"; }
            case "INTERVIEW", "INTERVIEW_SCHEDULED", "INTERVIEWING" -> { bgColor = "#CCFBF1"; textColor = "#0F766E"; }
            case "ACCEPTED", "HIRED", "OFFERED" -> { bgColor = "#DCFCE7"; textColor = "#16A34A"; }
            case "REJECTED" -> { bgColor = "#FEE2E2"; textColor = "#DC2626"; }
            default -> { bgColor = "#F3F4F6"; textColor = "#6B7280"; }
        }
        
        label.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + 
                       "; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: 600;");
        return label;
    }
    
    private void updateRecommendedJobs(List<?> jobs) {
        recommendedJobsContainer.getChildren().clear();
        
        for (Object job : jobs) {
            if (job instanceof com.jobos.shared.dto.job.JobListResponse jobResp) {
                recommendedJobsContainer.getChildren().add(createJobCard(jobResp));
            } else if (job instanceof LinkedHashMap<?, ?> map) {
                recommendedJobsContainer.getChildren().add(createJobCardFromMap(map));
            }
        }
        
        if (jobs.isEmpty()) {
            Label emptyLabel = new Label("No recommended jobs at this time");
            emptyLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px; -fx-padding: 16 0;");
            recommendedJobsContainer.getChildren().add(emptyLabel);
        }
    }
    
    private HBox createJobCard(com.jobos.shared.dto.job.JobListResponse job) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 8; -fx-cursor: hand;");
        
        VBox textBox = new VBox(2);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        
        Label titleLabel = new Label(job.getTitle() != null ? job.getTitle() : "Unknown");
        titleLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 14px; -fx-font-weight: 600;");
        
        HBox metaRow = new HBox(8);
        if (job.getCompany() != null) {
            Label companyLabel = new Label(job.getCompany());
            companyLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");
            metaRow.getChildren().add(companyLabel);
        }
        if (job.getLocation() != null) {
            Label locLabel = new Label("📍 " + job.getLocation());
            locLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
            metaRow.getChildren().add(locLabel);
        }
        
        textBox.getChildren().addAll(titleLabel, metaRow);
        card.getChildren().add(textBox);
        
        card.setOnMouseClicked(e -> {
            if (job.getId() != null) {
                router.navigate(Route.SEEKER_JOB_DETAIL, java.util.Map.of("id", job.getId()));
            }
        });
        
        card.setOnMouseEntered(ev -> card.setStyle("-fx-background-color: #F0FDFA; -fx-background-radius: 8; -fx-cursor: hand;"));
        card.setOnMouseExited(ev -> card.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 8; -fx-cursor: hand;"));
        
        return card;
    }
    
    private HBox createJobCardFromMap(LinkedHashMap<?, ?> job) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 8; -fx-cursor: hand;");
        
        String title = getString(job, "title");
        String company = getString(job, "companyName");
        String location = getString(job, "location");
        
        VBox textBox = new VBox(2);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        
        Label titleLabel = new Label(title != null ? title : "Unknown");
        titleLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 14px; -fx-font-weight: 600;");
        
        HBox metaRow = new HBox(8);
        if (company != null) {
            Label companyLabel = new Label(company);
            companyLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");
            metaRow.getChildren().add(companyLabel);
        }
        if (location != null) {
            Label locLabel = new Label("📍 " + location);
            locLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
            metaRow.getChildren().add(locLabel);
        }
        
        textBox.getChildren().addAll(titleLabel, metaRow);
        card.getChildren().add(textBox);
        
        card.setOnMouseClicked(e -> {
            Object idObj = job.get("id");
            if (idObj != null) {
                router.navigate(Route.SEEKER_JOB_DETAIL, java.util.Map.of("id", idObj.toString()));
            }
        });
        
        card.setOnMouseEntered(ev -> card.setStyle("-fx-background-color: #F0FDFA; -fx-background-radius: 8; -fx-cursor: hand;"));
        card.setOnMouseExited(ev -> card.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 8; -fx-cursor: hand;"));
        
        return card;
    }
    
    @FXML
    private void onBrowseJobs() {
        router.navigate(Route.SEEKER_JOBS);
    }
    
    @FXML
    private void onViewApplications() {
        router.navigate(Route.SEEKER_APPLICATIONS);
    }
    
    @FXML
    private void onManageCVs() {
        router.navigate(Route.SEEKER_CVS);
    }
    
    @FXML
    private void onViewSavedJobs() {
        router.navigate(Route.SEEKER_JOBS);
    }
    
    private String getString(LinkedHashMap<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
    
    private String formatStatus(String status) {
        if (status == null) return "Unknown";
        String formatted = status.replace("_", " ").toLowerCase();
        return Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1);
    }
}
