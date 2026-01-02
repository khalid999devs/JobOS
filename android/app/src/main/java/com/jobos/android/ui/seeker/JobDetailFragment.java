package com.jobos.android.ui.seeker;

import android.content.Intent;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.data.model.job.JobDTO;
import java.util.Locale;

public class JobDetailFragment extends BaseFragment {

    private ImageView backButton;
    private ImageView saveButton;
    private ImageView shareButton;
    private TextView jobTitle;
    private TextView companyName;
    private TextView location;
    private TextView jobType;
    private TextView salary;
    private TextView description;
    private TextView requirements;
    private TextView postedDate;
    private ChipGroup skillsChipGroup;
    private Button applyButton;
    private ProgressBar progressBar;

    private ApiService apiService;
    private String jobId;
    private JobDTO currentJob;
    private boolean isSaved = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_job_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getArguments() != null) {
            jobId = getArguments().getString("jobId");
        }
        
        apiService = new ApiService();
        initViews(view);
        setupClickListeners();
        
        if (jobId != null && !jobId.isEmpty()) {
            loadJobDetails();
        }
    }

    private void initViews(View view) {
        backButton = view.findViewById(R.id.back_button);
        saveButton = view.findViewById(R.id.save_button);
        shareButton = view.findViewById(R.id.share_button);
        jobTitle = view.findViewById(R.id.job_title);
        companyName = view.findViewById(R.id.company_name);
        location = view.findViewById(R.id.location);
        jobType = view.findViewById(R.id.job_type);
        salary = view.findViewById(R.id.salary);
        description = view.findViewById(R.id.description);
        requirements = view.findViewById(R.id.requirements);
        postedDate = view.findViewById(R.id.posted_date);
        skillsChipGroup = view.findViewById(R.id.skills_chip_group);
        applyButton = view.findViewById(R.id.apply_button);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> navController.navigateUp());
        
        saveButton.setOnClickListener(v -> toggleSave());
        
        shareButton.setOnClickListener(v -> shareJob());
        
        applyButton.setOnClickListener(v -> {
            if (currentJob != null) {
                Bundle args = new Bundle();
                args.putString("jobId", currentJob.getId());
                navController.navigate(R.id.action_job_detail_to_apply, args);
            }
        });
    }

    private void loadJobDetails() {
        progressBar.setVisibility(View.VISIBLE);

        apiService.getJobById(jobId, new ApiCallback<JobDTO>() {
            @Override
            public void onSuccess(JobDTO job) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    currentJob = job;
                    displayJob(job);
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    showToast(error);
                });
            }
        });
    }

    private void displayJob(JobDTO job) {
        jobTitle.setText(job.getTitle());
        companyName.setText(job.getCompanyName());
        location.setText(job.getLocation());
        jobType.setText(formatJobType(job.getJobType()));
        
        if (job.getSalaryMin() != null && job.getSalaryMax() != null) {
            salary.setText(String.format(Locale.getDefault(), "$%,d - $%,d/yr", 
                job.getSalaryMin().intValue(), job.getSalaryMax().intValue()));
        } else if (job.getSalaryMin() != null) {
            salary.setText(String.format(Locale.getDefault(), "From $%,d/yr", job.getSalaryMin().intValue()));
        } else {
            salary.setText("Not specified");
        }

        description.setText(job.getDescription());
        requirements.setText(job.getRequirements());

        if (job.getCreatedAt() != null) {
            postedDate.setText(job.getCreatedAt());
        }

        isSaved = Boolean.TRUE.equals(job.getSaved());
        updateSaveIcon();

        skillsChipGroup.removeAllViews();
        if (job.getSkills() != null) {
            for (String skill : job.getSkills()) {
                Chip chip = new Chip(requireContext());
                chip.setText(skill);
                chip.setClickable(false);
                skillsChipGroup.addView(chip);
            }
        }

        if (job.isApplied()) {
            applyButton.setText(R.string.already_applied);
            applyButton.setEnabled(false);
        }
    }

    private void toggleSave() {
        if (currentJob == null) return;

        String token = sessionManager.getAccessToken();
        if (isSaved) {
            apiService.unsaveJob(token, currentJob.getId(), new ApiCallback<String>() {
                @Override
                public void onSuccess(String response) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        isSaved = false;
                        updateSaveIcon();
                        showToast("Job removed from saved");
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> showToast(error));
                }
            });
        } else {
            apiService.saveJob(token, currentJob.getId(), new ApiCallback<String>() {
                @Override
                public void onSuccess(String response) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        isSaved = true;
                        updateSaveIcon();
                        showToast("Job saved");
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> showToast(error));
                }
            });
        }
    }

    private void updateSaveIcon() {
        saveButton.setImageResource(isSaved ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);
    }

    private void shareJob() {
        if (currentJob == null) return;

        String shareText = String.format("Check out this job: %s at %s\n\n%s",
            currentJob.getTitle(), currentJob.getCompanyName(), currentJob.getDescription());

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share Job"));
    }

    private String formatJobType(String type) {
        if (type == null) return "";
        return type.replace("_", " ");
    }
}
