package com.jobos.desktop.controller.poster;

import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.session.SessionManager;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.JobPostService;
import com.jobos.shared.dto.job.JobPostResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class PosterJobDetailController implements Initializable {

    @FXML private VBox contentContainer;
    @FXML private VBox loadingContainer;
    @FXML private Label statusLabel;
    @FXML private Label titleLabel;
    @FXML private Label companyLabel;
    @FXML private Label locationLabel;
    @FXML private Label jobTypeLabel;
    @FXML private Label postedLabel;
    @FXML private Label salaryLabel;
    @FXML private Label viewsLabel;
    @FXML private Label applicationsLabel;
    @FXML private Label shortlistedLabel;
    @FXML private Label applicantsCountLabel;
    @FXML private VBox descriptionContent;
    @FXML private VBox requirementsContent;
    @FXML private VBox responsibilitiesContent;
    @FXML private VBox benefitsContent;
    @FXML private FlowPane tagsContainer;
    @FXML private FlowPane skillsContainer;
    @FXML private VBox requirementsSection;
    @FXML private VBox responsibilitiesSection;
    @FXML private VBox skillsSection;
    @FXML private VBox benefitsSection;

    private final JobPostService jobPostService = new JobPostService();
    private final Router router = Router.getInstance();
    private JobPostResponse currentJob;
    private String jobId;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        jobId = router.getParam("jobId");
        if (jobId != null) {
            loadJobDetails(jobId);
        } else {
            Toast.error("Job not found");
            onBack();
        }
    }

    private void loadJobDetails(String jobId) {
        showLoading(true);
        
        jobPostService.getJobPostById(jobId)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    showLoading(false);
                    if (response != null) {
                        currentJob = response;
                        renderJob(response);
                        loadShortlistedCount();
                    } else {
                        Toast.error("Job not found");
                        onBack();
                    }
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    showLoading(false);
                    Toast.error("Failed to load job details");
                    onBack();
                });
                return null;
            });
    }

    private void loadShortlistedCount() {
        if (jobId == null) return;
        
        jobPostService.getJobApplicants(jobId, 0, 100, "SHORTLISTED")
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    if (response != null) {
                        Object totalObj = response.get("totalElements");
                        int count = totalObj != null ? ((Number) totalObj).intValue() : 0;
                        shortlistedLabel.setText(String.valueOf(count));
                    }
                });
            })
            .exceptionally(e -> null);
    }

    private void renderJob(JobPostResponse job) {
        titleLabel.setText(job.getTitle());
        
        var profile = SessionManager.getInstance().getProfile();
        if (profile != null && profile.getPosterProfile() != null && profile.getPosterProfile().getCompanyName() != null) {
            companyLabel.setText(profile.getPosterProfile().getCompanyName());
        } else {
            companyLabel.setText(job.getCompany() != null ? job.getCompany() : "Your Company");
        }
        
        locationLabel.setText(job.getLocation() != null ? job.getLocation() : "Not specified");
        jobTypeLabel.setText(formatJobType(job.getJobType()));
        postedLabel.setText("Posted " + formatTimeAgo(job.getCreatedAt()));
        
        String status = job.getStatus() != null ? job.getStatus() : "ACTIVE";
        statusLabel.setText(status);
        String bgColor = switch (status.toUpperCase()) {
            case "OPEN", "ACTIVE" -> "#10B981";
            case "PAUSED" -> "#F59E0B";
            case "CLOSED", "EXPIRED" -> "#EF4444";
            default -> "#6B7280";
        };
        statusLabel.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: white; -fx-padding: 6 12; -fx-background-radius: 4; -fx-font-weight: 600;");
        
        viewsLabel.setText(job.getViewCount() != null ? job.getViewCount().toString() : "0");
        applicationsLabel.setText(job.getApplicationCount() != null ? job.getApplicationCount().toString() : "0");
        shortlistedLabel.setText("0");
        
        int appCount = job.getApplicationCount() != null ? job.getApplicationCount() : 0;
        applicantsCountLabel.setText("View Applicants (" + appCount + ")");
        
        if (job.getSalaryMin() != null || job.getSalaryMax() != null) {
            salaryLabel.setText(formatSalary(job.getSalaryMin(), job.getSalaryMax(), job.getSalaryCurrency()));
        } else {
            salaryLabel.setText("Competitive");
        }
        
        renderTextContent(descriptionContent, job.getDescription() != null ? job.getDescription() : "No description provided.");
        
        if (job.getRequirements() != null && !job.getRequirements().isEmpty()) {
            renderTextContent(requirementsContent, job.getRequirements());
            requirementsSection.setVisible(true);
            requirementsSection.setManaged(true);
        } else {
            requirementsSection.setVisible(false);
            requirementsSection.setManaged(false);
        }
        
        if (job.getResponsibilities() != null && !job.getResponsibilities().isEmpty()) {
            renderTextContent(responsibilitiesContent, job.getResponsibilities());
            responsibilitiesSection.setVisible(true);
            responsibilitiesSection.setManaged(true);
        } else {
            responsibilitiesSection.setVisible(false);
            responsibilitiesSection.setManaged(false);
        }
        
        if (job.getBenefits() != null && !job.getBenefits().isEmpty()) {
            renderTextContent(benefitsContent, job.getBenefits());
            benefitsSection.setVisible(true);
            benefitsSection.setManaged(true);
        } else {
            benefitsSection.setVisible(false);
            benefitsSection.setManaged(false);
        }
        
        renderTags(job);
        renderSkills(job.getSkills());
    }

    private void renderTags(JobPostResponse job) {
        tagsContainer.getChildren().clear();
        
        if (job.getJobType() != null) {
            tagsContainer.getChildren().add(createTag(formatJobType(job.getJobType()), "#E0F2FE", "#0284C7"));
        }
        if (Boolean.TRUE.equals(job.getIsRemote())) {
            tagsContainer.getChildren().add(createTag("Remote", "#DCFCE7", "#16A34A"));
        }
        if (job.getExperienceLevel() != null) {
            tagsContainer.getChildren().add(createTag(formatExperience(job.getExperienceLevel()), "#F3E8FF", "#9333EA"));
        }
        if (job.getApplicationDeadline() != null) {
            tagsContainer.getChildren().add(createTag("Deadline: " + job.getApplicationDeadline().format(DateTimeFormatter.ofPattern("MMM d, yyyy")), "#FEF3C7", "#D97706"));
        }
    }

    private void renderSkills(List<String> skills) {
        skillsContainer.getChildren().clear();
        
        if (skills == null || skills.isEmpty()) {
            skillsSection.setVisible(false);
            skillsSection.setManaged(false);
            return;
        }
        
        skillsSection.setVisible(true);
        skillsSection.setManaged(true);
        
        for (String skill : skills) {
            skillsContainer.getChildren().add(createSkillTag(skill));
        }
    }

    private HBox createTag(String text, String bgColor, String textColor) {
        HBox tag = new HBox();
        tag.setAlignment(Pos.CENTER);
        tag.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 6; -fx-padding: 6 12;");
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 12px; -fx-font-weight: 500;");
        tag.getChildren().add(label);
        return tag;
    }

    private HBox createSkillTag(String skill) {
        HBox tag = new HBox();
        tag.setAlignment(Pos.CENTER);
        tag.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 6; -fx-padding: 6 12;");
        Label label = new Label(skill);
        label.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px;");
        tag.getChildren().add(label);
        return tag;
    }

    @FXML
    private void onBack() {
        router.navigate(Route.POSTER_JOB_POSTS);
    }

    @FXML
    private void onViewApplicants() {
        if (currentJob != null) {
            router.navigate(Route.POSTER_APPLICANTS, java.util.Map.of("jobId", currentJob.getId()));
        }
    }

    @FXML
    private void onEditJob() {
        if (currentJob != null) {
            router.navigate(Route.POSTER_JOB_FORM, java.util.Map.of("jobId", currentJob.getId()));
        }
    }

    private void showLoading(boolean show) {
        loadingContainer.setVisible(show);
        loadingContainer.setManaged(show);
        contentContainer.setVisible(!show);
        contentContainer.setManaged(!show);
    }

    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "recently";
        LocalDateTime now = LocalDateTime.now();
        long days = ChronoUnit.DAYS.between(dateTime, now);
        if (days == 0) return "today";
        if (days == 1) return "yesterday";
        if (days < 7) return days + " days ago";
        if (days < 30) return (days / 7) + " weeks ago";
        return dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }

    private String formatJobType(String type) {
        if (type == null) return "Full-time";
        return switch (type.toUpperCase()) {
            case "FULL_TIME", "FULLTIME" -> "Full-time";
            case "PART_TIME", "PARTTIME" -> "Part-time";
            case "CONTRACT" -> "Contract";
            case "INTERNSHIP" -> "Internship";
            case "FREELANCE" -> "Freelance";
            default -> type;
        };
    }

    private String formatExperience(String level) {
        if (level == null) return "";
        return switch (level.toUpperCase()) {
            case "ENTRY_LEVEL", "ENTRY" -> "Entry Level";
            case "MID_LEVEL", "MID" -> "Mid Level";
            case "SENIOR_LEVEL", "SENIOR" -> "Senior Level";
            case "LEAD" -> "Lead";
            case "EXECUTIVE" -> "Executive";
            default -> level;
        };
    }

    private String formatSalary(Integer min, Integer max, String currency) {
        String currSymbol = getCurrencySymbol(currency);
        NumberFormat formatter = NumberFormat.getNumberInstance(Locale.US);
        
        if (min != null && max != null) {
            return currSymbol + formatter.format(min) + " - " + currSymbol + formatter.format(max) + " / year";
        } else if (min != null) {
            return currSymbol + formatter.format(min) + "+ / year";
        } else if (max != null) {
            return "Up to " + currSymbol + formatter.format(max) + " / year";
        }
        return "Competitive";
    }
    
    private String getCurrencySymbol(String currency) {
        if (currency == null) return "$";
        return switch (currency.toUpperCase()) {
            case "USD" -> "$";
            case "EUR" -> "€";
            case "GBP" -> "£";
            case "BDT" -> "৳";
            case "INR" -> "₹";
            case "JPY" -> "¥";
            case "CNY" -> "¥";
            case "CAD" -> "CA$";
            case "AUD" -> "A$";
            default -> currency + " ";
        };
    }
    
    private void renderTextContent(VBox container, String text) {
        container.getChildren().clear();
        
        if (text == null || text.isBlank()) {
            Label emptyLabel = new Label("No content provided");
            emptyLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-style: italic;");
            container.getChildren().add(emptyLabel);
            return;
        }
        
        String[] lines = text.split("\n");
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            if (trimmed.isEmpty()) {
                Label spacer = new Label("");
                spacer.setStyle("-fx-padding: 4 0 0 0;");
                container.getChildren().add(spacer);
                continue;
            }
            
            Label lineLabel = new Label(line);
            lineLabel.setWrapText(true);
            lineLabel.setMaxWidth(Double.MAX_VALUE);
            
            if (trimmed.startsWith("#")) {
                lineLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 16px; -fx-font-weight: 700; -fx-padding: 8 0 4 0;");
                lineLabel.setText(trimmed.substring(1).trim());
            } else if (trimmed.startsWith("-") || trimmed.startsWith("•") || trimmed.startsWith("*")) {
                lineLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 14px; -fx-padding: 2 0 2 16;");
                if (!trimmed.startsWith("•")) {
                    lineLabel.setText("• " + trimmed.substring(1).trim());
                }
            } else if (trimmed.matches("^\\d+\\.\\.+")) {
                lineLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 14px; -fx-padding: 2 0 2 16;");
            } else {
                lineLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 14px; -fx-line-spacing: 1.5; -fx-padding: 2 0 2 0;");
            }
            
            container.getChildren().add(lineLabel);
        }
    }
}
