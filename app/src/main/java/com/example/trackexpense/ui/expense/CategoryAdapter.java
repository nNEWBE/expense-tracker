package com.example.trackexpense.ui.expense;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackexpense.R;
import com.example.trackexpense.data.model.Category;
import com.example.trackexpense.utils.CategoryHelper;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying categories in a RecyclerView.
 * Supports both static String arrays (legacy) and dynamic Category objects from
 * Firestore.
 */
public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<Category> categories = new ArrayList<>();
    private String[] legacyCategories; // For backward compatibility
    private boolean usingLegacyMode = false;
    private int selectedPosition = -1;
    private OnCategorySelectedListener listener;

    // Animation tracking
    private int lastAnimatedPosition = -1;
    private boolean animationsEnabled = true;
    private static final int STAGGER_DELAY_MS = 50; // Delay between each item animation

    /**
     * Constructor for dynamic Category list (from Firestore).
     */
    public CategoryAdapter(List<Category> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
        this.usingLegacyMode = false;
    }

    /**
     * Constructor for static String array (legacy mode).
     */
    public CategoryAdapter(String[] categories) {
        this.legacyCategories = categories;
        this.usingLegacyMode = true;

        // Convert to Category objects for consistent handling
        if (categories != null) {
            this.categories = new ArrayList<>();
            for (String name : categories) {
                Category cat = new Category();
                cat.setName(name);
                cat.setIconName("ic_" + name.toLowerCase().replace(" ", "_"));
                this.categories.add(cat);
            }
        }
    }

    /**
     * Update the categories list.
     */
    public void setCategories(List<Category> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
        this.usingLegacyMode = false;
        this.selectedPosition = -1;
        this.lastAnimatedPosition = -1; // Reset animation tracking
        this.animationsEnabled = true;
        notifyDataSetChanged();
    }

    /**
     * Update categories from string array (legacy mode).
     */
    public void setCategories(String[] categories) {
        this.legacyCategories = categories;
        this.usingLegacyMode = true;
        this.selectedPosition = -1;
        this.lastAnimatedPosition = -1; // Reset animation tracking
        this.animationsEnabled = true;

        if (categories != null) {
            this.categories = new ArrayList<>();
            for (String name : categories) {
                Category cat = new Category();
                cat.setName(name);
                cat.setIconName("ic_" + name.toLowerCase().replace(" ", "_"));
                this.categories.add(cat);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        if (position < categories.size()) {
            holder.bind(categories.get(position), position == selectedPosition);

            // Apply staggered fade-up animation
            if (animationsEnabled && position > lastAnimatedPosition) {
                // Animation removed
                lastAnimatedPosition = position;
            }
        }
    }

    /**
     * Run staggered entrance animation for each item.
     */

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void setOnCategorySelectedListener(OnCategorySelectedListener listener) {
        this.listener = listener;
    }

    public String getSelectedCategory() {
        if (selectedPosition >= 0 && selectedPosition < categories.size()) {
            return categories.get(selectedPosition).getName();
        }
        return null;
    }

    public Category getSelectedCategoryObject() {
        if (selectedPosition >= 0 && selectedPosition < categories.size()) {
            return categories.get(selectedPosition);
        }
        return null;
    }

    public interface OnCategorySelectedListener {
        void onCategorySelected(String category);
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardView;
        private View iconBg;
        private ImageView ivIcon;
        private TextView tvCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            iconBg = itemView.findViewById(R.id.iconBg);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);

            itemView.setOnClickListener(v -> {
                int oldPosition = selectedPosition;
                selectedPosition = getAdapterPosition();

                if (oldPosition != -1) {
                    notifyItemChanged(oldPosition);
                }
                notifyItemChanged(selectedPosition);

                if (listener != null && selectedPosition != RecyclerView.NO_POSITION
                        && selectedPosition < categories.size()) {
                    listener.onCategorySelected(categories.get(selectedPosition).getName());
                }
            });
        }

        public void bind(Category category, boolean isSelected) {
            tvCategoryName.setText(category.getName());

            // Get icon and color from Category object
            int iconRes = category.getIconResource();
            int categoryColor;

            // Use colorHex from Firestore if available, otherwise fall back to resource
            if (category.getColorHex() != null && !category.getColorHex().isEmpty()) {
                categoryColor = category.getColorInt();
            } else if (category.getColorResource() != 0) {
                categoryColor = ContextCompat.getColor(itemView.getContext(), category.getColorResource());
            } else {
                // Fallback to CategoryHelper for legacy support
                CategoryHelper.CategoryInfo info = CategoryHelper.getCategoryInfo(category.getName());
                iconRes = info.iconRes;
                categoryColor = ContextCompat.getColor(itemView.getContext(), info.colorRes);
            }

            // Set icon with category color
            ivIcon.setImageResource(iconRes);
            ivIcon.setColorFilter(categoryColor);

            // Set background with low opacity (15% of category color)
            GradientDrawable bgShape = new GradientDrawable();
            bgShape.setShape(GradientDrawable.OVAL);
            int lowOpacityColor = androidx.core.graphics.ColorUtils.setAlphaComponent(categoryColor, 38);
            bgShape.setColor(lowOpacityColor);
            iconBg.setBackground(bgShape);

            // Selection state
            if (isSelected) {
                cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.primary));
                cardView.setStrokeWidth(4);
                cardView.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.category_entertainment_bg));
            } else {
                cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), android.R.color.transparent));
                cardView.setStrokeWidth(0);
                cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.surface_light));
            }
        }
    }
}
