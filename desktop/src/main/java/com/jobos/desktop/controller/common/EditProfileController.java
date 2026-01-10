package com.jobos.desktop.controller.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jobos.desktop.core.session.SessionManager;
import com.jobos.desktop.core.ui.LoadingOverlay;
import com.jobos.desktop.core.ui.Toast;
import com.jobos.desktop.model.UserRole;
import com.jobos.desktop.service.ApiClient;
import com.jobos.shared.dto.profile.ProfileResponse;
import com.jobos.shared.dto.profile.UpdateProfileRequest;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class EditProfileController implements Initializable {

    // Basic Info
    @FXML private StackPane avatarContainer;
    @FXML private Circle avatarCircle;
    @FXML private Label avatarInitials;
    @FXML private ImageView avatarImage;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField locationField;
    @FXML private ComboBox<String> timezoneCombo;
    @FXML private TextArea bioField;
    @FXML private Label profileErrorLabel;
    @FXML private Button saveBasicInfoBtn;
    
    // Seeker Preferences Section
    @FXML private VBox seekerPreferencesSection;
    @FXML private TextField desiredRolesField;
    @FXML private TextField skillsField;
    @FXML private TextField minSalaryField;
    @FXML private TextField maxSalaryField;
    @FXML private CheckBox fullTimeCheck;
    @FXML private CheckBox partTimeCheck;
    @FXML private CheckBox contractCheck;
    @FXML private CheckBox internshipCheck;
    @FXML private CheckBox remoteCheck;
    @FXML private CheckBox hybridCheck;
    @FXML private CheckBox onsiteCheck;
    @FXML private CheckBox entryLevelCheck;
    @FXML private CheckBox midLevelCheck;
    @FXML private CheckBox seniorLevelCheck;
    @FXML private CheckBox leadLevelCheck;
    
    // Poster Profile Section
    @FXML private VBox posterProfileSection;
    @FXML private TextField companyNameField;
    @FXML private TextField companyWebsiteField;
    @FXML private ComboBox<String> industryCombo;
    @FXML private ToggleButton size1to10;
    @FXML private ToggleButton size11to50;
    @FXML private ToggleButton size51to200;
    @FXML private ToggleButton size201to500;
    @FXML private ToggleButton size500Plus;
    
    private final ApiClient apiClient = ApiClient.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private ProfileResponse currentProfile;
    private String selectedAvatarPath;
    private ToggleGroup companySizeGroup;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupComboBoxes();
        setupCompanySizeToggle();
        loadProfile();
    }
    
    private void setupComboBoxes() {
        // Timezones
        timezoneCombo.setItems(FXCollections.observableArrayList(
            "UTC-12:00", "UTC-11:00", "UTC-10:00", "UTC-09:00", "UTC-08:00 (PST)",
            "UTC-07:00 (MST)", "UTC-06:00 (CST)", "UTC-05:00 (EST)", "UTC-04:00",
            "UTC-03:00", "UTC-02:00", "UTC-01:00", "UTC+00:00 (GMT)", "UTC+01:00",
            "UTC+02:00", "UTC+03:00", "UTC+04:00", "UTC+05:00", "UTC+05:30 (IST)",
            "UTC+06:00 (BST)", "UTC+07:00", "UTC+08:00", "UTC+09:00", "UTC+10:00",
            "UTC+11:00", "UTC+12:00"
        ));
        
        // Industries
        industryCombo.setItems(FXCollections.observableArrayList(
            "Technology", "Healthcare", "Finance", "Education", "Manufacturing",
            "Retail", "Consulting", "Media & Entertainment", "Real Estate",
            "Transportation", "Energy", "Non-profit", "Government", "Other"
        ));
    }
    
    private void setupCompanySizeToggle() {
        companySizeGroup = new ToggleGroup();
        size1to10.setToggleGroup(companySizeGroup);
        size11to50.setToggleGroup(companySizeGroup);
        size51to200.setToggleGroup(companySizeGroup);
        size201to500.setToggleGroup(companySizeGroup);
        size500Plus.setToggleGroup(companySizeGroup);
    }
    
    private void loadProfile() {
        currentProfile = sessionManager.getProfile();
        if (currentProfile != null) {
            populateFields(currentProfile);
            showRoleSections();
        } else {
            fetchProfile();
        }
    }
    
    private void fetchProfile() {
        LoadingOverlay.show("Loading profile...");
        
        // Backend returns ProfileResponse directly, not wrapped in ApiResponse
        apiClient.get("/api/users/me", new TypeReference<ProfileResponse>() {})
            .thenAccept(profile -> {
                Platform.runLater(() -> {
                    LoadingOverlay.hide();
                    if (profile != null) {
                        currentProfile = profile;
                        sessionManager.setProfile(currentProfile);
                        populateFields(currentProfile);
                        showRoleSections();
                    } else {
                        Toast.error("Profile not found");
                    }
                });
            })
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    LoadingOverlay.hide();
                    Toast.error("Failed to load profile: " + e.getMessage());
                });
                return null;
            });
    }
    
    @SuppressWarnings("unchecked")
    private ProfileResponse parseProfile(Object result) {
        if (result instanceof ProfileResponse pr) return pr;
        
        if (result instanceof LinkedHashMap<?, ?> map) {
            ProfileResponse profile = new ProfileResponse();
            profile.setEmail(getString(map, "email"));
            profile.setFirstName(getString(map, "firstName"));
            profile.setLastName(getString(map, "lastName"));
            profile.setPhoneNumber(getString(map, "phoneNumber"));
            profile.setRole(getString(map, "role"));
            profile.setAvatarUrl(getString(map, "avatarUrl"));
            profile.setBio(getString(map, "bio"));
            profile.setLocation(getString(map, "location"));
            profile.setTimezone(getString(map, "timezone"));
            
            // Parse seeker preferences
            if (map.get("seekerPreferences") instanceof LinkedHashMap<?, ?> seekerMap) {
                ProfileResponse.SeekerPreferencesData prefs = new ProfileResponse.SeekerPreferencesData();
                if (seekerMap.get("desiredRoles") instanceof List<?> roles) {
                    prefs.setDesiredRoles((List<String>) roles);
                }
                if (seekerMap.get("skills") instanceof List<?> skills) {
                    prefs.setSkills((List<String>) skills);
                }
                if (seekerMap.get("jobTypes") instanceof List<?> jobTypes) {
                    prefs.setJobTypes((List<String>) jobTypes);
                }
                prefs.setWorkingHours(getString(seekerMap, "workingHours"));
                Object salaryMin = seekerMap.get("salaryMin");
                if (salaryMin instanceof Number) {
                    prefs.setSalaryMin(((Number) salaryMin).intValue());
                }
                Object salaryMax = seekerMap.get("salaryMax");
                if (salaryMax instanceof Number) {
                    prefs.setSalaryMax(((Number) salaryMax).intValue());
                }
                if (seekerMap.get("willingToRelocate") instanceof Boolean relocate) {
                    prefs.setWillingToRelocate(relocate);
                }
                profile.setSeekerPreferences(prefs);
            }
            
            // Parse poster profile
            if (map.get("posterProfile") instanceof LinkedHashMap<?, ?> posterMap) {
                ProfileResponse.PosterProfileData poster = new ProfileResponse.PosterProfileData();
                poster.setCompanyName(getString(posterMap, "companyName"));
                poster.setCompanySize(getString(posterMap, "companySize"));
                poster.setIndustry(getString(posterMap, "industry"));
                poster.setWebsite(getString(posterMap, "website"));
                poster.setVerificationStatus(getString(posterMap, "verificationStatus"));
                if (posterMap.get("verificationDocuments") instanceof List<?> docs) {
                    poster.setVerificationDocuments((List<String>) docs);
                }
                profile.setPosterProfile(poster);
            }
            
            return profile;
        }
        
        return new ProfileResponse();
    }
    
    private String getString(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }
    
    private void populateFields(ProfileResponse profile) {
        // Basic Info
        firstNameField.setText(profile.getFirstName() != null ? profile.getFirstName() : "");
        lastNameField.setText(profile.getLastName() != null ? profile.getLastName() : "");
        emailField.setText(profile.getEmail() != null ? profile.getEmail() : "");
        phoneField.setText(profile.getPhoneNumber() != null ? profile.getPhoneNumber() : "");
        locationField.setText(profile.getLocation() != null ? profile.getLocation() : "");
        bioField.setText(profile.getBio() != null ? profile.getBio() : "");
        
        if (profile.getTimezone() != null) {
            timezoneCombo.setValue(profile.getTimezone());
        }
        
        // Avatar
        updateAvatar(profile);
        
        // Seeker Preferences
        if (profile.getSeekerPreferences() != null) {
            var prefs = profile.getSeekerPreferences();
            if (prefs.getDesiredRoles() != null) {
                desiredRolesField.setText(String.join(", ", prefs.getDesiredRoles()));
            }
            if (prefs.getSkills() != null) {
                skillsField.setText(String.join(", ", prefs.getSkills()));
            }
            if (prefs.getSalaryMin() != null) {
                minSalaryField.setText(prefs.getSalaryMin().toString());
            }
            if (prefs.getSalaryMax() != null) {
                maxSalaryField.setText(prefs.getSalaryMax().toString());
            }
            if (prefs.getJobTypes() != null) {
                fullTimeCheck.setSelected(prefs.getJobTypes().contains("FULL_TIME"));
                partTimeCheck.setSelected(prefs.getJobTypes().contains("PART_TIME"));
                contractCheck.setSelected(prefs.getJobTypes().contains("CONTRACT"));
                internshipCheck.setSelected(prefs.getJobTypes().contains("INTERNSHIP"));
            }
            if (prefs.getWorkModes() != null) {
                remoteCheck.setSelected(prefs.getWorkModes().contains("REMOTE"));
                hybridCheck.setSelected(prefs.getWorkModes().contains("HYBRID"));
                onsiteCheck.setSelected(prefs.getWorkModes().contains("ONSITE"));
            }
            if (prefs.getExperienceLevels() != null) {
                entryLevelCheck.setSelected(prefs.getExperienceLevels().contains("ENTRY"));
                midLevelCheck.setSelected(prefs.getExperienceLevels().contains("MID"));
                seniorLevelCheck.setSelected(prefs.getExperienceLevels().contains("SENIOR"));
                leadLevelCheck.setSelected(prefs.getExperienceLevels().contains("LEAD"));
            }
        }
        
        // Poster Profile
        if (profile.getPosterProfile() != null) {
            var poster = profile.getPosterProfile();
            if (poster.getCompanyName() != null) {
                companyNameField.setText(poster.getCompanyName());
            }
            if (poster.getWebsite() != null) {
                companyWebsiteField.setText(poster.getWebsite());
            }
            if (poster.getIndustry() != null) {
                industryCombo.setValue(poster.getIndustry());
            }
            selectCompanySize(poster.getCompanySize());
        }
    }
    
    private void updateAvatar(ProfileResponse profile) {
        if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isEmpty()) {
            try {
                Image img = new Image(profile.getAvatarUrl(), true);
                avatarImage.setImage(img);
                avatarImage.setVisible(true);
                avatarInitials.setVisible(false);
            } catch (Exception e) {
                showInitials(profile);
            }
        } else {
            showInitials(profile);
        }
    }
    
    private void showInitials(ProfileResponse profile) {
        avatarImage.setVisible(false);
        avatarInitials.setVisible(true);
        
        StringBuilder initials = new StringBuilder();
        if (profile.getFirstName() != null && !profile.getFirstName().isEmpty()) {
            initials.append(profile.getFirstName().charAt(0));
        }
        if (profile.getLastName() != null && !profile.getLastName().isEmpty()) {
            initials.append(profile.getLastName().charAt(0));
        }
        if (initials.length() == 0 && profile.getEmail() != null) {
            initials.append(profile.getEmail().charAt(0));
        }
        avatarInitials.setText(initials.toString().toUpperCase());
    }
    
    private void showRoleSections() {
        UserRole role = sessionManager.getUserRole();
        
        if (role == UserRole.SEEKER) {
            seekerPreferencesSection.setVisible(true);
            seekerPreferencesSection.setManaged(true);
            posterProfileSection.setVisible(false);
            posterProfileSection.setManaged(false);
        } else if (role == UserRole.POSTER) {
            seekerPreferencesSection.setVisible(false);
            seekerPreferencesSection.setManaged(false);
            posterProfileSection.setVisible(true);
            posterProfileSection.setManaged(true);
        }
    }
    
    private void selectCompanySize(String size) {
        if (size == null) return;
        switch (size) {
            case "1-10" -> size1to10.setSelected(true);
            case "11-50" -> size11to50.setSelected(true);
            case "51-200" -> size51to200.setSelected(true);
            case "201-500" -> size201to500.setSelected(true);
            case "500+" -> size500Plus.setSelected(true);
        }
    }
    
    private String getSelectedCompanySize() {
        Toggle selected = companySizeGroup.getSelectedToggle();
        if (selected == size1to10) return "1-10";
        if (selected == size11to50) return "11-50";
        if (selected == size51to200) return "51-200";
        if (selected == size201to500) return "201-500";
        if (selected == size500Plus) return "500+";
        return null;
    }
    
    @FXML
    private void onUploadPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Photo");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        
        File file = fileChooser.showOpenDialog(avatarContainer.getScene().getWindow());
        if (file != null) {
            selectedAvatarPath = file.getAbsolutePath();
            try {
                Image img = new Image(file.toURI().toString());
                avatarImage.setImage(img);
                avatarImage.setVisible(true);
                avatarInitials.setVisible(false);
                Toast.info("Photo selected. Save to upload.");
            } catch (Exception e) {
                Toast.error("Failed to load image");
            }
        }
    }
    
    @FXML
    private void onSaveBasicInfo() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        
        if (firstName.isEmpty()) {
            showProfileError("First name is required");
            return;
        }
        
        hideProfileError();
        saveBasicInfoBtn.setDisable(true);
        LoadingOverlay.show("Saving profile...");
        
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setPhoneNumber(phoneField.getText().trim());
        request.setLocation(locationField.getText().trim());
        request.setTimezone(timezoneCombo.getValue());
        request.setBio(bioField.getText().trim());
        
        apiClient.patch("/api/users/me", request, ProfileResponse.class)
            .thenAccept(response -> Platform.runLater(() -> {
                LoadingOverlay.hide();
                saveBasicInfoBtn.setDisable(false);
                Toast.success("Profile updated successfully");
                // Update session with fresh data
                if (response != null) {
                    currentProfile = response;
                    sessionManager.setProfile(currentProfile);
                }
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    LoadingOverlay.hide();
                    saveBasicInfoBtn.setDisable(false);
                    showProfileError("Failed to update profile");
                });
                return null;
            });
    }
    
    @FXML
    private void onSaveSeekerPreferences() {
        LoadingOverlay.show("Saving preferences...");
        
        Map<String, Object> preferences = new HashMap<>();
        
        // Desired roles
        String rolesText = desiredRolesField.getText().trim();
        if (!rolesText.isEmpty()) {
            preferences.put("desiredRoles", Arrays.stream(rolesText.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
        }
        
        // Skills
        String skillsText = skillsField.getText().trim();
        if (!skillsText.isEmpty()) {
            preferences.put("skills", Arrays.stream(skillsText.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
        }
        
        // Salary
        try {
            if (!minSalaryField.getText().trim().isEmpty()) {
                preferences.put("salaryMin", Integer.parseInt(minSalaryField.getText().trim()));
            }
            if (!maxSalaryField.getText().trim().isEmpty()) {
                preferences.put("salaryMax", Integer.parseInt(maxSalaryField.getText().trim()));
            }
        } catch (NumberFormatException e) {
            Toast.error("Invalid salary value");
            LoadingOverlay.hide();
            return;
        }
        
        // Job types
        List<String> jobTypes = new ArrayList<>();
        if (fullTimeCheck.isSelected()) jobTypes.add("FULL_TIME");
        if (partTimeCheck.isSelected()) jobTypes.add("PART_TIME");
        if (contractCheck.isSelected()) jobTypes.add("CONTRACT");
        if (internshipCheck.isSelected()) jobTypes.add("INTERNSHIP");
        if (!jobTypes.isEmpty()) preferences.put("jobTypes", jobTypes);
        
        // Work modes
        List<String> workModes = new ArrayList<>();
        if (remoteCheck.isSelected()) workModes.add("REMOTE");
        if (hybridCheck.isSelected()) workModes.add("HYBRID");
        if (onsiteCheck.isSelected()) workModes.add("ONSITE");
        if (!workModes.isEmpty()) preferences.put("workModes", workModes);
        
        // Experience levels
        List<String> experienceLevels = new ArrayList<>();
        if (entryLevelCheck.isSelected()) experienceLevels.add("ENTRY");
        if (midLevelCheck.isSelected()) experienceLevels.add("MID");
        if (seniorLevelCheck.isSelected()) experienceLevels.add("SENIOR");
        if (leadLevelCheck.isSelected()) experienceLevels.add("LEAD");
        if (!experienceLevels.isEmpty()) preferences.put("experienceLevels", experienceLevels);
        
        apiClient.put("/api/users/me/preferences", preferences, ProfileResponse.class)
            .thenAccept(response -> Platform.runLater(() -> {
                LoadingOverlay.hide();
                Toast.success("Preferences saved successfully");
                if (response != null) {
                    currentProfile = response;
                    sessionManager.setProfile(currentProfile);
                }
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    LoadingOverlay.hide();
                    Toast.error("Failed to save preferences");
                });
                return null;
            });
    }
    
    @FXML
    private void onSavePosterProfile() {
        LoadingOverlay.show("Saving company info...");
        
        Map<String, Object> posterData = new HashMap<>();
        posterData.put("companyName", companyNameField.getText().trim());
        posterData.put("website", companyWebsiteField.getText().trim());
        posterData.put("industry", industryCombo.getValue());
        posterData.put("companySize", getSelectedCompanySize());
        
        apiClient.put("/api/users/me/preferences", posterData, ProfileResponse.class)
            .thenAccept(response -> Platform.runLater(() -> {
                LoadingOverlay.hide();
                Toast.success("Company info saved successfully");
                if (response != null) {
                    currentProfile = response;
                    sessionManager.setProfile(currentProfile);
                }
            }))
            .exceptionally(e -> {
                Platform.runLater(() -> {
                    LoadingOverlay.hide();
                    Toast.error("Failed to save company info");
                });
                return null;
            });
    }
    
    private void showProfileError(String message) {
        profileErrorLabel.setText(message);
        profileErrorLabel.setVisible(true);
        profileErrorLabel.setManaged(true);
    }
    
    private void hideProfileError() {
        profileErrorLabel.setVisible(false);
        profileErrorLabel.setManaged(false);
    }
}
