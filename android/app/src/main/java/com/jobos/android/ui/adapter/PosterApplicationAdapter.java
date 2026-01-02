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
import com.jobos.android.data.model.application.ApplicationDTO;
import java.util.List;

public class PosterApplicationAdapter extends RecyclerView.Adapter<PosterApplicationAdapter.ViewHolder> {

    private final List<ApplicationDTO> applications;
    private final OnApplicationClickListener onClickListener;

    public interface OnApplicationClickListener {
        void onClick(ApplicationDTO application);
    }

    public PosterApplicationAdapter(List<ApplicationDTO> applications, OnApplicationClickListener onClickListener) {
        this.applications = applications;
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_poster_application, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApplicationDTO application = applications.get(position);
        holder.bind(application);
    }

    @Override
    public int getItemCount() {
        return applications.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView avatar;
        private final TextView applicantName;
        private final TextView jobTitle;
        private final TextView appliedDate;
        private final TextView statusBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.avatar);
            applicantName = itemView.findViewById(R.id.applicant_name);
            jobTitle = itemView.findViewById(R.id.job_title);
            appliedDate = itemView.findViewById(R.id.applied_date);
            statusBadge = itemView.findViewById(R.id.status_badge);
        }

        void bind(ApplicationDTO application) {
            applicantName.setText(application.getApplicantName());
            jobTitle.setText(application.getJobTitle());
            
            if (application.getCreatedAt() != null) {
                appliedDate.setText(application.getCreatedAt());
            }

            setupStatusBadge(application.getStatus());

            itemView.setOnClickListener(v -> {
                if (onClickListener != null) {
                    onClickListener.onClick(application);
                }
            });
        }

        private void setupStatusBadge(String status) {
            if (status == null) status = "PENDING";
            
            int bgColor;
            int textColor;
            String displayText;

            switch (status) {
                case "REVIEWING":
                    bgColor = R.color.status_reviewing_bg;
                    textColor = R.color.status_reviewing;
                    displayText = "Reviewing";
                    break;
                case "SHORTLISTED":
                    bgColor = R.color.status_shortlisted_bg;
                    textColor = R.color.status_shortlisted;
                    displayText = "Shortlisted";
                    break;
                case "REJECTED":
                    bgColor = R.color.status_rejected_bg;
                    textColor = R.color.status_rejected;
                    displayText = "Rejected";
                    break;
                case "HIRED":
                    bgColor = R.color.status_hired_bg;
                    textColor = R.color.status_hired;
                    displayText = "Hired";
                    break;
                default:
                    bgColor = R.color.status_pending_bg;
                    textColor = R.color.status_pending;
                    displayText = "Pending";
                    break;
            }

            statusBadge.setText(displayText);
            statusBadge.setTextColor(ContextCompat.getColor(itemView.getContext(), textColor));
            statusBadge.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), bgColor));
        }
    }
}
