package com.jobos.desktop.controller.seeker;

import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.JobService;
import com.jobos.shared.dto.job.JobPostResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class JobDetailController implements Initializable {

    @FXML private VBox contentContainer;
    @FXML private VBox loadingContainer;
    @FXML private Label titleLabel;
    @FXML private Label companyLabel;
    @FXML private Label locationLabel;
    @FXML private Label jobTypeLabel;
    @FXML private Label postedLabel;
    @FXML private Label salaryLabel;
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
    @FXML private FontIcon saveIcon;
    @FXML private Button applyBtn;
    @FXML private Label applyBtnLabel;

    private final JobService jobService = new JobService();
    private JobPostResponse currentJob;
    private boolean isSaved = false;
    private boolean hasApplied = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        String jobId = Router.getInstance().getParam("id");
        if (jobId != null) {
            loadJobDetails(jobId);
        } else {
            Toast.error("Job not found");
            onBack();
        }
    }

    private void loadJobDetails(String jobId) {
        showLoading(true);
        
        jobService.getJobById(jobId).whenComplete((job, error) -> {
            Platform.runLater(() -> {
                showLoading(false);
                if (error != null) {
                    Toast.error("Failed to load job details");
                    onBack();
                    return;
                }
                currentJob = job;
                renderJob(job);
            });
        });
    }

    private void renderJob(JobPostResponse job) {
        titleLabel.setText(job.getTitle());
        companyLabel.setText(job.getCompany());
        locationLabel.setText(job.getLocation() != null ? job.getLocation() : "Not specified");
        jobTypeLabel.setText(formatJobType(job.getJobType()));
        postedLabel.setText("Posted " + formatTimeAgo(job.getCreatedAt()));
        
        if (job.getSalaryMin() != null || job.getSalaryMax() != null) {
            salaryLabel.setText(formatSalary(job.getSalaryMin(), job.getSalaryMax(), job.getSalaryCurrency()));
        } else {
            salaryLabel.setText("Competitive");
        }
        
        // Render text content
        String description = job.getDescription();
        renderTextContent(descriptionContent, description != null && !description.isBlank() ? description : "No description provided for this job posting.");
        
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
        
        isSaved = Boolean.TRUE.equals(job.getIsSaved());
        updateSaveIcon();
        
        hasApplied = Boolean.TRUE.equals(job.getHasApplied());
        updateApplyButton();
    }
    
    private void updateApplyButton() {
        if (hasApplied) {
            applyBtn.setDisable(true);
            // Better visual: gray background with clear text (more opacity)
            applyBtn.setStyle("-fx-background-color: #D1D5DB; -fx-min-width: 160; -fx-pref-height: 42; -fx-opacity: 0.9;");
            applyBtnLabel.setText("Already Applied");
            applyBtnLabel.setStyle("-fx-text-fill: #4B5563; -fx-font-weight: 600;");
        } else {
            applyBtn.setDisable(false);
            applyBtn.setStyle("-fx-min-width: 160; -fx-pref-height: 42;");
            applyBtnLabel.setText("Apply Now");
            applyBtnLabel.setStyle("-fx-text-fill: white; -fx-font-weight: 600;");
        }
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
        Router.getInstance().navigate(Route.SEEKER_JOBS);
    }

    @FXML
    private void onApply() {
        if (currentJob != null) {
            Router.getInstance().navigate(Route.SEEKER_APPLY, currentJob.getId());
        }
    }

    @FXML
    private void onSave() {
        if (currentJob == null) return;
        
        if (isSaved) {
            jobService.unsaveJob(currentJob.getId()).whenComplete((v, err) -> {
                Platform.runLater(() -> {
                    if (err == null) {
                        isSaved = false;
                        updateSaveIcon();
                        Toast.success("Job removed from saved");
                    }
                });
            });
        } else {
            jobService.saveJob(currentJob.getId()).whenComplete((v, err) -> {
                Platform.runLater(() -> {
                    if (err == null) {
                        isSaved = true;
                        updateSaveIcon();
                        Toast.success("Job saved");
                    }
                });
            });
        }
    }

    @FXML
    private void onShare() {
        Toast.info("Share feature coming soon");
    }

    private void updateSaveIcon() {
        saveIcon.setIconLiteral(isSaved ? "fas-heart" : "far-heart");
        saveIcon.setIconColor(isSaved ? Color.web("#EF4444") : Color.web("#9CA3AF"));
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
        
        // Split by lines and render with basic formatting
        String[] lines = text.split("\n");
        
        for (String line : lines) {
            String trimmed = line.trim();
            
            if (trimmed.isEmpty()) {
                // Add spacing for empty lines
                Label spacer = new Label("");
                spacer.setStyle("-fx-padding: 4 0 0 0;");
                container.getChildren().add(spacer);
                continue;
            }
            
            Label lineLabel = new Label(line);
            lineLabel.setWrapText(true);
            lineLabel.setMaxWidth(Double.MAX_VALUE);
            
            // Style based on line type
            if (trimmed.startsWith("#")) {
                // Header
                lineLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 16px; -fx-font-weight: 700; -fx-padding: 8 0 4 0;");
                lineLabel.setText(trimmed.substring(1).trim());
            } else if (trimmed.startsWith("-") || trimmed.startsWith("•") || trimmed.startsWith("*")) {
                // Bullet point
                lineLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 14px; -fx-padding: 2 0 2 16;");
                if (!trimmed.startsWith("•")) {
                    lineLabel.setText("• " + trimmed.substring(1).trim());
                }
            } else if (trimmed.matches("^\\d+\\.\\.+")) {
                // Numbered list
                lineLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 14px; -fx-padding: 2 0 2 16;");
            } else {
                // Regular text
                lineLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 14px; -fx-line-spacing: 1.5; -fx-padding: 2 0 2 0;");
            }
            
            container.getChildren().add(lineLabel);
        }
    }
}
