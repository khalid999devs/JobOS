package com.jobos.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.jobos.android.R;
import com.jobos.android.data.model.cv.CVTemplateDTO;

import java.util.ArrayList;
import java.util.List;

public class CVTemplateAdapter extends RecyclerView.Adapter<CVTemplateAdapter.ViewHolder> {

    private List<CVTemplateDTO> templates = new ArrayList<>();
    private OnTemplateActionListener listener;

    public interface OnTemplateActionListener {
        void onUseTemplate(CVTemplateDTO template);
        void onUnlockTemplate(CVTemplateDTO template);
    }

    public CVTemplateAdapter(OnTemplateActionListener listener) {
        this.listener = listener;
    }

    public void setTemplates(List<CVTemplateDTO> templates) {
        this.templates = templates != null ? templates : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cv_template, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CVTemplateDTO template = templates.get(position);
        holder.bind(template);
    }

    @Override
    public int getItemCount() {
        return templates.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView templatePreview;
        LinearLayout templatePlaceholder;
        ImageView templateIcon;
        LinearLayout premiumBadge;
        FrameLayout lockedOverlay;
        TextView templateName;
        TextView templateCategory;
        LinearLayout creditContainer;
        TextView creditCost;
        MaterialButton btnUseTemplate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            templatePreview = itemView.findViewById(R.id.template_preview);
            templatePlaceholder = itemView.findViewById(R.id.template_placeholder);
            templateIcon = itemView.findViewById(R.id.template_icon);
            premiumBadge = itemView.findViewById(R.id.premium_badge);
            lockedOverlay = itemView.findViewById(R.id.locked_overlay);
            templateName = itemView.findViewById(R.id.template_name);
            templateCategory = itemView.findViewById(R.id.template_category);
            creditContainer = itemView.findViewById(R.id.credit_container);
            creditCost = itemView.findViewById(R.id.credit_cost);
            btnUseTemplate = itemView.findViewById(R.id.btn_use_template);
        }

        void bind(CVTemplateDTO template) {
            templateName.setText(template.getName());
            
            // Set category
            String category = template.getCategory();
            if (category != null) {
                templateCategory.setText(formatCategory(category));
            } else {
                templateCategory.setText("Professional");
            }

            // Load preview image or show placeholder
            String previewUrl = template.getPreviewImageUrl();
            if (previewUrl != null && !previewUrl.isEmpty()) {
                // For now, just show placeholder since we don't have an image loader
                // In future, you can add Glide/Picasso/Coil dependency for image loading
                templatePlaceholder.setVisibility(View.VISIBLE);
                templatePreview.setVisibility(View.GONE);
                setIconColor(template.getCategory());
            } else {
                templatePlaceholder.setVisibility(View.VISIBLE);
                templatePreview.setVisibility(View.GONE);
                // Set different icon colors based on category
                setIconColor(template.getCategory());
            }

            // Handle premium status
            Boolean isPremiumValue = template.getIsPremium();
            Boolean isUnlockedValue = template.getIsUnlocked();
            boolean isPremium = isPremiumValue != null && isPremiumValue;
            boolean isUnlocked = isUnlockedValue != null && isUnlockedValue;
            
            if (isPremium) {
                premiumBadge.setVisibility(View.VISIBLE);
                
                if (!isUnlocked) {
                    // Show locked state
                    lockedOverlay.setVisibility(View.VISIBLE);
                    creditContainer.setVisibility(View.VISIBLE);
                    creditCost.setText(template.getCreditCost() + " Credits");
                    btnUseTemplate.setText("Unlock");
                    btnUseTemplate.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onUnlockTemplate(template);
                        }
                    });
                } else {
                    // Premium but unlocked
                    lockedOverlay.setVisibility(View.GONE);
                    creditContainer.setVisibility(View.GONE);
                    btnUseTemplate.setText("Use");
                    btnUseTemplate.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onUseTemplate(template);
                        }
                    });
                }
            } else {
                // Free template
                premiumBadge.setVisibility(View.GONE);
                lockedOverlay.setVisibility(View.GONE);
                creditContainer.setVisibility(View.GONE);
                btnUseTemplate.setText("Use");
                btnUseTemplate.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onUseTemplate(template);
                    }
                });
            }

            // Card click
            itemView.setOnClickListener(v -> {
                if (!isPremium || isUnlocked) {
                    if (listener != null) {
                        listener.onUseTemplate(template);
                    }
                } else {
                    if (listener != null) {
                        listener.onUnlockTemplate(template);
                    }
                }
            });
        }

        private void setIconColor(String category) {
            int color;
            if (category == null) {
                color = itemView.getContext().getResources().getColor(R.color.primary, null);
            } else {
                switch (category.toUpperCase()) {
                    case "CREATIVE":
                        color = itemView.getContext().getResources().getColor(R.color.tertiary, null);
                        break;
                    case "MODERN":
                        color = itemView.getContext().getResources().getColor(R.color.secondary, null);
                        break;
                    case "MINIMAL":
                        color = itemView.getContext().getResources().getColor(R.color.on_surface_secondary, null);
                        break;
                    case "PROFESSIONAL":
                    default:
                        color = itemView.getContext().getResources().getColor(R.color.primary, null);
                        break;
                }
            }
            templateIcon.setColorFilter(color);
        }

        private String formatCategory(String category) {
            if (category == null) return "Professional";
            String lower = category.toLowerCase();
            return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        }
    }
}
