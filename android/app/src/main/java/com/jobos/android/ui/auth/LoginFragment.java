package com.jobos.android.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.data.model.auth.LoginRequest;
import com.jobos.android.data.model.auth.AuthResponse;

public class LoginFragment extends BaseFragment {

    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private Button loginButton;
    private ProgressBar progressBar;
    private TextView forgotPassword;
    private TextView registerLink;

    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        apiService = new ApiService();
        initViews(view);
        setupClickListeners();
    }

    private void initViews(View view) {
        emailLayout = view.findViewById(R.id.email_layout);
        passwordLayout = view.findViewById(R.id.password_layout);
        emailInput = view.findViewById(R.id.email_input);
        passwordInput = view.findViewById(R.id.password_input);
        loginButton = view.findViewById(R.id.login_button);
        progressBar = view.findViewById(R.id.progress_bar);
        forgotPassword = view.findViewById(R.id.forgot_password);
        registerLink = view.findViewById(R.id.register_link);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> attemptLogin());
        
        forgotPassword.setOnClickListener(v -> 
            navController.navigate(R.id.action_login_to_forgot_password)
        );
        
        registerLink.setOnClickListener(v -> 
            navController.navigate(R.id.action_login_to_register)
        );
    }

    private void attemptLogin() {
        emailLayout.setError(null);
        passwordLayout.setError(null);

        String email = getText(emailInput);
        String password = getText(passwordInput);

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError(getString(R.string.error_required_field));
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.error_invalid_email));
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

        setLoading(true);

        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);

        apiService.login(request, new ApiCallback<AuthResponse>() {
            @Override
            public void onSuccess(AuthResponse response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    handleLoginSuccess(response);
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

    private void handleLoginSuccess(AuthResponse response) {
        sessionManager.saveAuthTokens(response.getAccessToken(), response.getRefreshToken());
        sessionManager.saveUserInfo(
            response.getUserId(),
            response.getEmail(),
            response.getName(),
            response.getRole()
        );

        String role = response.getRole();
        if (role == null || role.isEmpty()) {
            navController.navigate(R.id.action_login_to_role_selection);
        } else if ("POSTER".equals(role)) {
            navController.navigate(R.id.action_login_to_poster_dashboard);
        } else {
            navController.navigate(R.id.action_login_to_seeker_home);
        }
    }

    private void setLoading(boolean loading) {
        loginButton.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        emailInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
    }

    private String getText(TextInputEditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }
}
