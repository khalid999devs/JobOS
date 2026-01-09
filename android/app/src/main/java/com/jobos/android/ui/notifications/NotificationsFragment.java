package com.jobos.android.ui.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.service.JobOSFirebaseMessagingService;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.data.model.notification.NotificationDTO;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends BaseFragment implements NotificationAdapter.OnNotificationClickListener {

    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private ProgressBar progressBar;
    private NotificationAdapter adapter;
    private ApiService apiService;
    private List<NotificationDTO> allNotifications = new ArrayList<>();
    private String currentFilter = "ALL";
    
    // Broadcast receiver for real-time notifications
    private BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (JobOSFirebaseMessagingService.ACTION_NEW_NOTIFICATION.equals(intent.getAction())) {
                // Refresh notifications when new notification arrives
                loadNotifications();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        showBottomNav();
        
        apiService = new ApiService();
        initViews(view);
        setupTabs();
        setupRecyclerView();
        loadNotifications();
        
        // Register for Firebase notification broadcasts
        IntentFilter filter = new IntentFilter(JobOSFirebaseMessagingService.ACTION_NEW_NOTIFICATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(notificationReceiver, filter);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Unregister broadcast receiver
        try {
            requireContext().unregisterReceiver(notificationReceiver);
        } catch (Exception ignored) {}
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        tabLayout = view.findViewById(R.id.tab_layout);
        recyclerView = view.findViewById(R.id.notifications_recycler);
        emptyState = view.findViewById(R.id.empty_state);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Unread"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentFilter = tab.getPosition() == 0 ? "ALL" : "UNREAD";
                filterNotifications();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter();
        adapter.setOnNotificationClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadNotifications() {
        showLoading(true);
        apiService.getNotifications(sessionManager.getAccessToken(), 0, 50,
            new ApiCallback<List<NotificationDTO>>() {
                @Override
                public void onSuccess(List<NotificationDTO> result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        allNotifications = result != null ? result : new ArrayList<>();
                        filterNotifications();
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        allNotifications = new ArrayList<>();
                        filterNotifications();
                        showToast("Error loading notifications");
                    });
                }
            });
    }

    private void filterNotifications() {
        List<NotificationDTO> filtered;
        if ("UNREAD".equals(currentFilter)) {
            filtered = new ArrayList<>();
            for (NotificationDTO notif : allNotifications) {
                if (notif.getRead() == null || !notif.getRead()) {
                    filtered.add(notif);
                }
            }
        } else {
            filtered = new ArrayList<>(allNotifications);
        }

        if (filtered.isEmpty()) {
            showEmptyState(true);
        } else {
            showEmptyState(false);
            adapter.setNotifications(filtered);
        }
    }

    @Override
    public void onNotificationClick(NotificationDTO notification, int position) {
        markAsRead(notification);
        handleNotificationAction(notification);
    }

    private void markAsRead(NotificationDTO notification) {
        if (notification.getRead() != null && notification.getRead()) return;

        apiService.markNotificationRead(
            sessionManager.getAccessToken(),
            notification.getId(),
            new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                }

                @Override
                public void onError(String error) {
                }
            });
    }

    private void handleNotificationAction(NotificationDTO notification) {
        String type = notification.getType();
        String referenceId = notification.getReferenceId();
        
        if (type == null || referenceId == null || referenceId.isEmpty()) return;

        Bundle args = new Bundle();
        switch (type) {
            case "APPLICATION_UPDATE":
            case "NEW_APPLICATION":
                args.putString("applicationId", referenceId);
                break;
            case "JOB_MATCH":
                args.putString("jobId", referenceId);
                navController.navigate(R.id.jobDetailFragment, args);
                break;
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(boolean show) {
        emptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}
