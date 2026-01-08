package com.jobos.android.ui.seeker;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
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

    private MaterialToolbar toolbar;
    private TextInputEditText searchInput;
    private Chip chipLocation;
    private Chip chipJobType;
    private Chip chipSalary;
    private Chip chipRemote;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView jobsRv;
    private LinearLayout emptyContainer;
    private LinearLayout initialContainer;
    private ProgressBar progressBar;

    private ApiService apiService;
    private JobAdapter adapter;
    private List<JobDTO> jobs = new ArrayList<>();
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    private String currentKeyword = "";
    private boolean hasSearched = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_job_search, container, false);
    }

    // Filter state
    private String filterLocation = null;
    private String filterJobType = null;
    private Integer filterSalaryMin = null;
    private Integer filterSalaryMax = null;
    private Boolean filterRemote = null;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        apiService = new ApiService();
        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        setupFilterChips();
        
        // Load jobs automatically when entering the screen
        hasSearched = true;
        loadJobs();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        searchInput = view.findViewById(R.id.search_input);
        chipLocation = view.findViewById(R.id.chip_location);
        chipJobType = view.findViewById(R.id.chip_job_type);
        chipSalary = view.findViewById(R.id.chip_salary);
        chipRemote = view.findViewById(R.id.chip_remote);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        jobsRv = view.findViewById(R.id.jobs_rv);
        emptyContainer = view.findViewById(R.id.empty_container);
        initialContainer = view.findViewById(R.id.initial_container);
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

    private void setupFilterChips() {
        chipLocation.setOnClickListener(v -> showLocationFilterDialog());
        chipJobType.setOnClickListener(v -> showJobTypeFilterDialog());
        chipSalary.setOnClickListener(v -> showSalaryFilterDialog());
        chipRemote.setOnClickListener(v -> toggleRemoteFilter());
        
        updateFilterChipStates();
    }

    private void showLocationFilterDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Enter Location");
        
        android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("e.g., New York, Remote");
        input.setText(filterLocation);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);
        builder.setView(input);
        
        builder.setPositiveButton("Apply", (dialog, which) -> {
            String location = input.getText().toString().trim();
            filterLocation = location.isEmpty() ? null : location;
            updateFilterChipStates();
            reloadWithFilters();
        });
        builder.setNegativeButton("Clear", (dialog, which) -> {
            filterLocation = null;
            updateFilterChipStates();
            reloadWithFilters();
        });
        builder.setNeutralButton("Cancel", null);
        builder.show();
    }

    private void showJobTypeFilterDialog() {
        String[] jobTypes = {"Full-time", "Part-time", "Contract", "Freelance", "Internship"};
        String[] jobTypeValues = {"FULL_TIME", "PART_TIME", "CONTRACT", "FREELANCE", "INTERNSHIP"};
        int checkedIndex = -1;
        if (filterJobType != null) {
            for (int i = 0; i < jobTypeValues.length; i++) {
                if (jobTypeValues[i].equals(filterJobType)) {
                    checkedIndex = i;
                    break;
                }
            }
        }
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Select Job Type");
        builder.setSingleChoiceItems(jobTypes, checkedIndex, (dialog, which) -> {
            filterJobType = jobTypeValues[which];
        });
        builder.setPositiveButton("Apply", (dialog, which) -> {
            updateFilterChipStates();
            reloadWithFilters();
        });
        builder.setNegativeButton("Clear", (dialog, which) -> {
            filterJobType = null;
            updateFilterChipStates();
            reloadWithFilters();
        });
        builder.setNeutralButton("Cancel", null);
        builder.show();
    }

    private void showSalaryFilterDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Salary Range");
        
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        
        android.widget.EditText minInput = new android.widget.EditText(requireContext());
        minInput.setHint("Min Salary (e.g., 50000)");
        minInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (filterSalaryMin != null) minInput.setText(String.valueOf(filterSalaryMin));
        layout.addView(minInput);
        
        android.widget.EditText maxInput = new android.widget.EditText(requireContext());
        maxInput.setHint("Max Salary (e.g., 100000)");
        maxInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (filterSalaryMax != null) maxInput.setText(String.valueOf(filterSalaryMax));
        layout.addView(maxInput);
        
        builder.setView(layout);
        
        builder.setPositiveButton("Apply", (dialog, which) -> {
            String minStr = minInput.getText().toString().trim();
            String maxStr = maxInput.getText().toString().trim();
            filterSalaryMin = minStr.isEmpty() ? null : Integer.parseInt(minStr);
            filterSalaryMax = maxStr.isEmpty() ? null : Integer.parseInt(maxStr);
            updateFilterChipStates();
            reloadWithFilters();
        });
        builder.setNegativeButton("Clear", (dialog, which) -> {
            filterSalaryMin = null;
            filterSalaryMax = null;
            updateFilterChipStates();
            reloadWithFilters();
        });
        builder.setNeutralButton("Cancel", null);
        builder.show();
    }

    private void toggleRemoteFilter() {
        if (filterRemote == null) {
            filterRemote = true;
        } else if (filterRemote) {
            filterRemote = false;
        } else {
            filterRemote = null;
        }
        updateFilterChipStates();
        reloadWithFilters();
    }

    private void updateFilterChipStates() {
        chipLocation.setChecked(filterLocation != null);
        chipLocation.setText(filterLocation != null ? filterLocation : "Location");
        
        chipJobType.setChecked(filterJobType != null);
        if (filterJobType != null) {
            String display = filterJobType.replace("_", "-").toLowerCase();
            display = display.substring(0, 1).toUpperCase() + display.substring(1);
            chipJobType.setText(display);
        } else {
            chipJobType.setText("Job Type");
        }
        
        chipSalary.setChecked(filterSalaryMin != null || filterSalaryMax != null);
        if (filterSalaryMin != null || filterSalaryMax != null) {
            String salaryText = "";
            if (filterSalaryMin != null && filterSalaryMax != null) {
                salaryText = "$" + formatSalary(filterSalaryMin) + " - $" + formatSalary(filterSalaryMax);
            } else if (filterSalaryMin != null) {
                salaryText = "$" + formatSalary(filterSalaryMin) + "+";
            } else {
                salaryText = "Up to $" + formatSalary(filterSalaryMax);
            }
            chipSalary.setText(salaryText);
        } else {
            chipSalary.setText("Salary");
        }
        
        chipRemote.setChecked(filterRemote != null);
        if (filterRemote != null) {
            chipRemote.setText(filterRemote ? "Remote" : "On-site");
        } else {
            chipRemote.setText("Remote");
        }
    }

    private String formatSalary(int salary) {
        if (salary >= 1000) {
            return (salary / 1000) + "K";
        }
        return String.valueOf(salary);
    }

    private void reloadWithFilters() {
        currentPage = 0;
        hasMoreData = true;
        jobs.clear();
        adapter.notifyDataSetChanged();
        loadJobs();
    }

    private void setupClickListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.navigateUp());
        
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
        hasSearched = true;
        jobs.clear();
        adapter.notifyDataSetChanged();
        loadJobs();
    }

    private void loadJobs() {
        if (isLoading) return;
        isLoading = true;

        if (jobs.isEmpty()) {
            progressBar.setVisibility(View.VISIBLE);
            initialContainer.setVisibility(View.GONE);
        }

        JobSearchRequest request = new JobSearchRequest();
        request.setKeywords(currentKeyword.isEmpty() ? null : currentKeyword);
        request.setPage(currentPage);
        request.setSize(20);
        
        // Apply filters
        request.setLocation(filterLocation);
        if (filterJobType != null) {
            request.setJobTypes(java.util.Collections.singletonList(filterJobType));
        }
        request.setSalaryMin(filterSalaryMin);
        request.setSalaryMax(filterSalaryMax);
        request.setIsRemote(filterRemote);

        String token = sessionManager.getAccessToken();
        apiService.searchJobs(token, request, new ApiCallback<List<JobDTO>>() {
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
                    updateUIState();
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
                    updateUIState();
                });
            }
        });
    }

    private void loadMoreJobs() {
        loadJobs();
    }

    private void updateUIState() {
        if (!hasSearched) {
            initialContainer.setVisibility(View.VISIBLE);
            emptyContainer.setVisibility(View.GONE);
            jobsRv.setVisibility(View.GONE);
        } else if (jobs.isEmpty()) {
            initialContainer.setVisibility(View.GONE);
            emptyContainer.setVisibility(View.VISIBLE);
            jobsRv.setVisibility(View.GONE);
        } else {
            initialContainer.setVisibility(View.GONE);
            emptyContainer.setVisibility(View.GONE);
            jobsRv.setVisibility(View.VISIBLE);
        }
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
