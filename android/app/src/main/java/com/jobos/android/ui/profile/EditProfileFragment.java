package com.jobos.android.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.data.model.profile.ProfileResponse;
import com.jobos.android.data.model.profile.ProfileResponse.PosterProfileData;
import com.jobos.android.data.model.profile.UpdateProfileRequest;
import com.jobos.android.data.local.UserDataManager;
import java.util.ArrayList;
import java.util.List;

public class EditProfileFragment extends BaseFragment {

    private MaterialToolbar toolbar;
    private TextInputEditText firstNameInput;
    private TextInputEditText lastNameInput;
    private TextInputEditText emailInput;
    private TextInputEditText phoneInput;
    private TextInputEditText locationInput;
    private AutoCompleteTextView timezoneDropdown;
    private TextInputEditText bioInput;
    private LinearLayout seekerFields;
    private TextInputEditText jobTitleInput;
    private TextInputEditText skillsInput;
    private TextInputEditText desiredRolesInput;
    private TextInputEditText minSalaryInput;
    private TextInputEditText maxSalaryInput;
    private ChipGroup jobTypeChips;
    private ChipGroup workModeChips;
    private ChipGroup experienceLevelChips;
    private LinearLayout posterFields;
    private TextInputEditText companyNameInput;
    private TextInputEditText companyWebsiteInput;
    private ChipGroup companySizeChips;
    private AutoCompleteTextView industryDropdown;
    private ChipGroup verificationDocsChips;
    private TextInputEditText docUrlInput;
    private MaterialButton addDocButton;
    private MaterialButton saveButton;
    private ProgressBar progressBar;
    private TextInputLayout firstNameLayout;
    private TextInputLayout lastNameLayout;
    private TextInputLayout companyNameLayout;

    private boolean isPoster;
    private ApiService apiService;
    private List<String> verificationDocs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hideBottomNav();
        
        apiService = new ApiService();
        isPoster = "POSTER".equals(sessionManager.getUserRole());
        
        initViews(view);
        setupClickListeners();
        loadProfile();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        firstNameInput = view.findViewById(R.id.first_name_input);
        lastNameInput = view.findViewById(R.id.last_name_input);
        emailInput = view.findViewById(R.id.email_input);
        phoneInput = view.findViewById(R.id.phone_input);
        locationInput = view.findViewById(R.id.location_input);
        timezoneDropdown = view.findViewById(R.id.timezone_dropdown);
        bioInput = view.findViewById(R.id.bio_input);
        seekerFields = view.findViewById(R.id.seeker_fields);
        jobTitleInput = view.findViewById(R.id.job_title_input);
        skillsInput = view.findViewById(R.id.skills_input);
        desiredRolesInput = view.findViewById(R.id.desired_roles_input);
        minSalaryInput = view.findViewById(R.id.min_salary_input);
        maxSalaryInput = view.findViewById(R.id.max_salary_input);
        jobTypeChips = view.findViewById(R.id.job_type_chips);
        workModeChips = view.findViewById(R.id.work_mode_chips);
        experienceLevelChips = view.findViewById(R.id.experience_level_chips);
        posterFields = view.findViewById(R.id.poster_fields);
        companyNameInput = view.findViewById(R.id.company_name_input);
        companyWebsiteInput = view.findViewById(R.id.company_website_input);
        companySizeChips = view.findViewById(R.id.company_size_chips);
        industryDropdown = view.findViewById(R.id.industry_dropdown);
        verificationDocsChips = view.findViewById(R.id.verification_docs_chips);
        docUrlInput = view.findViewById(R.id.doc_url_input);
        addDocButton = view.findViewById(R.id.add_doc_button);
        saveButton = view.findViewById(R.id.save_button);
        progressBar = view.findViewById(R.id.progress_bar);
        firstNameLayout = view.findViewById(R.id.first_name_layout);
        lastNameLayout = view.findViewById(R.id.last_name_layout);
        companyNameLayout = view.findViewById(R.id.company_name_layout);

        if (isPoster) {
            posterFields.setVisibility(View.VISIBLE);
            setupIndustryDropdown();
        } else {
            seekerFields.setVisibility(View.VISIBLE);
        }
        setupTimezoneDropdown();
    }

    private void setupTimezoneDropdown() {
        String[] timezones = {
            "UTC-12:00", "UTC-11:00", "UTC-10:00", "UTC-09:00", "UTC-08:00 (PST)",
            "UTC-07:00 (MST)", "UTC-06:00 (CST)", "UTC-05:00 (EST)", "UTC-04:00",
            "UTC-03:00", "UTC-02:00", "UTC-01:00", "UTC+00:00 (GMT)", "UTC+01:00",
            "UTC+02:00", "UTC+03:00", "UTC+04:00", "UTC+05:00", "UTC+05:30 (IST)",
            "UTC+06:00 (BST)", "UTC+07:00", "UTC+08:00", "UTC+09:00", "UTC+10:00",
            "UTC+11:00", "UTC+12:00"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            timezones
        );
        timezoneDropdown.setAdapter(adapter);
    }

    private void setupIndustryDropdown() {
        String[] industries = {
            "Technology", "Healthcare", "Finance", "Education", "Retail",
            "Manufacturing", "Hospitality", "Real Estate", "Transportation",
            "Entertainment", "Consulting", "Other"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            industries
        );
        industryDropdown.setAdapter(adapter);
    }

    private void setupClickListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
        
        saveButton.setOnClickListener(v -> {
            if (validateForm()) {
                saveProfile();
            }
        });

        if (isPoster) {
            addDocButton.setOnClickListener(v -> addVerificationDoc());
        }
    }

    private void addVerificationDoc() {
        String url = docUrlInput.getText().toString().trim();
        if (!url.isEmpty()) {
            verificationDocs.add(url);
            addDocChip(url);
            docUrlInput.setText("");
        }
    }

    private void addDocChip(String url) {
        Chip chip = new Chip(requireContext());
        chip.setText(url);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            verificationDocsChips.removeView(chip);
            verificationDocs.remove(url);
        });
        verificationDocsChips.addView(chip);
    }

    private void loadProfile() {
        ProfileResponse profile = UserDataManager.getInstance().getCurrentUser();
        if (profile != null) {
            populateFields(profile);
        } else {
            // Load from API if not in UserDataManager
            showLoading(true);
            apiService.getProfile(sessionManager.getAccessToken(),
                new ApiCallback<ProfileResponse>() {
                    @Override
                    public void onSuccess(ProfileResponse result) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            showLoading(false);
                            UserDataManager.getInstance().setCurrentUser(result);
                            populateFields(result);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            showLoading(false);
                            showToast("Error loading profile: " + error);
                        });
                    }
                });
        }
    }

    private void populateFields(ProfileResponse profile) {
        firstNameInput.setText(profile.getFirstName());
        lastNameInput.setText(profile.getLastName());
        emailInput.setText(profile.getEmail());
        phoneInput.setText(profile.getPhoneNumber());
        locationInput.setText(profile.getLocation());
        bioInput.setText(profile.getBio());
        
        if (profile.getTimezone() != null) {
            timezoneDropdown.setText(profile.getTimezone(), false);
        }

        if (isPoster && profile.getPosterProfile() != null) {
            PosterProfileData posterData = profile.getPosterProfile();
            companyNameInput.setText(posterData.getCompanyName());
            companyWebsiteInput.setText(posterData.getWebsite());
            
            // Set company size
            String companySize = posterData.getCompanySize();
            if (companySize != null) {
                selectCompanySizeChip(companySize);
            }
            
            // Set industry
            industryDropdown.setText(posterData.getIndustry(), false);
            
            // Set verification documents
            List<String> docs = posterData.getVerificationDocuments();
            if (docs != null && !docs.isEmpty()) {
                verificationDocs.clear();
                verificationDocs.addAll(docs);
                verificationDocsChips.removeAllViews();
                for (String doc : docs) {
                    addDocChip(doc);
                }
            }
        } else {
            jobTitleInput.setText(profile.getJobTitle());
            List<String> skills = profile.getSkills();
            if (skills != null && !skills.isEmpty()) {
                skillsInput.setText(String.join(", ", skills));
            }
            
            // Populate seeker preferences
            ProfileResponse.SeekerPreferencesData prefs = profile.getSeekerPreferences();
            if (prefs != null) {
                // Desired roles
                if (prefs.getDesiredRoles() != null && !prefs.getDesiredRoles().isEmpty()) {
                    desiredRolesInput.setText(String.join(", ", prefs.getDesiredRoles()));
                }
                
                // Salary range
                if (prefs.getSalaryMin() != null) {
                    minSalaryInput.setText(String.valueOf(prefs.getSalaryMin()));
                }
                if (prefs.getSalaryMax() != null) {
                    maxSalaryInput.setText(String.valueOf(prefs.getSalaryMax()));
                }
                
                // Job types
                if (prefs.getJobTypes() != null) {
                    for (String type : prefs.getJobTypes()) {
                        setChipChecked(jobTypeChips, getJobTypeChipId(type));
                    }
                }
                
                // Work modes
                if (prefs.getWorkModes() != null) {
                    for (String mode : prefs.getWorkModes()) {
                        setChipChecked(workModeChips, getWorkModeChipId(mode));
                    }
                }
                
                // Experience levels
                if (prefs.getExperienceLevels() != null) {
                    for (String level : prefs.getExperienceLevels()) {
                        setChipChecked(experienceLevelChips, getExperienceLevelChipId(level));
                    }
                }
            }
        }
    }

    private void setChipChecked(ChipGroup chipGroup, int chipId) {
        if (chipId != -1 && chipGroup != null) {
            Chip chip = chipGroup.findViewById(chipId);
            if (chip != null) chip.setChecked(true);
        }
    }

    private int getJobTypeChipId(String type) {
        switch (type) {
            case "FULL_TIME": return R.id.chip_full_time;
            case "PART_TIME": return R.id.chip_part_time;
            case "CONTRACT": return R.id.chip_contract;
            case "INTERNSHIP": return R.id.chip_internship;
            default: return -1;
        }
    }

    private int getWorkModeChipId(String mode) {
        switch (mode) {
            case "REMOTE": return R.id.chip_remote;
            case "HYBRID": return R.id.chip_hybrid;
            case "ONSITE": return R.id.chip_onsite;
            default: return -1;
        }
    }

    private int getExperienceLevelChipId(String level) {
        switch (level) {
            case "ENTRY": return R.id.chip_entry;
            case "MID": return R.id.chip_mid;
            case "SENIOR": return R.id.chip_senior;
            case "LEAD": return R.id.chip_lead;
            default: return -1;
        }
    }

    private void selectCompanySizeChip(String size) {
        int chipId = -1;
        switch (size) {
            case "1-10": chipId = R.id.chip_size_1_10; break;
            case "11-50": chipId = R.id.chip_size_11_50; break;
            case "51-200": chipId = R.id.chip_size_51_200; break;
            case "201-500": chipId = R.id.chip_size_201_500; break;
            case "501+": chipId = R.id.chip_size_501_plus; break;
        }
        if (chipId != -1) {
            companySizeChips.check(chipId);
        }
    }

    private String getSelectedCompanySize() {
        int selectedId = companySizeChips.getCheckedChipId();
        if (selectedId == R.id.chip_size_1_10) return "1-10";
        if (selectedId == R.id.chip_size_11_50) return "11-50";
        if (selectedId == R.id.chip_size_51_200) return "51-200";
        if (selectedId == R.id.chip_size_201_500) return "201-500";
        if (selectedId == R.id.chip_size_501_plus) return "501+";
        return null;
    }

    private boolean validateForm() {
        boolean valid = true;
        
        String firstName = firstNameInput.getText().toString().trim();
        if (firstName.isEmpty()) {
            firstNameLayout.setError("First name is required");
            valid = false;
        } else {
            firstNameLayout.setError(null);
        }

        String lastName = lastNameInput.getText().toString().trim();
        if (lastName.isEmpty()) {
            lastNameLayout.setError("Last name is required");
            valid = false;
        } else {
            lastNameLayout.setError(null);
        }

        if (isPoster) {
            String companyName = companyNameInput.getText().toString().trim();
            if (companyName.isEmpty()) {
                companyNameLayout.setError("Company name is required");
                valid = false;
            } else {
                companyNameLayout.setError(null);
            }
        }

        return valid;
    }

    private void saveProfile() {
        showLoading(true);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName(firstNameInput.getText().toString().trim());
        request.setLastName(lastNameInput.getText().toString().trim());
        
        String phone = phoneInput.getText().toString().trim();
        if (!phone.isEmpty()) request.setPhone(phone);
        
        String location = locationInput.getText().toString().trim();
        if (!location.isEmpty()) request.setLocation(location);
        
        String timezone = timezoneDropdown.getText().toString().trim();
        if (!timezone.isEmpty()) request.setTimezone(timezone);
        
        String bio = bioInput.getText().toString().trim();
        if (!bio.isEmpty()) request.setBio(bio);

        if (isPoster) {
            String companyName = companyNameInput.getText().toString().trim();
            if (!companyName.isEmpty()) request.setCompanyName(companyName);
            
            String companyWebsite = companyWebsiteInput.getText().toString().trim();
            if (!companyWebsite.isEmpty()) request.setWebsite(companyWebsite);
            
            String companySize = getSelectedCompanySize();
            if (companySize != null) request.setCompanySize(companySize);
            
            String industry = industryDropdown.getText().toString().trim();
            if (!industry.isEmpty()) request.setIndustry(industry);
            
            if (!verificationDocs.isEmpty()) {
                request.setVerificationDocuments(verificationDocs);
            }
        } else {
            String jobTitle = jobTitleInput.getText().toString().trim();
            if (!jobTitle.isEmpty()) request.setJobTitle(jobTitle);
            
            String skills = skillsInput.getText().toString().trim();
            if (!skills.isEmpty()) {
                List<String> skillList = new ArrayList<>();
                for (String skill : skills.split(",")) {
                    String trimmed = skill.trim();
                    if (!trimmed.isEmpty()) skillList.add(trimmed);
                }
                request.setSkills(skillList);
            }
            
            // Seeker preferences
            String desiredRoles = desiredRolesInput.getText().toString().trim();
            if (!desiredRoles.isEmpty()) {
                List<String> rolesList = new ArrayList<>();
                for (String role : desiredRoles.split(",")) {
                    String trimmed = role.trim();
                    if (!trimmed.isEmpty()) rolesList.add(trimmed);
                }
                request.setDesiredRoles(rolesList);
            }
            
            // Salary range
            String minSalary = minSalaryInput.getText().toString().trim();
            if (!minSalary.isEmpty()) {
                try {
                    request.setSalaryMin(Integer.parseInt(minSalary));
                } catch (NumberFormatException e) {
                    showToast("Invalid minimum salary");
                    showLoading(false);
                    return;
                }
            }
            String maxSalary = maxSalaryInput.getText().toString().trim();
            if (!maxSalary.isEmpty()) {
                try {
                    request.setSalaryMax(Integer.parseInt(maxSalary));
                } catch (NumberFormatException e) {
                    showToast("Invalid maximum salary");
                    showLoading(false);
                    return;
                }
            }
            
            // Job types
            List<String> jobTypes = getCheckedChipValues(jobTypeChips, this::getJobTypeValue);
            if (!jobTypes.isEmpty()) request.setJobTypes(jobTypes);
            
            // Work modes
            List<String> workModes = getCheckedChipValues(workModeChips, this::getWorkModeValue);
            if (!workModes.isEmpty()) request.setWorkModes(workModes);
            
            // Experience levels
            List<String> experienceLevels = getCheckedChipValues(experienceLevelChips, this::getExperienceLevelValue);
            if (!experienceLevels.isEmpty()) request.setExperienceLevels(experienceLevels);
        }

        apiService.updateProfile(sessionManager.getAccessToken(), request,
            new ApiCallback<ProfileResponse>() {
                @Override
                public void onSuccess(ProfileResponse result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        UserDataManager.getInstance().setCurrentUser(result);
                        showToast("Profile updated successfully");
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
        saveButton.setEnabled(!show);
    }

    private List<String> getCheckedChipValues(ChipGroup chipGroup, ChipValueMapper mapper) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chip.isChecked()) {
                    String value = mapper.getValue(chip.getId());
                    if (value != null) values.add(value);
                }
            }
        }
        return values;
    }

    private interface ChipValueMapper {
        String getValue(int chipId);
    }

    private String getJobTypeValue(int chipId) {
        if (chipId == R.id.chip_full_time) return "FULL_TIME";
        if (chipId == R.id.chip_part_time) return "PART_TIME";
        if (chipId == R.id.chip_contract) return "CONTRACT";
        if (chipId == R.id.chip_internship) return "INTERNSHIP";
        return null;
    }

    private String getWorkModeValue(int chipId) {
        if (chipId == R.id.chip_remote) return "REMOTE";
        if (chipId == R.id.chip_hybrid) return "HYBRID";
        if (chipId == R.id.chip_onsite) return "ONSITE";
        return null;
    }

    private String getExperienceLevelValue(int chipId) {
        if (chipId == R.id.chip_entry) return "ENTRY";
        if (chipId == R.id.chip_mid) return "MID";
        if (chipId == R.id.chip_senior) return "SENIOR";
        if (chipId == R.id.chip_lead) return "LEAD";
        return null;
    }
}