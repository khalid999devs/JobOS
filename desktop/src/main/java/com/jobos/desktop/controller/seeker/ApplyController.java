package com.jobos.desktop.controller.seeker;

import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.ApplicationService;
import com.jobos.desktop.service.CVService;
import com.jobos.desktop.service.JobService;
import com.jobos.shared.dto.application.ApplicationRequest;
import com.jobos.shared.dto.cv.CVListResponse;
import com.jobos.shared.dto.job.JobPostResponse;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

public class ApplyController implements Initializable {

    @FXML private Label jobTitleLabel;
    @FXML private ComboBox<CVListResponse> cvCombo;
    @FXML private TextArea coverLetterArea;
    @FXML private Label charCountLabel;
    @FXML private Button submitBtn;
    @FXML private VBox contentContainer;
    @FXML private VBox loadingContainer;
    @FXML private VBox successContainer;
    @FXML private Label loadingLabel;

    private final JobService jobService = new JobService();
    private final CVService cvService = new CVService();
    private final ApplicationService applicationService = new ApplicationService();
    
    private String jobId;
    private JobPostResponse currentJob;
    private static final int MAX_COVER_LETTER = 2000;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        jobId = Router.getInstance().getParam("id");
        
        setupComboBox();
        setupCoverLetterCounter();
        
        if (jobId != null) {
            loadJobAndCVs();
        } else {
            Toast.error("Job not found");
            onBack();
        }
    }

    private void setupComboBox() {
        cvCombo.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(CVListResponse item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String text = item.getTitle();
                    if (Boolean.TRUE.equals(item.getIsDefault())) {
                        text += " (Default)";
                    }
                    setText(text);
                }
            }
        });
        
        cvCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(CVListResponse cv) {
                if (cv == null) return null;
                String text = cv.getTitle();
                if (Boolean.TRUE.equals(cv.getIsDefault())) {
                    text += " (Default)";
                }
                return text;
            }
            
            @Override
            public CVListResponse fromString(String string) {
                return null;
            }
        });
        
        cvCombo.valueProperty().addListener((obs, old, newVal) -> updateSubmitButton());
    }

    private void setupCoverLetterCounter() {
        coverLetterArea.textProperty().addListener((obs, old, newVal) -> {
            int len = newVal != null ? newVal.length() : 0;
            charCountLabel.setText(len + " / " + MAX_COVER_LETTER);
            if (len > MAX_COVER_LETTER) {
                charCountLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px;");
            } else {
                charCountLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
            }
        });
    }

    private void loadJobAndCVs() {
        showLoading(true, "Loading job details...");
        
        jobService.getJobById(jobId).whenComplete((job, jobError) -> {
            Platform.runLater(() -> {
                if (jobError != null) {
                    Toast.error("Failed to load job details");
                    onBack();
                    return;
                }
                
                currentJob = job;
                jobTitleLabel.setText(job.getTitle() + " at " + job.getCompany());
                loadCVs();
            });
        });
    }

    private void loadCVs() {
        showLoading(true, "Loading your CVs...");
        
        cvService.getAllCVs().whenComplete((cvs, error) -> {
            Platform.runLater(() -> {
                showLoading(false, null);
                
                if (error != null) {
                    Toast.error("Failed to load CVs");
                    return;
                }
                
                if (cvs.isEmpty()) {
                    Toast.info("You don't have any CVs yet. Create one first!");
                }
                
                cvCombo.setItems(FXCollections.observableArrayList(cvs));
                
                cvs.stream()
                        .filter(cv -> Boolean.TRUE.equals(cv.getIsDefault()))
                        .findFirst()
                        .ifPresent(defaultCv -> cvCombo.setValue(defaultCv));
            });
        });
    }

    private void updateSubmitButton() {
        submitBtn.setDisable(cvCombo.getValue() == null);
    }

    @FXML
    private void onSubmit() {
        CVListResponse selectedCV = cvCombo.getValue();
        if (selectedCV == null) {
            Toast.error("Please select a CV");
            return;
        }
        
        String coverLetter = coverLetterArea.getText();
        if (coverLetter != null && coverLetter.length() > MAX_COVER_LETTER) {
            Toast.error("Cover letter exceeds " + MAX_COVER_LETTER + " characters");
            return;
        }
        
        showLoading(true, "Submitting application...");
        
        ApplicationRequest request = new ApplicationRequest();
        request.setJobId(UUID.fromString(jobId));
        request.setCvFileUrl(selectedCV.getId());
        request.setCoverLetter(coverLetter);
        
        applicationService.apply(request).whenComplete((response, error) -> {
            Platform.runLater(() -> {
                showLoading(false, null);
                
                if (error != null) {
                    String errorMsg = error.getMessage();
                    if (errorMsg != null && errorMsg.contains("already applied")) {
                        Toast.error("You have already applied for this job");
                    } else {
                        Toast.error("Failed to submit application");
                    }
                    return;
                }
                
                showSuccess();
            });
        });
    }

    @FXML
    private void onBack() {
        if (jobId != null) {
            Router.getInstance().navigate(Route.SEEKER_JOB_DETAIL, jobId);
        } else {
            Router.getInstance().navigate(Route.SEEKER_JOBS);
        }
    }

    @FXML
    private void onCreateCV() {
        Router.getInstance().navigate(Route.SEEKER_CVS);
    }

    @FXML
    private void onViewApplications() {
        Router.getInstance().navigate(Route.SEEKER_APPLICATIONS);
    }

    @FXML
    private void onBrowseMore() {
        Router.getInstance().navigate(Route.SEEKER_JOBS);
    }

    private void showLoading(boolean show, String message) {
        loadingContainer.setVisible(show);
        loadingContainer.setManaged(show);
        contentContainer.setVisible(!show && !successContainer.isVisible());
        contentContainer.setManaged(!show && !successContainer.isVisible());
        
        if (message != null) {
            loadingLabel.setText(message);
        }
    }

    private void showSuccess() {
        contentContainer.setVisible(false);
        contentContainer.setManaged(false);
        loadingContainer.setVisible(false);
        loadingContainer.setManaged(false);
        successContainer.setVisible(true);
        successContainer.setManaged(true);
    }
}
