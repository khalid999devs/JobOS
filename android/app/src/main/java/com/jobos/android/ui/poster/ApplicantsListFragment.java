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
import com.google.android.material.tabs.TabLayout;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.ui.adapter.PosterApplicationAdapter;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.data.model.application.ApplicationDTO;
import java.util.ArrayList;
import java.util.List;

public class ApplicantsListFragment extends BaseFragment {

    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private RecyclerView applicantsRecycler;
    private LinearLayout emptyState;
    private TextView emptyMessage;
    private ProgressBar progressBar;

    private PosterApplicationAdapter adapter;
    private ApiService apiService;
    private List<ApplicationDTO> allApplications = new ArrayList<>();
    private List<ApplicationDTO> filteredApplications = new ArrayList<>();
    private String jobId = null;
    private String currentFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_applicants_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = new ApiService();
        hideBottomNav();
        
        if (getArguments() != null) {
            jobId = getArguments().getString("jobId");
        }
        
        initViews(view);
        setupTabs();
        setupRecyclerView();
        setupClickListeners();
        loadApplicants();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        tabLayout = view.findViewById(R.id.tab_layout);
        applicantsRecycler = view.findViewById(R.id.applicants_recycler);
        emptyState = view.findViewById(R.id.empty_state);
        emptyMessage = view.findViewById(R.id.empty_message);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Pending"));
        tabLayout.addTab(tabLayout.newTab().setText("Reviewing"));
        tabLayout.addTab(tabLayout.newTab().setText("Shortlisted"));
        tabLayout.addTab(tabLayout.newTab().setText("Rejected"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 1: currentFilter = "PENDING"; break;
                    case 2: currentFilter = "REVIEWING"; break;
                    case 3: currentFilter = "SHORTLISTED"; break;
                    case 4: currentFilter = "REJECTED"; break;
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

    private void setupClickListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
    }

    private void loadApplicants() {
        if (jobId == null || jobId.isEmpty()) {
            showToast("Invalid job");
            navController.popBackStack();
            return;
        }

        showLoading(true);
        apiService.getJobApplications(sessionManager.getAccessToken(), jobId, 
            new ApiCallback<List<ApplicationDTO>>() {
                @Override
                public void onSuccess(List<ApplicationDTO> result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        allApplications.clear();
                        allApplications.addAll(result);
                        filterApplications();
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Error loading applicants: " + error);
                        updateEmptyState();
                    });
                }
            });
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
                case "PENDING": emptyMessage.setText("No pending applications"); break;
                case "REVIEWING": emptyMessage.setText("No applications under review"); break;
                case "SHORTLISTED": emptyMessage.setText("No shortlisted applicants"); break;
                case "REJECTED": emptyMessage.setText("No rejected applications"); break;
                default: emptyMessage.setText("No applicants yet"); break;
            }
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
