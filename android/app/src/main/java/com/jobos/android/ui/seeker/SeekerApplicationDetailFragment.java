package com.jobos.android.ui.seeker;

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
import com.jobos.android.R;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.data.model.application.ApplicationDTO;

/**
 * Fragment for seekers to view their application details.
 * This is a read-only view - seekers cannot update application status.
 */
public class SeekerApplicationDetailFragment extends BaseFragment {

    private MaterialToolbar toolbar;
    private TextView jobTitle;
    private TextView companyName;
    private TextView statusBadge;
    private TextView appliedDate;
    private TextView statusDescription;
    private MaterialCardView coverLetterCard;
    private TextView coverLetter;
    private MaterialCardView cvCard;
    private TextView cvName;
    private MaterialButton viewCvButton;
    private MaterialButton viewJobButton;
    private ProgressBar progressBar;

    private String applicationId = null;
    private ApiService apiService;
    private ApplicationDTO currentApplication;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seeker_application_detail, container, false);
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
        jobTitle = view.findViewById(R.id.job_title);
        companyName = view.findViewById(R.id.company_name);
        statusBadge = view.findViewById(R.id.status_badge);
        appliedDate = view.findViewById(R.id.applied_date);
        statusDescription = view.findViewById(R.id.status_description);
        coverLetterCard = view.findViewById(R.id.cover_letter_card);
        coverLetter = view.findViewById(R.id.cover_letter);
        cvCard = view.findViewById(R.id.cv_card);
        cvName = view.findViewById(R.id.cv_name);
        viewCvButton = view.findViewById(R.id.view_cv_button);
        viewJobButton = view.findViewById(R.id.view_job_button);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupClickListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());

        viewCvButton.setOnClickListener(v -> {
            if (currentApplication != null && currentApplication.getCvId() != null) {
                Bundle args = new Bundle();
                args.putString("cvId", currentApplication.getCvId());
                navController.navigate(R.id.cvPreviewFragment, args);
            }
        });

        viewJobButton.setOnClickListener(v -> {
            if (currentApplication != null && currentApplication.getJobId() != null) {
                Bundle args = new Bundle();
                args.putString("jobId", currentApplication.getJobId());
                navController.navigate(R.id.jobDetailFragment, args);
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

        jobTitle.setText(currentApplication.getJobTitle());
        
        String company = currentApplication.getCompanyName();
        if (company != null && !company.isEmpty()) {
            companyName.setText(company);
            companyName.setVisibility(View.VISIBLE);
        }
        
        if (currentApplication.getCreatedAt() != null) {
            appliedDate.setText("Applied on " + currentApplication.getCreatedAt());
        }

        setupStatusBadge(currentApplication.getStatus());
        setupStatusDescription(currentApplication.getStatus());

        String coverLetterText = currentApplication.getCoverLetter();
        if (coverLetterText != null && !coverLetterText.isEmpty()) {
            coverLetter.setText(coverLetterText);
            coverLetterCard.setVisibility(View.VISIBLE);
        }

        String cvTitle = currentApplication.getCvTitle();
        if (cvTitle != null && !cvTitle.isEmpty()) {
            cvName.setText(cvTitle);
            cvCard.setVisibility(View.VISIBLE);
        }
    }

    private void setupStatusBadge(String status) {
        if (status == null) status = "PENDING";
        
        int bgColor;
        int textColor;
        String displayText;

        switch (status) {
            case "REVIEWED":
                bgColor = R.color.status_reviewing_bg;
                textColor = R.color.status_reviewing;
                displayText = "Reviewed";
                break;
            case "SHORTLISTED":
                bgColor = R.color.status_shortlisted_bg;
                textColor = R.color.status_shortlisted;
                displayText = "Shortlisted";
                break;
            case "ACCEPTED":
                bgColor = R.color.status_hired_bg;
                textColor = R.color.status_hired;
                displayText = "Accepted";
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

    private void setupStatusDescription(String status) {
        if (status == null) status = "PENDING";
        
        String description;
        switch (status) {
            case "REVIEWED":
                description = "Your application has been reviewed by the employer. They are still evaluating candidates.";
                break;
            case "SHORTLISTED":
                description = "Congratulations! You've been shortlisted for this position. The employer may contact you soon for the next steps.";
                break;
            case "ACCEPTED":
                description = "Great news! You've been accepted for this position. The employer should contact you with further details.";
                break;
            case "REJECTED":
                description = "Unfortunately, your application was not selected for this position. Don't give up - keep applying to other opportunities!";
                break;
            default:
                description = "Your application has been submitted and is awaiting review by the employer.";
                break;
        }
        statusDescription.setText(description);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        showBottomNav();
    }
}
