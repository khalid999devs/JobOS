package com.jobos.desktop.controller.poster;

import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.ui.LoadingOverlay;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.JobPostService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ApplicantsController implements Initializable {

    @FXML private Label jobTitle;
    @FXML private Label totalApplicantsLabel;
    @FXML private ComboBox<String> jobSelector;
    @FXML private ComboBox<String> statusFilter;
    @FXML private VBox applicantsList;
    @FXML private HBox paginationContainer;
    
    private final JobPostService jobPostService = new JobPostService();
    private final Router router = Router.getInstance();
    
    private String selectedJobId;
    private String selectedStatus = null;
    private int currentPage = 0;
    private int totalPages = 1;
    private List<Map<String, Object>> myJobs = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupFilters();
        loadInitialData();
    }

    private void setupFilters() {
        statusFilter.getItems().addAll("All", "PENDING", "REVIEWING", "SHORTLISTED", "INTERVIEWED", "OFFERED", "HIRED", "REJECTED");
        statusFilter.setValue("All");
        statusFilter.setOnAction(e -> {
            String value = statusFilter.getValue();
            selectedStatus = "All".equals(value) ? null : value;
            currentPage = 0;
            loadApplicants();
        });
        
        jobSelector.setOnAction(e -> {
            String selected = jobSelector.getValue();
            if (selected != null && !selected.isEmpty()) {
                for (Map<String, Object> job : myJobs) {
                    String title = getString(job, "title");
                    if (selected.equals(title)) {
                        selectedJobId = getString(job, "id");
                        currentPage = 0;
                        loadApplicants();
                        break;
                    }
                }
            }
        });
    }

    private void loadInitialData() {
        String paramJobId = router.getParam("jobId");
        
        LoadingOverlay.show("Loading...");
        
        jobPostService.getMyJobPosts(0, 100, null)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> jobs = (List<Map<String, Object>>) response.get("jobs");
                    myJobs = jobs != null ? jobs : new ArrayList<>();
                    
                    jobSelector.getItems().clear();
                    for (Map<String, Object> job : myJobs) {
                        String title = getString(job, "title");
                        if (title != null) {
                            jobSelector.getItems().add(title);
                        }
                    }
                    
                    if (paramJobId != null) {
                        selectedJobId = paramJobId;
                        for (Map<String, Object> job : myJobs) {
                            if (paramJobId.equals(getString(job, "id"))) {
                                jobSelector.setValue(getString(job, "title"));
                                break;
                            }
                        }
                    } else if (!myJobs.isEmpty()) {
                        selectedJobId = getString(myJobs.get(0), "id");
                        jobSelector.setValue(getString(myJobs.get(0), "title"));
                    }
                    
                    if (selectedJobId != null) {
                        loadApplicants();
                    } else {
                        LoadingOverlay.hide();
                        showEmptyState("No job posts found. Create a job post first to receive applications.");
                    }
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    LoadingOverlay.hide();
                    showEmptyState("Failed to load jobs. Please try again.");
                });
                return null;
            });
    }

    private void loadApplicants() {
        if (selectedJobId == null) return;
        
        LoadingOverlay.show("Loading applicants...");
        
        jobPostService.getJobApplicants(selectedJobId, currentPage, 10, selectedStatus)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    LoadingOverlay.hide();
                    renderApplicants(response);
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    LoadingOverlay.hide();
                    showEmptyState("Failed to load applicants. Please try again.");
                });
                return null;
            });
    }

    @SuppressWarnings("unchecked")
    private void renderApplicants(Map<String, Object> response) {
        applicantsList.getChildren().clear();
        
        List<Map<String, Object>> applicants = (List<Map<String, Object>>) response.get("applicants");
        Object totalObj = response.get("totalElements");
        int total = totalObj != null ? ((Number) totalObj).intValue() : 0;
        totalPages = response.get("totalPages") != null ? ((Number) response.get("totalPages")).intValue() : 1;
        
        totalApplicantsLabel.setText(total + " applicants");
        
        if (applicants == null || applicants.isEmpty()) {
            showEmptyState("No applicants yet for this job.");
            return;
        }
        
        for (Map<String, Object> applicant : applicants) {
            applicantsList.getChildren().add(createApplicantCard(applicant));
        }
        
        renderPagination();
    }

    private VBox createApplicantCard(Map<String, Object> applicant) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setStyle("-fx-cursor: hand;");
        
        String applicationId = getString(applicant, "applicationId");
        String name = getString(applicant, "seekerName");
        String email = getString(applicant, "seekerEmail");
        String status = getString(applicant, "status");
        String appliedAt = getString(applicant, "appliedAt");
        String coverLetter = getString(applicant, "coverLetter");
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        VBox avatar = new VBox();
        avatar.setAlignment(Pos.CENTER);
        avatar.setPrefSize(48, 48);
        avatar.setStyle("-fx-background-color: #0F766E; -fx-background-radius: 24;");
        Label initials = new Label(getInitials(name));
        initials.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        avatar.getChildren().add(initials);
        
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        
        Label nameLabel = new Label(name != null ? name : "Unknown");
        nameLabel.getStyleClass().add("h4");
        nameLabel.setStyle("-fx-font-weight: bold;");
        
        Label emailLabel = new Label(email != null ? email : "");
        emailLabel.getStyleClass().add("label-secondary");
        
        info.getChildren().addAll(nameLabel, emailLabel);
        
        Label statusBadge = createStatusBadge(status);
        
        header.getChildren().addAll(avatar, info, statusBadge);
        
        HBox meta = new HBox(16);
        meta.setAlignment(Pos.CENTER_LEFT);
        
        if (appliedAt != null) {
            Label dateLabel = new Label("Applied: " + formatDate(appliedAt));
            dateLabel.getStyleClass().add("label-muted");
            meta.getChildren().add(dateLabel);
        }
        
        if (coverLetter != null && !coverLetter.isEmpty()) {
            Label hasLetter = new Label("📝 Has cover letter");
            hasLetter.getStyleClass().add("label-muted");
            meta.getChildren().add(hasLetter);
        }
        
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button viewBtn = new Button("View Details");
        viewBtn.getStyleClass().add("button-secondary");
        viewBtn.setOnAction(e -> {
            e.consume();
            router.navigate(Route.POSTER_APPLICATION_DETAIL, Map.of("applicationId", applicationId));
        });
        
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("PENDING", "REVIEWING", "SHORTLISTED", "INTERVIEWED", "OFFERED", "HIRED", "REJECTED");
        statusCombo.setValue(status);
        statusCombo.setOnAction(e -> {
            e.consume();
            String newStatus = statusCombo.getValue();
            if (newStatus != null && !newStatus.equals(status)) {
                updateStatus(applicationId, newStatus);
            }
        });
        
        actions.getChildren().addAll(statusCombo, viewBtn);
        
        card.getChildren().addAll(header, meta, actions);
        
        card.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof Button || e.getTarget() instanceof ComboBox) return;
            router.navigate(Route.POSTER_APPLICATION_DETAIL, Map.of("applicationId", applicationId));
        });
        
        return card;
    }

    private Label createStatusBadge(String status) {
        Label badge = new Label(status != null ? status : "UNKNOWN");
        badge.getStyleClass().add("badge");
        
        String bgColor = switch (status != null ? status.toUpperCase() : "") {
            case "PENDING" -> "#F59E0B";
            case "REVIEWING" -> "#3B82F6";
            case "SHORTLISTED" -> "#8B5CF6";
            case "INTERVIEWED" -> "#06B6D4";
            case "OFFERED" -> "#10B981";
            case "HIRED" -> "#059669";
            case "REJECTED" -> "#EF4444";
            default -> "#6B7280";
        };
        
        badge.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4;");
        return badge;
    }

    private void updateStatus(String applicationId, String newStatus) {
        LoadingOverlay.show("Updating status...");
        
        com.jobos.shared.dto.application.ApplicationStatusUpdateRequest request = new com.jobos.shared.dto.application.ApplicationStatusUpdateRequest();
        request.setStatus(newStatus);
        
        jobPostService.updateApplicationStatus(applicationId, request)
            .thenAccept(response -> Platform.runLater(() -> {
                LoadingOverlay.hide();
                Toast.success("Status updated to " + newStatus);
                loadApplicants();
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    LoadingOverlay.hide();
                    Toast.error("Failed to update status");
                    loadApplicants();
                });
                return null;
            });
    }

    private void showEmptyState(String message) {
        applicantsList.getChildren().clear();
        
        VBox emptyState = new VBox(16);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(48));
        
        Label icon = new Label("👥");
        icon.setStyle("-fx-font-size: 48px;");
        
        Label title = new Label("No applicants yet");
        title.getStyleClass().add("h3");
        
        Label desc = new Label(message);
        desc.getStyleClass().add("label-secondary");
        desc.setWrapText(true);
        
        emptyState.getChildren().addAll(icon, title, desc);
        applicantsList.getChildren().add(emptyState);
    }

    private void renderPagination() {
        if (paginationContainer == null) return;
        paginationContainer.getChildren().clear();
        
        if (totalPages <= 1) return;
        
        Button prevBtn = new Button("← Previous");
        prevBtn.getStyleClass().add("button-secondary");
        prevBtn.setDisable(currentPage == 0);
        prevBtn.setOnAction(e -> {
            currentPage--;
            loadApplicants();
        });
        
        Label pageLabel = new Label("Page " + (currentPage + 1) + " of " + totalPages);
        pageLabel.getStyleClass().add("label-secondary");
        
        Button nextBtn = new Button("Next →");
        nextBtn.getStyleClass().add("button-secondary");
        nextBtn.setDisable(currentPage >= totalPages - 1);
        nextBtn.setOnAction(e -> {
            currentPage++;
            loadApplicants();
        });
        
        paginationContainer.getChildren().addAll(prevBtn, pageLabel, nextBtn);
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.split(" ");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private String formatDate(String dateStr) {
        try {
            LocalDateTime dt = LocalDateTime.parse(dateStr.substring(0, 19));
            return dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        } catch (Exception e) {
            return dateStr;
        }
    }
}
