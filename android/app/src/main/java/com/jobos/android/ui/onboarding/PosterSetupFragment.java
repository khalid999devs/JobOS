package com.jobos.android.ui.onboarding;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.data.model.profile.PosterProfileRequest;
import com.jobos.android.data.model.profile.ProfileResponse;
import java.util.ArrayList;
import java.util.List;

public class PosterSetupFragment extends BaseFragment {

    private ImageView backButton;
    private TextInputLayout companyNameLayout;
    private TextInputEditText companyNameInput;
    private ChipGroup companySizeChips;
    private AutoCompleteTextView industryDropdown;
    private TextInputEditText websiteInput;
    private ChipGroup verificationChips;
    private TextInputLayout verificationUrlLayout;
    private TextInputEditText verificationUrlInput;
    private Button continueButton;
    private Button skipButton;
    private ProgressBar progressBar;

    private ApiService apiService;
    private String selectedCompanySize = null;
    private List<String> verificationUrls = new ArrayList<>();

    private static final String[] COMPANY_SIZES = {"1-10", "11-50", "51-200", "201-500", "501-1000", "1000+"};
    private static final String[] INDUSTRIES = {
            "Technology", "Healthcare", "Finance", "Education", "E-commerce",
            "Manufacturing", "Consulting", "Real Estate", "Media", "Retail",
            "Transportation", "Energy", "Telecommunications", "Agriculture", "Other"
    };

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
        setupChips();
        setupDropdowns();
        setupClickListeners();
    }

    private void initViews(View view) {
        backButton = view.findViewById(R.id.back_button);
        companyNameLayout = view.findViewById(R.id.company_name_layout);
        companyNameInput = view.findViewById(R.id.company_name_input);
        companySizeChips = view.findViewById(R.id.company_size_chips);
        industryDropdown = view.findViewById(R.id.industry_dropdown);
        websiteInput = view.findViewById(R.id.website_input);
        verificationChips = view.findViewById(R.id.verification_chips);
        verificationUrlLayout = view.findViewById(R.id.verification_url_layout);
        verificationUrlInput = view.findViewById(R.id.verification_url_input);
        continueButton = view.findViewById(R.id.continue_button);
        skipButton = view.findViewById(R.id.skip_button);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupChips() {
        for (String size : COMPANY_SIZES) {
            Chip chip = new Chip(requireContext());
            chip.setText(size + " employees");
            chip.setCheckable(true);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedCompanySize = size;
                    for (int i = 0; i < companySizeChips.getChildCount(); i++) {
                        Chip c = (Chip) companySizeChips.getChildAt(i);
                        if (c != chip) c.setChecked(false);
                    }
                } else if (size.equals(selectedCompanySize)) {
                    selectedCompanySize = null;
                }
            });
            companySizeChips.addView(chip);
        }
    }

    private void setupDropdowns() {
        ArrayAdapter<String> industryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, INDUSTRIES);
        industryDropdown.setAdapter(industryAdapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> navController.navigateUp());
        continueButton.setOnClickListener(v -> saveProfile());
        skipButton.setOnClickListener(v -> navigateToDashboard());

        verificationUrlLayout.setEndIconOnClickListener(v -> addVerificationUrl());
        verificationUrlInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addVerificationUrl();
                return true;
            }
            return false;
        });
    }

    private void addVerificationUrl() {
        String url = getText(verificationUrlInput);
        if (!url.isEmpty() && !verificationUrls.contains(url)) {
            verificationUrls.add(url);
            Chip chip = new Chip(requireContext());
            chip.setText(url);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> {
                verificationChips.removeView(chip);
                verificationUrls.remove(url);
            });
            verificationChips.addView(chip);
            verificationUrlInput.setText("");
        }
    }

    private void saveProfile() {
        String companyName = getText(companyNameInput);

        if (TextUtils.isEmpty(companyName)) {
            companyNameLayout.setError("Company name is required");
            return;
        }
        companyNameLayout.setError(null);

        setLoading(true);

        PosterProfileRequest request = new PosterProfileRequest();
        request.setCompanyName(companyName);
        request.setCompanySize(selectedCompanySize);
        request.setIndustry(industryDropdown.getText().toString().trim());
        request.setWebsite(getText(websiteInput));
        request.setVerificationDocuments(verificationUrls.isEmpty() ? null : verificationUrls);

        String token = sessionManager.getAccessToken();
        apiService.updatePreferences(token, request, new ApiCallback<ProfileResponse>() {
            @Override
            public void onSuccess(ProfileResponse response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    showToast("Company profile saved successfully");
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
        skipButton.setEnabled(!loading);
    }

    private String getText(TextInputEditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }
}
