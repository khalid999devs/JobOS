package com.jobos.android.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jobos.android.R;
import com.jobos.android.ui.base.BaseFragment;

public class RegisterFragment extends BaseFragment {

    private TextInputLayout nameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private Button registerButton;
    private ProgressBar progressBar;
    private ImageView backButton;
    private TextView loginLink;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initViews(view);
        setupClickListeners();
    }

    private void initViews(View view) {
        nameLayout = view.findViewById(R.id.name_layout);
        emailLayout = view.findViewById(R.id.email_layout);
        passwordLayout = view.findViewById(R.id.password_layout);
        confirmPasswordLayout = view.findViewById(R.id.confirm_password_layout);
        nameInput = view.findViewById(R.id.name_input);
        emailInput = view.findViewById(R.id.email_input);
        passwordInput = view.findViewById(R.id.password_input);
        confirmPasswordInput = view.findViewById(R.id.confirm_password_input);
        registerButton = view.findViewById(R.id.register_button);
        progressBar = view.findViewById(R.id.progress_bar);
        backButton = view.findViewById(R.id.back_button);
        loginLink = view.findViewById(R.id.login_link);
    }

    private void setupClickListeners() {
        registerButton.setOnClickListener(v -> validateAndProceed());
        
        backButton.setOnClickListener(v -> navController.navigateUp());
        
        loginLink.setOnClickListener(v -> 
            navController.navigate(R.id.action_register_to_login)
        );
    }

    private void validateAndProceed() {
        clearErrors();

        String name = getText(nameInput);
        String email = getText(emailInput);
        String password = getText(passwordInput);
        String confirmPassword = getText(confirmPasswordInput);

        if (TextUtils.isEmpty(name)) {
            nameLayout.setError(getString(R.string.error_required_field));
            return;
        }

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

        if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError(getString(R.string.error_password_mismatch));
            return;
        }

        // Navigate to role selection with registration data
        Bundle args = new Bundle();
        args.putString("name", name);
        args.putString("email", email);
        args.putString("password", password);
        navController.navigate(R.id.action_register_to_role_selection, args);
    }

    private void clearErrors() {
        nameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        confirmPasswordLayout.setError(null);
    }

    private String getText(TextInputEditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }
}
