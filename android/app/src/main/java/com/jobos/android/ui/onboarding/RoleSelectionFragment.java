package com.jobos.android.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.card.MaterialCardView;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.data.model.auth.RegisterRequest;
import com.jobos.android.data.model.auth.AuthResponse;

public class RoleSelectionFragment extends BaseFragment {

    private MaterialCardView seekerCard;
    private MaterialCardView posterCard;
    private Button continueButton;
    private ProgressBar progressBar;

    private String selectedRole = null;
    private ApiService apiService;
    
    // Registration data from RegisterFragment
    private String regName;
    private String regEmail;
    private String regPassword;
    private boolean isNewRegistration = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_role_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        apiService = new ApiService();
        
        // Get registration data from arguments
        if (getArguments() != null) {
            regName = getArguments().getString("name");
            regEmail = getArguments().getString("email");
            regPassword = getArguments().getString("password");
            isNewRegistration = regEmail != null && regPassword != null;
        }
        
        initViews(view);
        setupClickListeners();
    }

    private void initViews(View view) {
        seekerCard = view.findViewById(R.id.seeker_card);
        posterCard = view.findViewById(R.id.poster_card);
        continueButton = view.findViewById(R.id.continue_button);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupClickListeners() {
        seekerCard.setOnClickListener(v -> selectRole("SEEKER"));
        posterCard.setOnClickListener(v -> selectRole("POSTER"));
        continueButton.setOnClickListener(v -> confirmRole());
    }

    private void selectRole(String role) {
        selectedRole = role;
        continueButton.setEnabled(true);

        if ("SEEKER".equals(role)) {
            seekerCard.setStrokeColor(getResources().getColor(R.color.primary, null));
            seekerCard.setStrokeWidth(4);
            posterCard.setStrokeColor(getResources().getColor(R.color.outline, null));
            posterCard.setStrokeWidth(1);
        } else {
            posterCard.setStrokeColor(getResources().getColor(R.color.secondary, null));
            posterCard.setStrokeWidth(4);
            seekerCard.setStrokeColor(getResources().getColor(R.color.outline, null));
            seekerCard.setStrokeWidth(1);
        }
    }

    private void confirmRole() {
        if (selectedRole == null) return;

        if (isNewRegistration) {
            // New user registration flow - call register API with role
            performRegistration();
        } else {
            // Existing user updating role - shouldn't happen normally
            showToast("Invalid state - please restart registration");
        }
    }
    
    private void performRegistration() {
        setLoading(true);
        
        RegisterRequest request = new RegisterRequest();
        request.setName(regName);
        request.setEmail(regEmail);
        request.setPassword(regPassword);
        request.setRole(selectedRole);
        
        apiService.register(request, new ApiCallback<AuthResponse>() {
            @Override
            public void onSuccess(AuthResponse response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    handleRegisterSuccess(response);
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
    
    private void handleRegisterSuccess(AuthResponse response) {
        sessionManager.saveAuthTokens(response.getAccessToken(), response.getRefreshToken());
        sessionManager.saveUserInfo(
            response.getUserId(),
            response.getEmail(),
            response.getName(),
            selectedRole
        );
        
        showToast(getString(R.string.success_register));
        navigateToDashboard();
    }
    
    private void navigateToDashboard() {
        if ("POSTER".equals(selectedRole)) {
            navController.navigate(R.id.action_role_to_poster_dashboard);
        } else {
            navController.navigate(R.id.action_role_to_seeker_home);
        }
    }
    
    private void setLoading(boolean loading) {
        continueButton.setEnabled(!loading && selectedRole != null);
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        seekerCard.setEnabled(!loading);
        posterCard.setEnabled(!loading);
    }
}
