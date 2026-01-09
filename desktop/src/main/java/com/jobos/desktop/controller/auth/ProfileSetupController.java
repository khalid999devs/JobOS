package com.jobos.desktop.controller.auth;

import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.session.SessionManager;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.model.UserRole;
import com.jobos.desktop.service.ApiClient;
import com.jobos.shared.dto.profile.ProfileResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.*;

public class ProfileSetupController implements Initializable {
    
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private VBox seekerFields;
    @FXML private VBox posterFields;
    @FXML private Label errorLabel;
    @FXML private Button submitButton;
    
    // Seeker fields
    @FXML private FlowPane desiredRolesChips;
    @FXML private TextField customRoleInput;
    @FXML private FlowPane skillsChips;
    @FXML private TextField customSkillInput;
    @FXML private FlowPane jobTypesChips;
    @FXML private ComboBox<String> workingHoursCombo;
    @FXML private TextField salaryMinInput;
    @FXML private TextField salaryMaxInput;
    @FXML private CheckBox relocateCheck;
    @FXML private DatePicker availableFromPicker;
    
    // Poster fields
    @FXML private TextField companyNameInput;
    @FXML private ComboBox<String> companySizeCombo;
    @FXML private ComboBox<String> industryCombo;
    @FXML private TextField websiteInput;
    @FXML private FlowPane verificationChips;
    @FXML private TextField verificationUrlInput;
    
    private final Router router = Router.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final ApiClient apiClient = ApiClient.getInstance();
    
    private final Set<String> selectedRoles = new HashSet<>();
    private final Set<String> selectedSkills = new HashSet<>();
    private final Set<String> selectedJobTypes = new HashSet<>();
    private final List<String> verificationUrls = new ArrayList<>();
    
    private static final String[] DEFAULT_ROLES = {"Software Engineer", "Product Manager", "Designer", "Data Analyst", "DevOps Engineer", "QA Engineer", "Backend Developer", "Frontend Developer", "Full Stack Developer"};
    private static final String[] DEFAULT_SKILLS = {"Java", "Python", "JavaScript", "React", "Node.js", "SQL", "AWS", "Docker", "Kotlin", "Spring Boot", "PostgreSQL", "Kubernetes"};
    private static final String[] JOB_TYPES = {"FULL_TIME", "PART_TIME", "CONTRACT", "FREELANCE", "INTERNSHIP", "REMOTE"};
    private static final String[] WORKING_HOURS = {"Flexible", "9-5", "Morning Shift", "Evening Shift", "Night Shift", "Weekend"};
    private static final String[] COMPANY_SIZES = {"1-10", "11-50", "51-200", "201-500", "501-1000", "1000+"};
    private static final String[] INDUSTRIES = {"Technology", "Healthcare", "Finance", "Education", "E-commerce", "Manufacturing", "Consulting", "Real Estate", "Media", "Retail", "Transportation", "Energy", "Telecommunications", "Agriculture", "Other"};
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        UserRole role = sessionManager.getUserRole();
        
        if (role == UserRole.SEEKER) {
            titleLabel.setText("Complete Your Job Seeker Profile");
            subtitleLabel.setText("Tell us about your job preferences to get personalized recommendations.");
            seekerFields.setVisible(true);
            seekerFields.setManaged(true);
            posterFields.setVisible(false);
            posterFields.setManaged(false);
            setupSeekerFields();
        } else if (role == UserRole.POSTER) {
            titleLabel.setText("Complete Your Company Profile");
            subtitleLabel.setText("Tell us about your company to build trust with job seekers.");
            seekerFields.setVisible(false);
            seekerFields.setManaged(false);
            posterFields.setVisible(true);
            posterFields.setManaged(true);
            setupPosterFields();
        }
    }
    
    private void setupSeekerFields() {
        // Setup desired roles chips
        for (String role : DEFAULT_ROLES) {
            addSelectableChip(desiredRolesChips, role, selectedRoles);
        }
        
        // Setup skills chips
        for (String skill : DEFAULT_SKILLS) {
            addSelectableChip(skillsChips, skill, selectedSkills);
        }
        
        // Setup job type chips
        for (String type : JOB_TYPES) {
            addSelectableChip(jobTypesChips, formatJobType(type), selectedJobTypes);
        }
        
        // Setup working hours dropdown
        workingHoursCombo.getItems().addAll(WORKING_HOURS);
        workingHoursCombo.setPromptText("Select working hours");
        
        // Number inputs validation
        salaryMinInput.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*")) {
                salaryMinInput.setText(val.replaceAll("[^\\d]", ""));
            }
        });
        salaryMaxInput.textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*")) {
                salaryMaxInput.setText(val.replaceAll("[^\\d]", ""));
            }
        });
    }
    
    private void setupPosterFields() {
        // Setup company size dropdown
        companySizeCombo.getItems().addAll(COMPANY_SIZES);
        companySizeCombo.setPromptText("Select company size");
        
        // Setup industry dropdown
        industryCombo.getItems().addAll(INDUSTRIES);
        industryCombo.setPromptText("Select industry");
    }
    
    private void addSelectableChip(FlowPane container, String text, Set<String> selectedSet) {
        Button chip = new Button(text);
        chip.getStyleClass().add("chip-selectable");
        chip.setOnAction(e -> {
            if (selectedSet.contains(text)) {
                selectedSet.remove(text);
                chip.getStyleClass().remove("chip-selected");
            } else {
                selectedSet.add(text);
                chip.getStyleClass().add("chip-selected");
            }
        });
        container.getChildren().add(chip);
    }
    
    private void addRemovableChip(FlowPane container, String text, Collection<String> collection) {
        HBox chip = new HBox(6);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("chip-removable");
        chip.setPadding(new Insets(6, 12, 6, 12));
        
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #0F766E; -fx-font-size: 13px;");
        
        Button removeBtn = new Button("×");
        removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #0F766E; -fx-font-size: 14px; -fx-padding: 0; -fx-cursor: hand;");
        removeBtn.setOnAction(e -> {
            container.getChildren().remove(chip);
            collection.remove(text);
        });
        
        chip.getChildren().addAll(label, removeBtn);
        container.getChildren().add(chip);
    }
    
    @FXML
    private void onAddCustomRole() {
        String role = customRoleInput.getText().trim();
        if (!role.isEmpty() && !selectedRoles.contains(role)) {
            selectedRoles.add(role);
            addRemovableChip(desiredRolesChips, role, selectedRoles);
            customRoleInput.clear();
        }
    }
    
    @FXML
    private void onAddCustomSkill() {
        String skill = customSkillInput.getText().trim();
        if (!skill.isEmpty() && !selectedSkills.contains(skill)) {
            selectedSkills.add(skill);
            addRemovableChip(skillsChips, skill, selectedSkills);
            customSkillInput.clear();
        }
    }
    
    @FXML
    private void onAddVerificationUrl() {
        String url = verificationUrlInput.getText().trim();
        if (!url.isEmpty() && !verificationUrls.contains(url)) {
            verificationUrls.add(url);
            addRemovableChip(verificationChips, url, verificationUrls);
            verificationUrlInput.clear();
        }
    }
    
    @FXML
    private void onSubmit() {
        UserRole role = sessionManager.getUserRole();
        
        if (role == UserRole.POSTER) {
            String companyName = companyNameInput.getText().trim();
            if (companyName.isEmpty()) {
                showError("Please enter your company name");
                return;
            }
        }
        
        hideError();
        setLoading(true);
        
        Map<String, Object> profileData = new HashMap<>();
        
        if (role == UserRole.SEEKER) {
            if (!selectedRoles.isEmpty()) {
                profileData.put("desiredRoles", new ArrayList<>(selectedRoles));
            }
            if (!selectedSkills.isEmpty()) {
                profileData.put("skills", new ArrayList<>(selectedSkills));
            }
            if (!selectedJobTypes.isEmpty()) {
                List<String> jobTypes = selectedJobTypes.stream()
                    .map(this::parseJobType)
                    .toList();
                profileData.put("jobTypes", jobTypes);
            }
            if (workingHoursCombo.getValue() != null) {
                profileData.put("workingHours", workingHoursCombo.getValue());
            }
            String salaryMin = salaryMinInput.getText().trim();
            if (!salaryMin.isEmpty()) {
                profileData.put("salaryMin", Integer.parseInt(salaryMin));
            }
            String salaryMax = salaryMaxInput.getText().trim();
            if (!salaryMax.isEmpty()) {
                profileData.put("salaryMax", Integer.parseInt(salaryMax));
            }
            profileData.put("willingToRelocate", relocateCheck.isSelected());
            if (availableFromPicker.getValue() != null) {
                profileData.put("availableFrom", availableFromPicker.getValue().toString());
            }
        } else if (role == UserRole.POSTER) {
            profileData.put("companyName", companyNameInput.getText().trim());
            if (companySizeCombo.getValue() != null) {
                profileData.put("companySize", companySizeCombo.getValue());
            }
            if (industryCombo.getValue() != null) {
                profileData.put("industry", industryCombo.getValue());
            }
            String website = websiteInput.getText().trim();
            if (!website.isEmpty()) {
                profileData.put("website", website);
            }
            if (!verificationUrls.isEmpty()) {
                profileData.put("verificationDocuments", new ArrayList<>(verificationUrls));
            }
        }
        
        apiClient.put("/api/users/me/preferences", profileData, ProfileResponse.class)
            .thenAccept(response -> {
                sessionManager.setProfileCompleted(true);
                
                Platform.runLater(() -> {
                    setLoading(false);
                    Toast.success("Profile completed successfully!");
                    router.navigateToRoleDashboard();
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("Failed to save profile. Please try again.");
                });
                return null;
            });
    }
    
    @FXML
    private void onSkip() {
        sessionManager.setProfileCompleted(true);
        Toast.info("You can complete your profile later in Settings");
        router.navigateToRoleDashboard();
    }
    
    @FXML
    private void onBack() {
        sessionManager.logout();
        router.navigate(Route.WELCOME);
    }
    
    private String formatJobType(String type) {
        return switch (type) {
            case "FULL_TIME" -> "Full Time";
            case "PART_TIME" -> "Part Time";
            case "CONTRACT" -> "Contract";
            case "FREELANCE" -> "Freelance";
            case "INTERNSHIP" -> "Internship";
            case "REMOTE" -> "Remote";
            default -> type;
        };
    }
    
    private String parseJobType(String formatted) {
        return switch (formatted) {
            case "Full Time" -> "FULL_TIME";
            case "Part Time" -> "PART_TIME";
            case "Contract" -> "CONTRACT";
            case "Freelance" -> "FREELANCE";
            case "Internship" -> "INTERNSHIP";
            case "Remote" -> "REMOTE";
            default -> formatted;
        };
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
    
    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
    
    private void setLoading(boolean loading) {
        submitButton.setDisable(loading);
        submitButton.setText(loading ? "Saving..." : "Complete Profile");
    }
}
