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

public class PosterSetupFragment extends BaseFragment {

    private ImageView backButton;
    private TextInputLayout companyNameLayout;
    private TextInputLayout phoneLayout;
    private TextInputLayout locationLayout;
    private TextInputLayout websiteLayout;
    private TextInputLayout descriptionLayout;
    private TextInputEditText companyNameInput;
    private TextInputEditText phoneInput;
    private TextInputEditText locationInput;
    private TextInputEditText websiteInput;
    private TextInputEditText descriptionInput;
    private Button continueButton;
    private ProgressBar progressBar;
    private TextView skipLink;

    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_poster_setup, container, false);
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
        companyNameLayout = view.findViewById(R.id.company_name_layout);
        phoneLayout = view.findViewById(R.id.phone_layout);
        locationLayout = view.findViewById(R.id.location_layout);
        websiteLayout = view.findViewById(R.id.website_layout);
        descriptionLayout = view.findViewById(R.id.description_layout);
        companyNameInput = view.findViewById(R.id.company_name_input);
        phoneInput = view.findViewById(R.id.phone_input);
        locationInput = view.findViewById(R.id.location_input);
        websiteInput = view.findViewById(R.id.website_input);
        descriptionInput = view.findViewById(R.id.description_input);
        continueButton = view.findViewById(R.id.continue_button);
        progressBar = view.findViewById(R.id.progress_bar);
        skipLink = view.findViewById(R.id.skip_link);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> navController.navigateUp());
        continueButton.setOnClickListener(v -> saveProfile());
        skipLink.setOnClickListener(v -> navigateToDashboard());
    }

    private void saveProfile() {
        setLoading(true);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setCompanyName(getText(companyNameInput));
        request.setPhone(getText(phoneInput));
        request.setLocation(getText(locationInput));
        request.setWebsite(getText(websiteInput));
        request.setCompanyDescription(getText(descriptionInput));

        String token = sessionManager.getAccessToken();
        apiService.updateProfile(token, request, new ApiCallback<ProfileResponse>() {
            @Override
            public void onSuccess(ProfileResponse response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    navigateToDashboard();
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

    private void navigateToDashboard() {
        navController.navigate(R.id.action_poster_setup_to_dashboard);
    }

    private void setLoading(boolean loading) {
        continueButton.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private String getText(TextInputEditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }
}
