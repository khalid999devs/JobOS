package com.jobos.desktop.controller.seeker;

import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.ApplicationService;
import com.jobos.shared.dto.application.ApplicationResponse;
import com.jobos.shared.dto.common.ApiResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ApplicationDetailController implements Initializable {

    @FXML private Label jobTitleLabel;
    @FXML private Label companyLabel;
    @FXML private Label locationLabel;
    @FXML private HBox statusBadgeContainer;
    @FXML private Label appliedDateLabel;
    @FXML private VBox coverLetterContainer;
    @FXML private Label coverLetterContent;
    @FXML private VBox cvContainer;
    @FXML private Label cvInfoLabel;
    @FXML private VBox answersContainer;
    @FXML private VBox answersContent;
    @FXML private VBox loadingContainer;
    @FXML private StackPane cvPreviewContainer;

    private final ApplicationService applicationService = new ApplicationService();
    private String applicationId;
    private ApplicationResponse application;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        applicationId = Router.getInstance().getParam("id");
        if (applicationId != null) {
            loadApplication();
        }
    }

    private void loadApplication() {
        showLoading(true);
        
        applicationService.getApplicationById(applicationId).whenComplete((response, error) -> {
            Platform.runLater(() -> {
                showLoading(false);
                if (error != null) {
                    Toast.error("Failed to load application");
                    return;
                }
                application = response.getResult();
                populateDetails();
            });
        });
    }

    private void populateDetails() {
        if (application == null) return;
        
        // Job info
        jobTitleLabel.setText(application.getJobTitle());
        companyLabel.setText(application.getCompany() != null ? application.getCompany() : "Company");
        locationLabel.setText(application.getLocation() != null ? application.getLocation() : "Location not specified");
        
        // Status badge
        statusBadgeContainer.getChildren().clear();
        statusBadgeContainer.getChildren().add(createStatusBadge(application.getStatus()));
        
        // Applied date
        if (application.getAppliedAt() != null) {
            appliedDateLabel.setText("Applied on " + formatDateTime(application.getAppliedAt()));
        } else {
            appliedDateLabel.setText("Applied recently");
        }
        
        // Cover letter
        String coverLetter = application.getCoverLetter();
        if (coverLetter != null && !coverLetter.isBlank()) {
            coverLetterContainer.setVisible(true);
            coverLetterContainer.setManaged(true);
            coverLetterContent.setText(coverLetter);
        } else {
            coverLetterContainer.setVisible(false);
            coverLetterContainer.setManaged(false);
        }
        
        // CV info
        String cvUrl = application.getCvFileUrl();
        if (cvUrl != null && !cvUrl.isBlank()) {
            cvContainer.setVisible(true);
            cvContainer.setManaged(true);
            cvInfoLabel.setText("CV submitted with this application");
            // If it's a direct link, we could show preview or download option
        } else {
            cvContainer.setVisible(false);
            cvContainer.setManaged(false);
        }
        
        // Custom answers
        String answers = application.getAnswers();
        if (answers != null && !answers.isBlank()) {
            answersContainer.setVisible(true);
            answersContainer.setManaged(true);
            renderAnswers(answers);
        } else {
            answersContainer.setVisible(false);
            answersContainer.setManaged(false);
        }
    }

    private void renderAnswers(String answersJson) {
        answersContent.getChildren().clear();
        
        // Parse JSON and format nicely
        try {
            // Try to parse as JSON object
            if (answersJson.trim().startsWith("{")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> answersMap = mapper.readValue(answersJson, java.util.Map.class);
                
                for (java.util.Map.Entry<String, Object> entry : answersMap.entrySet()) {
                    VBox questionBox = new VBox(4);
                    questionBox.setPadding(new Insets(8, 0, 8, 0));
                    
                    // Format question key to readable text
                    String questionLabel = formatQuestionKey(entry.getKey());
                    Label qLabel = new Label(questionLabel);
                    qLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px; -fx-font-weight: 600;");
                    
                    // Answer value
                    String answerValue = entry.getValue() != null ? entry.getValue().toString() : "Not provided";
                    Label aLabel = new Label(answerValue);
                    aLabel.setWrapText(true);
                    aLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 14px;");
                    
                    questionBox.getChildren().addAll(qLabel, aLabel);
                    answersContent.getChildren().add(questionBox);
                    
                    // Add separator
                    Region separator = new Region();
                    separator.setStyle("-fx-background-color: #E5E7EB; -fx-pref-height: 1; -fx-max-height: 1;");
                    separator.setMaxWidth(Double.MAX_VALUE);
                    answersContent.getChildren().add(separator);
                }
                
                // Remove last separator
                if (!answersContent.getChildren().isEmpty()) {
                    answersContent.getChildren().remove(answersContent.getChildren().size() - 1);
                }
            } else {
                // Plain text fallback
                Label answersLabel = new Label(answersJson);
                answersLabel.setWrapText(true);
                answersLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 14px;");
                answersContent.getChildren().add(answersLabel);
            }
        } catch (Exception e) {
            // Fallback to plain text
            Label answersLabel = new Label(answersJson);
            answersLabel.setWrapText(true);
            answersLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 14px;");
            answersContent.getChildren().add(answersLabel);
        }
    }
    
    private String formatQuestionKey(String key) {
        // Convert snake_case to Title Case
        String[] words = key.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        return result.toString().trim();
    }

    private HBox createStatusBadge(String status) {
        HBox badge = new HBox();
        badge.setAlignment(Pos.CENTER);
        
        String bgColor;
        String textColor;
        String iconCode;
        
        switch (status != null ? status.toUpperCase() : "") {
            case "PENDING" -> { bgColor = "#FEF3C7"; textColor = "#D97706"; iconCode = "fas-clock"; }
            case "REVIEWING" -> { bgColor = "#E0F2FE"; textColor = "#0284C7"; iconCode = "fas-eye"; }
            case "SHORTLISTED" -> { bgColor = "#F3E8FF"; textColor = "#9333EA"; iconCode = "fas-star"; }
            case "INTERVIEWING" -> { bgColor = "#CCFBF1"; textColor = "#0F766E"; iconCode = "fas-comments"; }
            case "OFFERED" -> { bgColor = "#DCFCE7"; textColor = "#16A34A"; iconCode = "fas-gift"; }
            case "HIRED" -> { bgColor = "#DCFCE7"; textColor = "#16A34A"; iconCode = "fas-check-circle"; }
            case "REJECTED" -> { bgColor = "#FEE2E2"; textColor = "#DC2626"; iconCode = "fas-times-circle"; }
            default -> { bgColor = "#F3F4F6"; textColor = "#6B7280"; iconCode = "fas-question-circle"; }
        }
        
        badge.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 16; -fx-padding: 6 12;");
        
        FontIcon icon = new FontIcon(iconCode);
        icon.setIconSize(12);
        icon.setIconColor(Color.web(textColor));
        
        Label label = new Label(formatStatus(status));
        label.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 0 0 0 6;");
        
        badge.getChildren().addAll(icon, label);
        return badge;
    }

    private String formatStatus(String status) {
        if (status == null) return "Unknown";
        return status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a"));
    }

    @FXML
    private void onBack() {
        Router.getInstance().navigate(Route.SEEKER_APPLICATIONS);
    }

    @FXML
    private void onViewJob() {
        if (application != null && application.getJobId() != null) {
            Router.getInstance().navigate(Route.SEEKER_JOB_DETAIL, application.getJobId());
        }
    }

    @FXML
    private void onDownloadCV() {
        if (application != null && application.getCvFileUrl() != null) {
            // Open CV URL in browser or download
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(application.getCvFileUrl()));
            } catch (Exception e) {
                Toast.error("Failed to open CV");
            }
        }
    }

    private void showLoading(boolean show) {
        loadingContainer.setVisible(show);
        loadingContainer.setManaged(show);
    }
}
