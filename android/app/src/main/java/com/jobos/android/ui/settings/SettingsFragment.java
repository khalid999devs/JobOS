package com.jobos.android.ui.settings;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.jobos.android.R;
import com.jobos.android.ui.base.BaseFragment;

public class SettingsFragment extends BaseFragment {

    private MaterialToolbar toolbar;
    private SwitchMaterial switchPushNotifications;
    private SwitchMaterial switchEmailNotifications;
    private SwitchMaterial switchJobAlerts;
    private LinearLayout themeOption;
    private TextView themeValue;
    private LinearLayout changePasswordOption;
    private LinearLayout privacyOption;
    private LinearLayout termsOption;
    private TextView versionText;
    private LinearLayout rateAppOption;
    private LinearLayout helpOption;
    private LinearLayout deleteAccountOption;

    private static final String PREF_PUSH_NOTIFICATIONS = "push_notifications";
    private static final String PREF_EMAIL_NOTIFICATIONS = "email_notifications";
    private static final String PREF_JOB_ALERTS = "job_alerts";
    private static final String PREF_THEME = "theme_mode";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hideBottomNav();
        
        initViews(view);
        loadSettings();
        setupClickListeners();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        switchPushNotifications = view.findViewById(R.id.switch_push_notifications);
        switchEmailNotifications = view.findViewById(R.id.switch_email_notifications);
        switchJobAlerts = view.findViewById(R.id.switch_job_alerts);
        themeOption = view.findViewById(R.id.theme_option);
        themeValue = view.findViewById(R.id.theme_value);
        changePasswordOption = view.findViewById(R.id.change_password_option);
        privacyOption = view.findViewById(R.id.privacy_option);
        termsOption = view.findViewById(R.id.terms_option);
        versionText = view.findViewById(R.id.version_text);
        rateAppOption = view.findViewById(R.id.rate_app_option);
        helpOption = view.findViewById(R.id.help_option);
        deleteAccountOption = view.findViewById(R.id.delete_account_option);

        try {
            PackageInfo pInfo = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
            versionText.setText(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            versionText.setText("1.0.0");
        }
    }

    private void loadSettings() {
        switchPushNotifications.setChecked(sessionManager.getPreferences().getBoolean(PREF_PUSH_NOTIFICATIONS, true));
        switchEmailNotifications.setChecked(sessionManager.getPreferences().getBoolean(PREF_EMAIL_NOTIFICATIONS, true));
        switchJobAlerts.setChecked(sessionManager.getPreferences().getBoolean(PREF_JOB_ALERTS, true));
        
        int themeMode = sessionManager.getPreferences().getInt(PREF_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        updateThemeText(themeMode);
    }

    private void setupClickListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());

        switchPushNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> 
            sessionManager.getPreferences().edit().putBoolean(PREF_PUSH_NOTIFICATIONS, isChecked).apply());

        switchEmailNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> 
            sessionManager.getPreferences().edit().putBoolean(PREF_EMAIL_NOTIFICATIONS, isChecked).apply());

        switchJobAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> 
            sessionManager.getPreferences().edit().putBoolean(PREF_JOB_ALERTS, isChecked).apply());

        themeOption.setOnClickListener(v -> showThemeDialog());
        changePasswordOption.setOnClickListener(v -> navController.navigate(R.id.changePasswordFragment));
        privacyOption.setOnClickListener(v -> openUrl("https://jobos.com/privacy"));
        termsOption.setOnClickListener(v -> openUrl("https://jobos.com/terms"));
        rateAppOption.setOnClickListener(v -> openPlayStore());
        helpOption.setOnClickListener(v -> openUrl("https://jobos.com/help"));
        deleteAccountOption.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void showThemeDialog() {
        String[] themes = {"Light", "Dark", "System default"};
        int currentTheme = sessionManager.getPreferences().getInt(PREF_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        int selectedIndex = getThemeIndex(currentTheme);

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose Theme")
            .setSingleChoiceItems(themes, selectedIndex, (dialog, which) -> {
                int mode;
                switch (which) {
                    case 0:
                        mode = AppCompatDelegate.MODE_NIGHT_NO;
                        break;
                    case 1:
                        mode = AppCompatDelegate.MODE_NIGHT_YES;
                        break;
                    default:
                        mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                }
                sessionManager.getPreferences().edit().putInt(PREF_THEME, mode).apply();
                AppCompatDelegate.setDefaultNightMode(mode);
                updateThemeText(mode);
                dialog.dismiss();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private int getThemeIndex(int mode) {
        switch (mode) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                return 0;
            case AppCompatDelegate.MODE_NIGHT_YES:
                return 1;
            default:
                return 2;
        }
    }

    private void updateThemeText(int mode) {
        switch (mode) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                themeValue.setText("Light");
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                themeValue.setText("Dark");
                break;
            default:
                themeValue.setText("System default");
        }
    }

    private void showDeleteAccountDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently removed.")
            .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteAccount() {
        showToast("Account deletion is not available in this version");
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            showToast("Unable to open link");
        }
    }

    private void openPlayStore() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + requireContext().getPackageName())));
        } catch (android.content.ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + requireContext().getPackageName())));
        }
    }
}
