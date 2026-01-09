package com.jobos.android.ui.cv;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.data.model.cv.CVDTO;
import com.jobos.android.data.model.cv.CVTemplateDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CVEditorFragment extends BaseFragment {

    private MaterialToolbar toolbar;
    private TextInputEditText titleInput;
    private TextInputEditText fullNameInput;
    private TextInputEditText emailInput;
    private TextInputEditText phoneInput;
    private TextInputEditText addressInput;
    private TextInputEditText summaryInput;
    private TextInputEditText skillsInput;
    private TextInputEditText educationInput;
    private TextInputEditText experienceInput;
    private TextInputEditText linkedinInput;
    private TextInputEditText portfolioInput;
    private MaterialButton saveButton;
    private ProgressBar progressBar;
    private TextInputLayout titleLayout;
    private TextView templateInfoText;

    private String cvId = null;
    private String templateId = null;
    private String templateName = null;
    private CVTemplateDTO selectedTemplate = null;
    private ApiService apiService;
    private boolean isEditMode = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cv_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = new ApiService();
        hideBottomNav();

        if (getArguments() != null) {
            cvId = getArguments().getString("cvId");
            templateId = getArguments().getString("templateId");
            templateName = getArguments().getString("templateName");
        }
        isEditMode = cvId != null && !cvId.isEmpty();

        initViews(view);
        setupClickListeners();

        if (isEditMode) {
            toolbar.setTitle("Edit CV");
            loadCVDetails();
        } else if (templateId != null && !templateId.isEmpty()) {
            toolbar.setTitle("Create CV");
            loadTemplateDetails();
        } else {
            toolbar.setTitle("Create CV");
            prefillUserInfo();
        }
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        titleInput = view.findViewById(R.id.title_input);
        fullNameInput = view.findViewById(R.id.full_name_input);
        emailInput = view.findViewById(R.id.email_input);
        phoneInput = view.findViewById(R.id.phone_input);
        addressInput = view.findViewById(R.id.address_input);
        summaryInput = view.findViewById(R.id.summary_input);
        skillsInput = view.findViewById(R.id.skills_input);
        educationInput = view.findViewById(R.id.education_input);
        experienceInput = view.findViewById(R.id.experience_input);
        linkedinInput = view.findViewById(R.id.linkedin_input);
        portfolioInput = view.findViewById(R.id.portfolio_input);
        saveButton = view.findViewById(R.id.save_button);
        progressBar = view.findViewById(R.id.progress_bar);
        titleLayout = view.findViewById(R.id.title_layout);
        templateInfoText = view.findViewById(R.id.template_info);
        
        // Show template info if template is selected
        if (templateName != null && !templateName.isEmpty()) {
            if (templateInfoText != null) {
                templateInfoText.setVisibility(View.VISIBLE);
                templateInfoText.setText("Template: " + templateName);
            }
        }
    }

    private void setupClickListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
        saveButton.setOnClickListener(v -> {
            if (validateForm()) {
                saveCV();
            }
        });
    }

    private void prefillUserInfo() {
        String userName = sessionManager.getUserName();
        String userEmail = sessionManager.getUserEmail();
        if (userName != null) fullNameInput.setText(userName);
        if (userEmail != null) emailInput.setText(userEmail);
    }

    private void loadTemplateDetails() {
        if (templateId == null) {
            prefillUserInfo();
            return;
        }

        showLoading(true);
        apiService.getCVTemplateById(sessionManager.getAccessToken(), templateId,
            new ApiCallback<CVTemplateDTO>() {
                @Override
                public void onSuccess(CVTemplateDTO result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        selectedTemplate = result;
                        prefillUserInfo();
                        applyTemplateConfig(result);
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        prefillUserInfo();
                        showToast("Using default template settings");
                    });
                }
            });
    }

    private void applyTemplateConfig(CVTemplateDTO template) {
        if (template == null) return;

        // Update template info display
        if (templateInfoText != null && template.getName() != null) {
            templateInfoText.setVisibility(View.VISIBLE);
            templateInfoText.setText("Template: " + template.getName() + 
                (template.getCategory() != null ? " (" + template.getCategory() + ")" : ""));
        }

        // Parse sectionsConfig if available to show/hide fields
        String sectionsConfig = template.getSectionsConfig();
        if (sectionsConfig != null && !sectionsConfig.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<Map<String, Object>> sections = mapper.readValue(sectionsConfig, 
                    new TypeReference<List<Map<String, Object>>>() {});
                
                // Configure form based on template sections
                configureSectionsFromTemplate(sections);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Set default title based on template
        if (titleInput.getText().toString().isEmpty() && template.getName() != null) {
            String userName = sessionManager.getUserName();
            if (userName != null) {
                titleInput.setText(userName + "'s " + template.getName() + " CV");
            }
        }
    }

    private void configureSectionsFromTemplate(List<Map<String, Object>> sections) {
        // This method can be expanded to show/hide form sections based on template config
        // For now, it sets hints based on the template structure
        for (Map<String, Object> section : sections) {
            String type = (String) section.get("type");
            String label = (String) section.get("label");
            Boolean required = (Boolean) section.get("required");
            
            if (type == null) continue;
            
            switch (type.toLowerCase()) {
                case "summary":
                case "professional_summary":
                    if (label != null) {
                        TextInputLayout summaryLayout = (TextInputLayout) summaryInput.getParent().getParent();
                        if (summaryLayout != null) {
                            summaryLayout.setHint(label + (Boolean.TRUE.equals(required) ? " *" : ""));
                        }
                    }
                    break;
                case "skills":
                    if (label != null) {
                        TextInputLayout skillsLayout = (TextInputLayout) skillsInput.getParent().getParent();
                        if (skillsLayout != null) {
                            skillsLayout.setHint(label + (Boolean.TRUE.equals(required) ? " *" : ""));
                        }
                    }
                    break;
                case "education":
                    if (label != null) {
                        TextInputLayout educationLayout = (TextInputLayout) educationInput.getParent().getParent();
                        if (educationLayout != null) {
                            educationLayout.setHint(label + (Boolean.TRUE.equals(required) ? " *" : ""));
                        }
                    }
                    break;
                case "experience":
                case "work_experience":
                    if (label != null) {
                        TextInputLayout experienceLayout = (TextInputLayout) experienceInput.getParent().getParent();
                        if (experienceLayout != null) {
                            experienceLayout.setHint(label + (Boolean.TRUE.equals(required) ? " *" : ""));
                        }
                    }
                    break;
            }
        }
    }

    private void loadCVDetails() {
        if (cvId == null) return;

        showLoading(true);
        apiService.getCVDetails(sessionManager.getAccessToken(), cvId,
            new ApiCallback<CVDTO>() {
                @Override
                public void onSuccess(CVDTO result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        populateFields(result);
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Error loading CV: " + error);
                    });
                }
            });
    }

    private void populateFields(CVDTO cv) {
        titleInput.setText(cv.getTitle());
        fullNameInput.setText(cv.getFullName());
        emailInput.setText(cv.getEmail());
        phoneInput.setText(cv.getPhone());
        addressInput.setText(cv.getAddress());
        summaryInput.setText(cv.getSummary());
        linkedinInput.setText(cv.getLinkedinUrl());
        portfolioInput.setText(cv.getPortfolioUrl());

        List<String> skills = cv.getSkills();
        if (skills != null && !skills.isEmpty()) {
            skillsInput.setText(String.join(", ", skills));
        }

        List<String> education = cv.getEducation();
        if (education != null && !education.isEmpty()) {
            educationInput.setText(String.join("\n", education));
        }

        List<String> experience = cv.getExperience();
        if (experience != null && !experience.isEmpty()) {
            experienceInput.setText(String.join("\n", experience));
        }
    }

    private boolean validateForm() {
        String title = titleInput.getText().toString().trim();
        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();

        if (title.isEmpty()) {
            titleLayout.setError("Title is required");
            return false;
        } else {
            titleLayout.setError(null);
        }

        if (fullName.isEmpty()) {
            showToast("Full name is required");
            return false;
        }

        if (email.isEmpty()) {
            showToast("Email is required");
            return false;
        }

        return true;
    }

    private void saveCV() {
        showLoading(true);

        Map<String, Object> cvData = new HashMap<>();
        cvData.put("title", titleInput.getText().toString().trim());
        cvData.put("fullName", fullNameInput.getText().toString().trim());
        cvData.put("email", emailInput.getText().toString().trim());

        String phone = phoneInput.getText().toString().trim();
        if (!phone.isEmpty()) cvData.put("phone", phone);

        String address = addressInput.getText().toString().trim();
        if (!address.isEmpty()) cvData.put("address", address);

        String summary = summaryInput.getText().toString().trim();
        if (!summary.isEmpty()) cvData.put("summary", summary);

        String linkedin = linkedinInput.getText().toString().trim();
        if (!linkedin.isEmpty()) cvData.put("linkedinUrl", linkedin);

        String portfolio = portfolioInput.getText().toString().trim();
        if (!portfolio.isEmpty()) cvData.put("portfolioUrl", portfolio);

        String skills = skillsInput.getText().toString().trim();
        if (!skills.isEmpty()) {
            List<String> skillList = new ArrayList<>();
            for (String skill : skills.split(",")) {
                String trimmed = skill.trim();
                if (!trimmed.isEmpty()) skillList.add(trimmed);
            }
            cvData.put("skills", skillList);
        }

        String education = educationInput.getText().toString().trim();
        if (!education.isEmpty()) {
            cvData.put("education", Arrays.asList(education.split("\n")));
        }

        String experience = experienceInput.getText().toString().trim();
        if (!experience.isEmpty()) {
            cvData.put("experience", Arrays.asList(experience.split("\n")));
        }

        // Include template ID if creating from template
        if (!isEditMode && templateId != null && !templateId.isEmpty()) {
            cvData.put("templateId", templateId);
        }

        ApiCallback<CVDTO> callback = new ApiCallback<CVDTO>() {
            @Override
            public void onSuccess(CVDTO result) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    showToast(isEditMode ? "CV updated successfully" : "CV created successfully");
                    navController.popBackStack();
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    showLoading(false);
                    showToast("Error: " + error);
                });
            }
        };

        if (isEditMode && cvId != null) {
            apiService.updateCV(sessionManager.getAccessToken(), cvId, cvData, callback);
        } else {
            apiService.createCV(sessionManager.getAccessToken(), cvData, callback);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!show);
    }
}
