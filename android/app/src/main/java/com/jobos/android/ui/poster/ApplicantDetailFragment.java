package com.jobos.android.ui.poster;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.data.model.application.ApplicationDTO;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ApplicantDetailFragment extends BaseFragment {

    private MaterialToolbar toolbar;
    private TextView applicantName;
    private TextView applicantEmail;
    private TextView statusBadge;
    private TextView jobTitle;
    private TextView appliedDate;
    private MaterialCardView coverLetterCard;
    private TextView coverLetter;
    private MaterialCardView cvCard;
    private TextView cvName;
    private MaterialButton viewCvButton;
    private ChipGroup statusChipGroup;
    private Chip chipReviewing;
    private Chip chipShortlisted;
    private Chip chipHired;
    private Chip chipRejected;
    private MaterialButton updateStatusButton;
    private ProgressBar progressBar;

    private String applicationId = null;
    private ApiService apiService;
    private ApplicationDTO currentApplication;
    private String selectedStatus;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_applicant_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = new ApiService();
        hideBottomNav();
        
        if (getArguments() != null) {
            applicationId = getArguments().getString("applicationId");
        }
        
        initViews(view);
        setupClickListeners();
        loadApplicationDetails();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        applicantName = view.findViewById(R.id.applicant_name);
        applicantEmail = view.findViewById(R.id.applicant_email);
        statusBadge = view.findViewById(R.id.status_badge);
        jobTitle = view.findViewById(R.id.job_title);
        appliedDate = view.findViewById(R.id.applied_date);
        coverLetterCard = view.findViewById(R.id.cover_letter_card);
        coverLetter = view.findViewById(R.id.cover_letter);
        cvCard = view.findViewById(R.id.cv_card);
        cvName = view.findViewById(R.id.cv_name);
        viewCvButton = view.findViewById(R.id.view_cv_button);
        statusChipGroup = view.findViewById(R.id.status_chip_group);
        chipReviewing = view.findViewById(R.id.chip_reviewing);
        chipShortlisted = view.findViewById(R.id.chip_shortlisted);
        chipHired = view.findViewById(R.id.chip_hired);
        chipRejected = view.findViewById(R.id.chip_rejected);
        updateStatusButton = view.findViewById(R.id.update_status_button);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupClickListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());

        statusChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chip_reviewing) {
                    selectedStatus = "REVIEWING";
                } else if (checkedId == R.id.chip_shortlisted) {
                    selectedStatus = "SHORTLISTED";
                } else if (checkedId == R.id.chip_hired) {
                    selectedStatus = "HIRED";
                } else if (checkedId == R.id.chip_rejected) {
                    selectedStatus = "REJECTED";
                }
            }
        });

        updateStatusButton.setOnClickListener(v -> {
            if (selectedStatus == null || selectedStatus.isEmpty()) {
                showToast("Please select a status");
                return;
            }
            updateApplicationStatus();
        });

        viewCvButton.setOnClickListener(v -> {
            if (currentApplication != null && currentApplication.getCvId() != null) {
                Bundle args = new Bundle();
                args.putString("cvId", currentApplication.getCvId());
                navController.navigate(R.id.cvPreviewFragment, args);
            }
        });
    }

    private void loadApplicationDetails() {
        if (applicationId == null || applicationId.isEmpty()) {
            showToast("Invalid application");
            navController.popBackStack();
            return;
        }

        showLoading(true);
        apiService.getApplicationDetails(sessionManager.getAccessToken(), applicationId, 
            new ApiCallback<ApplicationDTO>() {
                @Override
                public void onSuccess(ApplicationDTO result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        currentApplication = result;
                        displayApplicationDetails();
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Error loading application: " + error);
                    });
                }
            });
    }

    private void displayApplicationDetails() {
        if (currentApplication == null) return;

        applicantName.setText(currentApplication.getApplicantName());
        applicantEmail.setText(currentApplication.getApplicantEmail());
        jobTitle.setText(currentApplication.getJobTitle());
        
        if (currentApplication.getCreatedAt() != null) {
            appliedDate.setText("Applied on " + currentApplication.getCreatedAt());
        }

        setupStatusBadge(currentApplication.getStatus());
        selectedStatus = currentApplication.getStatus();
        selectCurrentStatusChip(currentApplication.getStatus());

        String coverLetterText = currentApplication.getCoverLetter();
        if (coverLetterText != null && !coverLetterText.isEmpty()) {
            coverLetter.setText(coverLetterText);
            coverLetterCard.setVisibility(View.VISIBLE);
        } else {
            coverLetterCard.setVisibility(View.GONE);
        }

        String cvTitle = currentApplication.getCvTitle();
        if (cvTitle != null && !cvTitle.isEmpty()) {
            cvName.setText(cvTitle);
            cvCard.setVisibility(View.VISIBLE);
        } else {
            cvCard.setVisibility(View.GONE);
        }
    }

    private void selectCurrentStatusChip(String status) {
        if (status == null) return;
        switch (status) {
            case "REVIEWING": chipReviewing.setChecked(true); break;
            case "SHORTLISTED": chipShortlisted.setChecked(true); break;
            case "HIRED": chipHired.setChecked(true); break;
            case "REJECTED": chipRejected.setChecked(true); break;
        }
    }

    private void setupStatusBadge(String status) {
        if (status == null) status = "PENDING";
        
        int bgColor;
        int textColor;
        String displayText;

        switch (status) {
            case "REVIEWING":
                bgColor = R.color.status_reviewing_bg;
                textColor = R.color.status_reviewing;
                displayText = "Reviewing";
                break;
            case "SHORTLISTED":
                bgColor = R.color.status_shortlisted_bg;
                textColor = R.color.status_shortlisted;
                displayText = "Shortlisted";
                break;
            case "HIRED":
                bgColor = R.color.status_hired_bg;
                textColor = R.color.status_hired;
                displayText = "Hired";
                break;
            case "REJECTED":
                bgColor = R.color.status_rejected_bg;
                textColor = R.color.status_rejected;
                displayText = "Rejected";
                break;
            default:
                bgColor = R.color.status_pending_bg;
                textColor = R.color.status_pending;
                displayText = "Pending";
                break;
        }

        statusBadge.setText(displayText);
        statusBadge.setTextColor(ContextCompat.getColor(requireContext(), textColor));
        statusBadge.setBackgroundColor(ContextCompat.getColor(requireContext(), bgColor));
    }

    private void updateApplicationStatus() {
        showLoading(true);
        apiService.updateApplicationStatus(
            sessionManager.getAccessToken(), applicationId, selectedStatus, 
            new ApiCallback<ApplicationDTO>() {
                @Override
                public void onSuccess(ApplicationDTO result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        currentApplication = result;
                        setupStatusBadge(result.getStatus());
                        showToast("Status updated successfully");
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Error updating status: " + error);
                    });
                }
            });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        updateStatusButton.setEnabled(!show);
    }
}
