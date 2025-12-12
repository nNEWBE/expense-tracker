package com.example.trackexpense.ui.dialog;

import android.content.Context;
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

import java.util.ArrayList;
import java.util.List;

public class CategoryListAdapter extends RecyclerView.Adapter<CategoryListAdapter.CategoryViewHolder> {

    private List<String> categories = new ArrayList<>();
    private Context context;

    public CategoryListAdapter(Context context) {
        this.context = context;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_list, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = categories.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private View viewIconBg;
        private ImageView ivCategoryIcon;
        private TextView tvCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            viewIconBg = itemView.findViewById(R.id.viewIconBg);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
        }

        public void bind(String category) {
            tvCategoryName.setText(category);

            CategoryHelper.CategoryInfo info = CategoryHelper.getCategoryInfo(category);

            // Set icon
            ivCategoryIcon.setImageResource(info.iconRes);
            ivCategoryIcon.setColorFilter(ContextCompat.getColor(context, info.colorRes));

            // Set background color - create a circle drawable
            GradientDrawable bgDrawable = new GradientDrawable();
            bgDrawable.setShape(GradientDrawable.OVAL);
            bgDrawable.setColor(ContextCompat.getColor(context, info.bgColorRes));
            viewIconBg.setBackground(bgDrawable);
        }
    }
}
