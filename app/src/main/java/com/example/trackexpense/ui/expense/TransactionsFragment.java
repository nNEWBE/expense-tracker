package com.example.trackexpense.ui.expense;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackexpense.R;
import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.ui.dashboard.ExpenseAdapter;
import com.example.trackexpense.utils.NotificationHelper;
import com.example.trackexpense.utils.PreferenceManager;
import com.example.trackexpense.viewmodel.ExpenseViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionsFragment extends Fragment {

    private ExpenseViewModel viewModel;
    private PreferenceManager preferenceManager;
    private NotificationHelper notificationHelper;
    private RecyclerView rvTransactions;
    private ExpenseAdapter adapter;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilter;
    private TextView tvEmpty;
    private List<Expense> allExpenses = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transactions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);
        preferenceManager = new PreferenceManager(requireContext());
        notificationHelper = new NotificationHelper(requireContext());

        initViews(view);
        setupRecyclerView();
        setupSearch();
        setupFilters();
        observeData();
    }

    private void initViews(View view) {
        rvTransactions = view.findViewById(R.id.rvTransactions);
        etSearch = view.findViewById(R.id.etSearch);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        ExtendedFloatingActionButton fabAdd = view.findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> Navigation.findNavController(view)
                .navigate(R.id.action_transactionsFragment_to_addExpenseFragment));
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter();
        adapter.setCurrencySymbol(preferenceManager.getCurrencySymbol());
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setAdapter(adapter);

        // Click to view details
        adapter.setOnItemClickListener(expense -> showExpenseDetailsDialog(expense));

        // Long click to edit/delete
        adapter.setOnItemLongClickListener((expense, position) -> showEditDeleteDialog(expense, position));
    }

    private void showExpenseDetailsDialog(Expense expense) {
        String symbol = preferenceManager.getCurrencySymbol();
        String type = expense.getType().equals("INCOME") ? "Income" : "Expense";

        String message = String.format(
                "Type: %s\nAmount: %s%.2f\nCategory: %s\nNotes: %s",
                type, symbol, expense.getAmount(), expense.getCategory(),
                expense.getNotes() != null ? expense.getNotes() : "No notes");

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(expense.getCategory())
                .setMessage(message)
                .setPositiveButton("Edit", (dialog, which) -> showEditDialog(expense))
                .setNegativeButton("Delete", (dialog, which) -> confirmDelete(expense))
                .setNeutralButton("Close", null)
                .show();
    }

    private void showEditDeleteDialog(Expense expense, int position) {
        String[] options = { "Edit", "Delete" };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Transaction Options")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showEditDialog(expense);
                    } else {
                        confirmDelete(expense);
                    }
                })
                .show();
    }

    private void showEditDialog(Expense expense) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_expense, null);

        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        AutoCompleteTextView etCategory = dialogView.findViewById(R.id.etCategory);
        TextInputEditText etNotes = dialogView.findViewById(R.id.etNotes);
        MaterialButtonToggleGroup toggleGroup = dialogView.findViewById(R.id.toggleButton);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        // Populate fields
        etAmount.setText(String.valueOf(expense.getAmount()));
        etNotes.setText(expense.getNotes());

        // Setup category dropdown
        String[] categories = { "Food", "Transport", "Shopping", "Entertainment",
                "Health", "Bills", "Education", "Salary", "Freelance", "Investment", "Other" };
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, categories);
        etCategory.setAdapter(categoryAdapter);
        etCategory.setText(expense.getCategory(), false);

        // Set type toggle
        if ("INCOME".equals(expense.getType())) {
            toggleGroup.check(R.id.btnIncome);
        } else {
            toggleGroup.check(R.id.btnExpense);
        }

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                etAmount.setError("Required");
                return;
            }

            expense.setAmount(Double.parseDouble(amountStr));
            expense.setCategory(etCategory.getText().toString());
            expense.setNotes(etNotes.getText().toString());
            expense.setType(toggleGroup.getCheckedButtonId() == R.id.btnIncome ? "INCOME" : "EXPENSE");

            viewModel.update(expense);
            dialog.dismiss();

            com.google.android.material.snackbar.Snackbar.make(
                    requireView(), "Transaction updated",
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void confirmDelete(Expense expense) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    viewModel.delete(expense);

                    String symbol = preferenceManager.getCurrencySymbol();
                    notificationHelper.showTransactionDeletedNotification(
                            expense.getCategory(), symbol, expense.getAmount());

                    com.google.android.material.snackbar.Snackbar.make(
                            requireView(), "Transaction deleted",
                            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterExpenses();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupFilters() {
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> filterExpenses());
    }

    private void observeData() {
        viewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            allExpenses = expenses;
            filterExpenses();
        });
    }

    private void filterExpenses() {
        List<Expense> filtered = new ArrayList<>(allExpenses);

        // Text search
        String query = etSearch.getText().toString().toLowerCase();
        if (!query.isEmpty()) {
            filtered = filtered.stream()
                    .filter(e -> (e.getNotes() != null && e.getNotes().toLowerCase().contains(query)) ||
                            e.getCategory().toLowerCase().contains(query) ||
                            String.valueOf(e.getAmount()).contains(query))
                    .collect(Collectors.toList());
        }

        // Date filter
        int checkedId = chipGroupFilter.getCheckedChipId();
        if (checkedId != R.id.chipAll && checkedId != View.NO_ID) {
            long now = System.currentTimeMillis();
            Calendar cal = Calendar.getInstance();

            if (checkedId == R.id.chipToday) {
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                long startOfDay = cal.getTimeInMillis();
                filtered = filtered.stream()
                        .filter(e -> e.getDate() >= startOfDay)
                        .collect(Collectors.toList());
            } else if (checkedId == R.id.chipWeek) {
                cal.add(Calendar.DAY_OF_YEAR, -7);
                long weekAgo = cal.getTimeInMillis();
                filtered = filtered.stream()
                        .filter(e -> e.getDate() >= weekAgo)
                        .collect(Collectors.toList());
            } else if (checkedId == R.id.chipMonth) {
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                long startOfMonth = cal.getTimeInMillis();
                filtered = filtered.stream()
                        .filter(e -> e.getDate() >= startOfMonth)
                        .collect(Collectors.toList());
            }
        }

        adapter.setExpenses(filtered);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
