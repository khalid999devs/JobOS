package com.jobos.android.ui.seeker;

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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.ui.adapter.JobAdapter;
import com.jobos.android.data.model.job.JobDTO;
import java.util.ArrayList;
import java.util.List;

public class SavedJobsFragment extends BaseFragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView jobsRv;
    private LinearLayout emptyContainer;
    private ProgressBar progressBar;

    private ApiService apiService;
    private JobAdapter adapter;
    private List<JobDTO> jobs = new ArrayList<>();
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMoreData = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved_jobs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        apiService = new ApiService();
        initViews(view);
        setupRecyclerView();
        loadSavedJobs();
    }

    private void initViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        jobsRv = view.findViewById(R.id.jobs_rv);
        emptyContainer = view.findViewById(R.id.empty_container);
        progressBar = view.findViewById(R.id.progress_bar);

        swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 0;
            hasMoreData = true;
            jobs.clear();
            adapter.notifyDataSetChanged();
            loadSavedJobs();
        });
    }

    private void setupRecyclerView() {
        adapter = new JobAdapter(jobs, this::onJobClick, this::onBookmarkClick);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        jobsRv.setLayoutManager(layoutManager);
        jobsRv.setAdapter(adapter);

        jobsRv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && hasMoreData) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3
                            && firstVisibleItemPosition >= 0) {
                        loadSavedJobs();
                    }
                }
            }
        });
    }

    private void loadSavedJobs() {
        if (isLoading) return;
        isLoading = true;

        if (jobs.isEmpty()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        String token = sessionManager.getAccessToken();
        apiService.getSavedJobs(token, currentPage, 20, new ApiCallback<List<JobDTO>>() {
            @Override
            public void onSuccess(List<JobDTO> result) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);

                    if (result.isEmpty()) {
                        hasMoreData = false;
                    } else {
                        for (JobDTO job : result) {
                            job.setSaved(true);
                        }
                        jobs.addAll(result);
                        adapter.notifyDataSetChanged();
                        currentPage++;
                    }
                    updateEmptyState();
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    showToast(error);
                    updateEmptyState();
                });
            }
        });
    }

    private void updateEmptyState() {
        emptyContainer.setVisibility(jobs.isEmpty() ? View.VISIBLE : View.GONE);
        jobsRv.setVisibility(jobs.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void onJobClick(JobDTO job) {
        Bundle args = new Bundle();
        args.putString("jobId", job.getId());
        navController.navigate(R.id.action_saved_jobs_to_job_detail, args);
    }

    private void onBookmarkClick(JobDTO job, int position) {
        String token = sessionManager.getAccessToken();
        apiService.unsaveJob(token, job.getId(), new ApiCallback<String>() {
            @Override
            public void onSuccess(String response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    jobs.remove(position);
                    adapter.notifyItemRemoved(position);
                    updateEmptyState();
                    showToast("Job removed from saved");
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> showToast(error));
            }
        });
    }
}
