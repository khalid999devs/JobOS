package com.jobos.android.ui.seeker;

import android.os.Bundle;
import android.view.LayoutInflater;
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
import com.google.android.material.card.MaterialCardView;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.ui.adapter.JobAdapter;
import com.jobos.android.data.model.job.JobDTO;
import com.jobos.android.data.model.job.JobSearchRequest;
import com.jobos.android.data.local.UserDataManager;
import java.util.ArrayList;
import java.util.List;

public class SeekerHomeFragment extends BaseFragment {

    private TextView greetingText;
    private TextView userName;
    private ImageView notificationIcon;
    private TextView notificationBadge;
    private MaterialCardView searchCard;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recommendedJobsRv;
    private RecyclerView recentJobsRv;
    private LinearLayout emptyContainer;
    private ProgressBar progressBar;
    private TextView seeAllRecommended;
    private TextView seeAllRecent;

    private ApiService apiService;
    private JobAdapter recommendedAdapter;
    private JobAdapter recentAdapter;
    private List<JobDTO> recommendedJobs = new ArrayList<>();
    private List<JobDTO> recentJobs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seeker_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        apiService = new ApiService();
        initViews(view);
        setupRecyclerViews();
        setupClickListeners();
        loadData();
    }

    private void initViews(View view) {
        greetingText = view.findViewById(R.id.greeting_text);
        userName = view.findViewById(R.id.user_name);
        notificationIcon = view.findViewById(R.id.notification_icon);
        notificationBadge = view.findViewById(R.id.notification_badge);
        searchCard = view.findViewById(R.id.search_card);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        recommendedJobsRv = view.findViewById(R.id.recommended_jobs_rv);
        recentJobsRv = view.findViewById(R.id.recent_jobs_rv);
        emptyContainer = view.findViewById(R.id.empty_container);
        progressBar = view.findViewById(R.id.progress_bar);
        seeAllRecommended = view.findViewById(R.id.see_all_recommended);
        seeAllRecent = view.findViewById(R.id.see_all_recent);

        String fullName = UserDataManager.getInstance().getFullName();
        if (fullName != null && !fullName.isEmpty()) {
            userName.setText(fullName);
        } else {
            String email = sessionManager.getUserEmail();
            userName.setText(email != null ? email.split("@")[0] : getString(R.string.seeker));
        }

        updateGreeting();
    }

    private void updateGreeting() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) {
            greeting = "Good Morning";
        } else if (hour < 17) {
            greeting = "Good Afternoon";
        } else {
            greeting = "Good Evening";
        }
        greetingText.setText(greeting);
    }

    private void setupRecyclerViews() {
        recommendedAdapter = new JobAdapter(recommendedJobs, this::onJobClick, this::onBookmarkClick);
        recommendedJobsRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        recommendedJobsRv.setAdapter(recommendedAdapter);

        recentAdapter = new JobAdapter(recentJobs, this::onJobClick, this::onBookmarkClick);
        recentJobsRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentJobsRv.setAdapter(recentAdapter);
    }

    private void setupClickListeners() {
        swipeRefresh.setOnRefreshListener(this::loadData);
        
        searchCard.setOnClickListener(v -> 
            navController.navigate(R.id.action_seeker_home_to_job_search)
        );
        
        notificationIcon.setOnClickListener(v -> 
            navController.navigate(R.id.action_seeker_home_to_notifications)
        );
        
        seeAllRecommended.setOnClickListener(v -> 
            navController.navigate(R.id.action_seeker_home_to_job_search)
        );
        
        seeAllRecent.setOnClickListener(v -> 
            navController.navigate(R.id.action_seeker_home_to_job_search)
        );
    }

    private void loadData() {
        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        String token = sessionManager.getAccessToken();
        
        loadNotificationCount();
        loadRecentJobs();
    }

    private void loadRecentJobs() {
        JobSearchRequest request = new JobSearchRequest();
        request.setPage(0);
        request.setSize(10);

        String token = sessionManager.getAccessToken();
        apiService.searchJobs(token, request, new ApiCallback<List<JobDTO>>() {
            @Override
            public void onSuccess(List<JobDTO> jobs) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    recentJobs.clear();
                    recentJobs.addAll(jobs);
                    recentAdapter.notifyDataSetChanged();
                    
                    if (jobs.size() >= 5) {
                        recommendedJobs.clear();
                        recommendedJobs.addAll(jobs.subList(0, Math.min(5, jobs.size())));
                        recommendedAdapter.notifyDataSetChanged();
                    }
                    
                    hideLoading();
                    updateEmptyState();
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    hideLoading();
                    updateEmptyState();
                    showToast(error);
                });
            }
        });
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
    }

    private void updateEmptyState() {
        boolean isEmpty = recommendedJobs.isEmpty() && recentJobs.isEmpty();
        emptyContainer.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void onJobClick(JobDTO job) {
        Bundle args = new Bundle();
        args.putString("jobId", job.getId());
        navController.navigate(R.id.action_seeker_home_to_job_detail, args);
    }

    private void onBookmarkClick(JobDTO job, int position) {
        String token = sessionManager.getAccessToken();
        if (job.isSaved()) {
            apiService.unsaveJob(token, job.getId(), new ApiCallback<String>() {
                @Override
                public void onSuccess(String response) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        job.setSaved(false);
                        recentAdapter.notifyItemChanged(position);
                    });
                }

                @Override
                public void onError(String error) {}
            });
        } else {
            apiService.saveJob(token, job.getId(), new ApiCallback<String>() {
                @Override
                public void onSuccess(String response) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        job.setSaved(true);
                        recentAdapter.notifyItemChanged(position);
                    });
                }

                @Override
                public void onError(String error) {}
            });
        }
    }

    private void loadNotificationCount() {
        apiService.getUnreadNotificationCount(sessionManager.getAccessToken(), new ApiCallback<Long>() {
            @Override
            public void onSuccess(Long count) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (count > 0) {
                        notificationBadge.setVisibility(View.VISIBLE);
                        notificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                    } else {
                        notificationBadge.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onError(String error) {
                // Silently fail for badge
            }
        });
    }
}
