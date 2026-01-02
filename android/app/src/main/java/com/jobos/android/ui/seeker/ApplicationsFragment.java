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
import com.google.android.material.tabs.TabLayout;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.ui.adapter.ApplicationAdapter;
import com.jobos.android.data.model.application.ApplicationDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ApplicationsFragment extends BaseFragment {

    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView applicationsRv;
    private LinearLayout emptyContainer;
    private ProgressBar progressBar;

    private ApiService apiService;
    private ApplicationAdapter adapter;
    private List<ApplicationDTO> allApplications = new ArrayList<>();
    private List<ApplicationDTO> filteredApplications = new ArrayList<>();
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMoreData = true;
    private String currentFilter = "ALL";

    private final String[] TABS = {"All", "Pending", "Reviewing", "Shortlisted", "Rejected"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_applications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        apiService = new ApiService();
        initViews(view);
        setupTabs();
        setupRecyclerView();
        loadApplications();
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tab_layout);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        applicationsRv = view.findViewById(R.id.applications_rv);
        emptyContainer = view.findViewById(R.id.empty_container);
        progressBar = view.findViewById(R.id.progress_bar);

        swipeRefresh.setOnRefreshListener(() -> {
            currentPage = 0;
            hasMoreData = true;
            allApplications.clear();
            loadApplications();
        });
    }

    private void setupTabs() {
        for (String tab : TABS) {
            tabLayout.addTab(tabLayout.newTab().setText(tab));
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                currentFilter = position == 0 ? "ALL" : TABS[position].toUpperCase();
                filterApplications();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new ApplicationAdapter(filteredApplications, this::onApplicationClick);
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        applicationsRv.setLayoutManager(layoutManager);
        applicationsRv.setAdapter(adapter);

        applicationsRv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && hasMoreData && "ALL".equals(currentFilter)) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3
                            && firstVisibleItemPosition >= 0) {
                        loadApplications();
                    }
                }
            }
        });
    }

    private void loadApplications() {
        if (isLoading) return;
        isLoading = true;

        if (allApplications.isEmpty()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        String token = sessionManager.getAccessToken();
        apiService.getMyApplications(token, currentPage, 20, new ApiCallback<List<ApplicationDTO>>() {
            @Override
            public void onSuccess(List<ApplicationDTO> result) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    isLoading = false;
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);

                    if (result.isEmpty()) {
                        hasMoreData = false;
                    } else {
                        allApplications.addAll(result);
                        currentPage++;
                    }
                    filterApplications();
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

    private void filterApplications() {
        filteredApplications.clear();
        if ("ALL".equals(currentFilter)) {
            filteredApplications.addAll(allApplications);
        } else {
            filteredApplications.addAll(
                allApplications.stream()
                    .filter(app -> currentFilter.equals(app.getStatus()))
                    .collect(Collectors.toList())
            );
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        emptyContainer.setVisibility(filteredApplications.isEmpty() ? View.VISIBLE : View.GONE);
        applicationsRv.setVisibility(filteredApplications.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void onApplicationClick(ApplicationDTO application) {
        Bundle args = new Bundle();
        args.putString("applicationId", application.getId());
        navController.navigate(R.id.action_applications_to_application_detail, args);
    }
}
