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

public class ForgotPasswordFragment extends BaseFragment {

    private TextInputLayout emailLayout;
    private TextInputEditText emailInput;
    private Button sendOtpButton;
    private ProgressBar progressBar;
    private ImageView backButton;

    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
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
        emailInput = view.findViewById(R.id.email_input);
        sendOtpButton = view.findViewById(R.id.send_otp_button);
        progressBar = view.findViewById(R.id.progress_bar);
        backButton = view.findViewById(R.id.back_button);
    }

    private void setupClickListeners() {
        sendOtpButton.setOnClickListener(v -> sendOtp());
        backButton.setOnClickListener(v -> navController.navigateUp());
    }

    private void sendOtp() {
        emailLayout.setError(null);

        String email = getText(emailInput);

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError(getString(R.string.error_required_field));
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.error_invalid_email));
            return;
        }

        setLoading(true);

        apiService.requestPasswordReset(email, new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    showToast("OTP sent to your email");
                    Bundle args = new Bundle();
                    args.putString("email", email);
                    navController.navigate(R.id.action_forgot_to_reset, args);
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

    private void setLoading(boolean loading) {
        sendOtpButton.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        emailInput.setEnabled(!loading);
    }

    private String getText(TextInputEditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }
}
