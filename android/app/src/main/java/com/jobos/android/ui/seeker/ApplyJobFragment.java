package com.jobos.android.ui.seeker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.jobos.android.R;
import com.jobos.android.data.network.ApiService;
import com.jobos.android.data.network.ApiCallback;
import com.jobos.android.ui.base.BaseFragment;
import com.jobos.android.ui.adapter.CVSelectAdapter;
import com.jobos.shared.dto.job.JobDTO;
import com.jobos.shared.dto.cv.CVDTO;
import com.jobos.shared.dto.application.CreateApplicationRequest;
import com.jobos.shared.dto.application.ApplicationDTO;
import java.util.ArrayList;
import java.util.List;

public class ApplyJobFragment extends BaseFragment {

    private ImageView backButton;
    private TextView jobTitle;
    private TextView companyName;
    private RecyclerView cvListRv;
    private TextInputEditText coverLetterInput;
    private Button submitButton;
    private ProgressBar progressBar;

    private ApiService apiService;
    private CVSelectAdapter cvAdapter;
    private List<CVDTO> cvList = new ArrayList<>();
    private Long jobId;
    private Long selectedCvId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_apply_job, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getArguments() != null) {
            jobId = getArguments().getLong("jobId", -1);
        }
        
        apiService = new ApiService();
        initViews(view);
        setupRecyclerView();
        setupClickListeners();
        loadJobDetails();
        loadCVs();
    }

    private void initViews(View view) {
        backButton = view.findViewById(R.id.back_button);
        jobTitle = view.findViewById(R.id.job_title);
        companyName = view.findViewById(R.id.company_name);
        cvListRv = view.findViewById(R.id.cv_list_rv);
        coverLetterInput = view.findViewById(R.id.cover_letter_input);
        submitButton = view.findViewById(R.id.submit_button);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupRecyclerView() {
        cvAdapter = new CVSelectAdapter(cvList, this::onCVSelected);
        cvListRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        cvListRv.setAdapter(cvAdapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> navController.navigateUp());
        submitButton.setOnClickListener(v -> submitApplication());
    }

    private void loadJobDetails() {
        if (jobId == null || jobId <= 0) return;

        apiService.getJobById(jobId, new ApiCallback<JobDTO>() {
            @Override
            public void onSuccess(JobDTO job) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    jobTitle.setText(job.getTitle());
                    companyName.setText(job.getCompanyName());
                });
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void loadCVs() {
        String token = sessionManager.getAccessToken();
        apiService.getMyCVs(token, new ApiCallback<List<CVDTO>>() {
            @Override
            public void onSuccess(List<CVDTO> result) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    cvList.clear();
                    cvList.addAll(result);
                    cvAdapter.notifyDataSetChanged();
                    
                    if (!cvList.isEmpty()) {
                        selectedCvId = cvList.get(0).getId();
                        cvAdapter.setSelectedId(selectedCvId);
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> showToast(error));
            }
        });
    }

    private void onCVSelected(CVDTO cv) {
        selectedCvId = cv.getId();
        cvAdapter.setSelectedId(selectedCvId);
    }

    private void submitApplication() {
        if (jobId == null || jobId <= 0) {
            showToast("Invalid job");
            return;
        }

        if (selectedCvId == null) {
            showToast("Please select a CV");
            return;
        }

        setLoading(true);

        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setJobId(jobId);
        request.setCvId(selectedCvId);
        
        String coverLetter = coverLetterInput.getText() != null ? 
            coverLetterInput.getText().toString().trim() : "";
        if (!coverLetter.isEmpty()) {
            request.setCoverLetter(coverLetter);
        }

        String token = sessionManager.getAccessToken();
        apiService.applyForJob(token, request, new ApiCallback<ApplicationDTO>() {
            @Override
            public void onSuccess(ApplicationDTO response) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    showToast(getString(R.string.success_application));
                    navController.navigateUp();
                });
            }

            @Override
            public void onError(String error) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    setLoading(false);
                    showToast(error);
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        submitButton.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
