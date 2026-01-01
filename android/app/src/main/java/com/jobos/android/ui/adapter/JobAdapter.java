package com.jobos.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jobos.android.R;
import com.jobos.shared.dto.job.JobDTO;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class JobAdapter extends RecyclerView.Adapter<JobAdapter.JobViewHolder> {

    private final List<JobDTO> jobs;
    private final OnJobClickListener onJobClick;
    private final OnBookmarkClickListener onBookmarkClick;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface OnJobClickListener {
        void onClick(JobDTO job);
    }

    public interface OnBookmarkClickListener {
        void onClick(JobDTO job, int position);
    }

    public JobAdapter(List<JobDTO> jobs, OnJobClickListener onJobClick, OnBookmarkClickListener onBookmarkClick) {
        this.jobs = jobs;
        this.onJobClick = onJobClick;
        this.onBookmarkClick = onBookmarkClick;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_job_card, parent, false);
        return new JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        JobDTO job = jobs.get(position);
        holder.bind(job, position);
    }

    @Override
    public int getItemCount() {
        return jobs.size();
    }

    class JobViewHolder extends RecyclerView.ViewHolder {
        private final TextView jobTitle;
        private final TextView companyName;
        private final TextView jobTypeTag;
        private final TextView locationTag;
        private final TextView salary;
        private final TextView postedDate;
        private final ImageView bookmarkButton;
        private final ImageView companyLogo;

        JobViewHolder(@NonNull View itemView) {
            super(itemView);
            jobTitle = itemView.findViewById(R.id.job_title);
            companyName = itemView.findViewById(R.id.company_name);
            jobTypeTag = itemView.findViewById(R.id.job_type_tag);
            locationTag = itemView.findViewById(R.id.location_tag);
            salary = itemView.findViewById(R.id.salary);
            postedDate = itemView.findViewById(R.id.posted_date);
            bookmarkButton = itemView.findViewById(R.id.bookmark_button);
            companyLogo = itemView.findViewById(R.id.company_logo);
        }

        void bind(JobDTO job, int position) {
            jobTitle.setText(job.getTitle());
            companyName.setText(job.getCompanyName());
            jobTypeTag.setText(formatJobType(job.getJobType()));
            locationTag.setText(job.getLocation());
            
            if (job.getSalaryMin() != null && job.getSalaryMax() != null) {
                salary.setText(String.format(Locale.getDefault(), "$%,d - $%,d", 
                    job.getSalaryMin().intValue(), job.getSalaryMax().intValue()));
            } else if (job.getSalaryMin() != null) {
                salary.setText(String.format(Locale.getDefault(), "From $%,d", job.getSalaryMin().intValue()));
            } else {
                salary.setText("Salary not specified");
            }

            if (job.getCreatedAt() != null) {
                postedDate.setText(dateFormat.format(job.getCreatedAt()));
            }

            bookmarkButton.setImageResource(job.isSaved() ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark_outline);

            itemView.setOnClickListener(v -> {
                if (onJobClick != null) onJobClick.onClick(job);
            });

            bookmarkButton.setOnClickListener(v -> {
                if (onBookmarkClick != null) onBookmarkClick.onClick(job, position);
            });
        }

        private String formatJobType(String jobType) {
            if (jobType == null) return "";
            return jobType.replace("_", " ");
        }
    }
}
