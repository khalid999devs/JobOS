package com.jobos.android.ui.poster;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.jobos.android.data.model.job.JobDTO;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditJobFragment extends BaseFragment {

    private MaterialToolbar toolbar;
    private TextInputEditText titleInput;
    private TextInputEditText descriptionInput;
    private TextInputEditText locationInput;
    private AutoCompleteTextView jobTypeDropdown;
    private AutoCompleteTextView workModeDropdown;
    private AutoCompleteTextView statusDropdown;
    private TextInputEditText salaryMinInput;
    private TextInputEditText salaryMaxInput;
    private TextInputEditText requirementsInput;
    private TextInputEditText skillsInput;
    private MaterialButton saveButton;
    private ProgressBar progressBar;

    private TextInputLayout titleLayout;
    private TextInputLayout descriptionLayout;
    private TextInputLayout locationLayout;

    private String jobId = null;
    private ApiService apiService;
    private JobDTO currentJob;

    private final String[] jobTypes = {"FULL_TIME", "PART_TIME", "CONTRACT", "INTERNSHIP", "FREELANCE"};
    private final String[] workModes = {"ONSITE", "REMOTE", "HYBRID"};
    private final String[] jobStatuses = {"ACTIVE", "CLOSED", "DRAFT"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_job, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = new ApiService();
        hideBottomNav();

        if (getArguments() != null) {
            jobId = getArguments().getString("jobId");
        }

        initViews(view);
        setupDropdowns();
        setupClickListeners();
        loadJobDetails();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        titleInput = view.findViewById(R.id.title_input);
        descriptionInput = view.findViewById(R.id.description_input);
        locationInput = view.findViewById(R.id.location_input);
        jobTypeDropdown = view.findViewById(R.id.job_type_dropdown);
        workModeDropdown = view.findViewById(R.id.work_mode_dropdown);
        statusDropdown = view.findViewById(R.id.status_dropdown);
        salaryMinInput = view.findViewById(R.id.salary_min_input);
        salaryMaxInput = view.findViewById(R.id.salary_max_input);
        requirementsInput = view.findViewById(R.id.requirements_input);
        skillsInput = view.findViewById(R.id.skills_input);
        saveButton = view.findViewById(R.id.save_button);
        progressBar = view.findViewById(R.id.progress_bar);

        titleLayout = view.findViewById(R.id.title_layout);
        descriptionLayout = view.findViewById(R.id.description_layout);
        locationLayout = view.findViewById(R.id.location_layout);
    }

    private void setupDropdowns() {
        jobTypeDropdown.setAdapter(new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_dropdown_item_1line, formatOptions(jobTypes)));

        workModeDropdown.setAdapter(new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_dropdown_item_1line, formatOptions(workModes)));

        statusDropdown.setAdapter(new ArrayAdapter<>(requireContext(),
            android.R.layout.simple_dropdown_item_1line, formatOptions(jobStatuses)));
    }

    private String[] formatOptions(String[] options) {
        String[] formatted = new String[options.length];
        for (int i = 0; i < options.length; i++) {
            formatted[i] = options[i].replace("_", " ");
        }
        return formatted;
    }

    private void setupClickListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete) {
                showDeleteConfirmation();
                return true;
            }
            return false;
        });

        saveButton.setOnClickListener(v -> {
            if (validateForm()) {
                updateJob();
            }
        });
    }

    private void loadJobDetails() {
        if (jobId == null || jobId.isEmpty()) {
            showToast("Invalid job");
            navController.popBackStack();
            return;
        }

        showLoading(true);
        apiService.getJobDetails(sessionManager.getAccessToken(), jobId,
            new ApiCallback<JobDTO>() {
                @Override
                public void onSuccess(JobDTO result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        currentJob = result;
                        populateFields();
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Error loading job: " + error);
                        navController.popBackStack();
                    });
                }
            });
    }

    private void populateFields() {
        if (currentJob == null) return;

        titleInput.setText(currentJob.getTitle());
        descriptionInput.setText(currentJob.getDescription());
        locationInput.setText(currentJob.getLocation());

        if (currentJob.getJobType() != null) {
            jobTypeDropdown.setText(currentJob.getJobType().replace("_", " "), false);
        }
        if (currentJob.getWorkMode() != null) {
            workModeDropdown.setText(currentJob.getWorkMode().replace("_", " "), false);
        }
        if (currentJob.getStatus() != null) {
            statusDropdown.setText(currentJob.getStatus().replace("_", " "), false);
        }

        if (currentJob.getSalaryMin() != null) {
            salaryMinInput.setText(String.valueOf(currentJob.getSalaryMin()));
        }
        if (currentJob.getSalaryMax() != null) {
            salaryMaxInput.setText(String.valueOf(currentJob.getSalaryMax()));
        }

        String requirements = currentJob.getRequirements();
        if (requirements != null && !requirements.isEmpty()) {
            requirementsInput.setText(requirements);
        }

        List<String> skills = currentJob.getSkills();
        if (skills != null && !skills.isEmpty()) {
            skillsInput.setText(String.join(", ", skills));
        }
    }

    private boolean validateForm() {
        boolean isValid = true;
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String location = locationInput.getText().toString().trim();

        if (title.isEmpty()) {
            titleLayout.setError("Title is required");
            isValid = false;
        } else {
            titleLayout.setError(null);
        }

        if (description.isEmpty()) {
            descriptionLayout.setError("Description is required");
            isValid = false;
        } else {
            descriptionLayout.setError(null);
        }

        if (location.isEmpty()) {
            locationLayout.setError("Location is required");
            isValid = false;
        } else {
            locationLayout.setError(null);
        }

        return isValid;
    }

    private void updateJob() {
        showLoading(true);

        Map<String, Object> jobData = new HashMap<>();
        jobData.put("title", titleInput.getText().toString().trim());
        jobData.put("description", descriptionInput.getText().toString().trim());
        jobData.put("location", locationInput.getText().toString().trim());
        jobData.put("jobType", jobTypeDropdown.getText().toString().replace(" ", "_").toUpperCase());
        jobData.put("workMode", workModeDropdown.getText().toString().replace(" ", "_").toUpperCase());
        jobData.put("status", statusDropdown.getText().toString().replace(" ", "_").toUpperCase());

        String minSalary = salaryMinInput.getText().toString().trim();
        String maxSalary = salaryMaxInput.getText().toString().trim();
        if (!minSalary.isEmpty()) {
            jobData.put("salaryMin", Long.parseLong(minSalary));
        }
        if (!maxSalary.isEmpty()) {
            jobData.put("salaryMax", Long.parseLong(maxSalary));
        }

        String requirements = requirementsInput.getText().toString().trim();
        if (!requirements.isEmpty()) {
            List<String> requirementList = Arrays.asList(requirements.split("\n"));
            jobData.put("requirements", requirementList);
        }

        String skills = skillsInput.getText().toString().trim();
        if (!skills.isEmpty()) {
            List<String> skillList = new ArrayList<>();
            for (String skill : skills.split(",")) {
                String trimmed = skill.trim();
                if (!trimmed.isEmpty()) {
                    skillList.add(trimmed);
                }
            }
            jobData.put("skills", skillList);
        }

        apiService.updateJob(sessionManager.getAccessToken(), jobId, jobData,
            new ApiCallback<JobDTO>() {
                @Override
                public void onSuccess(JobDTO result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Job updated successfully");
                        navController.popBackStack();
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Error updating job: " + error);
                    });
                }
            });
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete Job")
            .setMessage("Are you sure you want to delete this job? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> deleteJob())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteJob() {
        showLoading(true);
        apiService.deleteJob(sessionManager.getAccessToken(), jobId,
            new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Job deleted successfully");
                        navController.popBackStack();
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Error deleting job: " + error);
                    });
                }
            });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!show);
    }
}
