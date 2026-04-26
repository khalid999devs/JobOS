package com.jobos.desktop.controller.poster;

import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.ui.Dialogs;
import com.jobos.desktop.core.ui.SkeletonLoader;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.JobPostService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class JobPostsController implements Initializable {

    @FXML private VBox jobsList;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label totalJobsLabel;
    @FXML private HBox paginationContainer;
    
    private final JobPostService jobPostService = new JobPostService();
    private final Router router = Router.getInstance();
    
    private int currentPage = 0;
    private int totalPages = 1;
    private String selectedStatus = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupStatusFilter();
        loadJobPosts();
    }

    private void setupStatusFilter() {
        if (statusFilter != null) {
            statusFilter.getItems().addAll("All", "ACTIVE", "CLOSED", "DRAFT");
            statusFilter.setValue("All");
            statusFilter.setOnAction(e -> {
                String value = statusFilter.getValue();
                selectedStatus = "All".equals(value) ? null : value;
                currentPage = 0;
                loadJobPosts();
            });
        }
    }

    @FXML
    private void onCreateJob() {
        router.navigate(Route.POSTER_JOB_FORM);
    }

    private void loadJobPosts() {
        jobsList.getChildren().clear();
        jobsList.getChildren().add(SkeletonLoader.createSkeletonList(4));
        
        jobPostService.getMyJobPosts(currentPage, 10, selectedStatus)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    renderJobPosts(response);
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    showEmptyState("Failed to load job posts. Please try again.");
                });
                return null;
            });
    }

    @SuppressWarnings("unchecked")
    private void renderJobPosts(Map<String, Object> response) {
        jobsList.getChildren().clear();
        
        List<Map<String, Object>> jobs = (List<Map<String, Object>>) response.get("jobs");
        Object totalObj = response.get("totalElements");
        int total = totalObj != null ? ((Number) totalObj).intValue() : 0;
        totalPages = response.get("totalPages") != null ? ((Number) response.get("totalPages")).intValue() : 1;
        
        if (totalJobsLabel != null) {
            totalJobsLabel.setText(total + " job posts");
        }
        
        if (jobs == null || jobs.isEmpty()) {
            showEmptyState("No job posts yet. Create your first job post to start hiring!");
            return;
        }
        
        for (Map<String, Object> job : jobs) {
            jobsList.getChildren().add(createJobCard(job));
        }
        
        renderPagination();
    }

    private VBox createJobCard(Map<String, Object> job) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setStyle("-fx-cursor: hand;");
        
        String id = getString(job, "id");
        String title = getString(job, "title");
        String status = getString(job, "status");
        String location = getString(job, "location");
        String jobType = getString(job, "jobType");
        Object appCount = job.get("applicationCount");
        String createdAt = getString(job, "createdAt");
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label(title != null ? title : "Untitled Job");
        titleLabel.getStyleClass().add("h4");
        titleLabel.setStyle("-fx-font-weight: bold;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        
        Label statusBadge = createStatusBadge(status);
        
        header.getChildren().addAll(titleLabel, statusBadge);
        
        HBox meta = new HBox(16);
        meta.setAlignment(Pos.CENTER_LEFT);
        
        if (location != null && !location.isEmpty()) {
            HBox locBox = new HBox(4);
            locBox.setAlignment(Pos.CENTER_LEFT);
            FontIcon locIcon = new FontIcon("fas-map-marker-alt");
            locIcon.setIconSize(12);
            locIcon.setIconColor(javafx.scene.paint.Color.web("#6B7280"));
            Label locLabel = new Label(location);
            locLabel.getStyleClass().add("label-secondary");
            locBox.getChildren().addAll(locIcon, locLabel);
            meta.getChildren().add(locBox);
        }
        
        if (jobType != null && !jobType.isEmpty()) {
            HBox typeBox = new HBox(4);
            typeBox.setAlignment(Pos.CENTER_LEFT);
            FontIcon typeIcon = new FontIcon("fas-briefcase");
            typeIcon.setIconSize(12);
            typeIcon.setIconColor(javafx.scene.paint.Color.web("#6B7280"));
            Label typeLabel = new Label(formatJobType(jobType));
            typeLabel.getStyleClass().add("label-secondary");
            typeBox.getChildren().addAll(typeIcon, typeLabel);
            meta.getChildren().add(typeBox);
        }
        
        HBox appBox = new HBox(4);
        appBox.setAlignment(Pos.CENTER_LEFT);
        FontIcon appIcon = new FontIcon("fas-users");
        appIcon.setIconSize(12);
        appIcon.setIconColor(javafx.scene.paint.Color.web("#6B7280"));
        Label applicantsLabel = new Label((appCount != null ? appCount : "0") + " applicants");
        applicantsLabel.getStyleClass().add("label-secondary");
        appBox.getChildren().addAll(appIcon, applicantsLabel);
        meta.getChildren().add(appBox);
        
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);
        
        if (createdAt != null) {
            Label dateLabel = new Label("Posted: " + formatDate(createdAt));
            dateLabel.getStyleClass().add("label-muted");
            HBox.setHgrow(dateLabel, Priority.ALWAYS);
            footer.getChildren().add(dateLabel);
        }
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        footer.getChildren().add(spacer);
        
        Button viewApplicantsBtn = new Button("View Applicants");
        viewApplicantsBtn.getStyleClass().add("button-secondary");
        viewApplicantsBtn.setOnAction(e -> {
            e.consume();
            router.navigate(Route.POSTER_APPLICANTS, Map.of("jobId", id));
        });
        
        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("button-secondary");
        editBtn.setOnAction(e -> {
            e.consume();
            router.navigate(Route.POSTER_JOB_FORM, Map.of("jobId", id));
        });
        
        Button statusBtn;
        if ("OPEN".equals(status)) {
            statusBtn = new Button("Close");
            statusBtn.getStyleClass().add("button-danger");
            statusBtn.setOnAction(e -> {
                e.consume();
                closeJob(id);
            });
        } else if ("CLOSED".equals(status)) {
            statusBtn = new Button("Reopen");
            statusBtn.getStyleClass().add("button-primary");
            statusBtn.setOnAction(e -> {
                e.consume();
                reopenJob(id);
            });
        } else {
            statusBtn = null;
        }
        
        footer.getChildren().addAll(viewApplicantsBtn, editBtn);
        if (statusBtn != null) {
            footer.getChildren().add(statusBtn);
        }
        
        card.getChildren().addAll(header, meta, footer);
        
        card.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof Button) return;
            router.navigate(Route.POSTER_JOB_DETAIL, Map.of("jobId", id));
        });
        
        return card;
    }

    private Label createStatusBadge(String status) {
        Label badge = new Label(status != null ? status : "UNKNOWN");
        badge.getStyleClass().add("badge");
        
        if ("OPEN".equalsIgnoreCase(status)) {
            badge.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4;");
        } else if ("CLOSED".equalsIgnoreCase(status)) {
            badge.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4;");
        } else if ("DRAFT".equalsIgnoreCase(status)) {
            badge.setStyle("-fx-background-color: #6B7280; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4;");
        } else {
            badge.setStyle("-fx-background-color: #9CA3AF; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4;");
        }
        
        return badge;
    }

    private void closeJob(String jobId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Close Job");
        alert.setHeaderText("Close this job posting?");
        alert.setContentText("This will stop accepting new applications. You can reopen it later.");
        
        Optional<ButtonType> result = Dialogs.prepare(alert).showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Toast.info("Closing job...");
            jobPostService.closeJob(jobId)
                .thenAccept(r -> Platform.runLater(() -> {
                    Toast.success("Job closed successfully");
                    loadJobPosts();
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        Toast.error("Failed to close job");
                    });
                    return null;
                });
        }
    }

    private void reopenJob(String jobId) {
        Toast.info("Reopening job...");
        jobPostService.reopenJob(jobId)
            .thenAccept(r -> Platform.runLater(() -> {
                Toast.success("Job reopened successfully");
                loadJobPosts();
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    Toast.error("Failed to reopen job");
                });
                return null;
            });
    }

    private void showEmptyState(String message) {
        jobsList.getChildren().clear();
        
        VBox emptyState = new VBox(16);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(48));
        
        Label icon = new Label("📝");
        icon.setStyle("-fx-font-size: 48px;");
        
        Label title = new Label("No job posts yet");
        title.getStyleClass().add("h3");
        
        Label desc = new Label(message);
        desc.getStyleClass().add("label-secondary");
        desc.setWrapText(true);
        
        Button createBtn = new Button("+ Create New Job");
        createBtn.getStyleClass().add("button-primary");
        createBtn.setOnAction(e -> onCreateJob());
        
        emptyState.getChildren().addAll(icon, title, desc, createBtn);
        jobsList.getChildren().add(emptyState);
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
            loadJobPosts();
        });
        
        Label pageLabel = new Label("Page " + (currentPage + 1) + " of " + totalPages);
        pageLabel.getStyleClass().add("label-secondary");
        
        Button nextBtn = new Button("Next →");
        nextBtn.getStyleClass().add("button-secondary");
        nextBtn.setDisable(currentPage >= totalPages - 1);
        nextBtn.setOnAction(e -> {
            currentPage++;
            loadJobPosts();
        });
        
        paginationContainer.getChildren().addAll(prevBtn, pageLabel, nextBtn);
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private String formatJobType(String type) {
        if (type == null) return "";
        return type.replace("_", " ").toLowerCase();
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
