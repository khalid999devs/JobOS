package com.jobos.desktop.controller.poster;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.ui.Dialogs;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.ApplicationService;
import com.jobos.desktop.service.JobPostService;
import com.jobos.desktop.util.CvPdfGenerator;
import com.jobos.shared.dto.application.ApplicationResponse;
import com.jobos.shared.dto.application.ApplicationStatusUpdateRequest;
import com.jobos.shared.dto.cv.CVResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.ResourceBundle;

public class PosterApplicationDetailController implements Initializable {

    @FXML private VBox contentContainer;
    @FXML private VBox avatarContainer;
    @FXML private Label avatarInitials;
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
        statusCombo.getItems().addAll("PENDING", "REVIEWED", "SHORTLISTED", "ACCEPTED", "REJECTED");
        statusCombo.setOnAction(e -> {
            String newStatus = statusCombo.getValue();
            if (newStatus != null && currentApplication != null && !newStatus.equals(currentApplication.getStatus())) {
                updateStatus(newStatus);
            }
        });
    }

    private void loadApplication() {
        Toast.info("Loading application...");
        
        applicationService.getApplicationById(applicationId)
            .thenAccept(response -> {
                Platform.runLater(() -> {
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
                    Toast.error("Failed to load application");
                    router.navigate(Route.POSTER_APPLICANTS);
                });
                return null;
            });
    }

    private void populateDetails() {
        String name = currentApplication.getApplicantName();
        if (name != null && !name.isEmpty()) {
            applicantName.setText(name);
            String initials = getInitials(name);
            avatarInitials.setText(initials);
        } else {
            applicantName.setText("Unknown Applicant");
            avatarInitials.setText("?");
        }
        
        String email = currentApplication.getApplicantEmail();
        if (email != null && !email.isEmpty()) {
            applicantEmail.setText(email);
        } else {
            applicantEmail.setText("");
        }
        
        jobTitleLabel.setText(currentApplication.getJobTitle() != null ? currentApplication.getJobTitle() : "Unknown Job");
        
        statusCombo.setValue(currentApplication.getStatus());
        updateStatusLabel(currentApplication.getStatus());
        
        if (currentApplication.getAppliedAt() != null) {
            appliedDateLabel.setText("Applied: " + formatDateTime(currentApplication.getAppliedAt()));
        }
        
        coverLetterContainer.getChildren().clear();
        String coverLetter = currentApplication.getCoverLetter();
        if (coverLetter != null && !coverLetter.isEmpty()) {
            VBox letterBox = createSelectableTextBox(coverLetter);
            coverLetterContainer.getChildren().add(letterBox);
        } else {
            Label noLetter = new Label("No cover letter provided");
            noLetter.getStyleClass().add("label-muted");
            coverLetterContainer.getChildren().add(noLetter);
        }
        
        cvContainer.getChildren().clear();
        String cvId = currentApplication.getCvId();
        String cvUrl = currentApplication.getCvFileUrl();
        if ((cvId != null && !cvId.isEmpty()) || (cvUrl != null && !cvUrl.isEmpty())) {
            HBox cvRow = new HBox(12);
            cvRow.setAlignment(Pos.CENTER_LEFT);
            
            VBox cvIconBox = new VBox();
            cvIconBox.setAlignment(Pos.CENTER);
            cvIconBox.setPrefSize(40, 40);
            cvIconBox.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 8;");
            FontIcon cvIcon = new FontIcon("fas-file-alt");
            cvIcon.setIconSize(20);
            cvIcon.setIconColor(javafx.scene.paint.Color.web("#6B7280"));
            cvIconBox.getChildren().add(cvIcon);
            
            VBox cvInfo = new VBox(4);
            Label cvLabel = new Label("CV Document");
            cvLabel.getStyleClass().add("label-bold");
            String applicantNameStr = currentApplication.getApplicantName() != null ? currentApplication.getApplicantName() : "Applicant";
            Label cvUrlLabel = new Label(applicantNameStr + "'s CV");
            cvUrlLabel.getStyleClass().add("label-muted");
            cvInfo.getChildren().addAll(cvLabel, cvUrlLabel);
            HBox.setHgrow(cvInfo, Priority.ALWAYS);
            
            Button downloadBtn = new Button("Download");
            downloadBtn.getStyleClass().add("button-secondary");
            downloadBtn.setOnAction(e -> onDownloadCV());
            
            cvRow.getChildren().addAll(cvIconBox, cvInfo, downloadBtn);
            cvContainer.getChildren().add(cvRow);
        } else {
            Label noCv = new Label("No CV attached");
            noCv.getStyleClass().add("label-muted");
            cvContainer.getChildren().add(noCv);
        }
        
        answersContainer.getChildren().clear();
        String answers = currentApplication.getAnswers();
        if (answers != null && !answers.isEmpty()) {
            VBox answersBox = formatAnswers(answers);
            answersContainer.getChildren().add(answersBox);
        } else {
            Label noAnswers = new Label("No additional answers provided");
            noAnswers.getStyleClass().add("label-muted");
            answersContainer.getChildren().add(noAnswers);
        }
    }
    
    private VBox createSelectableTextBox(String content) {
        VBox box = new VBox(8);
        box.setStyle("-fx-background-color: #F9FAFB; -fx-padding: 16; -fx-background-radius: 8; -fx-border-color: #E5E7EB; -fx-border-radius: 8;");
        
        Label textLabel = new Label(content);
        textLabel.setWrapText(true);
        textLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 14px; -fx-line-spacing: 4;");
        
        HBox copyBtnContent = new HBox(4);
        copyBtnContent.setAlignment(Pos.CENTER);
        FontIcon copyIcon = new FontIcon("fas-copy");
        copyIcon.setIconSize(12);
        copyIcon.setIconColor(javafx.scene.paint.Color.web("#0F766E"));
        copyBtnContent.getChildren().addAll(copyIcon, new Label("Copy"));
        Button copyBtn = new Button();
        copyBtn.setGraphic(copyBtnContent);
        copyBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #0F766E; -fx-cursor: hand; -fx-font-size: 12px;");
        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent clipContent = new ClipboardContent();
            clipContent.putString(content);
            clipboard.setContent(clipContent);
            Toast.success("Copied to clipboard");
        });
        
        HBox actionRow = new HBox();
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        actionRow.getChildren().add(copyBtn);
        
        box.getChildren().addAll(textLabel, actionRow);
        return box;
    }
    
    private VBox formatAnswers(String answersJson) {
        VBox box = new VBox(12);
        box.setStyle("-fx-background-color: #F9FAFB; -fx-padding: 16; -fx-background-radius: 8; -fx-border-color: #E5E7EB; -fx-border-radius: 8;");
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> answersMap = mapper.readValue(answersJson, new TypeReference<Map<String, Object>>() {});
            
            for (Map.Entry<String, Object> entry : answersMap.entrySet()) {
                VBox answerItem = new VBox(4);
                
                String questionKey = entry.getKey().replace("_", " ");
                String formattedKey = questionKey.substring(0, 1).toUpperCase() + questionKey.substring(1);
                
                Label questionLabel = new Label(formattedKey);
                questionLabel.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px; -fx-font-weight: 600;");
                
                String answerValue = entry.getValue() != null ? entry.getValue().toString() : "N/A";
                Label answerLabel = new Label(answerValue);
                answerLabel.setWrapText(true);
                answerLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 14px;");
                
                answerItem.getChildren().addAll(questionLabel, answerLabel);
                box.getChildren().add(answerItem);
            }
            
            HBox copyBtnContent = new HBox(4);
            copyBtnContent.setAlignment(Pos.CENTER);
            FontIcon copyAllIcon = new FontIcon("fas-copy");
            copyAllIcon.setIconSize(12);
            copyAllIcon.setIconColor(javafx.scene.paint.Color.web("#0F766E"));
            copyBtnContent.getChildren().addAll(copyAllIcon, new Label("Copy All"));
            Button copyBtn = new Button();
            copyBtn.setGraphic(copyBtnContent);
            copyBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #0F766E; -fx-cursor: hand; -fx-font-size: 12px;");
            copyBtn.setOnAction(e -> {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Object> entry : answersMap.entrySet()) {
                    String key = entry.getKey().replace("_", " ");
                    sb.append(key.substring(0, 1).toUpperCase()).append(key.substring(1))
                      .append(": ").append(entry.getValue()).append("\n");
                }
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent clipContent = new ClipboardContent();
                clipContent.putString(sb.toString().trim());
                clipboard.setContent(clipContent);
                Toast.success("Copied to clipboard");
            });
            
            HBox actionRow = new HBox();
            actionRow.setAlignment(Pos.CENTER_RIGHT);
            actionRow.getChildren().add(copyBtn);
            box.getChildren().add(actionRow);
            
        } catch (Exception e) {
            Label rawLabel = new Label(answersJson);
            rawLabel.setWrapText(true);
            rawLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 14px;");
            box.getChildren().add(rawLabel);
        }
        
        return box;
    }

    private void updateStatusLabel(String status) {
        statusLabel.setText(status != null ? status : "UNKNOWN");
        
        String bgColor = switch (status != null ? status.toUpperCase() : "") {
            case "PENDING" -> "#F59E0B";
            case "REVIEWED" -> "#3B82F6";
            case "SHORTLISTED" -> "#8B5CF6";
            case "ACCEPTED" -> "#10B981";
            case "REJECTED" -> "#EF4444";
            default -> "#6B7280";
        };
        
        statusLabel.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: white; -fx-padding: 4 12; -fx-background-radius: 4;");
    }

    private void updateStatus(String newStatus) {
        Toast.info("Updating status...");
        
        ApplicationStatusUpdateRequest request = new ApplicationStatusUpdateRequest();
        request.setStatus(newStatus);
        
        System.out.println("Updating application " + applicationId + " to status: " + newStatus);
        
        jobPostService.updateApplicationStatus(applicationId, request)
            .thenAccept(response -> Platform.runLater(() -> {
                if (response != null && response.isSuccess()) {
                    Toast.success("Status updated to " + newStatus);
                    currentApplication.setStatus(newStatus);
                    updateStatusLabel(newStatus);
                } else {
                    String errorMsg = response != null ? response.getMessage() : "Unknown error";
                    Toast.error("Failed: " + errorMsg);
                    statusCombo.setValue(currentApplication.getStatus());
                }
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    e.printStackTrace();
                    Toast.error("Failed to update status: " + e.getMessage());
                    statusCombo.setValue(currentApplication.getStatus());
                });
                return null;
            });
    }

    @FXML
    private void onBack() {
        router.navigate(Route.POSTER_APPLICANTS);
    }

    private void onDownloadCV() {
        if (currentApplication == null || applicationId == null) {
            showErrorDialog("Application Data Not Available", 
                "Unable to download CV because the application data is not loaded.", 
                "Please try refreshing the page.");
            return;
        }
        
        Toast.info("Fetching CV data...");
        
        applicationService.getApplicantCV(applicationId).whenComplete((cv, error) -> {
            Platform.runLater(() -> {
                if (error != null) {
                    showErrorDialog("Failed to Fetch CV", 
                        "An error occurred while retrieving the CV data from the server.", 
                        "Error: " + error.getMessage());
                    return;
                }
                
                if (cv == null) {
                    showErrorDialog("CV Not Found", 
                        "The CV associated with this application could not be found.", 
                        "The CV may have been deleted or is no longer available.");
                    return;
                }
                
                String applicantNameStr = currentApplication.getApplicantName() != null ? 
                    currentApplication.getApplicantName().replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", "_") : "Applicant";
                String fileName = applicantNameStr + "_CV.pdf";
                
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save CV as PDF");
                fileChooser.setInitialFileName(fileName);
                fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
                );
                
                try {
                    File downloadsDir = new File(System.getProperty("user.home") + "/Downloads");
                    if (downloadsDir.exists()) {
                        fileChooser.setInitialDirectory(downloadsDir);
                    }
                } catch (Exception e) {
                    // Ignore if can't set initial directory
                }
                
                File saveFile = fileChooser.showSaveDialog(cvContainer.getScene().getWindow());
                if (saveFile == null) {
                    return;
                }
                
                Toast.info("Generating PDF...");
                
                try {
                    CvPdfGenerator.generatePdf(cv, saveFile);
                    Toast.success("CV downloaded successfully!");
                    
                    // Open the file
                    if (java.awt.Desktop.isDesktopSupported()) {
                        new Thread(() -> {
                            try {
                                java.awt.Desktop.getDesktop().open(saveFile);
                            } catch (Exception ex) {
                                // Ignore if can't open
                            }
                        }).start();
                    }
                } catch (Exception e) {
                    showErrorDialog("Failed to Generate PDF", 
                        "An error occurred while generating the PDF file.", 
                        "Error: " + e.getMessage());
                }
            });
        });
    }
    
    private void showErrorDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        Dialogs.prepare(alert).showAndWait();
    }

    @FXML
    private void onViewJob() {
        if (currentApplication != null && currentApplication.getJobId() != null) {
            router.navigate(Route.POSTER_JOB_DETAIL, java.util.Map.of("jobId", currentApplication.getJobId()));
        }
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"));
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
