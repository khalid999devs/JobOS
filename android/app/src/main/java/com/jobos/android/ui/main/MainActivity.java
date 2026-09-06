package com.jobos.android.ui.main;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.jobos.android.R;
import com.jobos.android.data.local.SessionManager;
import com.jobos.android.ui.util.SystemBarsUtil;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private BottomNavigationView bottomNavigation;
    private SessionManager sessionManager;

    private final Set<Integer> noBottomNavDestinations = new HashSet<>(Arrays.asList(
            R.id.splashFragment,
            R.id.loginFragment,
            R.id.registerFragment,
            R.id.forgotPasswordFragment,
            R.id.resetPasswordFragment,
            R.id.roleSelectionFragment,
            R.id.seekerSetupFragment,
            R.id.posterSetupFragment
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        setupNavigation();
        registerFcmTokenIfLoggedIn();
    }

    private void registerFcmTokenIfLoggedIn() {
        String accessToken = sessionManager.getAccessToken();
        if (accessToken != null && !accessToken.isEmpty()) {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String fcmToken = task.getResult();
                        com.jobos.android.data.network.ApiService apiService = new com.jobos.android.data.network.ApiService();
                        apiService.registerFcmToken(accessToken, fcmToken, new com.jobos.android.data.network.ApiCallback<String>() {
                            @Override
                            public void onSuccess(String result) {
                                android.util.Log.d("MainActivity", "FCM token registered on app start");
                            }

                            @Override
                            public void onError(String error) {
                                android.util.Log.e("MainActivity", "Failed to register FCM token: " + error);
                            }
                        });
                    }
                });
        }
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        bottomNavigation = findViewById(R.id.bottom_navigation);
        if (bottomNavigation != null) {
            SystemBarsUtil.applyBottomInset(bottomNavigation);
            bottomNavigation.setVisibility(View.GONE);
        }
        
        if (navController != null) {
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                updateBottomNavigationVisibility(destination);
            });
        }
    }

    private String currentMenuRole = null;

    private void updateBottomNavigationVisibility(NavDestination destination) {
        if (bottomNavigation == null) return;
        
        int destId = destination.getId();
        
        if (noBottomNavDestinations.contains(destId)) {
            bottomNavigation.setVisibility(View.GONE);
            return;
        }

        String userRole = sessionManager.getUserRole();
        if (userRole == null || userRole.isEmpty()) {
            bottomNavigation.setVisibility(View.GONE);
            return;
        }
        
        bottomNavigation.setVisibility(View.VISIBLE);
        
        if (!userRole.equals(currentMenuRole)) {
            currentMenuRole = userRole;
            bottomNavigation.getMenu().clear();
            if ("POSTER".equals(userRole)) {
                bottomNavigation.inflateMenu(R.menu.menu_bottom_nav_poster);
            } else {
                bottomNavigation.inflateMenu(R.menu.menu_bottom_nav_seeker);
            }
            NavigationUI.setupWithNavController(bottomNavigation, navController);
        }
    }

    public void updateBottomNavForRole(String role) {
        if (bottomNavigation == null) return;
        
        if (role != null && !role.equals(currentMenuRole)) {
            currentMenuRole = role;
            bottomNavigation.getMenu().clear();
            if ("POSTER".equals(role)) {
                bottomNavigation.inflateMenu(R.menu.menu_bottom_nav_poster);
            } else {
                bottomNavigation.inflateMenu(R.menu.menu_bottom_nav_seeker);
            }
            NavigationUI.setupWithNavController(bottomNavigation, navController);
        }
    }

    public void showBottomNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.setVisibility(View.VISIBLE);
        }
    }

    public void hideBottomNavigation() {
        if (bottomNavigation != null) {
            bottomNavigation.setVisibility(View.GONE);
        }
    }

    public NavController getNavController() {
        return navController;
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
