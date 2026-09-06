package com.jobos.android.ui.seeker;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import java.text.SimpleDateFormat;
import java.util.Date;
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
        } else {
            showToast("Invalid job");
            navigateBack();
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
        backButton.setOnClickListener(v -> navigateBack());
        
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

        String token = sessionManager.getAccessToken();
        apiService.getJobDetails(token, jobId, new ApiCallback<JobDTO>() {
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
        jobTitle.setText(orDefault(job.getTitle(), "Untitled job"));
        companyName.setText(orDefault(job.getCompanyName(), "Unknown company"));
        location.setText(orDefault(job.getLocation(), "Location not specified"));
        jobType.setText(formatJobType(job.getJobType()));
        salary.setText(formatSalaryRange(job));

        description.setText(orDefault(formatLongText(job.getDescription()), "No description provided."));
        requirements.setText(orDefault(formatLongText(job.getRequirements()), "No requirements listed."));

        if (job.getCreatedAt() != null) {
            postedDate.setText(formatDate(job.getCreatedAt()));
        } else {
            postedDate.setText("Not available");
        }

        isSaved = Boolean.TRUE.equals(job.getSaved());
        updateSaveIcon();

        skillsChipGroup.removeAllViews();
        if (job.getSkills() != null) {
            for (String skill : job.getSkills()) {
                if (TextUtils.isEmpty(skill) || "null".equalsIgnoreCase(skill.trim())) {
                    continue;
                }
                Chip chip = new Chip(requireContext());
                chip.setText(skill.trim());
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
        if (TextUtils.isEmpty(type)) return "Type not specified";
        String[] parts = type.toLowerCase(Locale.getDefault()).split("_");
        StringBuilder formatted = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (formatted.length() > 0) formatted.append(' ');
            formatted.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return formatted.length() > 0 ? formatted.toString() : type;
    }

    private String formatSalaryRange(JobDTO job) {
        Integer min = job.getSalaryMin();
        Integer max = job.getSalaryMax();
        String suffix = "/yr";

        if (min != null && max != null) {
            return "$" + compactSalary(min) + " - $" + compactSalary(max) + suffix;
        }
        if (min != null) {
            return "From $" + compactSalary(min) + suffix;
        }
        if (max != null) {
            return "Up to $" + compactSalary(max) + suffix;
        }
        return "Not specified";
    }

    private String compactSalary(int amount) {
        if (amount >= 1000 && amount % 1000 == 0) {
            return String.format(Locale.getDefault(), "%dK", amount / 1000);
        }
        if (amount >= 1000) {
            return String.format(Locale.getDefault(), "%.1fK", amount / 1000f).replace(".0K", "K");
        }
        return String.format(Locale.getDefault(), "%,d", amount);
    }

    private String formatDate(String rawDate) {
        if (TextUtils.isEmpty(rawDate)) {
            return "Not available";
        }

        String normalized = rawDate.trim();
        if (normalized.length() >= 19 && normalized.charAt(10) == 'T') {
            Date parsed = parseDate(normalized.substring(0, 19), "yyyy-MM-dd'T'HH:mm:ss");
            if (parsed != null) {
                return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(parsed);
            }
        }

        Date parsedDateOnly = parseDate(normalized, "yyyy-MM-dd");
        if (parsedDateOnly != null) {
            return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(parsedDateOnly);
        }

        return rawDate.replace('T', ' ');
    }

    private Date parseDate(String value, String pattern) {
        try {
            return new SimpleDateFormat(pattern, Locale.US).parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String formatLongText(String text) {
        if (TextUtils.isEmpty(text) || "null".equalsIgnoreCase(text.trim())) {
            return "";
        }

        String cleaned = text.trim();
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1)
                    .replace("\",\"", "\n")
                    .replace("\", \"", "\n")
                    .replace("\"", "")
                    .trim();
        }
        return cleaned;
    }

    private String orDefault(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }
}
