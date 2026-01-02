package com.jobos.android.ui.seeker;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.ui.adapter.JobAdapter;
import com.jobos.android.data.model.job.JobDTO;
import com.jobos.android.data.model.job.JobSearchRequest;
import java.util.ArrayList;
import java.util.List;

public class JobSearchFragment extends BaseFragment {

    private ImageView backButton;
    private ImageView filterButton;
    private TextInputEditText searchInput;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView jobsRv;
    private TextView emptyText;
    private ProgressBar progressBar;

    private ApiService apiService;
    private JobAdapter adapter;
    private List<JobDTO> jobs = new ArrayList<>();
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    private String currentKeyword = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_job_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        apiService = new ApiService();
        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        loadJobs();
    }

    private void initViews(View view) {
        backButton = view.findViewById(R.id.back_button);
        filterButton = view.findViewById(R.id.filter_button);
        searchInput = view.findViewById(R.id.search_input);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        jobsRv = view.findViewById(R.id.jobs_rv);
        emptyText = view.findViewById(R.id.empty_text);
        progressBar = view.findViewById(R.id.progress_bar);
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
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5
                            && firstVisibleItemPosition >= 0) {
                        loadMoreJobs();
                    }
                }
            }
        });
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> navController.navigateUp());
        
        filterButton.setOnClickListener(v -> {
            // Open filter dialog
        });
        
        swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 0;
            hasMoreData = true;
            jobs.clear();
            adapter.notifyDataSetChanged();
            loadJobs();
        });
        
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSearch();
                return true;
            }
            return false;
        });
    }

    private void performSearch() {
        currentKeyword = searchInput.getText() != null ? searchInput.getText().toString().trim() : "";
        currentPage = 0;
        hasMoreData = true;
        jobs.clear();
        adapter.notifyDataSetChanged();
        loadJobs();
    }

    private void loadJobs() {
        if (isLoading) return;
        isLoading = true;

        if (jobs.isEmpty()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        JobSearchRequest request = new JobSearchRequest();
        request.setKeyword(currentKeyword.isEmpty() ? null : currentKeyword);
        request.setPage(currentPage);
        request.setSize(20);

        apiService.searchJobs(request, new ApiCallback<List<JobDTO>>() {
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

    private void loadMoreJobs() {
        loadJobs();
    }

    private void updateEmptyState() {
        emptyText.setVisibility(jobs.isEmpty() ? View.VISIBLE : View.GONE);
        jobsRv.setVisibility(jobs.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void onJobClick(JobDTO job) {
        Bundle args = new Bundle();
        args.putString("jobId", job.getId());
        navController.navigate(R.id.action_job_search_to_job_detail, args);
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
                        adapter.notifyItemChanged(position);
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
                        adapter.notifyItemChanged(position);
                    });
                }

                @Override
                public void onError(String error) {}
            });
        }
    }
}
