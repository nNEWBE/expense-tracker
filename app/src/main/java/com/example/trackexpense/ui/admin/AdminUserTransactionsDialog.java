package com.example.trackexpense.ui.admin;

import android.app.Dialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackexpense.R;
import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.data.model.User;
import com.example.trackexpense.data.remote.AdminService;
import com.example.trackexpense.utils.CategoryHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminUserTransactionsDialog extends DialogFragment {

    private User user;
    private AdminService adminService;
    private RecyclerView rvTransactions;
    private TextView tvEmpty, tvTitle;
    private FloatingActionButton fabAdd;
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
        rvTransactions = view.findViewById(R.id.rvTransactions);
        fabAdd = view.findViewById(R.id.fabAdd);

        String displayName = user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                ? user.getDisplayName()
                : user.getEmail();
        tvTitle.setText(displayName + "'s Transactions");

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
                tvEmpty.setVisibility(View.VISIBLE);
                rvTransactions.setVisibility(View.GONE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                rvTransactions.setVisibility(View.VISIBLE);
                adapter.setExpenses(expenses);
            }
        });
    }

    private void showAddEditTransactionDialog(Expense existingExpense) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_admin_transaction, null);

        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        MaterialButtonToggleGroup toggleType = dialogView.findViewById(R.id.toggleType);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        TextInputEditText etCategory = dialogView.findViewById(R.id.etCategory);
        TextInputEditText etNotes = dialogView.findViewById(R.id.etNotes);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);

        if (existingExpense != null) {
            tvDialogTitle.setText("Edit Transaction");
            etAmount.setText(String.valueOf(existingExpense.getAmount()));
            etCategory.setText(existingExpense.getCategory());
            etNotes.setText(existingExpense.getNotes());
            if ("INCOME".equals(existingExpense.getType())) {
                toggleType.check(R.id.btnIncome);
            }
        } else {
            tvDialogTitle.setText("Add Transaction");
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
            String category = etCategory.getText() != null ? etCategory.getText().toString().trim() : "";
            String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

            if (amountStr.isEmpty()) {
                etAmount.setError("Required");
                return;
            }
            if (category.isEmpty()) {
                etCategory.setError("Required");
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
                existingExpense.setCategory(category);
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
                adminService.addTransaction(user.getId(), amount, category, notes, type,
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

    // Adapter
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
            View iconBg;
            ImageView ivIcon, btnEdit, btnDelete;
            TextView tvCategory, tvDate, tvAmount, tvNotes;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                iconBg = itemView.findViewById(R.id.iconBg);
                ivIcon = itemView.findViewById(R.id.ivIcon);
                tvCategory = itemView.findViewById(R.id.tvCategory);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                tvNotes = itemView.findViewById(R.id.tvNotes);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }

            public void bind(Expense expense) {
                tvCategory.setText(expense.getCategory());

                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                tvDate.setText(sdf.format(new Date(expense.getDate())));

                if (expense.getNotes() != null && !expense.getNotes().isEmpty()) {
                    tvNotes.setText(expense.getNotes());
                    tvNotes.setVisibility(View.VISIBLE);
                } else {
                    tvNotes.setVisibility(View.GONE);
                }

                // Set amount and color
                if ("INCOME".equals(expense.getType())) {
                    tvAmount.setText(String.format("+$%.2f", expense.getAmount()));
                    tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.income_green));
                } else {
                    tvAmount.setText(String.format("-$%.2f", expense.getAmount()));
                    tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.expense_red));
                }

                // Set category icon and color
                CategoryHelper.CategoryInfo info = CategoryHelper.getCategoryInfo(expense.getCategory());
                ivIcon.setImageResource(info.iconRes);
                ivIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), android.R.color.white));

                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(ContextCompat.getColor(itemView.getContext(), info.colorRes));
                iconBg.setBackground(bg);

                // Click handlers
                btnEdit.setOnClickListener(v -> showAddEditTransactionDialog(expense));
                btnDelete.setOnClickListener(v -> confirmDeleteTransaction(expense));
            }
        }
    }
}
