package com.jobos.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.jobos.android.R;
import com.jobos.shared.dto.cv.CVDTO;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CVAdapter extends RecyclerView.Adapter<CVAdapter.CVViewHolder> {

    private final List<CVDTO> cvList;
    private final OnCVActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface OnCVActionListener {
        void onEdit(CVDTO cv);
        void onPreview(CVDTO cv);
        void onDelete(CVDTO cv);
        void onSetDefault(CVDTO cv);
    }

    public CVAdapter(List<CVDTO> cvList, OnCVActionListener listener) {
        this.cvList = cvList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CVViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cv, parent, false);
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
        private final TextView cvTitle;
        private final TextView lastUpdated;
        private final ImageView moreButton;
        private final MaterialButton editButton;
        private final MaterialButton previewButton;

        CVViewHolder(@NonNull View itemView) {
            super(itemView);
            cvTitle = itemView.findViewById(R.id.cv_title);
            lastUpdated = itemView.findViewById(R.id.last_updated);
            moreButton = itemView.findViewById(R.id.more_button);
            editButton = itemView.findViewById(R.id.edit_button);
            previewButton = itemView.findViewById(R.id.preview_button);
        }

        void bind(CVDTO cv) {
            cvTitle.setText(cv.getTitle());

            if (cv.getUpdatedAt() != null) {
                lastUpdated.setText("Updated " + dateFormat.format(cv.getUpdatedAt()));
            } else if (cv.getCreatedAt() != null) {
                lastUpdated.setText("Created " + dateFormat.format(cv.getCreatedAt()));
            }

            editButton.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(cv);
            });

            previewButton.setOnClickListener(v -> {
                if (listener != null) listener.onPreview(cv);
            });

            moreButton.setOnClickListener(v -> showPopupMenu(v, cv));
        }

        private void showPopupMenu(View anchor, CVDTO cv) {
            PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
            popup.inflate(R.menu.menu_cv_item);
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_set_default) {
                    if (listener != null) listener.onSetDefault(cv);
                    return true;
                } else if (id == R.id.action_delete) {
                    if (listener != null) listener.onDelete(cv);
                    return true;
                }
                return false;
            });
            popup.show();
        }
    }
}
