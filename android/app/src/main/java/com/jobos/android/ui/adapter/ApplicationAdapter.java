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
import com.jobos.shared.dto.application.ApplicationDTO;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ApplicationAdapter extends RecyclerView.Adapter<ApplicationAdapter.ApplicationViewHolder> {

    private final List<ApplicationDTO> applications;
    private final OnApplicationClickListener onClickListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface OnApplicationClickListener {
        void onClick(ApplicationDTO application);
    }

    public ApplicationAdapter(List<ApplicationDTO> applications, OnApplicationClickListener onClickListener) {
        this.applications = applications;
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public ApplicationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_application, parent, false);
        return new ApplicationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ApplicationViewHolder holder, int position) {
        ApplicationDTO application = applications.get(position);
        holder.bind(application);
    }

    @Override
    public int getItemCount() {
        return applications.size();
    }

    class ApplicationViewHolder extends RecyclerView.ViewHolder {
        private final TextView jobTitle;
        private final TextView companyName;
        private final TextView statusBadge;
        private final TextView appliedDate;
        private final ImageView companyLogo;

        ApplicationViewHolder(@NonNull View itemView) {
            super(itemView);
            jobTitle = itemView.findViewById(R.id.job_title);
            companyName = itemView.findViewById(R.id.company_name);
            statusBadge = itemView.findViewById(R.id.status_badge);
            appliedDate = itemView.findViewById(R.id.applied_date);
            companyLogo = itemView.findViewById(R.id.company_logo);
        }

        void bind(ApplicationDTO application) {
            jobTitle.setText(application.getJobTitle());
            companyName.setText(application.getCompanyName());
            
            if (application.getCreatedAt() != null) {
                appliedDate.setText("Applied " + dateFormat.format(application.getCreatedAt()));
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
