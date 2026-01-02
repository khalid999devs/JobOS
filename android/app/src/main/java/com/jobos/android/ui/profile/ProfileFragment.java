package com.jobos.android.ui.profile;

import android.app.AlertDialog;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.data.model.profile.ProfileResponse;
import java.util.Map;

public class ProfileFragment extends BaseFragment {

    private MaterialToolbar toolbar;
    private TextView userName;
    private TextView userEmail;
    private MaterialCardView statsCard;
    private TextView statValue1;
    private TextView statLabel1;
    private TextView statValue2;
    private TextView statLabel2;
    private TextView statValue3;
    private TextView statLabel3;
    private LinearLayout editProfileItem;
    private LinearLayout myCvsItem;
    private LinearLayout notificationsItem;
    private LinearLayout settingsItem;
    private MaterialButton logoutButton;
    private ProgressBar progressBar;

    private boolean isSeeker;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        showBottomNav();
        
        apiService = new ApiService();
        isSeeker = "SEEKER".equals(sessionManager.getUserRole());
        
        initViews(view);
        setupClickListeners();
        displayUserInfo();
        loadProfileStats();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        userName = view.findViewById(R.id.user_name);
        userEmail = view.findViewById(R.id.user_email);
        statsCard = view.findViewById(R.id.stats_card);
        statValue1 = view.findViewById(R.id.stat_value_1);
        statLabel1 = view.findViewById(R.id.stat_label_1);
        statValue2 = view.findViewById(R.id.stat_value_2);
        statLabel2 = view.findViewById(R.id.stat_label_2);
        statValue3 = view.findViewById(R.id.stat_value_3);
        statLabel3 = view.findViewById(R.id.stat_label_3);
        editProfileItem = view.findViewById(R.id.edit_profile_item);
        myCvsItem = view.findViewById(R.id.my_cvs_item);
        notificationsItem = view.findViewById(R.id.notifications_item);
        settingsItem = view.findViewById(R.id.settings_item);
        logoutButton = view.findViewById(R.id.logout_button);
        progressBar = view.findViewById(R.id.progress_bar);

        if (isSeeker) {
            myCvsItem.setVisibility(View.VISIBLE);
        }
    }

    private void setupClickListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit) {
                navController.navigate(R.id.editProfileFragment);
                return true;
            }
            return false;
        });

        editProfileItem.setOnClickListener(v -> navController.navigate(R.id.editProfileFragment));
        
        myCvsItem.setOnClickListener(v -> navController.navigate(R.id.cvListFragment));
        
        notificationsItem.setOnClickListener(v -> navController.navigate(R.id.notificationsFragment));
        
        settingsItem.setOnClickListener(v -> navController.navigate(R.id.settingsFragment));

        logoutButton.setOnClickListener(v -> showLogoutConfirmation());
    }

    private void displayUserInfo() {
        String name = sessionManager.getUserName();
        String email = sessionManager.getUserEmail();

        userName.setText(name != null ? name : "User");
        userEmail.setText(email != null ? email : "");
    }

    private void loadProfileStats() {
        apiService.getProfileStats(sessionManager.getAccessToken(),
            new ApiCallback<Map<String, Object>>() {
                @Override
                public void onSuccess(Map<String, Object> result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        displayStats(result);
                    });
                }

                @Override
                public void onError(String error) {
                }
            });
    }

    private void displayStats(Map<String, Object> stats) {
        statsCard.setVisibility(View.VISIBLE);
        
        if (isSeeker) {
            statValue1.setText(String.valueOf(getIntValue(stats, "applicationCount")));
            statLabel1.setText("Applications");
            
            statValue2.setText(String.valueOf(getIntValue(stats, "savedJobsCount")));
            statLabel2.setText("Saved Jobs");
            
            statValue3.setText(String.valueOf(getIntValue(stats, "cvCount")));
            statLabel3.setText("CVs");
        } else {
            statValue1.setText(String.valueOf(getIntValue(stats, "postedJobsCount")));
            statLabel1.setText("Jobs Posted");
            
            statValue2.setText(String.valueOf(getIntValue(stats, "activeJobsCount")));
            statLabel2.setText("Active");
            
            statValue3.setText(String.valueOf(getIntValue(stats, "totalApplicationsCount")));
            statLabel3.setText("Applications");
        }
    }

    private int getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout", (dialog, which) -> logout())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void logout() {
        sessionManager.clearSession();
        navController.navigate(R.id.loginFragment);
    }
}
