package com.jobos.android.ui.cv;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Rect;
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
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.ui.adapter.CVAdapter;
import com.jobos.android.adapter.CVTemplateAdapter;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.data.model.cv.CVDTO;
import com.jobos.android.data.model.cv.CVTemplateDTO;
import com.jobos.android.ui.cv.PdfPreviewActivity;
import java.util.ArrayList;
import java.util.List;

public class CVListFragment extends BaseFragment implements CVAdapter.OnCVActionListener, CVTemplateAdapter.OnTemplateActionListener {

    private MaterialToolbar toolbar;
    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefresh;
    private SwipeRefreshLayout templatesSwipeRefresh;
    private RecyclerView cvList;
    private RecyclerView templatesList;
    private LinearLayout emptyContainer;
    private ImageView emptyIcon;
    private TextView emptyTitle;
    private TextView emptyMessage;
    private FloatingActionButton fabAddCv;
    private ProgressBar progressBar;

    private CVAdapter adapter;
    private CVTemplateAdapter templateAdapter;
    private ApiService apiService;
    private final List<CVDTO> cvs = new ArrayList<>();
    private final List<CVTemplateDTO> templates = new ArrayList<>();
    
    private int currentTab = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cv_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        apiService = new ApiService();
        showBottomNav(); // Keep bottom nav visible - this is a main tab
        initViews(view);
        setupTabs();
        setupRecyclerView();
        setupTemplatesRecyclerView();
        setupClickListeners();
        updateTabVisibility(); // Set initial visibility
        loadCVs();
        loadTemplates();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        tabLayout = view.findViewById(R.id.tab_layout);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        templatesSwipeRefresh = view.findViewById(R.id.templates_swipe_refresh);
        cvList = view.findViewById(R.id.cv_list);
        templatesList = view.findViewById(R.id.templates_list);
        emptyContainer = view.findViewById(R.id.empty_container);
        emptyIcon = view.findViewById(R.id.empty_icon);
        emptyTitle = view.findViewById(R.id.empty_title);
        emptyMessage = view.findViewById(R.id.empty_message);
        fabAddCv = view.findViewById(R.id.fab_add_cv);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("My CVs"));
        tabLayout.addTab(tabLayout.newTab().setText("Templates"));
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                updateTabVisibility();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void updateTabVisibility() {
        if (currentTab == 0) {
            swipeRefresh.setVisibility(View.VISIBLE);
            templatesSwipeRefresh.setVisibility(View.GONE);
            updateEmptyState();
        } else {
            swipeRefresh.setVisibility(View.GONE);
            templatesSwipeRefresh.setVisibility(View.VISIBLE);
            updateTemplatesEmptyState();
        }
    }

    private void setupRecyclerView() {
        adapter = new CVAdapter(cvs, this);
        cvList.setLayoutManager(new LinearLayoutManager(requireContext()));
        cvList.setAdapter(adapter);
    }

    private void setupTemplatesRecyclerView() {
        templateAdapter = new CVTemplateAdapter(this);
        androidx.recyclerview.widget.GridLayoutManager gridLayoutManager = 
            new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2);
        templatesList.setLayoutManager(gridLayoutManager);
        
        int spacing = (int) (12 * getResources().getDisplayMetrics().density);
        templatesList.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, 
                    @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(view);
                int column = position % 2;
                outRect.left = column == 0 ? spacing : spacing / 2;
                outRect.right = column == 0 ? spacing / 2 : spacing;
                outRect.top = spacing;
                outRect.bottom = 0;
            }
        });
        
        templatesList.setAdapter(templateAdapter);
    }

    private void setupClickListeners() {
        fabAddCv.setOnClickListener(v -> {
            if (currentTab == 1 && !templates.isEmpty()) {
                showTemplateSelectionDialog();
            } else {
                navController.navigate(R.id.cvEditorFragment);
            }
        });
        
        swipeRefresh.setOnRefreshListener(this::loadCVs);
        
        if (templatesSwipeRefresh != null) {
            templatesSwipeRefresh.setOnRefreshListener(this::loadTemplates);
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
                        if (result != null) {
                            cvs.addAll(result);
                        }
                        adapter.notifyDataSetChanged();
                        if (currentTab == 0) {
                            updateEmptyState();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        swipeRefresh.setRefreshing(false);
                        showToast("Error loading CVs: " + error);
                        if (currentTab == 0) {
                            updateEmptyState();
                        }
                    });
                }
            });
    }

    private void loadTemplates() {
        // Only show loading indicator if we're on the Templates tab
        if (currentTab == 1) {
            if (templatesSwipeRefresh != null && !templatesSwipeRefresh.isRefreshing()) {
                showLoading(true);
            }
        }
        apiService.getCVTemplates(sessionManager.getAccessToken(), null,
            new ApiCallback<List<CVTemplateDTO>>() {
                @Override
                public void onSuccess(List<CVTemplateDTO> result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        if (templatesSwipeRefresh != null) {
                            templatesSwipeRefresh.setRefreshing(false);
                        }
                        templates.clear();
                        if (result != null) {
                            templates.addAll(result);
                        }
                        templateAdapter.setTemplates(templates);
                        if (currentTab == 1) {
                            updateTemplatesEmptyState();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        if (templatesSwipeRefresh != null) {
                            templatesSwipeRefresh.setRefreshing(false);
                        }
                        showToast("Error loading templates: " + error);
                        if (currentTab == 1) {
                            updateTemplatesEmptyState();
                        }
                    });
                }
            });
    }

    private void updateEmptyState() {
        boolean isEmpty = cvs.isEmpty();
        emptyContainer.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        cvList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        
        if (isEmpty) {
            emptyIcon.setImageResource(R.drawable.ic_document);
            emptyTitle.setText(R.string.no_cvs);
            emptyMessage.setText(R.string.no_cvs_hint);
        }
    }

    private void updateTemplatesEmptyState() {
        boolean isEmpty = templates.isEmpty();
        emptyContainer.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        templatesList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        
        if (isEmpty) {
            emptyIcon.setImageResource(R.drawable.ic_document);
            emptyTitle.setText("No Templates Available");
            emptyMessage.setText("Check your connection and pull to refresh");
        }
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

    @Override
    public void onUseTemplate(CVTemplateDTO template) {
        Bundle args = new Bundle();
        args.putString("templateId", template.getId());
        args.putString("templateName", template.getName());
        navController.navigate(R.id.cvEditorFragment, args);
    }

    @Override
    public void onUnlockTemplate(CVTemplateDTO template) {
        Integer cost = template.getCreditCost();
        new AlertDialog.Builder(requireContext())
            .setTitle("Unlock Template")
            .setMessage("Unlock \"" + template.getName() + "\" for " + (cost != null ? cost : 0) + " credits?")
            .setPositiveButton("Unlock", (dialog, which) -> unlockTemplate(template))
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public void onPreviewTemplate(CVTemplateDTO template) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_template_preview, null);
        builder.setView(dialogView);
        
        ImageView previewImage = dialogView.findViewById(R.id.preview_image);
        LinearLayout placeholderContainer = dialogView.findViewById(R.id.placeholder_container);
        TextView templateName = dialogView.findViewById(R.id.template_name);
        TextView templateDescription = dialogView.findViewById(R.id.template_description);
        TextView templateCategory = dialogView.findViewById(R.id.template_category);
        TextView sectionsCount = dialogView.findViewById(R.id.sections_count);
        MaterialButton btnClose = dialogView.findViewById(R.id.btn_close);
        MaterialButton btnUseTemplate = dialogView.findViewById(R.id.btn_use_template);
        MaterialButton btnViewPdf = dialogView.findViewById(R.id.btn_view_pdf);
        
        templateName.setText(template.getName());
        
        String description = template.getDescription();
        if (description != null && !description.isEmpty()) {
            templateDescription.setText(description);
            templateDescription.setVisibility(View.VISIBLE);
        } else {
            templateDescription.setVisibility(View.GONE);
        }
        
        String category = template.getCategory();
        if (category != null) {
            templateCategory.setText(formatCategory(category));
        } else {
            templateCategory.setText("Professional");
        }
        
        String sectionsConfig = template.getSectionsConfig();
        if (sectionsConfig != null && !sectionsConfig.isEmpty()) {
            try {
                org.json.JSONArray sections = new org.json.JSONArray(sectionsConfig);
                sectionsCount.setText(sections.length() + " sections");
            } catch (Exception e) {
                sectionsCount.setText("Standard layout");
            }
        } else {
            sectionsCount.setText("Standard layout");
        }
        
        String previewUrl = template.getPreviewImageUrl();
        if (previewUrl != null && !previewUrl.isEmpty()) {
            previewImage.setVisibility(View.VISIBLE);
            placeholderContainer.setVisibility(View.GONE);
            Glide.with(requireContext())
                    .load(previewUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerInside()
                    .placeholder(R.drawable.ic_document)
                    .error(R.drawable.ic_document)
                    .into(previewImage);
        } else {
            previewImage.setVisibility(View.GONE);
            placeholderContainer.setVisibility(View.VISIBLE);
        }
        
        Boolean isPremium = template.getIsPremium();
        Boolean isUnlocked = template.getIsUnlocked();
        boolean locked = isPremium != null && isPremium && (isUnlocked == null || !isUnlocked);
        
        if (locked) {
            Integer cost = template.getCreditCost();
            btnUseTemplate.setText("Unlock (" + (cost != null ? cost : 0) + ")");
        } else {
            btnUseTemplate.setText("Use Template");
        }
        
        AlertDialog dialog = builder.create();
        
        btnViewPdf.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PdfPreviewActivity.class);
            intent.putExtra("title", template.getName());
            intent.putExtra("isTemplate", true);
            intent.putExtra("templateCategory", template.getCategory());
            startActivity(intent);
        });
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnUseTemplate.setOnClickListener(v -> {
            dialog.dismiss();
            if (locked) {
                onUnlockTemplate(template);
            } else {
                onUseTemplate(template);
            }
        });
        
        dialog.show();
    }

    private String formatCategory(String category) {
        if (category == null) return "Professional";
        String lower = category.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private void showTemplateSelectionDialog() {
        if (templates.isEmpty()) {
            showToast("No templates available");
            return;
        }

        String[] templateNames = new String[templates.size() + 1];
        templateNames[0] = "Start from scratch";
        for (int i = 0; i < templates.size(); i++) {
            CVTemplateDTO template = templates.get(i);
            String name = template.getName();
            Boolean isPremium = template.getIsPremium();
            Boolean isUnlocked = template.getIsUnlocked();
            if (isPremium != null && isPremium) {
                if (isUnlocked != null && isUnlocked) {
                    name += " ✓";
                } else {
                    Integer cost = template.getCreditCost();
                    name += " (🔒 " + (cost != null ? cost : 0) + " credits)";
                }
            }
            templateNames[i + 1] = name;
        }

        new AlertDialog.Builder(requireContext())
            .setTitle("Choose a Template")
            .setItems(templateNames, (dialog, which) -> {
                if (which == 0) {
                    navController.navigate(R.id.cvEditorFragment);
                } else {
                    CVTemplateDTO selectedTemplate = templates.get(which - 1);
                    Boolean isPremium = selectedTemplate.getIsPremium();
                    Boolean isUnlocked = selectedTemplate.getIsUnlocked();
                    if (isPremium != null && isPremium && (isUnlocked == null || !isUnlocked)) {
                        onUnlockTemplate(selectedTemplate);
                    } else {
                        onUseTemplate(selectedTemplate);
                    }
                }
            })
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
