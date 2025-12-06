package com.example.trackexpense.ui.admin;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackexpense.R;
import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.data.model.User;
import com.example.trackexpense.data.remote.AdminService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminUserTransactionsDialog extends DialogFragment {

    private User user;
    private AdminService adminService;
    private RecyclerView rvTransactions;
    private TextView tvTitle;
    private TransactionAdapter adapter;

    public AdminUserTransactionsDialog(User user) {
        this.user = user;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);

        adminService = AdminService.getInstance();

        tvTitle = view.findViewById(R.id.tvEmpty);
        if (tvTitle != null) {
            tvTitle.setText("Loading transactions...");
        }

        rvTransactions = view.findViewById(R.id.rvTransactions);
        if (rvTransactions != null) {
            adapter = new TransactionAdapter();
            rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvTransactions.setAdapter(adapter);
        }

        // Hide FAB if exists
        View fab = view.findViewById(R.id.fabAdd);
        if (fab != null)
            fab.setVisibility(View.GONE);

        loadTransactions();

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(user.getEmail() + "'s Transactions")
                .setView(onCreateDialogView())
                .setPositiveButton("Close", null)
                .create();
    }

    private View onCreateDialogView() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_user_transactions, null);

        adminService = AdminService.getInstance();

        rvTransactions = view.findViewById(R.id.rvTransactions);
        adapter = new TransactionAdapter();
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setAdapter(adapter);

        loadTransactions();

        return view;
    }

    private void loadTransactions() {
        adminService.getUserTransactions(user.getId()).observe(this, expenses -> {
            adapter.setExpenses(expenses);
        });
    }

    private void deleteTransaction(Expense expense) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Transaction")
                .setMessage("Delete this transaction?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    adminService.deleteTransaction(user.getId(), expense.getFirestoreId(),
                            new AdminService.OnCompleteListener() {
                                @Override
                                public void onSuccess() {
                                    if (getView() != null) {
                                        Snackbar.make(getView(), "Deleted", Snackbar.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    if (getView() != null) {
                                        Snackbar.make(getView(), "Error: " + e.getMessage(), Snackbar.LENGTH_SHORT)
                                                .show();
                                    }
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
        private List<Expense> expenses = new ArrayList<>();

        public void setExpenses(List<Expense> expenses) {
            this.expenses = expenses;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
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
            TextView tvCategory, tvDate, tvAmount;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCategory = itemView.findViewById(R.id.tvCategory);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvAmount = itemView.findViewById(R.id.tvAmount);

                itemView.setOnLongClickListener(v -> {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        deleteTransaction(expenses.get(pos));
                    }
                    return true;
                });
            }

            public void bind(Expense expense) {
                tvCategory.setText(expense.getCategory());

                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                tvDate.setText(sdf.format(new Date(expense.getDate())));

                if ("INCOME".equals(expense.getType())) {
                    tvAmount.setText(String.format("+$%.2f", expense.getAmount()));
                    tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.income_green));
                } else {
                    tvAmount.setText(String.format("-$%.2f", expense.getAmount()));
                    tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.expense_red));
                }
            }
        }
    }
}
