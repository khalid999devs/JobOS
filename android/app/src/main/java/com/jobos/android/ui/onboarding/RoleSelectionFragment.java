package com.jobos.android.ui.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.card.MaterialCardView;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.shared.dto.profile.UpdateProfileRequest;
import com.jobos.shared.dto.profile.ProfileResponse;

public class RoleSelectionFragment extends BaseFragment {

    private MaterialCardView seekerCard;
    private MaterialCardView posterCard;
    private Button continueButton;

    private String selectedRole = null;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_role_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        apiService = new ApiService();
        initViews(view);
        setupClickListeners();
    }

    private void initViews(View view) {
        seekerCard = view.findViewById(R.id.seeker_card);
        posterCard = view.findViewById(R.id.poster_card);
        continueButton = view.findViewById(R.id.continue_button);
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

        continueButton.setEnabled(false);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setRole(selectedRole);

        String token = sessionManager.getAccessToken();
        apiService.updateProfile(token, request, new ApiCallback<ProfileResponse>() {
            @Override
            public void onSuccess(ProfileResponse response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    sessionManager.saveUserInfo(
                        sessionManager.getUserId(),
                        sessionManager.getUserEmail(),
                        sessionManager.getUserName(),
                        selectedRole
                    );
                    navigateToSetup();
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    continueButton.setEnabled(true);
                    showToast(error);
                });
            }
        });
    }

    private void navigateToSetup() {
        if ("POSTER".equals(selectedRole)) {
            navController.navigate(R.id.action_role_to_poster_setup);
        } else {
            navController.navigate(R.id.action_role_to_seeker_setup);
        }
    }
}
