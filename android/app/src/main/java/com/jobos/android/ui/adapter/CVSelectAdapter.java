package com.jobos.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.jobos.android.R;
import com.jobos.android.data.model.cv.CVDTO;
import java.util.List;

public class CVSelectAdapter extends RecyclerView.Adapter<CVSelectAdapter.CVViewHolder> {

    private final List<CVDTO> cvList;
    private final OnCVSelectListener onSelectListener;
    private String selectedId = null;

    public interface OnCVSelectListener {
        void onSelect(CVDTO cv);
    }

    public CVSelectAdapter(List<CVDTO> cvList, OnCVSelectListener onSelectListener) {
        this.cvList = cvList;
        this.onSelectListener = onSelectListener;
    }

    public void setSelectedId(String id) {
        this.selectedId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CVViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cv_select, parent, false);
        return new CVViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CVViewHolder holder, int position) {
        CVDTO cv = cvList.get(position);
        holder.bind(cv);
    }

    @Override
    public int getItemCount() {
        return cvList.size();
    }

    class CVViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView cvName;
        private final TextView lastUpdated;
        private final ImageView checkIcon;

        CVViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            cvName = itemView.findViewById(R.id.cv_name);
            lastUpdated = itemView.findViewById(R.id.last_updated);
            checkIcon = itemView.findViewById(R.id.check_icon);
        }

        void bind(CVDTO cv) {
            cvName.setText(cv.getName());
            
            if (cv.getUpdatedAt() != null) {
                lastUpdated.setText("Updated " + cv.getUpdatedAt());
            }

            boolean isSelected = selectedId != null && selectedId.equals(cv.getId());
            checkIcon.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), 
                isSelected ? R.color.primary : R.color.outline));
            cardView.setStrokeWidth(isSelected ? 4 : 1);

            itemView.setOnClickListener(v -> {
                if (onSelectListener != null) {
                    onSelectListener.onSelect(cv);
                }
            });
        }
    }
}
