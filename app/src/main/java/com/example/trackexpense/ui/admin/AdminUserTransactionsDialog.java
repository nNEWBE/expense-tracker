package com.example.trackexpense.ui.admin;

import android.app.Dialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackexpense.R;
import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.data.model.User;
import com.example.trackexpense.data.remote.AdminService;
import com.example.trackexpense.utils.CategoryHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminUserTransactionsDialog extends DialogFragment {

    // Listener interface for category selection
    interface OnCategorySelectedListener {
        void onSelected(String category);
    }

    private User user;
    private AdminService adminService;
    private RecyclerView rvTransactions;
    private TextView tvEmpty, tvTitle, tvTotalCount, tvIncomeCount, tvExpenseCount;
    private View emptyState;
    private ExtendedFloatingActionButton fabAdd;
    private TransactionAdapter adapter;

    public AdminUserTransactionsDialog(User user) {
        this.user = user;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_admin_user_transactions, null);

        adminService = AdminService.getInstance();

        tvTitle = view.findViewById(R.id.tvTitle);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        emptyState = view.findViewById(R.id.emptyState);
        tvTotalCount = view.findViewById(R.id.tvTotalCount);
        tvIncomeCount = view.findViewById(R.id.tvIncomeCount);
        tvExpenseCount = view.findViewById(R.id.tvExpenseCount);
        rvTransactions = view.findViewById(R.id.rvTransactions);
        fabAdd = view.findViewById(R.id.fabAdd);

        String displayName = user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                ? user.getDisplayName()
                : user.getEmail();
        // Show only first name/first part
        String firstName = displayName.split(" ")[0];
        if (firstName.contains("@")) {
            firstName = firstName.split("@")[0]; // Handle email case
        }
        tvTitle.setText(firstName + "'s Transactions");

        setupRecyclerView();
        setupFab();
        loadTransactions();

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(view)
                .create();
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter();
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setAdapter(adapter);
    }

    private void setupFab() {
        fabAdd.setOnClickListener(v -> showAddEditTransactionDialog(null));
    }

    private void loadTransactions() {
        adminService.getUserTransactions(user.getId()).observe(this, expenses -> {
            if (expenses == null || expenses.isEmpty()) {
                if (emptyState != null)
                    emptyState.setVisibility(View.VISIBLE);
                rvTransactions.setVisibility(View.GONE);
                updateCounts(0, 0, 0);
            } else {
                if (emptyState != null)
                    emptyState.setVisibility(View.GONE);
                rvTransactions.setVisibility(View.VISIBLE);
                adapter.setExpenses(expenses);

                // Calculate counts
                int incomeCount = 0;
                int expenseCount = 0;
                for (Expense expense : expenses) {
                    if ("INCOME".equals(expense.getType())) {
                        incomeCount++;
                    } else {
                        expenseCount++;
                    }
                }
                updateCounts(expenses.size(), incomeCount, expenseCount);
            }
        });
    }

    private void updateCounts(int total, int income, int expense) {
        if (tvTotalCount != null)
            tvTotalCount.setText(String.valueOf(total));
        if (tvIncomeCount != null)
            tvIncomeCount.setText(String.valueOf(income));
        if (tvExpenseCount != null)
            tvExpenseCount.setText(String.valueOf(expense));
    }

    private void showAddEditTransactionDialog(Expense existingExpense) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_admin_transaction, null);

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        MaterialButtonToggleGroup toggleType = dialogView.findViewById(R.id.toggleType);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        RecyclerView rvCategories = dialogView.findViewById(R.id.rvCategories);
        TextInputEditText etNotes = dialogView.findViewById(R.id.etNotes);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);

        // Category selection state
        final String[] selectedCategory = { null };

        // Setup category grid - use 3 columns for better visibility
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 3);
        rvCategories.setLayoutManager(gridLayoutManager);

        // Add spacing between grid items
        int spacing = (int) (8 * requireContext().getResources().getDisplayMetrics().density);
        rvCategories.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view,
                    @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.left = spacing / 2;
                outRect.right = spacing / 2;
                outRect.top = spacing / 2;
                outRect.bottom = spacing / 2;
            }
        });

        CategorySelectionAdapter categoryAdapter = new CategorySelectionAdapter(
                CategoryHelper.EXPENSE_CATEGORIES,
                category -> selectedCategory[0] = category);
        rvCategories.setAdapter(categoryAdapter);

        // Handle type toggle to switch categories
        toggleType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                String[] categories = checkedId == R.id.btnIncome
                        ? CategoryHelper.INCOME_CATEGORIES
                        : CategoryHelper.EXPENSE_CATEGORIES;
                categoryAdapter.setCategories(categories);
                selectedCategory[0] = null; // Reset selection
            }
        });

        if (existingExpense != null) {
            tvDialogTitle.setText("Edit Transaction");
            etAmount.setText(String.valueOf(existingExpense.getAmount()));
            etNotes.setText(existingExpense.getNotes());
            selectedCategory[0] = existingExpense.getCategory();

            if ("INCOME".equals(existingExpense.getType())) {
                toggleType.check(R.id.btnIncome);
                categoryAdapter.setCategories(CategoryHelper.INCOME_CATEGORIES);
            }
            categoryAdapter.setSelectedCategory(existingExpense.getCategory());
        } else {
            tvDialogTitle.setText("Add Transaction");
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        // Set dialog width after showing
        dialog.setOnShowListener(dialogInterface -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
            String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

            if (amountStr.isEmpty()) {
                etAmount.setError("Required");
                return;
            }
            if (selectedCategory[0] == null) {
                showSnackbar("Please select a category");
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                etAmount.setError("Invalid amount");
                return;
            }

            String type = toggleType.getCheckedButtonId() == R.id.btnIncome ? "INCOME" : "EXPENSE";

            if (existingExpense != null) {
                // Update existing
                existingExpense.setAmount(amount);
                existingExpense.setCategory(selectedCategory[0]);
                existingExpense.setNotes(notes);
                existingExpense.setType(type);

                adminService.updateTransaction(user.getId(), existingExpense, new AdminService.OnCompleteListener() {
                    @Override
                    public void onSuccess() {
                        dialog.dismiss();
                        showSnackbar("Transaction updated");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        showSnackbar("Error: " + e.getMessage());
                    }
                });
            } else {
                // Add new transaction
                adminService.addTransaction(user.getId(), amount, selectedCategory[0], notes, type,
                        new AdminService.OnCompleteListener() {
                            @Override
                            public void onSuccess() {
                                dialog.dismiss();
                                showSnackbar("Transaction added");
                            }

                            @Override
                            public void onFailure(Exception e) {
                                showSnackbar("Error: " + e.getMessage());
                            }
                        });
            }
        });

        dialog.show();
    }

    private void confirmDeleteTransaction(Expense expense) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Transaction")
                .setMessage("Delete this " + expense.getCategory() + " transaction?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    adminService.deleteTransaction(user.getId(), expense.getFirestoreId(),
                            new AdminService.OnCompleteListener() {
                                @Override
                                public void onSuccess() {
                                    showSnackbar("Transaction deleted");
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    showSnackbar("Error: " + e.getMessage());
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    // Category Selection Adapter
    class CategorySelectionAdapter extends RecyclerView.Adapter<CategorySelectionAdapter.ViewHolder> {
        private String[] categories;
        private String selectedCategory = null;
        private OnCategorySelectedListener listener;

        CategorySelectionAdapter(String[] categories, OnCategorySelectedListener listener) {
            this.categories = categories;
            this.listener = listener;
        }

        public void setCategories(String[] categories) {
            this.categories = categories;
            this.selectedCategory = null;
            notifyDataSetChanged();
        }

        public void setSelectedCategory(String category) {
            this.selectedCategory = category;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(categories[position]);
        }

        @Override
        public int getItemCount() {
            return categories.length;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardView;
            View iconBg;
            ImageView ivIcon;
            TextView tvName;

            ViewHolder(View itemView) {
                super(itemView);
                cardView = (MaterialCardView) itemView;
                iconBg = itemView.findViewById(R.id.iconBg);
                ivIcon = itemView.findViewById(R.id.ivIcon);
                tvName = itemView.findViewById(R.id.tvCategoryName);
            }

            void bind(String category) {
                tvName.setText(category);

                CategoryHelper.CategoryInfo info = CategoryHelper.getCategoryInfo(category);
                ivIcon.setImageResource(info.iconRes);
                ivIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), android.R.color.white));

                int categoryColor = ContextCompat.getColor(itemView.getContext(), info.colorRes);

                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(categoryColor);
                iconBg.setBackground(bg);

                // Selection state
                boolean isSelected = category.equals(selectedCategory);
                if (isSelected) {
                    cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.primary));
                    cardView.setStrokeWidth(6);
                    cardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.gray_50));
                } else {
                    cardView.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.gray_200));
                    cardView.setStrokeWidth(3);
                    cardView.setCardBackgroundColor(
                            ContextCompat.getColor(itemView.getContext(), android.R.color.white));
                }

                itemView.setOnClickListener(v -> {
                    String oldSelected = selectedCategory;
                    selectedCategory = category;

                    if (oldSelected != null) {
                        int oldPos = findCategoryPosition(oldSelected);
                        if (oldPos >= 0)
                            notifyItemChanged(oldPos);
                    }
                    notifyItemChanged(getAdapterPosition());

                    if (listener != null) {
                        listener.onSelected(category);
                    }
                });
            }

            private int findCategoryPosition(String category) {
                for (int i = 0; i < categories.length; i++) {
                    if (categories[i].equals(category))
                        return i;
                }
                return -1;
            }
        }
    }

    // Transaction Adapter
    class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
        private List<Expense> expenses = new ArrayList<>();

        public void setExpenses(List<Expense> expenses) {
            this.expenses = expenses;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_transaction, parent,
                    false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(expenses.get(position));
        }

        @Override
        public int getItemCount() {
            return expenses.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View iconBg, expandableSection;
            ImageView ivIcon, ivExpand;
            FrameLayout btnEdit, btnDelete;
            TextView tvCategory, tvDate, tvAmount;
            boolean isExpanded = false;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                iconBg = itemView.findViewById(R.id.iconBg);
                ivIcon = itemView.findViewById(R.id.ivIcon);
                ivExpand = itemView.findViewById(R.id.ivExpand);
                tvCategory = itemView.findViewById(R.id.tvCategory);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                expandableSection = itemView.findViewById(R.id.expandableSection);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }

            public void bind(Expense expense) {
                // Category - show only first word if too long
                String category = expense.getCategory();
                if (category.length() > 10) {
                    String[] parts = category.split(" ");
                    category = parts[0];
                }
                tvCategory.setText(category);

                // Date - shorter format
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                tvDate.setText(sdf.format(new Date(expense.getDate())));

                // Amount with color based on type
                boolean isIncome = "INCOME".equals(expense.getType());
                if (isIncome) {
                    tvAmount.setText(String.format("+$%,.0f", expense.getAmount()));
                    tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.income_green));
                } else {
                    tvAmount.setText(String.format("-$%,.0f", expense.getAmount()));
                    tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.expense_red));
                }

                // Set category icon and color
                CategoryHelper.CategoryInfo info = CategoryHelper.getCategoryInfo(expense.getCategory());
                ivIcon.setImageResource(info.iconRes);
                ivIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), android.R.color.white));

                int categoryColor = ContextCompat.getColor(itemView.getContext(), info.colorRes);

                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(categoryColor);
                iconBg.setBackground(bg);

                // Expand/collapse functionality
                isExpanded = false;
                if (expandableSection != null) {
                    expandableSection.setVisibility(View.GONE);
                }
                if (ivExpand != null) {
                    ivExpand.setImageResource(R.drawable.ic_expand_more);
                }

                // Card click to toggle expand
                itemView.setOnClickListener(v -> {
                    isExpanded = !isExpanded;
                    if (expandableSection != null) {
                        expandableSection.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                    }
                    if (ivExpand != null) {
                        ivExpand.setImageResource(isExpanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);
                    }
                });

                // Action button handlers
                if (btnEdit != null) {
                    btnEdit.setOnClickListener(v -> showAddEditTransactionDialog(expense));
                }
                if (btnDelete != null) {
                    btnDelete.setOnClickListener(v -> confirmDeleteTransaction(expense));
                }
            }
        }
    }
}
