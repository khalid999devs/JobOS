package com.jobos.android.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.shared.dto.profile.UpdateProfileRequest;
import com.jobos.shared.dto.profile.ProfileResponse;

public class SeekerSetupFragment extends BaseFragment {

    private ImageView backButton;
    private TextInputLayout phoneLayout;
    private TextInputLayout locationLayout;
    private TextInputLayout titleLayout;
    private TextInputLayout experienceLayout;
    private TextInputLayout bioLayout;
    private TextInputEditText phoneInput;
    private TextInputEditText locationInput;
    private TextInputEditText titleInput;
    private TextInputEditText experienceInput;
    private TextInputEditText bioInput;
    private Button continueButton;
    private ProgressBar progressBar;
    private TextView skipLink;

    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seeker_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        apiService = new ApiService();
        initViews(view);
        setupClickListeners();
    }

    private void initViews(View view) {
        backButton = view.findViewById(R.id.back_button);
        phoneLayout = view.findViewById(R.id.phone_layout);
        locationLayout = view.findViewById(R.id.location_layout);
        titleLayout = view.findViewById(R.id.title_layout);
        experienceLayout = view.findViewById(R.id.experience_layout);
        bioLayout = view.findViewById(R.id.bio_layout);
        phoneInput = view.findViewById(R.id.phone_input);
        locationInput = view.findViewById(R.id.location_input);
        titleInput = view.findViewById(R.id.title_input);
        experienceInput = view.findViewById(R.id.experience_input);
        bioInput = view.findViewById(R.id.bio_input);
        continueButton = view.findViewById(R.id.continue_button);
        progressBar = view.findViewById(R.id.progress_bar);
        skipLink = view.findViewById(R.id.skip_link);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> navController.navigateUp());
        continueButton.setOnClickListener(v -> saveProfile());
        skipLink.setOnClickListener(v -> navigateToHome());
    }

    private void saveProfile() {
        setLoading(true);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone(getText(phoneInput));
        request.setLocation(getText(locationInput));
        request.setJobTitle(getText(titleInput));
        request.setBio(getText(bioInput));
        
        String expText = getText(experienceInput);
        if (!expText.isEmpty()) {
            try {
                request.setExperienceYears(Integer.parseInt(expText));
            } catch (NumberFormatException ignored) {}
        }

        String token = sessionManager.getAccessToken();
        apiService.updateProfile(token, request, new ApiCallback<ProfileResponse>() {
            @Override
            public void onSuccess(ProfileResponse response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    navigateToHome();
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    showToast(error);
                });
            }
        });
    }

    private void navigateToHome() {
        navController.navigate(R.id.action_seeker_setup_to_home);
    }

    private void setLoading(boolean loading) {
        continueButton.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private String getText(TextInputEditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }
}
