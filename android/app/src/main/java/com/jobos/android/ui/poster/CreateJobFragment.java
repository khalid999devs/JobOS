package com.jobos.android.ui.poster;

import android.app.DatePickerDialog;
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
import com.jobos.android.network.ApiCallback;
import com.jobos.android.network.ApiService;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.shared.dto.job.JobDTO;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateJobFragment extends BaseFragment {

    private MaterialToolbar toolbar;
    private TextInputEditText titleInput;
    private TextInputEditText descriptionInput;
    private TextInputEditText locationInput;
    private AutoCompleteTextView jobTypeDropdown;
    private AutoCompleteTextView workModeDropdown;
    private TextInputEditText salaryMinInput;
    private TextInputEditText salaryMaxInput;
    private AutoCompleteTextView currencyDropdown;
    private TextInputEditText requirementsInput;
    private TextInputEditText skillsInput;
    private AutoCompleteTextView experienceDropdown;
    private TextInputEditText benefitsInput;
    private TextInputEditText deadlineInput;
    private MaterialButton saveDraftButton;
    private MaterialButton publishButton;
    private ProgressBar progressBar;

    private TextInputLayout titleLayout;
    private TextInputLayout descriptionLayout;
    private TextInputLayout locationLayout;

    private final Calendar deadlineCalendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final String[] jobTypes = {"FULL_TIME", "PART_TIME", "CONTRACT", "INTERNSHIP", "FREELANCE"};
    private final String[] workModes = {"ONSITE", "REMOTE", "HYBRID"};
    private final String[] currencies = {"USD", "EUR", "GBP", "BDT", "INR"};
    private final String[] experienceLevels = {"ENTRY", "JUNIOR", "MID", "SENIOR", "LEAD", "EXECUTIVE"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_job, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hideBottomNav();
        initViews(view);
        setupDropdowns();
        setupClickListeners();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        titleInput = view.findViewById(R.id.title_input);
        descriptionInput = view.findViewById(R.id.description_input);
        locationInput = view.findViewById(R.id.location_input);
        jobTypeDropdown = view.findViewById(R.id.job_type_dropdown);
        workModeDropdown = view.findViewById(R.id.work_mode_dropdown);
        salaryMinInput = view.findViewById(R.id.salary_min_input);
        salaryMaxInput = view.findViewById(R.id.salary_max_input);
        currencyDropdown = view.findViewById(R.id.currency_dropdown);
        requirementsInput = view.findViewById(R.id.requirements_input);
        skillsInput = view.findViewById(R.id.skills_input);
        experienceDropdown = view.findViewById(R.id.experience_dropdown);
        benefitsInput = view.findViewById(R.id.benefits_input);
        deadlineInput = view.findViewById(R.id.deadline_input);
        saveDraftButton = view.findViewById(R.id.save_draft_button);
        publishButton = view.findViewById(R.id.publish_button);
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
        
        currencyDropdown.setAdapter(new ArrayAdapter<>(requireContext(), 
            android.R.layout.simple_dropdown_item_1line, currencies));
        currencyDropdown.setText("USD", false);
        
        experienceDropdown.setAdapter(new ArrayAdapter<>(requireContext(), 
            android.R.layout.simple_dropdown_item_1line, formatOptions(experienceLevels)));
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

        deadlineInput.setOnClickListener(v -> showDatePicker());

        saveDraftButton.setOnClickListener(v -> {
            if (validateForm()) {
                createJob("DRAFT");
            }
        });

        publishButton.setOnClickListener(v -> {
            if (validateForm()) {
                createJob("ACTIVE");
            }
        });
    }

    private void showDatePicker() {
        DatePickerDialog picker = new DatePickerDialog(requireContext(),
            (view, year, month, dayOfMonth) -> {
                deadlineCalendar.set(Calendar.YEAR, year);
                deadlineCalendar.set(Calendar.MONTH, month);
                deadlineCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                deadlineInput.setText(dateFormat.format(deadlineCalendar.getTime()));
            },
            deadlineCalendar.get(Calendar.YEAR),
            deadlineCalendar.get(Calendar.MONTH),
            deadlineCalendar.get(Calendar.DAY_OF_MONTH));
        picker.getDatePicker().setMinDate(System.currentTimeMillis());
        picker.show();
    }

    private boolean validateForm() {
        boolean isValid = true;
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String location = locationInput.getText().toString().trim();
        String jobType = jobTypeDropdown.getText().toString().trim();
        String workMode = workModeDropdown.getText().toString().trim();

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

        if (jobType.isEmpty()) {
            showToast("Please select job type");
            isValid = false;
        }

        if (workMode.isEmpty()) {
            showToast("Please select work mode");
            isValid = false;
        }

        return isValid;
    }

    private void createJob(String status) {
        showLoading(true);

        Map<String, Object> jobData = new HashMap<>();
        jobData.put("title", titleInput.getText().toString().trim());
        jobData.put("description", descriptionInput.getText().toString().trim());
        jobData.put("location", locationInput.getText().toString().trim());
        jobData.put("jobType", jobTypeDropdown.getText().toString().replace(" ", "_").toUpperCase());
        jobData.put("workMode", workModeDropdown.getText().toString().replace(" ", "_").toUpperCase());
        jobData.put("status", status);

        String minSalary = salaryMinInput.getText().toString().trim();
        String maxSalary = salaryMaxInput.getText().toString().trim();
        if (!minSalary.isEmpty()) {
            jobData.put("salaryMin", Long.parseLong(minSalary));
        }
        if (!maxSalary.isEmpty()) {
            jobData.put("salaryMax", Long.parseLong(maxSalary));
        }
        
        String currency = currencyDropdown.getText().toString().trim();
        if (!currency.isEmpty()) {
            jobData.put("currency", currency);
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

        String experience = experienceDropdown.getText().toString().trim();
        if (!experience.isEmpty()) {
            jobData.put("experienceLevel", experience.replace(" ", "_").toUpperCase());
        }

        String benefits = benefitsInput.getText().toString().trim();
        if (!benefits.isEmpty()) {
            List<String> benefitList = Arrays.asList(benefits.split("\n"));
            jobData.put("benefits", benefitList);
        }

        String deadline = deadlineInput.getText().toString().trim();
        if (!deadline.isEmpty()) {
            jobData.put("deadline", deadline);
        }

        ApiService.getInstance(requireContext()).createJob(sessionManager.getAccessToken(), jobData, 
            new ApiCallback<JobDTO>() {
                @Override
                public void onSuccess(JobDTO result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        String message = status.equals("DRAFT") ? "Job saved as draft" : "Job published successfully";
                        showToast(message);
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
        saveDraftButton.setEnabled(!show);
        publishButton.setEnabled(!show);
    }
}
