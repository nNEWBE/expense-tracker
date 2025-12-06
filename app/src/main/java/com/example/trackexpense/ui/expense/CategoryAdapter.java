package com.example.trackexpense.ui.expense;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackexpense.R;
import com.example.trackexpense.utils.CategoryHelper;
import com.google.android.material.card.MaterialCardView;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private String[] categories;
    private int selectedPosition = -1;
    private OnCategorySelectedListener listener;

    public CategoryAdapter(String[] categories) {
        this.categories = categories;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        holder.bind(categories[position], position == selectedPosition);
    }

    @Override
    public int getItemCount() {
        return categories.length;
    }

    public void setOnCategorySelectedListener(OnCategorySelectedListener listener) {
        this.listener = listener;
    }

    public String getSelectedCategory() {
        if (selectedPosition >= 0 && selectedPosition < categories.length) {
            return categories[selectedPosition];
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

                if (listener != null && selectedPosition != RecyclerView.NO_POSITION) {
                    listener.onCategorySelected(categories[selectedPosition]);
                }
            });
        }

        public void bind(String category, boolean isSelected) {
            tvCategoryName.setText(category);

            CategoryHelper.CategoryInfo info = CategoryHelper.getCategoryInfo(category);

            // Set icon
            ivIcon.setImageResource(info.iconRes);
            ivIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), android.R.color.white));

            // Set background color
            GradientDrawable bgShape = new GradientDrawable();
            bgShape.setShape(GradientDrawable.OVAL);
            bgShape.setColor(ContextCompat.getColor(itemView.getContext(), info.colorRes));
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
