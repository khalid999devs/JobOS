package com.jobos.android.ui.splash;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.jobos.android.R;
import com.jobos.android.data.model.profile.ProfileResponse;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.data.network.TokenManager;
import com.jobos.android.ui.base.BaseFragment;

public class SplashFragment extends BaseFragment {

    private static final long SPLASH_DELAY = 1500;
    private ApiService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = new ApiService();
        
        new Handler(Looper.getMainLooper()).postDelayed(this::checkAuthAndNavigate, SPLASH_DELAY);
    }

    private void checkAuthAndNavigate() {
        if (!isAdded()) return;

        if (!sessionManager.isLoggedIn()) {
            navController.navigate(R.id.action_splash_to_login);
            return;
        }

        String accessToken = sessionManager.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            clearAndNavigateToLogin();
            return;
        }

        validateTokenWithBackend(accessToken);
    }

    private void validateTokenWithBackend(String token) {
        apiService.getProfile(token, new ApiCallback<ProfileResponse>() {
            @Override
            public void onSuccess(ProfileResponse profile) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    updateUserInfoFromProfile(profile);
                    navigateToDashboard();
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    tryRefreshToken();
                });
            }
        });
    }

    private void tryRefreshToken() {
        String refreshToken = sessionManager.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            clearAndNavigateToLogin();
            return;
        }

        tokenManager.refreshAccessToken(new TokenManager.TokenCallback() {
            @Override
            public void onTokenReady(String accessToken) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> navigateToDashboard());
            }

            @Override
            public void onTokenRefreshFailed(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> clearAndNavigateToLogin());
            }
        });
    }

    private void updateUserInfoFromProfile(ProfileResponse profile) {
        if (profile == null) return;
        
        String name = "";
        if (profile.getFirstName() != null && !profile.getFirstName().isEmpty()) {
            name = profile.getFirstName();
            if (profile.getLastName() != null && !profile.getLastName().isEmpty()) {
                name += " " + profile.getLastName();
            }
        } else if (profile.getEmail() != null) {
            name = profile.getEmail().split("@")[0];
        }
        
        sessionManager.saveUserInfo(
            profile.getId(),
            profile.getEmail(),
            name,
            profile.getRole()
        );
    }

    private void navigateToDashboard() {
        if (!isAdded()) return;
        String role = sessionManager.getUserRole();
        if ("POSTER".equals(role)) {
            navController.navigate(R.id.action_splash_to_poster_dashboard);
        } else {
            navController.navigate(R.id.action_splash_to_seeker_home);
        }
    }

    private void clearAndNavigateToLogin() {
        sessionManager.clearSession();
        if (!isAdded()) return;
        navController.navigate(R.id.action_splash_to_login);
    }
}
