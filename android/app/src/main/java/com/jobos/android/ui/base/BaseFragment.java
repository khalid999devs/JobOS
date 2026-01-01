package com.jobos.android.ui.base;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.jobos.android.data.local.SessionManager;
import com.jobos.android.ui.main.MainActivity;

public abstract class BaseFragment extends Fragment {
    
    protected SessionManager sessionManager;
    protected NavController navController;
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sessionManager = new SessionManager(requireContext());
        navController = Navigation.findNavController(view);
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
}
