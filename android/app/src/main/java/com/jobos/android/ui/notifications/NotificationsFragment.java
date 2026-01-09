package com.jobos.android.ui.notifications;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
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
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private ProgressBar progressBar;
    private NotificationAdapter adapter;
    private ApiService apiService;
    private List<NotificationDTO> allNotifications = new ArrayList<>();
    private String currentFilter = "ALL";
    
    private BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (JobOSFirebaseMessagingService.ACTION_NEW_NOTIFICATION.equals(intent.getAction())) {
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
        setupToolbar();
        setupTabs();
        setupRecyclerView();
        setupSwipeRefresh();
        loadNotifications();
        
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
        try {
            requireContext().unregisterReceiver(notificationReceiver);
        } catch (Exception ignored) {}
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        tabLayout = view.findViewById(R.id.tab_layout);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        recyclerView = view.findViewById(R.id.notifications_recycler);
        emptyState = view.findViewById(R.id.empty_state);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupToolbar() {
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_mark_all_read) {
                markAllAsRead();
                return true;
            }
            return false;
        });
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

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(this::loadNotifications);
    }

    private void loadNotifications() {
        if (!swipeRefresh.isRefreshing()) {
            showLoading(true);
        }
        apiService.getNotifications(sessionManager.getAccessToken(), 0, 50,
            new ApiCallback<List<NotificationDTO>>() {
                @Override
                public void onSuccess(List<NotificationDTO> result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        swipeRefresh.setRefreshing(false);
                        allNotifications = result != null ? result : new ArrayList<>();
                        filterNotifications();
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        swipeRefresh.setRefreshing(false);
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
        showNotificationDialog(notification);
    }

    private void showNotificationDialog(NotificationDTO notification) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_notification_detail, null);
        builder.setView(dialogView);
        
        ImageView iconView = dialogView.findViewById(R.id.notification_icon);
        TextView typeView = dialogView.findViewById(R.id.notification_type);
        TextView timeView = dialogView.findViewById(R.id.notification_time);
        TextView titleView = dialogView.findViewById(R.id.notification_title);
        TextView messageView = dialogView.findViewById(R.id.notification_message);
        MaterialButton btnDismiss = dialogView.findViewById(R.id.btn_dismiss);
        MaterialButton btnAction = dialogView.findViewById(R.id.btn_action);
        
        titleView.setText(notification.getTitle());
        messageView.setText(notification.getMessage());
        
        String type = notification.getType();
        if (type != null) {
            typeView.setText(formatType(type));
            iconView.setImageResource(getIconForType(type));
        } else {
            typeView.setText("Notification");
            iconView.setImageResource(R.drawable.ic_notifications);
        }
        
        String createdAt = notification.getCreatedAt();
        timeView.setText(createdAt != null ? createdAt : "");
        
        String referenceId = notification.getReferenceId();
        boolean hasAction = type != null && referenceId != null && !referenceId.isEmpty();
        btnAction.setVisibility(hasAction ? View.VISIBLE : View.GONE);
        
        if (hasAction) {
            btnAction.setText(getActionText(type));
        }
        
        AlertDialog dialog = builder.create();
        
        btnDismiss.setOnClickListener(v -> dialog.dismiss());
        btnAction.setOnClickListener(v -> {
            dialog.dismiss();
            handleNotificationAction(notification);
        });
        
        dialog.show();
        
        markAsRead(notification);
    }

    private String formatType(String type) {
        if (type == null) return "Notification";
        switch (type) {
            case "APPLICATION_UPDATE": return "Application Update";
            case "JOB_MATCH": return "Job Match";
            case "NEW_APPLICATION": return "New Application";
            case "MESSAGE": return "Message";
            default: return "Notification";
        }
    }

    private int getIconForType(String type) {
        if (type == null) return R.drawable.ic_notifications;
        switch (type) {
            case "APPLICATION_UPDATE": return R.drawable.ic_description;
            case "JOB_MATCH": return R.drawable.ic_work;
            case "NEW_APPLICATION": return R.drawable.ic_person;
            case "MESSAGE": return R.drawable.ic_email;
            default: return R.drawable.ic_notifications;
        }
    }

    private String getActionText(String type) {
        if (type == null) return "View";
        switch (type) {
            case "APPLICATION_UPDATE": return "View Application";
            case "JOB_MATCH": return "View Job";
            case "NEW_APPLICATION": return "View Details";
            default: return "View";
        }
    }

    private void markAsRead(NotificationDTO notification) {
        if (notification.getRead() != null && notification.getRead()) return;

        notification.setRead(true);
        adapter.notifyDataSetChanged();

        apiService.markNotificationRead(
            sessionManager.getAccessToken(),
            notification.getId(),
            new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {}

                @Override
                public void onError(String error) {}
            });
    }

    private void markAllAsRead() {
        boolean hasUnread = false;
        for (NotificationDTO notif : allNotifications) {
            if (notif.getRead() == null || !notif.getRead()) {
                hasUnread = true;
                break;
            }
        }
        
        if (!hasUnread) {
            showToast("All notifications are read");
            return;
        }

        showLoading(true);
        apiService.markAllNotificationsRead(sessionManager.getAccessToken(),
            new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        for (NotificationDTO notif : allNotifications) {
                            notif.setRead(true);
                        }
                        filterNotifications();
                        showToast("All notifications marked as read");
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Error: " + error);
                    });
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
