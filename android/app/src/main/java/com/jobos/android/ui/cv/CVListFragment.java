package com.jobos.android.ui.cv;

import android.app.AlertDialog;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.ui.adapter.CVAdapter;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.data.model.cv.CVDTO;
import java.util.ArrayList;
import java.util.List;

public class CVListFragment extends BaseFragment implements CVAdapter.OnCVActionListener {

    private MaterialToolbar toolbar;
    private RecyclerView cvRecycler;
    private LinearLayout emptyState;
    private MaterialButton emptyCreateCvButton;
    private ExtendedFloatingActionButton fabCreateCv;
    private ProgressBar progressBar;

    private CVAdapter adapter;
    private ApiService apiService;
    private final List<CVDTO> cvList = new ArrayList<>();

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
        setupClickListeners();
        loadCVs();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        cvRecycler = view.findViewById(R.id.cv_recycler);
        emptyState = view.findViewById(R.id.empty_state);
        emptyCreateCvButton = view.findViewById(R.id.empty_create_cv_button);
        fabCreateCv = view.findViewById(R.id.fab_create_cv);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupRecyclerView() {
        adapter = new CVAdapter(cvList, this);
        cvRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        cvRecycler.setAdapter(adapter);
    }

    private void setupClickListeners() {
        toolbar.setNavigationOnClickListener(v -> navController.popBackStack());
        fabCreateCv.setOnClickListener(v -> navController.navigate(R.id.cvEditorFragment));
        emptyCreateCvButton.setOnClickListener(v -> navController.navigate(R.id.cvEditorFragment));
    }

    private void loadCVs() {
        showLoading(true);
        apiService.getMyCVs(sessionManager.getAccessToken(),
            new ApiCallback<List<CVDTO>>() {
                @Override
                public void onSuccess(List<CVDTO> result) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        cvList.clear();
                        cvList.addAll(result);
                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                    });
                }

                @Override
                public void onError(String error) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        showLoading(false);
                        showToast("Error loading CVs: " + error);
                        updateEmptyState();
                    });
                }
            });
    }

    private void updateEmptyState() {
        boolean isEmpty = cvList.isEmpty();
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        cvRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        fabCreateCv.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
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
}
