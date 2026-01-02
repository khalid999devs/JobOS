package com.jobos.android.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.data.model.profile.ProfileResponse;
import com.jobos.android.data.model.profile.UpdateProfileRequest;
import java.util.ArrayList;
import java.util.List;

public class EditProfileFragment extends BaseFragment {

    private MaterialToolbar toolbar;
    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText phoneInput;
    private TextInputEditText locationInput;
    private TextInputEditText bioInput;
    private LinearLayout seekerFields;
    private TextInputEditText jobTitleInput;
    private TextInputEditText skillsInput;
    private LinearLayout posterFields;
    private TextInputEditText companyNameInput;
    private TextInputEditText companyWebsiteInput;
    private MaterialButton saveButton;
    private ProgressBar progressBar;
    private TextInputLayout nameLayout;

    private boolean isSeeker;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hideBottomNav();
        
        apiService = new ApiService();
        isSeeker = "SEEKER".equals(sessionManager.getUserRole());
        
        initViews(view);
        setupClickListeners();
        loadProfile();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        nameInput = view.findViewById(R.id.name_input);
        emailInput = view.findViewById(R.id.email_input);
        phoneInput = view.findViewById(R.id.phone_input);
        locationInput = view.findViewById(R.id.location_input);
        bioInput = view.findViewById(R.id.bio_input);
        seekerFields = view.findViewById(R.id.seeker_fields);
        jobTitleInput = view.findViewById(R.id.job_title_input);
        skillsInput = view.findViewById(R.id.skills_input);
        posterFields = view.findViewById(R.id.poster_fields);
        companyNameInput = view.findViewById(R.id.company_name_input);
        companyWebsiteInput = view.findViewById(R.id.company_website_input);
        saveButton = view.findViewById(R.id.save_button);
        progressBar = view.findViewById(R.id.progress_bar);
        nameLayout = view.findViewById(R.id.name_layout);

        if (isSeeker) {
            seekerFields.setVisibility(View.VISIBLE);
        } else {
            posterFields.setVisibility(View.VISIBLE);
        }
    }

    private void setupClickListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
        saveButton.setOnClickListener(v -> {
            if (validateForm()) {
                saveProfile();
            }
        });
    }

    private void loadProfile() {
        showLoading(true);
        apiService.getProfile(sessionManager.getAccessToken(),
            new ApiCallback<ProfileResponse>() {
                @Override
                public void onSuccess(ProfileResponse result) {
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
                        showToast("Error loading profile: " + error);
                    });
                }
            });
    }

    private void populateFields(ProfileResponse profile) {
        nameInput.setText(profile.getName());
        emailInput.setText(profile.getEmail());
        phoneInput.setText(profile.getPhone());
        locationInput.setText(profile.getLocation());
        bioInput.setText(profile.getBio());

        if (isSeeker) {
            jobTitleInput.setText(profile.getJobTitle());
            List<String> skills = profile.getSkills();
            if (skills != null && !skills.isEmpty()) {
                skillsInput.setText(String.join(", ", skills));
            }
        } else {
            companyNameInput.setText(profile.getCompanyName());
            companyWebsiteInput.setText(profile.getCompanyWebsite());
        }
    }

    private boolean validateForm() {
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            nameLayout.setError("Name is required");
            return false;
        } else {
            nameLayout.setError(null);
        }
        return true;
    }

    private void saveProfile() {
        showLoading(true);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setName(nameInput.getText().toString().trim());
        
        String phone = phoneInput.getText().toString().trim();
        if (!phone.isEmpty()) request.setPhone(phone);
        
        String location = locationInput.getText().toString().trim();
        if (!location.isEmpty()) request.setLocation(location);
        
        String bio = bioInput.getText().toString().trim();
        if (!bio.isEmpty()) request.setBio(bio);

        if (isSeeker) {
            String jobTitle = jobTitleInput.getText().toString().trim();
            if (!jobTitle.isEmpty()) request.setJobTitle(jobTitle);
            
            String skills = skillsInput.getText().toString().trim();
            if (!skills.isEmpty()) {
                List<String> skillList = new ArrayList<>();
                for (String skill : skills.split(",")) {
                    String trimmed = skill.trim();
                    if (!trimmed.isEmpty()) skillList.add(trimmed);
                }
                request.setSkills(skillList);
            }
        } else {
            String companyName = companyNameInput.getText().toString().trim();
            if (!companyName.isEmpty()) request.setCompanyName(companyName);
            
            String companyWebsite = companyWebsiteInput.getText().toString().trim();
            if (!companyWebsite.isEmpty()) request.setCompanyWebsite(companyWebsite);
        }

        apiService.updateProfile(sessionManager.getAccessToken(), request,
            new ApiCallback<ProfileResponse>() {
                @Override
                public void onSuccess(ProfileResponse result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        sessionManager.saveUserInfo(result.getId(), result.getEmail(), result.getName(), sessionManager.getUserRole());
                        showToast("Profile updated successfully");
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
            });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!show);
    }
}
