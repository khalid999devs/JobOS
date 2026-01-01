package com.jobos.android.ui.notifications;

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
import com.jobos.android.R;
import com.jobos.android.network.ApiCallback;
import com.jobos.android.network.ApiService;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.shared.dto.notification.NotificationDTO;
import java.util.List;

public class NotificationsFragment extends BaseFragment implements NotificationAdapter.OnNotificationClickListener {

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private ProgressBar progressBar;
    private NotificationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hideBottomNav();
        
        initViews(view);
        setupRecyclerView();
        loadNotifications();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        recyclerView = view.findViewById(R.id.notifications_recycler);
        emptyState = view.findViewById(R.id.empty_state);
        progressBar = view.findViewById(R.id.progress_bar);

        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter();
        adapter.setOnNotificationClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadNotifications() {
        showLoading(true);
        ApiService.getInstance(requireContext()).getNotifications(sessionManager.getAccessToken(),
            new ApiCallback<List<NotificationDTO>>() {
                @Override
                public void onSuccess(List<NotificationDTO> result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        if (result == null || result.isEmpty()) {
                            showEmptyState(true);
                        } else {
                            showEmptyState(false);
                            adapter.setNotifications(result);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showEmptyState(true);
                        showToast("Error loading notifications");
                    });
                }
            });
    }

    @Override
    public void onNotificationClick(NotificationDTO notification, int position) {
        markAsRead(notification);
        handleNotificationAction(notification);
    }

    private void markAsRead(NotificationDTO notification) {
        if (notification.getRead() != null && notification.getRead()) return;

        ApiService.getInstance(requireContext()).markNotificationAsRead(
            sessionManager.getAccessToken(),
            notification.getId(),
            new ApiCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                }

                @Override
                public void onError(String error) {
                }
            });
    }

    private void handleNotificationAction(NotificationDTO notification) {
        String type = notification.getType();
        Long referenceId = notification.getReferenceId();
        
        if (type == null || referenceId == null) return;

        Bundle args = new Bundle();
        switch (type) {
            case "APPLICATION_UPDATE":
            case "NEW_APPLICATION":
                args.putLong("applicationId", referenceId);
                break;
            case "JOB_MATCH":
                args.putLong("jobId", referenceId);
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
