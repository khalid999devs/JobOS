package com.jobos.android.ui.cv;

import android.app.AlertDialog;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.ui.adapter.CVAdapter;
import com.jobos.android.adapter.CVTemplateAdapter;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.data.model.cv.CVDTO;
import com.jobos.android.data.model.cv.CVTemplateDTO;
import java.util.ArrayList;
import java.util.List;

public class CVListFragment extends BaseFragment implements CVAdapter.OnCVActionListener, CVTemplateAdapter.OnTemplateActionListener {

    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView cvList;
    private RecyclerView templatesList;
    private LinearLayout emptyContainer;
    private LinearLayout templatesSection;
    private ProgressBar templatesProgress;
    private TextView seeAllTemplates;
    private FloatingActionButton fabAddCv;
    private ProgressBar progressBar;

    private CVAdapter adapter;
    private CVTemplateAdapter templateAdapter;
    private ApiService apiService;
    private final List<CVDTO> cvs = new ArrayList<>();
    private final List<CVTemplateDTO> templates = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cv_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = new ApiService();
        hideBottomNav();
        initViews(view);
        setupRecyclerView();
        setupTemplatesRecyclerView();
        setupClickListeners();
        loadCVs();
        loadTemplates();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        cvList = view.findViewById(R.id.cv_list);
        templatesList = view.findViewById(R.id.templates_list);
        emptyContainer = view.findViewById(R.id.empty_container);
        templatesSection = view.findViewById(R.id.templates_section);
        templatesProgress = view.findViewById(R.id.templates_progress);
        seeAllTemplates = view.findViewById(R.id.see_all_templates);
        fabAddCv = view.findViewById(R.id.fab_add_cv);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupRecyclerView() {
        adapter = new CVAdapter(cvs, this);
        cvList.setLayoutManager(new LinearLayoutManager(requireContext()));
        cvList.setAdapter(adapter);
    }

    private void setupTemplatesRecyclerView() {
        templateAdapter = new CVTemplateAdapter(this);
        templatesList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        templatesList.setAdapter(templateAdapter);
    }

    private void setupClickListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
        fabAddCv.setOnClickListener(v -> navController.navigate(R.id.cvEditorFragment));
        swipeRefresh.setOnRefreshListener(() -> {
            loadCVs();
            loadTemplates();
        });
        if (seeAllTemplates != null) {
            seeAllTemplates.setOnClickListener(v -> {
                // Navigate to templates list or show all templates
                showToast("All templates");
            });
        }
    }

    private void loadCVs() {
        if (!swipeRefresh.isRefreshing()) {
            showLoading(true);
        }
        apiService.getMyCVs(sessionManager.getAccessToken(),
            new ApiCallback<List<CVDTO>>() {
                @Override
                public void onSuccess(List<CVDTO> result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        swipeRefresh.setRefreshing(false);
                        cvs.clear();
                        cvs.addAll(result);
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        swipeRefresh.setRefreshing(false);
                        showToast("Error loading CVs: " + error);
                        updateEmptyState();
                    });
                }
            });
    }

    private void updateEmptyState() {
        boolean isEmpty = cvs.isEmpty();
        emptyContainer.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        cvList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onEdit(CVDTO cv) {
        Bundle args = new Bundle();
        args.putString("cvId", cv.getId());
        navController.navigate(R.id.cvEditorFragment, args);
    }

    @Override
    public void onPreview(CVDTO cv) {
        Bundle args = new Bundle();
        args.putString("cvId", cv.getId());
        navController.navigate(R.id.cvPreviewFragment, args);
    }

    @Override
    public void onDelete(CVDTO cv) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete CV")
            .setMessage("Are you sure you want to delete \"" + cv.getTitle() + "\"?")
            .setPositiveButton("Delete", (dialog, which) -> deleteCV(cv.getId()))
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public void onSetDefault(CVDTO cv) {
        showLoading(true);
        apiService.setDefaultCV(sessionManager.getAccessToken(), cv.getId(),
            new ApiCallback<CVDTO>() {
                @Override
                public void onSuccess(CVDTO result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Default CV updated");
                        loadCVs();
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Error: " + error);
                    });
                }
            });
    }

    private void deleteCV(String cvId) {
        showLoading(true);
        apiService.deleteCV(sessionManager.getAccessToken(), cvId,
            new ApiCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("CV deleted");
                        loadCVs();
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Error: " + error);
                    });
                }
            });
    }

    private void loadTemplates() {
        if (templatesProgress != null) {
            templatesProgress.setVisibility(View.VISIBLE);
        }
        apiService.getCVTemplates(sessionManager.getAccessToken(), null,
            new ApiCallback<List<CVTemplateDTO>>() {
                @Override
                public void onSuccess(List<CVTemplateDTO> result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (templatesProgress != null) {
                            templatesProgress.setVisibility(View.GONE);
                        }
                        templates.clear();
                        if (result != null) {
                            templates.addAll(result);
                        }
                        templateAdapter.setTemplates(templates);
                        if (templatesSection != null) {
                            templatesSection.setVisibility(templates.isEmpty() ? View.GONE : View.VISIBLE);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        if (templatesProgress != null) {
                            templatesProgress.setVisibility(View.GONE);
                        }
                        // Don't show error for templates, just hide section
                        if (templatesSection != null) {
                            templatesSection.setVisibility(View.GONE);
                        }
                    });
                }
            });
    }

    @Override
    public void onUseTemplate(CVTemplateDTO template) {
        // Navigate to CV editor with template
        Bundle args = new Bundle();
        args.putString("templateId", template.getId());
        args.putString("templateName", template.getName());
        navController.navigate(R.id.cvEditorFragment, args);
    }

    @Override
    public void onUnlockTemplate(CVTemplateDTO template) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Unlock Template")
            .setMessage("Unlock \"" + template.getName() + "\" for " + template.getCreditCost() + " credits?")
            .setPositiveButton("Unlock", (dialog, which) -> unlockTemplate(template))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void unlockTemplate(CVTemplateDTO template) {
        showLoading(true);
        apiService.unlockCVTemplate(sessionManager.getAccessToken(), template.getId(),
            new ApiCallback<CVTemplateDTO>() {
                @Override
                public void onSuccess(CVTemplateDTO result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Template unlocked!");
                        loadTemplates();
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Error: " + error);
                    });
                }
            });
    }
}
