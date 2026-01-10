package com.jobos.desktop.controller.poster;

import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.service.JobPostService;
import com.jobos.shared.dto.job.JobPostRequest;
import com.jobos.shared.dto.job.JobPostResponse;
import com.jobos.shared.dto.job.JobPostUpdateRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.time.LocalDate;
import java.util.*;

public class JobFormController implements Initializable {

    @FXML private Label pageTitle;
    @FXML private TextField titleField;
    @FXML private TextField companyField;
    @FXML private TextField locationField;
    @FXML private CheckBox remoteCheckbox;
    @FXML private ComboBox<String> jobTypeCombo;
    @FXML private ComboBox<String> experienceLevelCombo;
    @FXML private TextField salaryMinField;
    @FXML private TextField salaryMaxField;
    @FXML private ComboBox<String> currencyCombo;
    @FXML private TextField skillInput;
    @FXML private FlowPane skillsContainer;
    @FXML private TextArea descriptionArea;
    @FXML private TextArea responsibilitiesArea;
    @FXML private TextArea requirementsArea;
    @FXML private TextArea benefitsArea;
    @FXML private DatePicker deadlinePicker;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    
    private final JobPostService jobPostService = new JobPostService();
    private final Router router = Router.getInstance();
    private final List<String> skills = new ArrayList<>();
    
    private String editingJobId = null;
    private boolean isEditMode = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupDropdowns();
        setupSkillInput();
        checkForEditMode();
    }

    private void setupDropdowns() {
        jobTypeCombo.getItems().addAll("FULL_TIME", "PART_TIME", "CONTRACT", "INTERNSHIP", "FREELANCE");
        jobTypeCombo.setValue("FULL_TIME");
        
        experienceLevelCombo.getItems().addAll("ENTRY", "JUNIOR", "MID", "SENIOR", "LEAD", "EXECUTIVE");
        experienceLevelCombo.setValue("MID");
        
        currencyCombo.getItems().addAll("USD", "EUR", "GBP", "BDT", "INR");
        currencyCombo.setValue("USD");
        
        statusCombo.getItems().addAll("DRAFT", "ACTIVE", "CLOSED");
        statusCombo.setValue("ACTIVE");
        
        deadlinePicker.setValue(LocalDate.now().plusMonths(1));
    }

    private void setupSkillInput() {
        skillInput.setOnAction(e -> addSkill());
    }

    @FXML
    private void addSkill() {
        String skill = skillInput.getText().trim();
        if (!skill.isEmpty() && !skills.contains(skill) && skills.size() < 20) {
            skills.add(skill);
            renderSkills();
            skillInput.clear();
        }
    }

    private void renderSkills() {
        skillsContainer.getChildren().clear();
        for (String skill : skills) {
            HBox chip = new HBox(8);
            chip.getStyleClass().add("skill-chip");
            chip.setStyle("-fx-background-color: #E5E7EB; -fx-padding: 4 8; -fx-background-radius: 16;");
            
            Label label = new Label(skill);
            Button removeBtn = new Button("×");
            removeBtn.setStyle("-fx-background-color: transparent; -fx-padding: 0 4; -fx-cursor: hand;");
            removeBtn.setOnAction(e -> {
                skills.remove(skill);
                renderSkills();
            });
            
            chip.getChildren().addAll(label, removeBtn);
            skillsContainer.getChildren().add(chip);
        }
    }

    private void checkForEditMode() {
        editingJobId = router.getParam("jobId");
        isEditMode = editingJobId != null;
        
        if (isEditMode) {
            pageTitle.setText("Edit Job Post");
            saveButton.setText("Update Job");
            loadJobDetails();
        } else {
            pageTitle.setText("Create New Job");
            saveButton.setText("Post Job");
        }
    }

    private void loadJobDetails() {
        Toast.info("Loading job details...");
        
        jobPostService.getJobPostById(editingJobId)
            .thenAccept(job -> Platform.runLater(() -> {
                populateForm(job);
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    Toast.error("Failed to load job details");
                    router.navigate(Route.POSTER_JOB_POSTS);
                });
                return null;
            });
    }

    private void populateForm(JobPostResponse job) {
        titleField.setText(job.getTitle());
        companyField.setText(job.getCompany());
        locationField.setText(job.getLocation());
        remoteCheckbox.setSelected(Boolean.TRUE.equals(job.getIsRemote()));
        
        if (job.getJobType() != null) {
            jobTypeCombo.setValue(job.getJobType());
        }
        if (job.getExperienceLevel() != null) {
            experienceLevelCombo.setValue(job.getExperienceLevel());
        }
        if (job.getSalaryMin() != null) {
            salaryMinField.setText(job.getSalaryMin().toString());
        }
        if (job.getSalaryMax() != null) {
            salaryMaxField.setText(job.getSalaryMax().toString());
        }
        if (job.getSalaryCurrency() != null) {
            currencyCombo.setValue(job.getSalaryCurrency());
        }
        if (job.getSkills() != null) {
            skills.clear();
            skills.addAll(job.getSkills());
            renderSkills();
        }
        
        descriptionArea.setText(job.getDescription());
        responsibilitiesArea.setText(job.getResponsibilities());
        requirementsArea.setText(job.getRequirements());
        benefitsArea.setText(job.getBenefits());
        
        if (job.getApplicationDeadline() != null) {
            deadlinePicker.setValue(job.getApplicationDeadline());
        }
        if (job.getStatus() != null) {
            statusCombo.setValue(job.getStatus());
        }
    }

    @FXML
    private void onSave() {
        if (!validateForm()) return;
        
        Toast.info(isEditMode ? "Updating job..." : "Creating job...");
        
        if (isEditMode) {
            updateJob();
        } else {
            createJob();
        }
    }

    private boolean validateForm() {
        List<String> errors = new ArrayList<>();
        
        if (titleField.getText().trim().isEmpty()) {
            errors.add("Title is required");
        }
        if (jobTypeCombo.getValue() == null) {
            errors.add("Job type is required");
        }
        if (skills.isEmpty()) {
            errors.add("At least one skill is required");
        }
        if (descriptionArea.getText().trim().isEmpty()) {
            errors.add("Description is required");
        }
        
        if (!errors.isEmpty()) {
            Toast.error(String.join(", ", errors));
            return false;
        }
        return true;
    }

    private void createJob() {
        JobPostRequest request = new JobPostRequest();
        populateRequest(request);
        
        jobPostService.createJobPost(request)
            .thenAccept(response -> Platform.runLater(() -> {
                Toast.success("Job created successfully!");
                router.navigate(Route.POSTER_JOB_POSTS);
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    Toast.error("Failed to create job: " + e.getMessage());
                });
                return null;
            });
    }

    private void updateJob() {
        JobPostUpdateRequest request = new JobPostUpdateRequest();
        populateUpdateRequest(request);
        
        jobPostService.updateJobPost(editingJobId, request)
            .thenAccept(response -> Platform.runLater(() -> {
                Toast.success("Job updated successfully!");
                router.navigate(Route.POSTER_JOB_POSTS);
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    Toast.error("Failed to update job: " + e.getMessage());
                });
                return null;
            });
    }

    private void populateRequest(JobPostRequest request) {
        request.setTitle(titleField.getText().trim());
        request.setCompany(companyField.getText().trim());
        request.setLocation(locationField.getText().trim());
        request.setIsRemote(remoteCheckbox.isSelected());
        request.setJobType(jobTypeCombo.getValue());
        request.setExperienceLevel(experienceLevelCombo.getValue());
        
        String minSalary = salaryMinField.getText().trim();
        if (!minSalary.isEmpty()) {
            try { request.setSalaryMin(Integer.parseInt(minSalary)); } catch (NumberFormatException ignored) {}
        }
        
        String maxSalary = salaryMaxField.getText().trim();
        if (!maxSalary.isEmpty()) {
            try { request.setSalaryMax(Integer.parseInt(maxSalary)); } catch (NumberFormatException ignored) {}
        }
        
        request.setSalaryCurrency(currencyCombo.getValue());
        request.setSkills(new ArrayList<>(skills));
        request.setDescription(descriptionArea.getText().trim());
        request.setResponsibilities(responsibilitiesArea.getText().trim());
        request.setRequirements(requirementsArea.getText().trim());
        request.setBenefits(benefitsArea.getText().trim());
        request.setApplicationDeadline(deadlinePicker.getValue());
        request.setStatus(statusCombo.getValue());
    }

    private void populateUpdateRequest(JobPostUpdateRequest request) {
        request.setTitle(titleField.getText().trim());
        request.setCompany(companyField.getText().trim());
        request.setLocation(locationField.getText().trim());
        request.setIsRemote(remoteCheckbox.isSelected());
        request.setJobType(jobTypeCombo.getValue());
        request.setExperienceLevel(experienceLevelCombo.getValue());
        
        String minSalary = salaryMinField.getText().trim();
        if (!minSalary.isEmpty()) {
            try { request.setSalaryMin(Integer.parseInt(minSalary)); } catch (NumberFormatException ignored) {}
        }
        
        String maxSalary = salaryMaxField.getText().trim();
        if (!maxSalary.isEmpty()) {
            try { request.setSalaryMax(Integer.parseInt(maxSalary)); } catch (NumberFormatException ignored) {}
        }
        
        request.setSalaryCurrency(currencyCombo.getValue());
        request.setSkills(new ArrayList<>(skills));
        request.setDescription(descriptionArea.getText().trim());
        request.setResponsibilities(responsibilitiesArea.getText().trim());
        request.setRequirements(requirementsArea.getText().trim());
        request.setBenefits(benefitsArea.getText().trim());
        request.setApplicationDeadline(deadlinePicker.getValue());
        request.setStatus(statusCombo.getValue());
    }

    @FXML
    private void onCancel() {
        router.navigate(Route.POSTER_JOB_POSTS);
    }
}
