package com.jobos.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.jobos.android.R;
import com.jobos.android.data.model.job.JobDTO;
import java.util.List;

public class PosterJobAdapter extends RecyclerView.Adapter<PosterJobAdapter.JobViewHolder> {

    private final List<JobDTO> jobs;
    private final OnJobClickListener onJobClick;
    private final OnJobEditListener onJobEdit;

    public interface OnJobClickListener {
        void onClick(JobDTO job);
    }

    public interface OnJobEditListener {
        void onEdit(JobDTO job);
    }

    public PosterJobAdapter(List<JobDTO> jobs, OnJobClickListener onJobClick, OnJobEditListener onJobEdit) {
        this.jobs = jobs;
        this.onJobClick = onJobClick;
        this.onJobEdit = onJobEdit;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_poster_job, parent, false);
        return new JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        JobDTO job = jobs.get(position);
        holder.bind(job);
    }

    @Override
    public int getItemCount() {
        return jobs.size();
    }

    class JobViewHolder extends RecyclerView.ViewHolder {
        private final TextView jobTitle;
        private final TextView statusBadge;
        private final TextView applicantsCount;
        private final TextView postedDate;
        private final ImageView editButton;

        JobViewHolder(@NonNull View itemView) {
            super(itemView);
            jobTitle = itemView.findViewById(R.id.job_title);
            statusBadge = itemView.findViewById(R.id.status_badge);
            applicantsCount = itemView.findViewById(R.id.applicants_count);
            postedDate = itemView.findViewById(R.id.posted_date);
            editButton = itemView.findViewById(R.id.edit_button);
        }

        void bind(JobDTO job) {
            jobTitle.setText(job.getTitle());
            applicantsCount.setText(job.getApplicationCount() + " applicants");
            
            if (job.getCreatedAt() != null) {
                postedDate.setText("Posted " + job.getCreatedAt());
            }

            setupStatusBadge(job.getStatus());

            itemView.setOnClickListener(v -> {
                if (onJobClick != null) onJobClick.onClick(job);
            });

            editButton.setOnClickListener(v -> {
                if (onJobEdit != null) onJobEdit.onEdit(job);
            });
        }

        private void setupStatusBadge(String status) {
            if (status == null) status = "ACTIVE";
            
            int bgColor;
            int textColor;

            switch (status) {
                case "CLOSED":
                    bgColor = R.color.job_status_closed_bg;
                    textColor = R.color.job_status_closed;
                    break;
                case "DRAFT":
                    bgColor = R.color.job_status_draft_bg;
                    textColor = R.color.job_status_draft;
                    break;
                default:
                    bgColor = R.color.job_status_active_bg;
                    textColor = R.color.job_status_active;
                    break;
            }

            statusBadge.setText(status);
            statusBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), textColor));
            statusBadge.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), bgColor));
        }
    }
}
