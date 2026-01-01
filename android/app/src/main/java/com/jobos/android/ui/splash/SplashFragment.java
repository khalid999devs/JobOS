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
import com.jobos.android.ui.base.BaseFragment;

public class SplashFragment extends BaseFragment {

    private static final long SPLASH_DELAY = 2000;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateNext, SPLASH_DELAY);
    }

    private void navigateNext() {
        if (!isAdded()) return;

        if (sessionManager.isLoggedIn()) {
            String role = sessionManager.getUserRole();
            if ("POSTER".equals(role)) {
                navController.navigate(R.id.action_splash_to_poster_dashboard);
            } else {
                navController.navigate(R.id.action_splash_to_seeker_home);
            }
        } else {
            navController.navigate(R.id.action_splash_to_login);
        }
    }
}
