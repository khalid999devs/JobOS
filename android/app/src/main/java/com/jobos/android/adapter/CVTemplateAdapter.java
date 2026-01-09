package com.jobos.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.button.MaterialButton;
import com.jobos.android.R;
import com.jobos.android.data.model.cv.CVTemplateDTO;

import java.util.ArrayList;
import java.util.List;

public class CVTemplateAdapter extends RecyclerView.Adapter<CVTemplateAdapter.ViewHolder> {

    private List<CVTemplateDTO> templates = new ArrayList<>();
    private OnTemplateActionListener listener;
    private boolean useGridLayout = true; // Default to grid layout for thumbnail view

    public interface OnTemplateActionListener {
        void onUseTemplate(CVTemplateDTO template);
        void onUnlockTemplate(CVTemplateDTO template);
    }

    public CVTemplateAdapter(OnTemplateActionListener listener) {
        this.listener = listener;
    }

    public void setUseGridLayout(boolean useGrid) {
        this.useGridLayout = useGrid;
        notifyDataSetChanged();
    }

    public void setTemplates(List<CVTemplateDTO> templates) {
        this.templates = templates != null ? templates : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = useGridLayout ? R.layout.item_cv_template_grid : R.layout.item_cv_template_list;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
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
        // Common views
        ImageView templateIcon;
        TextView templateName;
        TextView templateDescription;
        TextView templateCategory;
        LinearLayout creditContainer;
        TextView creditCost;
        MaterialButton btnUseTemplate;
        
        // Grid layout specific views
        ImageView templateThumbnail;
        LinearLayout placeholderContainer;
        LinearLayout premiumOverlay;
        
        // List layout specific view
        ImageView premiumBadge;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Common views
            templateIcon = itemView.findViewById(R.id.template_icon);
            templateName = itemView.findViewById(R.id.template_name);
            templateDescription = itemView.findViewById(R.id.template_description);
            templateCategory = itemView.findViewById(R.id.template_category);
            creditContainer = itemView.findViewById(R.id.credit_container);
            creditCost = itemView.findViewById(R.id.credit_cost);
            btnUseTemplate = itemView.findViewById(R.id.btn_use_template);
            
            // Grid specific
            templateThumbnail = itemView.findViewById(R.id.template_thumbnail);
            placeholderContainer = itemView.findViewById(R.id.placeholder_container);
            premiumOverlay = itemView.findViewById(R.id.premium_overlay);
            
            // List specific
            premiumBadge = itemView.findViewById(R.id.premium_badge);
        }

        void bind(CVTemplateDTO template) {
            templateName.setText(template.getName());
            
            // Set description
            String description = template.getDescription();
            if (description != null && !description.isEmpty()) {
                templateDescription.setText(description);
                templateDescription.setVisibility(View.VISIBLE);
            } else {
                templateDescription.setVisibility(View.GONE);
            }
            
            // Set category
            String category = template.getCategory();
            if (category != null) {
                templateCategory.setText(formatCategory(category));
            } else {
                templateCategory.setText("Professional");
            }

            // Load thumbnail image (grid layout)
            if (useGridLayout && templateThumbnail != null) {
                String previewUrl = template.getPreviewImageUrl();
                if (previewUrl != null && !previewUrl.isEmpty()) {
                    templateThumbnail.setVisibility(View.VISIBLE);
                    if (placeholderContainer != null) {
                        placeholderContainer.setVisibility(View.GONE);
                    }
                    
                    Glide.with(itemView.getContext())
                            .load(previewUrl)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .centerCrop()
                            .placeholder(R.drawable.ic_document)
                            .error(R.drawable.ic_document)
                            .into(templateThumbnail);
                } else {
                    templateThumbnail.setVisibility(View.GONE);
                    if (placeholderContainer != null) {
                        placeholderContainer.setVisibility(View.VISIBLE);
                    }
                }
            }

            // Set icon color based on category (for list layout or placeholder)
            if (templateIcon != null) {
                setIconColor(template.getCategory());
            }

            // Handle premium status
            Boolean isPremiumValue = template.getIsPremium();
            Boolean isUnlockedValue = template.getIsUnlocked();
            boolean isPremium = isPremiumValue != null && isPremiumValue;
            boolean isUnlocked = isUnlockedValue != null && isUnlockedValue;
            
            // Grid layout premium badge
            if (premiumOverlay != null) {
                premiumOverlay.setVisibility(isPremium ? View.VISIBLE : View.GONE);
            }
            
            // List layout premium badge
            if (premiumBadge != null) {
                premiumBadge.setVisibility(isPremium ? View.VISIBLE : View.GONE);
            }
            
            if (isPremium && !isUnlocked) {
                // Show locked state
                creditContainer.setVisibility(View.VISIBLE);
                Integer cost = template.getCreditCost();
                creditCost.setText((cost != null ? cost : 0) + " Credits");
                btnUseTemplate.setText("Unlock");
                btnUseTemplate.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onUnlockTemplate(template);
                    }
                });
            } else {
                // Free or unlocked template
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
            if (templateIcon == null) return;
            
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
