package com.jobos.desktop.controller.seeker;

import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.JobService;
import com.jobos.shared.dto.job.JobListResponse;
import com.jobos.shared.dto.job.JobSearchResponse;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SeekerJobsController implements Initializable {

    @FXML private TextField searchField;
    @FXML private TextField locationField;
    @FXML private ComboBox<String> workModeCombo;
    @FXML private ComboBox<String> jobTypeCombo;
    @FXML private ComboBox<String> experienceCombo;
    @FXML private ComboBox<String> salaryRangeCombo;
    @FXML private VBox jobsList;
    @FXML private VBox loadingContainer;
    @FXML private VBox emptyContainer;
    @FXML private Label resultsLabel;
    @FXML private Label pageLabel;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private HBox paginationContainer;

    private final JobService jobService = new JobService();
    private int currentPage = 0;
    private int totalPages = 0;
    private static final int PAGE_SIZE = 15;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupFilters();
        loadJobs();
    }

    private void setupFilters() {
        workModeCombo.setItems(FXCollections.observableArrayList(
                "Remote", "On-site", "Hybrid"
        ));
        jobTypeCombo.setItems(FXCollections.observableArrayList(
                "Full-time", "Part-time", "Contract", "Internship", "Freelance"
        ));
        experienceCombo.setItems(FXCollections.observableArrayList(
                "Entry Level", "Mid Level", "Senior Level", "Lead", "Executive"
        ));
        salaryRangeCombo.setItems(FXCollections.observableArrayList(
                "$0 - $30k", "$30k - $50k", "$50k - $80k", "$80k - $100k", 
                "$100k - $150k", "$150k - $200k", "$200k+"
        ));
        
        searchField.setOnAction(e -> onSearch());
        locationField.setOnAction(e -> onSearch());
    }

    @FXML
    private void onSearch() {
        currentPage = 0;
        loadJobs();
    }

    @FXML
    private void onClearFilters() {
        searchField.clear();
        locationField.clear();
        workModeCombo.getSelectionModel().clearSelection();
        jobTypeCombo.getSelectionModel().clearSelection();
        experienceCombo.getSelectionModel().clearSelection();
        salaryRangeCombo.getSelectionModel().clearSelection();
        currentPage = 0;
        loadJobs();
    }

    @FXML
    private void onSavedJobs() {
        Toast.info("Saved jobs feature coming soon");
    }

    @FXML
    private void onPrevPage() {
        if (currentPage > 0) {
            currentPage--;
            loadJobs();
        }
    }

    @FXML
    private void onNextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++;
            loadJobs();
        }
    }

    private void loadJobs() {
        showLoading(true);
        
        String keywords = searchField.getText().trim();
        String location = locationField.getText().trim();
        
        List<String> workModes = new ArrayList<>();
        if (workModeCombo.getValue() != null) {
            String workMode = workModeCombo.getValue();
            if ("Remote".equals(workMode)) workModes.add("REMOTE");
            else if ("On-site".equals(workMode)) workModes.add("ONSITE");
            else if ("Hybrid".equals(workMode)) workModes.add("HYBRID");
        }
        
        List<String> jobTypes = new ArrayList<>();
        if (jobTypeCombo.getValue() != null) {
            jobTypes.add(jobTypeCombo.getValue().toUpperCase().replace("-", "_").replace(" ", "_"));
        }
        
        List<String> experienceLevels = new ArrayList<>();
        if (experienceCombo.getValue() != null) {
            String exp = experienceCombo.getValue();
            if ("Entry Level".equals(exp)) experienceLevels.add("ENTRY");
            else if ("Mid Level".equals(exp)) experienceLevels.add("MID");
            else if ("Senior Level".equals(exp)) experienceLevels.add("SENIOR");
            else if ("Lead".equals(exp)) experienceLevels.add("LEAD");
            else if ("Executive".equals(exp)) experienceLevels.add("EXECUTIVE");
        }
        
        Integer salaryMin = null;
        Integer salaryMax = null;
        if (salaryRangeCombo.getValue() != null) {
            String range = salaryRangeCombo.getValue();
            int[] salaryRange = parseSalaryRange(range);
            salaryMin = salaryRange[0] >= 0 ? salaryRange[0] : null;
            salaryMax = salaryRange[1] >= 0 ? salaryRange[1] : null;
        }

        jobService.searchJobs(
                keywords.isEmpty() ? null : keywords,
                location.isEmpty() ? null : location,
                workModes.isEmpty() ? null : workModes,
                jobTypes.isEmpty() ? null : jobTypes,
                experienceLevels.isEmpty() ? null : experienceLevels,
                salaryMin,
                salaryMax,
                currentPage,
                PAGE_SIZE
        ).whenComplete((response, error) -> {
            Platform.runLater(() -> {
                showLoading(false);
                if (error != null) {
                    Toast.error("Failed to load jobs");
                    showEmpty(true);
                    return;
                }
                renderJobs(response);
            });
        });
    }
    
    private int[] parseSalaryRange(String range) {
        // Returns [min, max], where null is represented as -1
        return switch (range) {
            case "$0 - $30k" -> new int[]{0, 30000};
            case "$30k - $50k" -> new int[]{30000, 50000};
            case "$50k - $80k" -> new int[]{50000, 80000};
            case "$80k - $100k" -> new int[]{80000, 100000};
            case "$100k - $150k" -> new int[]{100000, 150000};
            case "$150k - $200k" -> new int[]{150000, 200000};
            case "$200k+" -> new int[]{200000, -1};
            default -> new int[]{-1, -1};
        };
    }

    private void renderJobs(JobSearchResponse response) {
        jobsList.getChildren().clear();
        
        if (response == null || response.getJobs() == null || response.getJobs().isEmpty()) {
            showEmpty(true);
            resultsLabel.setText("No jobs found");
            updatePagination(0, 0);
            return;
        }

        showEmpty(false);
        totalPages = response.getTotalPages() != null ? response.getTotalPages() : 1;
        long totalElements = response.getTotalElements() != null ? response.getTotalElements() : response.getJobs().size();
        
        resultsLabel.setText(totalElements + " jobs found");
        updatePagination(currentPage + 1, totalPages);

        for (JobListResponse job : response.getJobs()) {
            jobsList.getChildren().add(createJobCard(job));
        }
    }

    private VBox createJobCard(JobListResponse job) {
        VBox card = new VBox(10);
        card.getStyleClass().add("job-card");
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 10; -fx-cursor: hand;");
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        StackPane companyIcon = new StackPane();
        companyIcon.setStyle("-fx-background-color: #F0FDFA; -fx-background-radius: 8; -fx-min-width: 44; -fx-min-height: 44;");
        FontIcon icon = new FontIcon("fas-building");
        icon.setIconSize(18);
        icon.setIconColor(Color.web("#0F766E"));
        companyIcon.getChildren().add(icon);
        
        VBox titleBox = new VBox(2);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        
        Label titleLabel = new Label(job.getTitle());
        titleLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 15px; -fx-font-weight: 600;");
        
        Label companyLabel = new Label(job.getCompany() + (job.getLocation() != null ? " • " + job.getLocation() : ""));
        companyLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
        
        titleBox.getChildren().addAll(titleLabel, companyLabel);
        
        VBox actionBox = new VBox(4);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button saveBtn = new Button();
        saveBtn.setStyle("-fx-background-color: transparent; -fx-padding: 6; -fx-cursor: hand;");
        FontIcon heartIcon = new FontIcon(Boolean.TRUE.equals(job.getIsSaved()) ? "fas-heart" : "far-heart");
        heartIcon.setIconSize(16);
        heartIcon.setIconColor(Boolean.TRUE.equals(job.getIsSaved()) ? Color.web("#EF4444") : Color.web("#9CA3AF"));
        saveBtn.setGraphic(heartIcon);
        saveBtn.setOnAction(e -> {
            e.consume();
            toggleSaveJob(job, heartIcon);
        });
        
        Label timeLabel = new Label(formatTimeAgo(job.getCreatedAt()));
        timeLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
        
        actionBox.getChildren().addAll(saveBtn, timeLabel);
        
        header.getChildren().addAll(companyIcon, titleBox, actionBox);
        
        HBox tagsRow = new HBox(8);
        tagsRow.setAlignment(Pos.CENTER_LEFT);
        
        if (job.getJobType() != null) {
            tagsRow.getChildren().add(createTag(formatJobType(job.getJobType()), "#E0F2FE", "#0284C7"));
        }
        if (job.getWorkMode() != null) {
            tagsRow.getChildren().add(createTag(formatWorkMode(job.getWorkMode()), "#DCFCE7", "#16A34A"));
        } else if (Boolean.TRUE.equals(job.getIsRemote())) {
            tagsRow.getChildren().add(createTag("Remote", "#DCFCE7", "#16A34A"));
        }
        if (job.getExperienceLevel() != null) {
            tagsRow.getChildren().add(createTag(formatExperience(job.getExperienceLevel()), "#F3E8FF", "#9333EA"));
        }
        if (job.getSalaryMin() != null || job.getSalaryMax() != null) {
            String salary = formatSalary(job.getSalaryMin(), job.getSalaryMax(), job.getSalaryCurrency());
            tagsRow.getChildren().add(createTag(salary, "#FEF3C7", "#D97706"));
        }
        if (Boolean.TRUE.equals(job.getHasApplied())) {
            tagsRow.getChildren().add(createTag("Applied", "#DBEAFE", "#2563EB"));
        }
        
        card.getChildren().addAll(header, tagsRow);
        
        card.setOnMouseClicked(e -> openJobDetail(job.getId()));
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 10; -fx-border-color: #0F766E; -fx-border-width: 1; -fx-border-radius: 10; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 10; -fx-cursor: hand;"));
        
        return card;
    }

    private HBox createTag(String text, String bgColor, String textColor) {
        HBox tag = new HBox();
        tag.setAlignment(Pos.CENTER);
        tag.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 4; -fx-padding: 3 8;");
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 11px; -fx-font-weight: 500;");
        tag.getChildren().add(label);
        return tag;
    }

    private void toggleSaveJob(JobListResponse job, FontIcon heartIcon) {
        boolean isSaved = "fas-heart".equals(heartIcon.getIconLiteral());
        
        if (isSaved) {
            jobService.unsaveJob(job.getId()).whenComplete((v, err) -> {
                Platform.runLater(() -> {
                    if (err == null) {
                        heartIcon.setIconLiteral("far-heart");
                        heartIcon.setIconColor(Color.web("#9CA3AF"));
                        Toast.success("Job removed from saved");
                    }
                });
            });
        } else {
            jobService.saveJob(job.getId()).whenComplete((v, err) -> {
                Platform.runLater(() -> {
                    if (err == null) {
                        heartIcon.setIconLiteral("fas-heart");
                        heartIcon.setIconColor(Color.web("#EF4444"));
                        Toast.success("Job saved");
                    }
                });
            });
        }
    }

    private void openJobDetail(String jobId) {
        Router.getInstance().navigate(Route.SEEKER_JOB_DETAIL, jobId);
    }

    private void showLoading(boolean show) {
        loadingContainer.setVisible(show);
        loadingContainer.setManaged(show);
    }

    private void showEmpty(boolean show) {
        emptyContainer.setVisible(show);
        emptyContainer.setManaged(show);
    }

    private void updatePagination(int current, int total) {
        if (total <= 1) {
            paginationContainer.setVisible(false);
            paginationContainer.setManaged(false);
            return;
        }
        paginationContainer.setVisible(true);
        paginationContainer.setManaged(true);
        pageLabel.setText("Page " + current + " of " + total);
        prevBtn.setDisable(currentPage <= 0);
        nextBtn.setDisable(currentPage >= total - 1);
    }

    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        LocalDateTime now = LocalDateTime.now();
        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days == 0) return "Today";
        if (days == 1) return "Yesterday";
        if (days < 7) return days + "d ago";
        if (days < 30) return (days / 7) + "w ago";
        return dateTime.format(DateTimeFormatter.ofPattern("MMM d"));
    }

    private String formatJobType(String type) {
        if (type == null) return "";
        return type.replace("_", " ").replace("-", " ")
                .toLowerCase()
                .substring(0, 1).toUpperCase() + 
                type.replace("_", " ").replace("-", " ").toLowerCase().substring(1);
    }

    private String formatWorkMode(String mode) {
        if (mode == null) return "";
        switch (mode.toUpperCase()) {
            case "REMOTE": return "Remote";
            case "ONSITE": return "On-site";
            case "HYBRID": return "Hybrid";
            default: return mode;
        }
    }

    private String formatExperience(String level) {
        if (level == null) return "";
        return level.replace("_", " ").replace("-", " ")
                .toLowerCase()
                .substring(0, 1).toUpperCase() + 
                level.replace("_", " ").replace("-", " ").toLowerCase().substring(1);
    }

    private String formatSalary(Integer min, Integer max, String currency) {
        String currSymbol = getCurrencySymbol(currency);
        if (min != null && max != null) {
            return currSymbol + formatNumber(min) + " - " + currSymbol + formatNumber(max);
        } else if (min != null) {
            return currSymbol + formatNumber(min) + "+";
        } else if (max != null) {
            return "Up to " + currSymbol + formatNumber(max);
        }
        return "";
    }
    
    private String getCurrencySymbol(String currency) {
        if (currency == null) return "$";
        return switch (currency.toUpperCase()) {
            case "USD" -> "$";
            case "EUR" -> "€";
            case "GBP" -> "£";
            case "BDT" -> "৳";
            case "INR" -> "₹";
            case "JPY", "CNY" -> "¥";
            default -> currency + " ";
        };
    }

    private String formatNumber(int num) {
        if (num >= 1000000) {
            return String.format("%.1fM", num / 1000000.0);
        }
        if (num >= 1000) {
            return (num / 1000) + "k";
        }
        return String.valueOf(num);
    }
}
