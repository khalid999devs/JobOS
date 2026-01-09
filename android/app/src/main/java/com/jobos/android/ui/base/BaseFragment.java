package com.jobos.android.ui.base;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.jobos.android.R;
import com.jobos.android.data.local.SessionManager;
import com.jobos.android.data.network.TokenManager;
import com.jobos.android.ui.main.MainActivity;

public abstract class BaseFragment extends Fragment {
    
    protected SessionManager sessionManager;
    protected NavController navController;
    protected TokenManager tokenManager;
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        navController = Navigation.findNavController(view);
        tokenManager = new TokenManager(requireContext());
    }
    
    protected void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
    
    protected void showLongToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
    }
    
    protected void showBottomNav() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showBottomNavigation();
        }
    }
    
    protected void hideBottomNav() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNavigation();
        }
    }
    
    protected void updateBottomNavRole(String role) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).updateBottomNavForRole(role);
        }
    }
    
    protected boolean isLoggedIn() {
        return sessionManager != null && sessionManager.isLoggedIn();
    }
    
    protected String getAccessToken() {
        return sessionManager != null ? sessionManager.getAccessToken() : null;
    }
    
    /**
     * Get a valid access token with automatic refresh if needed.
     * Use this for all authenticated API calls.
     */
    protected void getValidToken(TokenManager.TokenCallback callback) {
        if (tokenManager == null) {
            callback.onTokenRefreshFailed("Token manager not initialized");
            return;
        }
        tokenManager.getValidToken(callback);
    }
    
    /**
     * Handle authentication errors by attempting token refresh
     * Returns true if refresh was attempted, false if user needs to re-login
     */
    protected void handleAuthError(String error, Runnable retryAction) {
        if (error != null && (error.contains("expired") || error.contains("Invalid") || error.contains("token"))) {
            tokenManager.refreshAccessToken(new TokenManager.TokenCallback() {
                @Override
                public void onTokenReady(String accessToken) {
                    if (isAdded() && retryAction != null) {
                        requireActivity().runOnUiThread(retryAction);
                    }
                }

                @Override
                public void onTokenRefreshFailed(String refreshError) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            showToast("Session expired. Please login again.");
                            navigateToLogin();
                        });
                    }
                }
            });
        } else {
            showToast(error);
        }
    }
    
    /**
     * Navigate to login and clear back stack
     */
    protected void navigateToLogin() {
        sessionManager.clearSession();
        navController.navigate(R.id.loginFragment);
    }
}
