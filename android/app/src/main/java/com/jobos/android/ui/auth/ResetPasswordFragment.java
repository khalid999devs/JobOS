package com.jobos.android.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.ui.base.BaseFragment;

public class ResetPasswordFragment extends BaseFragment {

    private TextInputLayout otpLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText otpInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private Button resetButton;
    private ProgressBar progressBar;
    private ImageView backButton;

    private String email;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reset_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getArguments() != null) {
            email = getArguments().getString("email", "");
        }
        
        apiService = new ApiService();
        initViews(view);
        setupClickListeners();
    }

    private void initViews(View view) {
        otpLayout = view.findViewById(R.id.otp_layout);
        passwordLayout = view.findViewById(R.id.password_layout);
        confirmPasswordLayout = view.findViewById(R.id.confirm_password_layout);
        otpInput = view.findViewById(R.id.otp_input);
        passwordInput = view.findViewById(R.id.password_input);
        confirmPasswordInput = view.findViewById(R.id.confirm_password_input);
        resetButton = view.findViewById(R.id.reset_button);
        progressBar = view.findViewById(R.id.progress_bar);
        backButton = view.findViewById(R.id.back_button);
    }

    private void setupClickListeners() {
        resetButton.setOnClickListener(v -> resetPassword());
        backButton.setOnClickListener(v -> navController.navigateUp());
    }

    private void resetPassword() {
        clearErrors();

        String otp = getText(otpInput);
        String password = getText(passwordInput);
        String confirmPassword = getText(confirmPasswordInput);

        if (TextUtils.isEmpty(otp)) {
            otpLayout.setError(getString(R.string.error_required_field));
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError(getString(R.string.error_required_field));
            return;
        }

        if (password.length() < 8) {
            passwordLayout.setError(getString(R.string.error_password_short));
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError(getString(R.string.error_password_mismatch));
            return;
        }

        setLoading(true);

        apiService.resetPassword(email, otp, password, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    showToast(getString(R.string.success_password_reset));
                    navController.navigate(R.id.action_reset_to_login);
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

    private void clearErrors() {
        otpLayout.setError(null);
        passwordLayout.setError(null);
        confirmPasswordLayout.setError(null);
    }

    private void setLoading(boolean loading) {
        resetButton.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        otpInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
        confirmPasswordInput.setEnabled(!loading);
    }

    private String getText(TextInputEditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }
}
