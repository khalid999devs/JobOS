package com.jobos.android.ui.onboarding;

import android.app.DatePickerDialog;
import android.os.Bundle;
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
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.data.model.profile.SeekerPreferencesRequest;
import com.jobos.android.data.model.profile.ProfileResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SeekerSetupFragment extends BaseFragment {

    private ImageView backButton;
    private ChipGroup desiredRolesChips;
    private TextInputLayout customRoleLayout;
    private TextInputEditText customRoleInput;
    private ChipGroup skillsChips;
    private TextInputLayout customSkillLayout;
    private TextInputEditText customSkillInput;
    private ChipGroup jobTypesChips;
    private AutoCompleteTextView workingHoursDropdown;
    private TextInputEditText salaryMinInput;
    private TextInputEditText salaryMaxInput;
    private SwitchMaterial relocateSwitch;
    private TextInputEditText availableFromInput;
    private Button continueButton;
    private Button skipButton;
    private ProgressBar progressBar;

    private ApiService apiService;
    private Set<String> selectedRoles = new HashSet<>();
    private Set<String> selectedSkills = new HashSet<>();
    private Set<String> selectedJobTypes = new HashSet<>();
    private String selectedAvailableFrom = null;
    private Calendar calendar = Calendar.getInstance();

    private static final String[] DEFAULT_ROLES = {"Software Engineer", "Product Manager", "Designer", "Data Analyst", "DevOps Engineer", "QA Engineer"};
    private static final String[] DEFAULT_SKILLS = {"Java", "Python", "JavaScript", "React", "Node.js", "SQL", "AWS", "Docker", "Kotlin", "Swift"};
    private static final String[] JOB_TYPES = {"FULL_TIME", "PART_TIME", "CONTRACT", "FREELANCE", "INTERNSHIP", "REMOTE"};
    private static final String[] WORKING_HOURS = {"Flexible", "9-5", "Morning Shift", "Evening Shift", "Night Shift", "Weekend"};

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
        setupChips();
        setupDropdowns();
        setupClickListeners();
    }

    private void initViews(View view) {
        backButton = view.findViewById(R.id.back_button);
        desiredRolesChips = view.findViewById(R.id.desired_roles_chips);
        customRoleLayout = view.findViewById(R.id.custom_role_layout);
        customRoleInput = view.findViewById(R.id.custom_role_input);
        skillsChips = view.findViewById(R.id.skills_chips);
        customSkillLayout = view.findViewById(R.id.custom_skill_layout);
        customSkillInput = view.findViewById(R.id.custom_skill_input);
        jobTypesChips = view.findViewById(R.id.job_types_chips);
        workingHoursDropdown = view.findViewById(R.id.working_hours_dropdown);
        salaryMinInput = view.findViewById(R.id.salary_min_input);
        salaryMaxInput = view.findViewById(R.id.salary_max_input);
        relocateSwitch = view.findViewById(R.id.relocate_switch);
        availableFromInput = view.findViewById(R.id.available_from_input);
        continueButton = view.findViewById(R.id.continue_button);
        skipButton = view.findViewById(R.id.skip_button);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupChips() {
        for (String role : DEFAULT_ROLES) {
            addFilterChip(desiredRolesChips, role, selectedRoles);
        }

        for (String skill : DEFAULT_SKILLS) {
            addFilterChip(skillsChips, skill, selectedSkills);
        }

        for (String type : JOB_TYPES) {
            addFilterChip(jobTypesChips, formatJobType(type), selectedJobTypes);
        }
    }

    private void addFilterChip(ChipGroup group, String text, Set<String> selectedSet) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setCheckedIconVisible(true);
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedSet.add(text);
            } else {
                selectedSet.remove(text);
            }
        });
        group.addView(chip);
    }

    private void addSelectedChip(ChipGroup group, String text, Set<String> selectedSet) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setChecked(true);
        chip.setCheckedIconVisible(true);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            group.removeView(chip);
            selectedSet.remove(text);
        });
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedSet.add(text);
            } else {
                selectedSet.remove(text);
            }
        });
        selectedSet.add(text);
        group.addView(chip);
    }

    private void setupDropdowns() {
        ArrayAdapter<String> hoursAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, WORKING_HOURS);
        workingHoursDropdown.setAdapter(hoursAdapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> navController.navigateUp());
        continueButton.setOnClickListener(v -> savePreferences());
        skipButton.setOnClickListener(v -> navigateToHome());

        customRoleLayout.setEndIconOnClickListener(v -> addCustomRole());
        customRoleInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addCustomRole();
                return true;
            }
            return false;
        });

        customSkillLayout.setEndIconOnClickListener(v -> addCustomSkill());
        customSkillInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addCustomSkill();
                return true;
            }
            return false;
        });

        availableFromInput.setOnClickListener(v -> showDatePicker());
    }

    private void addCustomRole() {
        String role = getText(customRoleInput);
        if (!role.isEmpty() && !selectedRoles.contains(role)) {
            addSelectedChip(desiredRolesChips, role, selectedRoles);
            customRoleInput.setText("");
        }
    }

    private void addCustomSkill() {
        String skill = getText(customSkillInput);
        if (!skill.isEmpty() && !selectedSkills.contains(skill)) {
            addSelectedChip(skillsChips, skill, selectedSkills);
            customSkillInput.setText("");
        }
    }

    private void showDatePicker() {
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            selectedAvailableFrom = sdf.format(calendar.getTime());
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            availableFromInput.setText(displayFormat.format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis());
        dialog.show();
    }

    private void savePreferences() {
        setLoading(true);

        SeekerPreferencesRequest request = new SeekerPreferencesRequest();
        request.setDesiredRoles(new ArrayList<>(selectedRoles));
        request.setSkills(new ArrayList<>(selectedSkills));
        request.setJobTypes(convertJobTypes(new ArrayList<>(selectedJobTypes)));
        request.setWorkingHours(workingHoursDropdown.getText().toString().trim());
        request.setWillingToRelocate(relocateSwitch.isChecked());
        request.setAvailableFrom(selectedAvailableFrom);

        String minText = getText(salaryMinInput);
        String maxText = getText(salaryMaxInput);
        if (!minText.isEmpty()) {
            try {
                request.setSalaryMin(Integer.parseInt(minText));
            } catch (NumberFormatException ignored) {}
        }
        if (!maxText.isEmpty()) {
            try {
                request.setSalaryMax(Integer.parseInt(maxText));
            } catch (NumberFormatException ignored) {}
        }

        String token = sessionManager.getAccessToken();
        apiService.updatePreferences(token, request, new ApiCallback<ProfileResponse>() {
            @Override
            public void onSuccess(ProfileResponse response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    showToast("Preferences saved successfully");
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

    private List<String> convertJobTypes(List<String> displayTypes) {
        List<String> types = new ArrayList<>();
        for (String display : displayTypes) {
            for (String type : JOB_TYPES) {
                if (formatJobType(type).equals(display)) {
                    types.add(type);
                    break;
                }
            }
        }
        return types;
    }

    private String formatJobType(String type) {
        return type.replace("_", " ").toLowerCase().substring(0, 1).toUpperCase() + type.replace("_", " ").toLowerCase().substring(1);
    }

    private void navigateToHome() {
        navController.navigate(R.id.action_seeker_setup_to_home);
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
