package com.jobos.android.ui.poster;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.ui.adapter.PosterJobAdapter;
import com.jobos.android.ui.adapter.PosterApplicationAdapter;
import com.jobos.shared.dto.job.JobDTO;
import com.jobos.shared.dto.application.ApplicationDTO;
import java.util.ArrayList;
import java.util.List;

public class PosterDashboardFragment extends BaseFragment {

    private TextView greetingText;
    private TextView userName;
    private ImageView notificationIcon;
    private TextView activeJobsCount;
    private TextView applicationsCount;
    private TextView viewsCount;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recentApplicationsRv;
    private RecyclerView myJobsRv;
    private TextView emptyApplications;
    private TextView emptyJobs;
    private ExtendedFloatingActionButton createJobFab;
    private ProgressBar progressBar;
    private TextView seeAllApplications;
    private TextView seeAllJobs;

    private ApiService apiService;
    private PosterJobAdapter jobAdapter;
    private PosterApplicationAdapter applicationAdapter;
    private List<JobDTO> jobs = new ArrayList<>();
    private List<ApplicationDTO> applications = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_poster_dashboard, container, false);
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
        activeJobsCount = view.findViewById(R.id.active_jobs_count);
        applicationsCount = view.findViewById(R.id.applications_count);
        viewsCount = view.findViewById(R.id.views_count);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        recentApplicationsRv = view.findViewById(R.id.recent_applications_rv);
        myJobsRv = view.findViewById(R.id.my_jobs_rv);
        emptyApplications = view.findViewById(R.id.empty_applications);
        emptyJobs = view.findViewById(R.id.empty_jobs);
        createJobFab = view.findViewById(R.id.create_job_fab);
        progressBar = view.findViewById(R.id.progress_bar);
        seeAllApplications = view.findViewById(R.id.see_all_applications);
        seeAllJobs = view.findViewById(R.id.see_all_jobs);

        String name = sessionManager.getUserName();
        if (name != null && !name.isEmpty()) {
            userName.setText(name);
        } else {
            userName.setText(getString(R.string.poster));
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
        applicationAdapter = new PosterApplicationAdapter(applications, this::onApplicationClick);
        recentApplicationsRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentApplicationsRv.setAdapter(applicationAdapter);

        jobAdapter = new PosterJobAdapter(jobs, this::onJobClick, this::onJobEditClick);
        myJobsRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        myJobsRv.setAdapter(jobAdapter);
    }

    private void setupClickListeners() {
        swipeRefresh.setOnRefreshListener(this::loadData);
        
        createJobFab.setOnClickListener(v -> 
            navController.navigate(R.id.action_poster_dashboard_to_create_job)
        );
        
        notificationIcon.setOnClickListener(v -> 
            navController.navigate(R.id.action_poster_dashboard_to_notifications)
        );
        
        seeAllApplications.setOnClickListener(v -> 
            navController.navigate(R.id.action_poster_dashboard_to_manage_jobs)
        );
        
        seeAllJobs.setOnClickListener(v -> 
            navController.navigate(R.id.action_poster_dashboard_to_manage_jobs)
        );
    }

    private void loadData() {
        if (!swipeRefresh.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        loadMyJobs();
    }

    private void loadMyJobs() {
        String token = sessionManager.getAccessToken();
        apiService.getMyPostedJobs(token, 0, 5, new ApiCallback<List<JobDTO>>() {
            @Override
            public void onSuccess(List<JobDTO> result) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    jobs.clear();
                    jobs.addAll(result);
                    jobAdapter.notifyDataSetChanged();
                    
                    int activeCount = (int) result.stream().filter(j -> "ACTIVE".equals(j.getStatus())).count();
                    activeJobsCount.setText(String.valueOf(activeCount));
                    
                    emptyJobs.setVisibility(jobs.isEmpty() ? View.VISIBLE : View.GONE);
                    
                    loadRecentApplications();
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    hideLoading();
                    showToast(error);
                });
            }
        });
    }

    private void loadRecentApplications() {
        if (jobs.isEmpty()) {
            hideLoading();
            return;
        }

        String token = sessionManager.getAccessToken();
        Long firstJobId = jobs.get(0).getId();
        
        apiService.getApplicationsForJob(token, firstJobId, 0, 5, new ApiCallback<List<ApplicationDTO>>() {
            @Override
            public void onSuccess(List<ApplicationDTO> result) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    applications.clear();
                    applications.addAll(result);
                    applicationAdapter.notifyDataSetChanged();
                    applicationsCount.setText(String.valueOf(result.size()));
                    emptyApplications.setVisibility(applications.isEmpty() ? View.VISIBLE : View.GONE);
                    hideLoading();
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    hideLoading();
                });
            }
        });
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
    }

    private void onApplicationClick(ApplicationDTO application) {
        Bundle args = new Bundle();
        args.putLong("applicationId", application.getId());
        navController.navigate(R.id.action_poster_dashboard_to_applicant_detail, args);
    }

    private void onJobClick(JobDTO job) {
        Bundle args = new Bundle();
        args.putLong("jobId", job.getId());
        navController.navigate(R.id.action_poster_dashboard_to_applicants, args);
    }

    private void onJobEditClick(JobDTO job) {
        Bundle args = new Bundle();
        args.putLong("jobId", job.getId());
        navController.navigate(R.id.action_poster_dashboard_to_edit_job, args);
    }
}
