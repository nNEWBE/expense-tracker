package com.example.trackexpense.ui.dashboard;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackexpense.R;
import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.utils.CategoryHelper;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenses = new ArrayList<>();
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;
    private OnEditClickListener editClickListener;
    private OnDeleteClickListener deleteClickListener;
    private String currencySymbol = "$";
    private Set<Integer> expandedPositions = new HashSet<>();

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        boolean isExpanded = expandedPositions.contains(position);
        holder.bind(expense, isExpanded, position);
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
        this.expandedPositions.clear();
        notifyDataSetChanged();
    }

    public void setCurrencySymbol(String symbol) {
        this.currencySymbol = symbol;
    }

    public Expense getExpenseAt(int position) {
        if (position >= 0 && position < expenses.size()) {
            return expenses.get(position);
        }
        return null;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnEditClickListener(OnEditClickListener listener) {
        this.editClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(Expense expense);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Expense expense, int position);
    }

    public interface OnEditClickListener {
        void onEditClick(Expense expense, int position);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Expense expense, int position);
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private View iconBg;
        private ImageView ivIcon, ivExpandIndicator;
        private TextView tvCategory, tvDate, tvAmount, tvNotes;
        private LinearLayout expandableSection, notesContainer;
        private MaterialButton btnEdit, btnDelete;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            iconBg = itemView.findViewById(R.id.iconBg);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            ivExpandIndicator = itemView.findViewById(R.id.ivExpandIndicator);
            expandableSection = itemView.findViewById(R.id.expandableSection);
            notesContainer = itemView.findViewById(R.id.notesContainer);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(Expense expense, boolean isExpanded, int position) {
            tvCategory.setText(expense.getCategory());

            // Format date with AM/PM
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(expense.getDate())));

            // Get category info for icon and colors
            CategoryHelper.CategoryInfo categoryInfo = CategoryHelper.getCategoryInfo(expense.getCategory());
            int categoryColor = ContextCompat.getColor(itemView.getContext(), categoryInfo.colorRes);

            // Set icon with category color
            ivIcon.setImageResource(categoryInfo.iconRes);
            ivIcon.setColorFilter(categoryColor);

            // Set icon background with low opacity (15% opacity of category color)
            GradientDrawable bgShape = new GradientDrawable();
            bgShape.setShape(GradientDrawable.OVAL);
            int lowOpacityColor = androidx.core.graphics.ColorUtils.setAlphaComponent(categoryColor, 38);
            bgShape.setColor(lowOpacityColor);
            iconBg.setBackground(bgShape);

            // Set amount with color based on type (no decimals)
            if ("INCOME".equals(expense.getType())) {
                tvAmount.setText(String.format("+%s%,.0f", currencySymbol, expense.getAmount()));
                tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.income_green));
            } else {
                tvAmount.setText(String.format("-%s%,.0f", currencySymbol, expense.getAmount()));
                tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.expense_red));
            }

            // Handle expandable section
            if (expandableSection != null) {
                expandableSection.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

                if (ivExpandIndicator != null) {
                    ivExpandIndicator.setRotation(isExpanded ? 180f : 0f);
                }

                // Show notes if available
                if (notesContainer != null && tvNotes != null) {
                    String notes = expense.getNotes();
                    if (notes != null && !notes.trim().isEmpty()) {
                        notesContainer.setVisibility(View.VISIBLE);
                        tvNotes.setText(notes);
                    } else {
                        notesContainer.setVisibility(View.GONE);
                    }
                }

                // Edit button
                if (btnEdit != null) {
                    btnEdit.setOnClickListener(v -> {
                        if (editClickListener != null) {
                            editClickListener.onEditClick(expense, position);
                        }
                    });
                }

                // Delete button
                if (btnDelete != null) {
                    btnDelete.setOnClickListener(v -> {
                        if (deleteClickListener != null) {
                            deleteClickListener.onDeleteClick(expense, position);
                        }
                    });
                }
            }

            // Click to expand/collapse
            itemView.setOnClickListener(v -> {
                int adapterPosition = getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    if (expandedPositions.contains(adapterPosition)) {
                        expandedPositions.remove(adapterPosition);
                    } else {
                        expandedPositions.add(adapterPosition);
                    }
                    notifyItemChanged(adapterPosition);

                    if (listener != null) {
                        listener.onItemClick(expense);
                    }
                }
            });

            // Long click listener
            itemView.setOnLongClickListener(v -> {
                int adapterPosition = getAdapterPosition();
                if (longClickListener != null && adapterPosition != RecyclerView.NO_POSITION) {
                    longClickListener.onItemLongClick(expense, adapterPosition);
                    return true;
                }
                return false;
            });
        }
    }
}
