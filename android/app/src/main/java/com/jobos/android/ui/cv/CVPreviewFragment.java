package com.jobos.android.ui.cv;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.data.model.cv.CVDTO;
import java.util.List;

public class CVPreviewFragment extends BaseFragment {

    private MaterialToolbar toolbar;
    private TextView fullName;
    private TextView email;
    private TextView phone;
    private TextView address;
    private MaterialCardView summaryCard;
    private TextView summary;
    private MaterialCardView skillsCard;
    private ChipGroup skillsChipGroup;
    private MaterialCardView experienceCard;
    private LinearLayout experienceContainer;
    private MaterialCardView educationCard;
    private LinearLayout educationContainer;
    private MaterialCardView linksCard;
    private TextView linkedinLink;
    private TextView portfolioLink;
    private ProgressBar progressBar;

    private String cvId = null;
    private ApiService apiService;
    private CVDTO currentCV;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cv_preview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = new ApiService();
        hideBottomNav();

        if (getArguments() != null) {
            cvId = getArguments().getString("cvId");
        }

        initViews(view);
        setupClickListeners();
        loadCVDetails();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        fullName = view.findViewById(R.id.full_name);
        email = view.findViewById(R.id.email);
        phone = view.findViewById(R.id.phone);
        address = view.findViewById(R.id.address);
        summaryCard = view.findViewById(R.id.summary_card);
        summary = view.findViewById(R.id.summary);
        skillsCard = view.findViewById(R.id.skills_card);
        skillsChipGroup = view.findViewById(R.id.skills_chip_group);
        experienceCard = view.findViewById(R.id.experience_card);
        experienceContainer = view.findViewById(R.id.experience_container);
        educationCard = view.findViewById(R.id.education_card);
        educationContainer = view.findViewById(R.id.education_container);
        linksCard = view.findViewById(R.id.links_card);
        linkedinLink = view.findViewById(R.id.linkedin_link);
        portfolioLink = view.findViewById(R.id.portfolio_link);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupClickListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());

        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit) {
                if (cvId != null) {
                    Bundle args = new Bundle();
                    args.putString("cvId", cvId);
                    navController.navigate(R.id.cvEditorFragment, args);
                }
                return true;
            } else if (id == R.id.action_share) {
                shareCV();
                return true;
            }
            return false;
        });

        linkedinLink.setOnClickListener(v -> {
            String url = linkedinLink.getText().toString();
            if (!url.isEmpty()) openUrl(url);
        });

        portfolioLink.setOnClickListener(v -> {
            String url = portfolioLink.getText().toString();
            if (!url.isEmpty()) openUrl(url);
        });
    }

    private void loadCVDetails() {
        if (cvId == null) {
            showToast("Invalid CV");
            navController.popBackStack();
            return;
        }

        showLoading(true);
        apiService.getCVDetails(sessionManager.getAccessToken(), cvId,
            new ApiCallback<CVDTO>() {
                @Override
                public void onSuccess(CVDTO result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        currentCV = result;
                        displayCV();
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Error loading CV: " + error);
                    });
                }
            });
    }

    private void displayCV() {
        if (currentCV == null) return;

        toolbar.setTitle(currentCV.getTitle());
        fullName.setText(currentCV.getFullName());
        email.setText(currentCV.getEmail());

        String phoneText = currentCV.getPhone();
        if (phoneText != null && !phoneText.isEmpty()) {
            phone.setText(phoneText);
            phone.setVisibility(View.VISIBLE);
        }

        String addressText = currentCV.getAddress();
        if (addressText != null && !addressText.isEmpty()) {
            address.setText(addressText);
            address.setVisibility(View.VISIBLE);
        }

        String summaryText = currentCV.getSummary();
        if (summaryText != null && !summaryText.isEmpty()) {
            summary.setText(summaryText);
            summaryCard.setVisibility(View.VISIBLE);
        }

        List<String> skills = currentCV.getSkills();
        if (skills != null && !skills.isEmpty()) {
            skillsChipGroup.removeAllViews();
            for (String skill : skills) {
                Chip chip = new Chip(requireContext());
                chip.setText(skill);
                chip.setClickable(false);
                skillsChipGroup.addView(chip);
            }
            skillsCard.setVisibility(View.VISIBLE);
        }

        List<String> experience = currentCV.getExperience();
        if (experience != null && !experience.isEmpty()) {
            experienceContainer.removeAllViews();
            for (String exp : experience) {
                TextView textView = createListItem(exp);
                experienceContainer.addView(textView);
            }
            experienceCard.setVisibility(View.VISIBLE);
        }

        List<String> education = currentCV.getEducation();
        if (education != null && !education.isEmpty()) {
            educationContainer.removeAllViews();
            for (String edu : education) {
                TextView textView = createListItem(edu);
                educationContainer.addView(textView);
            }
            educationCard.setVisibility(View.VISIBLE);
        }

        String linkedin = currentCV.getLinkedinUrl();
        String portfolio = currentCV.getPortfolioUrl();
        boolean hasLinks = false;

        if (linkedin != null && !linkedin.isEmpty()) {
            linkedinLink.setText(linkedin);
            linkedinLink.setVisibility(View.VISIBLE);
            hasLinks = true;
        }

        if (portfolio != null && !portfolio.isEmpty()) {
            portfolioLink.setText(portfolio);
            portfolioLink.setVisibility(View.VISIBLE);
            hasLinks = true;
        }

        if (hasLinks) {
            linksCard.setVisibility(View.VISIBLE);
        }
    }

    private TextView createListItem(String text) {
        TextView textView = new TextView(requireContext());
        textView.setText("• " + text);
        textView.setTextSize(14);
        textView.setTextColor(getResources().getColor(R.color.on_surface_secondary, null));
        textView.setPadding(0, 8, 0, 8);
        return textView;
    }

    private void shareCV() {
        if (currentCV == null) return;

        StringBuilder shareText = new StringBuilder();
        shareText.append(currentCV.getFullName()).append("\n");
        shareText.append(currentCV.getEmail()).append("\n\n");

        if (currentCV.getSummary() != null) {
            shareText.append("Summary:\n").append(currentCV.getSummary()).append("\n\n");
        }

        List<String> skills = currentCV.getSkills();
        if (skills != null && !skills.isEmpty()) {
            shareText.append("Skills: ").append(String.join(", ", skills)).append("\n\n");
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, currentCV.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        startActivity(Intent.createChooser(shareIntent, "Share CV"));
    }

    private void openUrl(String url) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            showToast("Unable to open link");
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
