package com.jobos.android.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.HashMap;
import java.util.Map;

public class ChangePasswordFragment extends BaseFragment {

    private MaterialToolbar toolbar;
    private TextInputLayout currentPasswordLayout;
    private TextInputEditText currentPasswordInput;
    private TextInputLayout newPasswordLayout;
    private TextInputEditText newPasswordInput;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText confirmPasswordInput;
    private MaterialButton changePasswordButton;
    private ProgressBar progressBar;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_change_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hideBottomNav();
        
        apiService = new ApiService();
        initViews(view);
        setupClickListeners();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        currentPasswordLayout = view.findViewById(R.id.current_password_layout);
        currentPasswordInput = view.findViewById(R.id.current_password_input);
        newPasswordLayout = view.findViewById(R.id.new_password_layout);
        newPasswordInput = view.findViewById(R.id.new_password_input);
        confirmPasswordLayout = view.findViewById(R.id.confirm_password_layout);
        confirmPasswordInput = view.findViewById(R.id.confirm_password_input);
        changePasswordButton = view.findViewById(R.id.change_password_button);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupClickListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
        changePasswordButton.setOnClickListener(v -> {
            if (validateForm()) {
                changePassword();
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;
        
        String currentPassword = currentPasswordInput.getText().toString();
        String newPassword = newPasswordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        if (currentPassword.isEmpty()) {
            currentPasswordLayout.setError("Current password is required");
            isValid = false;
        } else {
            currentPasswordLayout.setError(null);
        }

        if (newPassword.isEmpty()) {
            newPasswordLayout.setError("New password is required");
            isValid = false;
        } else if (newPassword.length() < 8) {
            newPasswordLayout.setError("Password must be at least 8 characters");
            isValid = false;
        } else {
            newPasswordLayout.setError(null);
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.setError("Please confirm your password");
            isValid = false;
        } else if (!confirmPassword.equals(newPassword)) {
            confirmPasswordLayout.setError("Passwords do not match");
            isValid = false;
        } else {
            confirmPasswordLayout.setError(null);
        }

        return isValid;
    }

    private void changePassword() {
        showLoading(true);

        Map<String, String> passwordData = new HashMap<>();
        passwordData.put("currentPassword", currentPasswordInput.getText().toString());
        passwordData.put("newPassword", newPasswordInput.getText().toString());

        apiService.changePassword(
            sessionManager.getAccessToken(),
            passwordData,
            new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Password changed successfully");
                        navController.popBackStack();
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        if (error.contains("incorrect") || error.contains("wrong")) {
                            currentPasswordLayout.setError("Incorrect password");
                        } else {
                            showToast("Error: " + error);
                        }
                    });
                }
            });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        changePasswordButton.setEnabled(!show);
    }
}
