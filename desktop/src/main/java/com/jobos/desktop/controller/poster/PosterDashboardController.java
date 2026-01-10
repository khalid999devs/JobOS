package com.jobos.desktop.controller.poster;

import com.jobos.desktop.core.session.SessionManager;
import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.ui.SkeletonLoader;
import com.jobos.desktop.service.ApiClient;
import com.jobos.shared.dto.common.ApiResponse;
import com.jobos.shared.dto.profile.ProfileResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import com.fasterxml.jackson.core.type.TypeReference;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class PosterDashboardController implements Initializable {
    
    @FXML private Label welcomeLabel;
    @FXML private Label totalJobsLabel;
    @FXML private Label activeJobsLabel;
    @FXML private Label totalApplicationsLabel;
    @FXML private Label creditsLabel;
    @FXML private VBox recentApplicationsList;
    @FXML private VBox myJobsContainer;
    @FXML private HBox statsContainer;
    
    private final ApiClient apiClient = ApiClient.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final Router router = Router.getInstance();
    
    private int totalApplicationsCount = 0;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupWelcome();
        loadDashboardData();
        loadCreditsBalance();
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
    
    private void loadCreditsBalance() {
        apiClient.get("/api/credits/balance", ApiResponse.class)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    @SuppressWarnings("unchecked")
                    ApiResponse<Object> apiResp = (ApiResponse<Object>) response;
                    Object result = apiResp.getResult();
                    
                    if (result instanceof LinkedHashMap<?, ?> balanceMap) {
                        Object balance = balanceMap.get("balance");
                        if (balance != null) {
                            creditsLabel.setText(balance.toString());
                        }
                    }
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> creditsLabel.setText("0"));
                return null;
            });
    }
    
    private void loadDashboardData() {
        myJobsContainer.getChildren().clear();
        myJobsContainer.getChildren().add(SkeletonLoader.createSkeletonJobList(3));
        
        recentApplicationsList.getChildren().clear();
        recentApplicationsList.getChildren().add(SkeletonLoader.createSkeletonApplicantsList(3));
        
        apiClient.get("/api/job-posts?size=100", new TypeReference<Map<String, Object>>() {})
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    List<?> jobs = new ArrayList<>();
                    if (response != null) {
                        Object jobsObj = response.get("jobs");
                        if (jobsObj instanceof List<?>) {
                            jobs = (List<?>) jobsObj;
                        }
                    }
                    
                    updateJobStats(jobs);
                    updateMyJobs(jobs);
                    loadApplicationsForJobs(jobs);
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    setDefaultStats();
                    updateMyJobs(Collections.emptyList());
                    updateRecentApplicants(Collections.emptyList());
                });
                return null;
            });
    }
    
    private void loadApplicationsForJobs(List<?> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            totalApplicationsLabel.setText("0");
            updateRecentApplicants(Collections.emptyList());
            return;
        }
        
        int totalApps = 0;
        String firstJobIdWithApplicants = null;
        
        for (Object job : jobs) {
            if (job instanceof LinkedHashMap<?, ?> map) {
                Object appCount = map.get("applicationCount");
                if (appCount instanceof Number) {
                    int count = ((Number) appCount).intValue();
                    totalApps += count;
                    if (count > 0 && firstJobIdWithApplicants == null) {
                        Object idObj = map.get("id");
                        if (idObj != null) {
                            firstJobIdWithApplicants = idObj.toString();
                        }
                    }
                }
            }
        }
        
        totalApplicationsCount = totalApps;
        totalApplicationsLabel.setText(String.valueOf(totalApps));
        
        if (firstJobIdWithApplicants != null) {
            loadRecentApplicantsFromJob(firstJobIdWithApplicants);
        } else {
            updateRecentApplicants(Collections.emptyList());
        }
    }
    
    private void loadRecentApplicantsFromJob(String jobId) {
        apiClient.get("/api/job-posts/" + jobId + "/applicants?page=0&size=5", new TypeReference<Map<String, Object>>() {})
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    List<?> applicants = new ArrayList<>();
                    if (response != null) {
                        Object applicantsObj = response.get("applicants");
                        if (applicantsObj instanceof List<?>) {
                            applicants = (List<?>) applicantsObj;
                        }
                    }
                    
                    updateRecentApplicants(applicants);
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    updateRecentApplicants(Collections.emptyList());
                });
                return null;
            });
    }
    
    private void updateJobStats(List<?> jobs) {
        if (jobs == null) {
            totalJobsLabel.setText("0");
            activeJobsLabel.setText("0");
            return;
        }
        
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
    }
    
    private void setDefaultStats() {
        totalJobsLabel.setText("0");
        activeJobsLabel.setText("0");
        totalApplicationsLabel.setText("0");
        creditsLabel.setText("0");
    }
    
    private void updateMyJobs(List<?> jobs) {
        myJobsContainer.getChildren().clear();
        
        if (jobs == null || jobs.isEmpty()) {
            Label emptyLabel = new Label("No job posts yet. Create your first job!");
            emptyLabel.getStyleClass().add("empty-state-text");
            myJobsContainer.getChildren().add(emptyLabel);
            return;
        }
        
        int displayCount = Math.min(jobs.size(), 4);
        for (int i = 0; i < displayCount; i++) {
            Object job = jobs.get(i);
            if (job instanceof LinkedHashMap<?, ?> map) {
                myJobsContainer.getChildren().add(createJobCard(map));
            }
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
        meta.setAlignment(Pos.CENTER_LEFT);
        if (location != null) {
            HBox locBox = new HBox(4);
            locBox.setAlignment(Pos.CENTER_LEFT);
            FontIcon locIcon = new FontIcon("fas-map-marker-alt");
            locIcon.setIconSize(12);
            locIcon.setIconColor(javafx.scene.paint.Color.web("#6B7280"));
            Label locLabel = new Label(location);
            locLabel.getStyleClass().add("job-card-meta");
            locBox.getChildren().addAll(locIcon, locLabel);
            meta.getChildren().add(locBox);
        }
        
        Object applicantCount = job.get("applicationCount");
        HBox appBox = new HBox(4);
        appBox.setAlignment(Pos.CENTER_LEFT);
        FontIcon appIcon = new FontIcon("fas-users");
        appIcon.setIconSize(12);
        appIcon.setIconColor(javafx.scene.paint.Color.web("#6B7280"));
        Label applicantsLabel = new Label((applicantCount != null ? applicantCount : "0") + " applicants");
        applicantsLabel.getStyleClass().add("job-card-meta");
        appBox.getChildren().addAll(appIcon, applicantsLabel);
        meta.getChildren().add(appBox);
        
        card.getChildren().addAll(titleRow, meta);
        
        card.setOnMouseClicked(e -> {
            Object idObj = job.get("id");
            if (idObj != null) {
                router.navigate(Route.POSTER_JOB_DETAIL, java.util.Map.of("jobId", idObj.toString()));
            }
        });
        
        return card;
    }
    
    private void updateRecentApplicants(List<?> applications) {
        recentApplicationsList.getChildren().clear();
        
        if (applications == null || applications.isEmpty()) {
            Label emptyLabel = new Label("No applications received yet");
            emptyLabel.getStyleClass().add("empty-state-text");
            recentApplicationsList.getChildren().add(emptyLabel);
            return;
        }
        
        for (Object app : applications) {
            if (app instanceof LinkedHashMap<?, ?> map) {
                recentApplicationsList.getChildren().add(createApplicantItem(map));
            }
        }
    }
    
    private VBox createApplicantItem(LinkedHashMap<?, ?> app) {
        VBox item = new VBox(4);
        item.getStyleClass().add("list-item");
        
        String applicantName = getString(app, "seekerName");
        if (applicantName == null || applicantName.isEmpty()) {
            applicantName = "Unknown Applicant";
        }
        
        Label nameLabel = new Label(applicantName);
        nameLabel.getStyleClass().add("list-item-title");
        
        String seekerEmail = getString(app, "seekerEmail");
        Label emailLabel = new Label(seekerEmail != null ? seekerEmail : "");
        emailLabel.getStyleClass().add("list-item-subtitle");
        
        String status = getString(app, "status");
        Label statusLabel = new Label(status != null ? formatStatus(status) : "Unknown");
        statusLabel.getStyleClass().addAll("badge", getBadgeClass(status));
        
        HBox row = new HBox(8);
        row.getChildren().addAll(nameLabel, statusLabel);
        
        item.getChildren().addAll(row, emailLabel);
        
        item.setOnMouseClicked(e -> {
            Object appIdObj = app.get("applicationId");
            if (appIdObj == null) {
                appIdObj = app.get("id");
            }
            if (appIdObj != null) {
                router.navigate(Route.POSTER_APPLICATION_DETAIL, java.util.Map.of("applicationId", appIdObj.toString()));
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
            case "PENDING" -> "badge-warning";
            case "REVIEWED" -> "badge-info";
            case "SHORTLISTED" -> "badge-info";
            case "ACCEPTED" -> "badge-success";
            case "REJECTED" -> "badge-danger";
            default -> "badge-default";
        };
    }
}
