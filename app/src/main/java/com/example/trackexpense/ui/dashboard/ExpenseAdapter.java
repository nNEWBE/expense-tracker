package com.example.trackexpense.ui.dashboard;

import android.content.Context;
import android.graphics.Color;
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
import com.example.trackexpense.data.model.Category;
import com.example.trackexpense.utils.CategoryHelper;
import com.example.trackexpense.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenses = new ArrayList<>();
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;
    private OnEditClickListener editClickListener;
    private OnDeleteClickListener deleteClickListener;
    private OnPinClickListener pinClickListener;
    private String currencySymbol = "$";
    private Set<Integer> expandedPositions = new HashSet<>();
    private boolean expandableEnabled = true; // Default is enabled

    // Category cache for dynamic icons/colors from Firestore
    private Map<String, Category> categoryCache = new HashMap<>();
    private PreferenceManager preferenceManager;

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);

        // Initialize category cache on first view creation
        if (preferenceManager == null) {
            preferenceManager = new PreferenceManager(parent.getContext());
            loadCategoryCache();
        }

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

    public void setOnPinClickListener(OnPinClickListener listener) {
        this.pinClickListener = listener;
    }

    /**
     * Enable or disable the expandable feature.
     * When disabled, the expand indicator is hidden and clicking won't expand.
     */
    public void setExpandableEnabled(boolean enabled) {
        this.expandableEnabled = enabled;
        notifyDataSetChanged();
    }

    /**
     * Load category data from cache for dynamic icons and colors.
     */
    private void loadCategoryCache() {
        categoryCache.clear();

        // Load expense categories
        String expenseData = preferenceManager.getCachedExpenseCategories();
        parseCacheData(expenseData, "EXPENSE");

        // Load income categories
        String incomeData = preferenceManager.getCachedIncomeCategories();
        parseCacheData(incomeData, "INCOME");
    }

    /**
     * Parse cache data string into Category objects.
     */
    private void parseCacheData(String data, String type) {
        if (data == null || data.isEmpty())
            return;

        String[] items = data.split(";");
        for (int i = 0; i < items.length; i++) {
            String[] parts = items[i].split("\\|");
            if (parts.length >= 3) {
                Category cat = new Category(parts[0], type, parts[1], parts[2], i, true);
                categoryCache.put(parts[0].toLowerCase(), cat);
            }
        }
    }

    /**
     * Get category from cache or create fallback.
     */
    private Category getCachedCategory(String categoryName) {
        if (categoryName == null)
            return null;
        return categoryCache.get(categoryName.toLowerCase());
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

    public interface OnPinClickListener {
        void onPinClick(Expense expense, int position);
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private View iconBg;
        private ImageView ivIcon, ivExpandIndicator, ivPinIndicator;
        private TextView tvCategory, tvDate, tvAmount, tvNotes;
        private LinearLayout expandableSection, notesContainer;
        private MaterialButton btnEdit, btnDelete, btnPin;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            iconBg = itemView.findViewById(R.id.iconBg);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            ivPinIndicator = itemView.findViewById(R.id.ivPinIndicator);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            ivExpandIndicator = itemView.findViewById(R.id.ivExpandIndicator);
            expandableSection = itemView.findViewById(R.id.expandableSection);
            notesContainer = itemView.findViewById(R.id.notesContainer);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnPin = itemView.findViewById(R.id.btnPin);
        }

        public void bind(Expense expense, boolean isExpanded, int position) {
            tvCategory.setText(expense.getCategory());

            // Format date with AM/PM
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(expense.getDate())));

            // Get category info - try cached Firestore data first, then fallback to
            // CategoryHelper
            int iconRes;
            int categoryColor;

            Category cachedCategory = getCachedCategory(expense.getCategory());
            if (cachedCategory != null) {
                // Use cached category from Firestore
                iconRes = cachedCategory.getIconResource();

                // Use colorHex if available, otherwise use color resource
                if (cachedCategory.getColorHex() != null && !cachedCategory.getColorHex().isEmpty()) {
                    try {
                        categoryColor = Color.parseColor(cachedCategory.getColorHex());
                    } catch (Exception e) {
                        categoryColor = ContextCompat.getColor(itemView.getContext(),
                                cachedCategory.getColorResource());
                    }
                } else {
                    categoryColor = ContextCompat.getColor(itemView.getContext(), cachedCategory.getColorResource());
                }
            } else {
                // Fallback to CategoryHelper for legacy support
                CategoryHelper.CategoryInfo categoryInfo = CategoryHelper.getCategoryInfo(expense.getCategory());
                iconRes = categoryInfo.iconRes;
                categoryColor = ContextCompat.getColor(itemView.getContext(), categoryInfo.colorRes);
            }

            // Set icon with category color
            ivIcon.setImageResource(iconRes);
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

            // Pin indicator visibility
            if (ivPinIndicator != null) {
                ivPinIndicator.setVisibility(expense.isPinned() ? View.VISIBLE : View.GONE);
            }

            // Handle expand indicator visibility based on expandableEnabled
            if (ivExpandIndicator != null) {
                if (expandableEnabled) {
                    ivExpandIndicator.setVisibility(View.VISIBLE);
                    ivExpandIndicator.setRotation(isExpanded ? 180f : 0f);
                } else {
                    ivExpandIndicator.setVisibility(View.GONE);
                }
            }

            // Handle expandable section
            if (expandableSection != null) {
                if (expandableEnabled) {
                    expandableSection.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

                    // Show notes - always show container with "No notes available" if empty
                    if (notesContainer != null && tvNotes != null) {
                        notesContainer.setVisibility(View.VISIBLE);
                        String notes = expense.getNotes();
                        if (notes != null && !notes.trim().isEmpty()) {
                            tvNotes.setText(notes);
                        } else {
                            tvNotes.setText("No notes available");
                        }
                    }

                    // Pin button
                    if (btnPin != null) {
                        boolean isPinned = expense.isPinned();
                        btnPin.setText(isPinned ? "Unpin" : "Pin");
                        btnPin.setIcon(ContextCompat.getDrawable(itemView.getContext(),
                                isPinned ? R.drawable.ic_pin : R.drawable.ic_pin_outline));
                        btnPin.setOnClickListener(v -> {
                            if (pinClickListener != null) {
                                pinClickListener.onPinClick(expense, position);
                            }
                        });
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
                } else {
                    expandableSection.setVisibility(View.GONE);
                }
            }

            // Click to expand/collapse (only if expandable is enabled)
            itemView.setOnClickListener(v -> {
                int adapterPosition = getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    if (expandableEnabled) {
                        if (expandedPositions.contains(adapterPosition)) {
                            expandedPositions.remove(adapterPosition);
                        } else {
                            expandedPositions.add(adapterPosition);
                        }
                        notifyItemChanged(adapterPosition);
                    }

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
