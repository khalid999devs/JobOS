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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.jobos.android.R;
import com.jobos.android.network.ApiCallback;
import com.jobos.android.network.ApiService;
import com.jobos.android.ui.adapter.PosterJobAdapter;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.shared.dto.job.JobDTO;
import java.util.ArrayList;
import java.util.List;

public class ManageJobsFragment extends BaseFragment {

    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private RecyclerView jobsRecycler;
    private LinearLayout emptyState;
    private TextView emptyMessage;
    private ProgressBar progressBar;
    private ExtendedFloatingActionButton fabCreateJob;

    private PosterJobAdapter adapter;
    private List<JobDTO> allJobs = new ArrayList<>();
    private List<JobDTO> filteredJobs = new ArrayList<>();
    private String currentFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_jobs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hideBottomNav();
        initViews(view);
        setupTabs();
        setupRecyclerView();
        setupClickListeners();
        loadJobs();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        tabLayout = view.findViewById(R.id.tab_layout);
        jobsRecycler = view.findViewById(R.id.jobs_recycler);
        emptyState = view.findViewById(R.id.empty_state);
        emptyMessage = view.findViewById(R.id.empty_message);
        progressBar = view.findViewById(R.id.progress_bar);
        fabCreateJob = view.findViewById(R.id.fab_create_job);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Active"));
        tabLayout.addTab(tabLayout.newTab().setText("Closed"));
        tabLayout.addTab(tabLayout.newTab().setText("Draft"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 1: currentFilter = "ACTIVE"; break;
                    case 2: currentFilter = "CLOSED"; break;
                    case 3: currentFilter = "DRAFT"; break;
                    default: currentFilter = "ALL"; break;
                }
                filterJobs();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new PosterJobAdapter(filteredJobs,
            job -> {
                Bundle args = new Bundle();
                args.putLong("jobId", job.getId());
                navController.navigate(R.id.applicantsListFragment, args);
            },
            job -> {
                Bundle args = new Bundle();
                args.putLong("jobId", job.getId());
                navController.navigate(R.id.editJobFragment, args);
            });
        jobsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        jobsRecycler.setAdapter(adapter);
    }

    private void setupClickListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
        fabCreateJob.setOnClickListener(v -> navController.navigate(R.id.createJobFragment));
    }

    private void loadJobs() {
        showLoading(true);
        ApiService.getInstance(requireContext()).getMyPostedJobs(sessionManager.getAccessToken(), 
            new ApiCallback<List<JobDTO>>() {
                @Override
                public void onSuccess(List<JobDTO> result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        allJobs.clear();
                        allJobs.addAll(result);
                        filterJobs();
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

    private void filterJobs() {
        filteredJobs.clear();
        if (currentFilter.equals("ALL")) {
            filteredJobs.addAll(allJobs);
        } else {
            for (JobDTO job : allJobs) {
                if (currentFilter.equals(job.getStatus())) {
                    filteredJobs.add(job);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean isEmpty = filteredJobs.isEmpty();
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        jobsRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        
        if (isEmpty) {
            switch (currentFilter) {
                case "ACTIVE": emptyMessage.setText("No active jobs"); break;
                case "CLOSED": emptyMessage.setText("No closed jobs"); break;
                case "DRAFT": emptyMessage.setText("No draft jobs"); break;
                default: emptyMessage.setText("No jobs posted yet"); break;
            }
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
