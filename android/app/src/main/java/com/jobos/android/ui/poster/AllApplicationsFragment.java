package com.jobos.android.ui.poster;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.ui.adapter.PosterApplicationAdapter;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.data.model.application.ApplicationDTO;
import com.jobos.android.data.model.job.JobDTO;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment showing all applications received by a poster across all their jobs.
 * This is the entry point from the bottom navigation tab.
 */
public class AllApplicationsFragment extends BaseFragment {

    private TabLayout tabLayout;
    private RecyclerView applicantsRecycler;
    private LinearLayout emptyState;
    private TextView emptyTitle;
    private TextView emptyMessage;
    private ProgressBar progressBar;

    private PosterApplicationAdapter adapter;
    private ApiService apiService;
    private List<ApplicationDTO> allApplications = new ArrayList<>();
    private List<ApplicationDTO> filteredApplications = new ArrayList<>();
    private List<JobDTO> posterJobs = new ArrayList<>();
    private String currentFilter = "ALL";
    private int loadedJobCount = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_all_applications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = new ApiService();
        showBottomNav();
        
        initViews(view);
        setupTabs();
        setupRecyclerView();
        loadPosterJobs();
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tab_layout);
        applicantsRecycler = view.findViewById(R.id.applicants_recycler);
        emptyState = view.findViewById(R.id.empty_state);
        emptyTitle = view.findViewById(R.id.empty_title);
        emptyMessage = view.findViewById(R.id.empty_message);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Pending"));
        tabLayout.addTab(tabLayout.newTab().setText("Reviewed"));
        tabLayout.addTab(tabLayout.newTab().setText("Shortlisted"));
        tabLayout.addTab(tabLayout.newTab().setText("Accepted"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 1: currentFilter = "PENDING"; break;
                    case 2: currentFilter = "REVIEWED"; break;
                    case 3: currentFilter = "SHORTLISTED"; break;
                    case 4: currentFilter = "ACCEPTED"; break;
                    default: currentFilter = "ALL"; break;
                }
                filterApplications();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new PosterApplicationAdapter(filteredApplications, application -> {
            Bundle args = new Bundle();
            args.putString("applicationId", application.getId());
            navController.navigate(R.id.applicantDetailFragment, args);
        });
        applicantsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        applicantsRecycler.setAdapter(adapter);
    }

    private void loadPosterJobs() {
        showLoading(true);
        allApplications.clear();
        loadedJobCount = 0;

        apiService.getMyPostedJobs(sessionManager.getAccessToken(), 0, 100,
            new ApiCallback<List<JobDTO>>() {
                @Override
                public void onSuccess(List<JobDTO> result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (result == null || result.isEmpty()) {
                            showLoading(false);
                            updateEmptyState();
                        } else {
                            posterJobs.clear();
                            posterJobs.addAll(result);
                            loadApplicationsForJobs();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Error loading jobs: " + error);
                        updateEmptyState();
                    });
                }
            });
    }

    private void loadApplicationsForJobs() {
        if (posterJobs.isEmpty()) {
            showLoading(false);
            updateEmptyState();
            return;
        }

        // Load applications for each job
        for (JobDTO job : posterJobs) {
            apiService.getJobApplications(sessionManager.getAccessToken(), job.getId(),
                new ApiCallback<List<ApplicationDTO>>() {
                    @Override
                    public void onSuccess(List<ApplicationDTO> result) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            if (result != null) {
                                // Add job title to each application for context
                                for (ApplicationDTO app : result) {
                                    app.setJobTitle(job.getTitle());
                                }
                                allApplications.addAll(result);
                            }
                            loadedJobCount++;
                            checkAllJobsLoaded();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            loadedJobCount++;
                            checkAllJobsLoaded();
                        });
                    }
                });
        }
    }

    private void checkAllJobsLoaded() {
        if (loadedJobCount >= posterJobs.size()) {
            showLoading(false);
            filterApplications();
        }
    }

    private void filterApplications() {
        filteredApplications.clear();
        if (currentFilter.equals("ALL")) {
            filteredApplications.addAll(allApplications);
        } else {
            for (ApplicationDTO app : allApplications) {
                if (currentFilter.equals(app.getStatus())) {
                    filteredApplications.add(app);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean isEmpty = filteredApplications.isEmpty();
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        applicantsRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        
        if (isEmpty) {
            switch (currentFilter) {
                case "PENDING":
                    emptyTitle.setText("No pending applications");
                    emptyMessage.setText("Applications waiting for review will appear here");
                    break;
                case "REVIEWING":
                    emptyTitle.setText("No applications under review");
                    emptyMessage.setText("Applications you're reviewing will appear here");
                    break;
                case "SHORTLISTED":
                    emptyTitle.setText("No shortlisted applicants");
                    emptyMessage.setText("Shortlisted candidates will appear here");
                    break;
                case "REJECTED":
                    emptyTitle.setText("No rejected applications");
                    emptyMessage.setText("Rejected applications will appear here");
                    break;
                default:
                    emptyTitle.setText("No applications yet");
                    emptyMessage.setText("Post jobs to start receiving applications");
                    break;
            }
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
