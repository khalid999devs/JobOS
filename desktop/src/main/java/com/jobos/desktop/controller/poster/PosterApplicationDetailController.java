package com.jobos.desktop.controller.poster;

import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.ui.LoadingOverlay;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.ApplicationService;
import com.jobos.desktop.service.JobPostService;
import com.jobos.shared.dto.application.ApplicationResponse;
import com.jobos.shared.dto.application.ApplicationStatusUpdateRequest;
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
import java.util.ResourceBundle;

public class PosterApplicationDetailController implements Initializable {

    @FXML private VBox contentContainer;
    @FXML private Label applicantName;
    @FXML private Label applicantEmail;
    @FXML private Label jobTitleLabel;
    @FXML private Label statusLabel;
    @FXML private Label appliedDateLabel;
    @FXML private VBox coverLetterContainer;
    @FXML private VBox cvContainer;
    @FXML private VBox answersContainer;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextArea notesArea;
    
    private final ApplicationService applicationService = new ApplicationService();
    private final JobPostService jobPostService = new JobPostService();
    private final Router router = Router.getInstance();
    
    private String applicationId;
    private ApplicationResponse currentApplication;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        applicationId = router.getParam("applicationId");
        
        if (applicationId == null) {
            Toast.error("No application ID provided");
            router.navigate(Route.POSTER_APPLICANTS);
            return;
        }
        
        setupStatusCombo();
        loadApplication();
    }

    private void setupStatusCombo() {
        statusCombo.getItems().addAll("PENDING", "REVIEWING", "SHORTLISTED", "INTERVIEWED", "OFFERED", "HIRED", "REJECTED");
        statusCombo.setOnAction(e -> {
            String newStatus = statusCombo.getValue();
            if (newStatus != null && currentApplication != null && !newStatus.equals(currentApplication.getStatus())) {
                updateStatus(newStatus);
            }
        });
    }

    private void loadApplication() {
        LoadingOverlay.show("Loading application...");
        
        applicationService.getApplicationById(applicationId)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    LoadingOverlay.hide();
                    if (response != null && response.getResult() != null) {
                        currentApplication = response.getResult();
                        populateDetails();
                    } else {
                        Toast.error("Application not found");
                        router.navigate(Route.POSTER_APPLICANTS);
                    }
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    LoadingOverlay.hide();
                    Toast.error("Failed to load application");
                    router.navigate(Route.POSTER_APPLICANTS);
                });
                return null;
            });
    }

    private void populateDetails() {
        jobTitleLabel.setText(currentApplication.getJobTitle() != null ? currentApplication.getJobTitle() : "Unknown Job");
        
        statusCombo.setValue(currentApplication.getStatus());
        updateStatusLabel(currentApplication.getStatus());
        
        if (currentApplication.getAppliedAt() != null) {
            appliedDateLabel.setText("Applied: " + formatDateTime(currentApplication.getAppliedAt()));
        }
        
        // Cover Letter
        coverLetterContainer.getChildren().clear();
        String coverLetter = currentApplication.getCoverLetter();
        if (coverLetter != null && !coverLetter.isEmpty()) {
            TextArea letterArea = new TextArea(coverLetter);
            letterArea.setWrapText(true);
            letterArea.setEditable(false);
            letterArea.setPrefRowCount(8);
            letterArea.getStyleClass().add("text-area-readonly");
            coverLetterContainer.getChildren().add(letterArea);
        } else {
            Label noLetter = new Label("No cover letter provided");
            noLetter.getStyleClass().add("label-muted");
            coverLetterContainer.getChildren().add(noLetter);
        }
        
        // CV
        cvContainer.getChildren().clear();
        String cvUrl = currentApplication.getCvFileUrl();
        if (cvUrl != null && !cvUrl.isEmpty()) {
            HBox cvRow = new HBox(12);
            cvRow.setAlignment(Pos.CENTER_LEFT);
            
            Label cvIcon = new Label("📄");
            cvIcon.setStyle("-fx-font-size: 24px;");
            
            VBox cvInfo = new VBox(4);
            Label cvLabel = new Label("CV Document");
            cvLabel.getStyleClass().add("label-bold");
            Label cvUrlLabel = new Label(cvUrl);
            cvUrlLabel.getStyleClass().add("label-muted");
            cvInfo.getChildren().addAll(cvLabel, cvUrlLabel);
            HBox.setHgrow(cvInfo, Priority.ALWAYS);
            
            Button downloadBtn = new Button("Download");
            downloadBtn.getStyleClass().add("button-secondary");
            downloadBtn.setOnAction(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(cvUrl));
                } catch (Exception ex) {
                    Toast.error("Could not open CV");
                }
            });
            
            cvRow.getChildren().addAll(cvIcon, cvInfo, downloadBtn);
            cvContainer.getChildren().add(cvRow);
        } else {
            Label noCv = new Label("No CV attached");
            noCv.getStyleClass().add("label-muted");
            cvContainer.getChildren().add(noCv);
        }
        
        // Answers
        answersContainer.getChildren().clear();
        String answers = currentApplication.getAnswers();
        if (answers != null && !answers.isEmpty()) {
            TextArea answersArea = new TextArea(answers);
            answersArea.setWrapText(true);
            answersArea.setEditable(false);
            answersArea.setPrefRowCount(5);
            answersArea.getStyleClass().add("text-area-readonly");
            answersContainer.getChildren().add(answersArea);
        } else {
            Label noAnswers = new Label("No additional answers provided");
            noAnswers.getStyleClass().add("label-muted");
            answersContainer.getChildren().add(noAnswers);
        }
    }

    private void updateStatusLabel(String status) {
        statusLabel.setText(status != null ? status : "UNKNOWN");
        
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
        
        statusLabel.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: white; -fx-padding: 4 12; -fx-background-radius: 4;");
    }

    private void updateStatus(String newStatus) {
        LoadingOverlay.show("Updating status...");
        
        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus(newStatus);
        
        jobPostService.updateApplicationStatus(applicationId, request)
            .thenAccept(response -> Platform.runLater(() -> {
                LoadingOverlay.hide();
                Toast.success("Status updated to " + newStatus);
                currentApplication.setStatus(newStatus);
                updateStatusLabel(newStatus);
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    LoadingOverlay.hide();
                    Toast.error("Failed to update status");
                    statusCombo.setValue(currentApplication.getStatus());
                });
                return null;
            });
    }

    @FXML
    private void onBack() {
        router.navigate(Route.POSTER_APPLICANTS);
    }

    @FXML
    private void onViewJob() {
        if (currentApplication != null && currentApplication.getJobId() != null) {
            router.navigate(Route.POSTER_APPLICANTS, java.util.Map.of("jobId", currentApplication.getJobId()));
        }
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"));
    }
}
