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
import com.jobos.android.data.local.UserDataManager;

public class RoleSelectionFragment extends BaseFragment {

    private MaterialCardView seekerCard;
    private MaterialCardView posterCard;
    private Button continueButton;
    private ProgressBar progressBar;

    private String selectedRole = null;
    private ApiService apiService;
    
    private String regFirstName;
    private String regLastName;
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
        
        if (getArguments() != null) {
            regFirstName = getArguments().getString("firstName");
            regLastName = getArguments().getString("lastName");
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
            performRegistration();
        } else {
            showToast("Invalid state - please restart registration");
        }
    }
    
    private void performRegistration() {
        setLoading(true);
        
        RegisterRequest request = new RegisterRequest(regFirstName, regLastName, regEmail, regPassword, selectedRole);
        
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
        navigateToSetup();
    }
    
    private void navigateToSetup() {
        if ("POSTER".equals(selectedRole)) {
            navController.navigate(R.id.action_role_to_poster_setup);
        } else {
            navController.navigate(R.id.action_role_to_seeker_setup);
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
