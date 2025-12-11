package com.example.trackexpense.ui.expense;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
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
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TransactionsFragment extends Fragment {

    private static final int PAGE_SIZE = 15; // Number of transactions per page

    private ExpenseViewModel viewModel;
    private PreferenceManager preferenceManager;
    private NotificationHelper notificationHelper;
    private RecyclerView rvTransactions;
    private ExpenseAdapter adapter;
    private EditText etSearch;
    private ChipGroup chipGroupFilter, chipGroupCategory;
    private TextView tvEmpty, tvTransactionCount;
    private LinearLayout emptyState;
    private MaterialButton btnTypeAll, btnTypeIncome, btnTypeExpense, btnLoadMore;
    private ProgressBar progressLoadMore;
    private List<Expense> allExpenses = new ArrayList<>();
    private List<Expense> filteredExpenses = new ArrayList<>();
    private String currentTypeFilter = "ALL"; // ALL, INCOME, EXPENSE
    private String currentCategoryFilter = "ALL"; // ALL or specific category
    private int currentPage = 1;

    // Category chip ID to category name mapping
    private Map<Integer, String> categoryChipMap = new HashMap<>();

    // Skeleton loading
    private View skeletonView;
    private boolean isFirstLoad = true;

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

        // Check if data is already available (cached)
        List<com.example.trackexpense.data.local.Expense> cachedData = viewModel.getAllExpenses().getValue();
        if (cachedData == null || cachedData.isEmpty()) {
            isFirstLoad = true;
            showSkeletonLoading(view);
        } else {
            isFirstLoad = false;
        }

        initCategoryMap();
        initViews(view);
        setupRecyclerView();
        setupSearch();
        setupFilters();
        setupTypeFilters();
        setupCategoryFilters();
        setupPagination();
        observeData();
    }

    /**
     * Show skeleton loading placeholder while data loads.
     */
    private void showSkeletonLoading(View rootView) {
        if (rootView instanceof ViewGroup) {
            skeletonView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.skeleton_transactions, (ViewGroup) rootView, false);
            ((ViewGroup) rootView).addView(skeletonView);
            skeletonView.setElevation(100f);
        }
    }

    /**
     * Hide skeleton loading with smooth fade animation.
     */
    private void hideSkeletonLoading(Runnable onAnimationEndAction) {
        if (skeletonView == null) {
            if (onAnimationEndAction != null)
                onAnimationEndAction.run();
            return;
        }

        isFirstLoad = false;

        skeletonView.animate()
                .alpha(0f)
                .setDuration(400)
                .setListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        if (skeletonView != null && skeletonView.getParent() != null) {
                            ((ViewGroup) skeletonView.getParent()).removeView(skeletonView);
                            skeletonView = null;
                        }
                        if (onAnimationEndAction != null) {
                            onAnimationEndAction.run();
                        }
                    }
                })
                .start();
    }

    private void initCategoryMap() {
        categoryChipMap.put(R.id.chipCatAll, "ALL");
        categoryChipMap.put(R.id.chipCatFood, "Food");
        categoryChipMap.put(R.id.chipCatTransport, "Transport");
        categoryChipMap.put(R.id.chipCatShopping, "Shopping");
        categoryChipMap.put(R.id.chipCatBills, "Bills");
        categoryChipMap.put(R.id.chipCatHealth, "Health");
        categoryChipMap.put(R.id.chipCatEntertainment, "Entertainment");
        categoryChipMap.put(R.id.chipCatSalary, "Salary");
        categoryChipMap.put(R.id.chipCatOther, "Other");
    }

    private void initViews(View view) {
        rvTransactions = view.findViewById(R.id.rvTransactions);
        etSearch = view.findViewById(R.id.etSearch);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        chipGroupCategory = view.findViewById(R.id.chipGroupCategory);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        tvTransactionCount = view.findViewById(R.id.tvTransactionCount);
        emptyState = view.findViewById(R.id.emptyState);
        btnLoadMore = view.findViewById(R.id.btnLoadMore);
        progressLoadMore = view.findViewById(R.id.progressLoadMore);

        // Type filter buttons
        btnTypeAll = view.findViewById(R.id.btnTypeAll);
        btnTypeIncome = view.findViewById(R.id.btnTypeIncome);
        btnTypeExpense = view.findViewById(R.id.btnTypeExpense);
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter();
        adapter.setCurrencySymbol(preferenceManager.getCurrencySymbol());
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setAdapter(adapter);

        // Edit click listener
        adapter.setOnEditClickListener((expense, position) -> showEditDialog(expense));

        // Delete click listener
        adapter.setOnDeleteClickListener((expense, position) -> confirmDelete(expense));
    }

    private void setupPagination() {
        if (btnLoadMore != null) {
            btnLoadMore.setOnClickListener(v -> loadMoreTransactions());
        }
    }

    private void loadMoreTransactions() {
        currentPage++;
        displayPaginatedResults();
    }

    private void setupCategoryFilters() {
        if (chipGroupCategory != null) {
            chipGroupCategory.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (!checkedIds.isEmpty()) {
                    int checkedId = checkedIds.get(0);
                    currentCategoryFilter = categoryChipMap.getOrDefault(checkedId, "ALL");
                } else {
                    currentCategoryFilter = "ALL";
                }
                currentPage = 1; // Reset pagination
                filterExpenses();
            });
        }
    }

    private void setupTypeFilters() {
        if (btnTypeAll != null) {
            btnTypeAll.setOnClickListener(v -> {
                currentTypeFilter = "ALL";
                currentPage = 1;
                updateTypeFilterUI();
                filterExpenses();
            });
        }

        if (btnTypeIncome != null) {
            btnTypeIncome.setOnClickListener(v -> {
                currentTypeFilter = "INCOME";
                currentPage = 1;
                updateTypeFilterUI();
                filterExpenses();
            });
        }

        if (btnTypeExpense != null) {
            btnTypeExpense.setOnClickListener(v -> {
                currentTypeFilter = "EXPENSE";
                currentPage = 1;
                updateTypeFilterUI();
                filterExpenses();
            });
        }

        // Initialize UI
        updateTypeFilterUI();
    }

    private void updateTypeFilterUI() {
        int primaryColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary);
        int incomeColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.income_green);
        int expenseColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.expense_red);
        int whiteColor = androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.white);

        // Reset all buttons to outlined style
        if (btnTypeAll != null) {
            if ("ALL".equals(currentTypeFilter)) {
                btnTypeAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor));
                btnTypeAll.setTextColor(whiteColor);
                btnTypeAll.setIconTint(android.content.res.ColorStateList.valueOf(whiteColor));
                btnTypeAll.setStrokeWidth(0);
            } else {
                btnTypeAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(whiteColor));
                btnTypeAll.setTextColor(primaryColor);
                btnTypeAll.setIconTint(android.content.res.ColorStateList.valueOf(primaryColor));
                btnTypeAll.setStrokeColor(android.content.res.ColorStateList.valueOf(primaryColor));
                btnTypeAll.setStrokeWidth(2);
            }
        }

        if (btnTypeIncome != null) {
            if ("INCOME".equals(currentTypeFilter)) {
                btnTypeIncome.setBackgroundTintList(android.content.res.ColorStateList.valueOf(incomeColor));
                btnTypeIncome.setTextColor(whiteColor);
                btnTypeIncome.setIconTint(android.content.res.ColorStateList.valueOf(whiteColor));
                btnTypeIncome.setStrokeWidth(0);
            } else {
                btnTypeIncome.setBackgroundTintList(android.content.res.ColorStateList.valueOf(whiteColor));
                btnTypeIncome.setTextColor(incomeColor);
                btnTypeIncome.setIconTint(android.content.res.ColorStateList.valueOf(incomeColor));
                btnTypeIncome.setStrokeColor(android.content.res.ColorStateList.valueOf(incomeColor));
                btnTypeIncome.setStrokeWidth(2);
            }
        }

        if (btnTypeExpense != null) {
            if ("EXPENSE".equals(currentTypeFilter)) {
                btnTypeExpense.setBackgroundTintList(android.content.res.ColorStateList.valueOf(expenseColor));
                btnTypeExpense.setTextColor(whiteColor);
                btnTypeExpense.setIconTint(android.content.res.ColorStateList.valueOf(whiteColor));
                btnTypeExpense.setStrokeWidth(0);
            } else {
                btnTypeExpense.setBackgroundTintList(android.content.res.ColorStateList.valueOf(whiteColor));
                btnTypeExpense.setTextColor(expenseColor);
                btnTypeExpense.setIconTint(android.content.res.ColorStateList.valueOf(expenseColor));
                btnTypeExpense.setStrokeColor(android.content.res.ColorStateList.valueOf(expenseColor));
                btnTypeExpense.setStrokeWidth(2);
            }
        }
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
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentPage = 1; // Reset pagination on search
                    filterExpenses();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
    }

    private void setupFilters() {
        if (chipGroupFilter != null) {
            chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
                currentPage = 1; // Reset pagination
                filterExpenses();
            });
        }
    }

    private void observeData() {
        viewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            allExpenses = expenses;
            currentPage = 1;

            if (isFirstLoad && skeletonView != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    hideSkeletonLoading(() -> {
                        filterExpenses();
                    });
                }, 500);
            } else {
                filterExpenses();
            }
        });
    }

    private void filterExpenses() {
        filteredExpenses = new ArrayList<>(allExpenses);

        // Type filter (Income/Expense)
        if (!"ALL".equals(currentTypeFilter)) {
            filteredExpenses = filteredExpenses.stream()
                    .filter(e -> currentTypeFilter.equals(e.getType()))
                    .collect(Collectors.toList());
        }

        // Category filter
        if (!"ALL".equals(currentCategoryFilter)) {
            filteredExpenses = filteredExpenses.stream()
                    .filter(e -> currentCategoryFilter.equalsIgnoreCase(e.getCategory()))
                    .collect(Collectors.toList());
        }

        // Text search
        String query = etSearch != null ? etSearch.getText().toString().toLowerCase() : "";
        if (!query.isEmpty()) {
            filteredExpenses = filteredExpenses.stream()
                    .filter(e -> (e.getNotes() != null && e.getNotes().toLowerCase().contains(query)) ||
                            e.getCategory().toLowerCase().contains(query) ||
                            String.valueOf(e.getAmount()).contains(query))
                    .collect(Collectors.toList());
        }

        // Date filter
        if (chipGroupFilter != null) {
            int checkedId = chipGroupFilter.getCheckedChipId();
            if (checkedId != R.id.chipAll && checkedId != View.NO_ID) {
                Calendar cal = Calendar.getInstance();

                if (checkedId == R.id.chipToday) {
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    long startOfDay = cal.getTimeInMillis();
                    filteredExpenses = filteredExpenses.stream()
                            .filter(e -> e.getDate() >= startOfDay)
                            .collect(Collectors.toList());
                } else if (checkedId == R.id.chipWeek) {
                    cal.add(Calendar.DAY_OF_YEAR, -7);
                    long weekAgo = cal.getTimeInMillis();
                    filteredExpenses = filteredExpenses.stream()
                            .filter(e -> e.getDate() >= weekAgo)
                            .collect(Collectors.toList());
                } else if (checkedId == R.id.chipMonth) {
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    long startOfMonth = cal.getTimeInMillis();
                    filteredExpenses = filteredExpenses.stream()
                            .filter(e -> e.getDate() >= startOfMonth)
                            .collect(Collectors.toList());
                }
            }
        }

        // Update transaction count
        if (tvTransactionCount != null) {
            int count = filteredExpenses.size();
            tvTransactionCount.setText(count + " transaction" + (count != 1 ? "s" : ""));
        }

        displayPaginatedResults();
    }

    private void displayPaginatedResults() {
        int totalItems = filteredExpenses.size();
        int itemsToShow = Math.min(currentPage * PAGE_SIZE, totalItems);

        List<Expense> paginatedList = filteredExpenses.subList(0, itemsToShow);

        adapter.setExpenses(new ArrayList<>(paginatedList));

        // Show/hide Load More button
        boolean hasMoreItems = itemsToShow < totalItems;
        if (btnLoadMore != null) {
            btnLoadMore.setVisibility(hasMoreItems ? View.VISIBLE : View.GONE);
            btnLoadMore.setText("Load More (" + (totalItems - itemsToShow) + " remaining)");
        }

        // Show/hide empty state
        if (emptyState != null) {
            emptyState.setVisibility(filteredExpenses.isEmpty() ? View.VISIBLE : View.GONE);
        }
        if (tvEmpty != null) {
            tvEmpty.setVisibility(View.GONE); // Using emptyState instead
        }
        rvTransactions.setVisibility(filteredExpenses.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
