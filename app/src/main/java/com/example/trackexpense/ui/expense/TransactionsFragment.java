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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackexpense.R;
import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.ui.dashboard.ExpenseAdapter;
import com.example.trackexpense.utils.BeautifulNotification;
import com.example.trackexpense.utils.NotificationHelper;
import com.example.trackexpense.utils.PreferenceManager;
import com.example.trackexpense.viewmodel.ExpenseViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TransactionsFragment extends Fragment {

    private static final int PAGE_SIZE = 15; // Number of transactions per page

    private ExpenseViewModel viewModel;
    private PreferenceManager preferenceManager;
    private NotificationHelper notificationHelper;
    private RecyclerView rvTransactions, rvPinnedTransactions;
    private ExpenseAdapter adapter, pinnedAdapter;
    private EditText etSearch;
    private TextView tvEmpty, tvTransactionCount, tvPinnedCount;
    private LinearLayout emptyState, categoryChipsContainer, pinnedSection, allTransactionsHeader;
    private MaterialCardView chipAll, chipToday, chipWeek, chipMonth;
    private MaterialCardView btnTypeAll, btnTypeIncome, btnTypeExpense;
    private MaterialCardView chipCatAll;
    private MaterialButton btnLoadMore;
    private ProgressBar progressLoadMore;
    private List<Expense> allExpenses = new ArrayList<>();
    private List<Expense> filteredExpenses = new ArrayList<>();
    private List<Expense> pinnedExpenses = new ArrayList<>();
    private String currentTypeFilter = "ALL"; // ALL, INCOME, EXPENSE
    private String currentCategoryFilter = "ALL"; // ALL or specific category
    private String currentDateFilter = "ALL"; // ALL, TODAY, WEEK, MONTH
    private int currentPage = 1;

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

        initViews(view);
        setupRecyclerView();
        setupSearch();
        setupDateFilters();
        setupTypeFilters();
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

    private void initViews(View view) {
        rvTransactions = view.findViewById(R.id.rvTransactions);
        rvPinnedTransactions = view.findViewById(R.id.rvPinnedTransactions);
        etSearch = view.findViewById(R.id.etSearch);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        tvTransactionCount = view.findViewById(R.id.tvTransactionCount);
        tvPinnedCount = view.findViewById(R.id.tvPinnedCount);
        emptyState = view.findViewById(R.id.emptyState);
        btnLoadMore = view.findViewById(R.id.btnLoadMore);
        progressLoadMore = view.findViewById(R.id.progressLoadMore);
        categoryChipsContainer = view.findViewById(R.id.categoryChipsContainer);
        pinnedSection = view.findViewById(R.id.pinnedSection);
        allTransactionsHeader = view.findViewById(R.id.allTransactionsHeader);

        // Date filter chips
        chipAll = view.findViewById(R.id.chipAll);
        chipToday = view.findViewById(R.id.chipToday);
        chipWeek = view.findViewById(R.id.chipWeek);
        chipMonth = view.findViewById(R.id.chipMonth);

        // Type filter cards
        btnTypeAll = view.findViewById(R.id.btnTypeAll);
        btnTypeIncome = view.findViewById(R.id.btnTypeIncome);
        btnTypeExpense = view.findViewById(R.id.btnTypeExpense);

        // Category all chip
        chipCatAll = view.findViewById(R.id.chipCatAll);
    }

    private void setupRecyclerView() {
        // Main transactions adapter
        adapter = new ExpenseAdapter();
        adapter.setCurrencySymbol(preferenceManager.getCurrencySymbol());
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setAdapter(adapter);

        // Edit click listener
        adapter.setOnEditClickListener((expense, position) -> showEditDialog(expense));

        // Delete click listener
        adapter.setOnDeleteClickListener((expense, position) -> confirmDelete(expense));

        // Pin click listener
        adapter.setOnPinClickListener((expense, position) -> togglePin(expense));

        // Pinned transactions adapter
        pinnedAdapter = new ExpenseAdapter();
        pinnedAdapter.setCurrencySymbol(preferenceManager.getCurrencySymbol());
        if (rvPinnedTransactions != null) {
            rvPinnedTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvPinnedTransactions.setAdapter(pinnedAdapter);

            // Same listeners for pinned adapter
            pinnedAdapter.setOnEditClickListener((expense, position) -> showEditDialog(expense));
            pinnedAdapter.setOnDeleteClickListener((expense, position) -> confirmDelete(expense));
            pinnedAdapter.setOnPinClickListener((expense, position) -> togglePin(expense));
        }
    }

    private void togglePin(Expense expense) {
        expense.setPinned(!expense.isPinned());
        viewModel.update(expense);

        String message = expense.isPinned() ? "Transaction pinned" : "Transaction unpinned";
        BeautifulNotification.showSuccess(requireActivity(), message);
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

    private void setupDateFilters() {
        View.OnClickListener dateFilterListener = v -> {
            int id = v.getId();
            if (id == R.id.chipAll) {
                currentDateFilter = "ALL";
            } else if (id == R.id.chipToday) {
                currentDateFilter = "TODAY";
            } else if (id == R.id.chipWeek) {
                currentDateFilter = "WEEK";
            } else if (id == R.id.chipMonth) {
                currentDateFilter = "MONTH";
            }
            currentPage = 1;
            updateDateFilterUI();
            filterExpenses();
        };

        if (chipAll != null)
            chipAll.setOnClickListener(dateFilterListener);
        if (chipToday != null)
            chipToday.setOnClickListener(dateFilterListener);
        if (chipWeek != null)
            chipWeek.setOnClickListener(dateFilterListener);
        if (chipMonth != null)
            chipMonth.setOnClickListener(dateFilterListener);

        updateDateFilterUI();
    }

    private void updateDateFilterUI() {
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
        int blueColor = ContextCompat.getColor(requireContext(), R.color.blue_500);
        int violetColor = ContextCompat.getColor(requireContext(), R.color.violet_500);
        int amberColor = ContextCompat.getColor(requireContext(), R.color.amber_600);
        int whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white);

        // All chip
        if (chipAll != null) {
            boolean isSelected = "ALL".equals(currentDateFilter);
            chipAll.setCardBackgroundColor(isSelected ? primaryColor : whiteColor);
            chipAll.setStrokeColor(primaryColor);
            TextView tv = chipAll.findViewById(R.id.tvChipAll);
            ImageView icon = chipAll.findViewById(R.id.iconAll);
            if (tv != null)
                tv.setTextColor(isSelected ? whiteColor : primaryColor);
            if (icon != null)
                icon.setColorFilter(isSelected ? whiteColor : primaryColor);
        }

        // Today chip
        if (chipToday != null) {
            boolean isSelected = "TODAY".equals(currentDateFilter);
            chipToday.setCardBackgroundColor(
                    isSelected ? blueColor : ContextCompat.getColor(requireContext(), R.color.blue_50));
            TextView tv = chipToday.findViewById(R.id.tvChipToday);
            ImageView icon = chipToday.findViewById(R.id.iconToday);
            if (tv != null)
                tv.setTextColor(isSelected ? whiteColor : blueColor);
            if (icon != null)
                icon.setColorFilter(isSelected ? whiteColor : blueColor);
        }

        // Week chip
        if (chipWeek != null) {
            boolean isSelected = "WEEK".equals(currentDateFilter);
            chipWeek.setCardBackgroundColor(isSelected ? violetColor : 0xFFF5F3FF);
            TextView tv = chipWeek.findViewById(R.id.tvChipWeek);
            ImageView icon = chipWeek.findViewById(R.id.iconWeek);
            if (tv != null)
                tv.setTextColor(isSelected ? whiteColor : violetColor);
            if (icon != null)
                icon.setColorFilter(isSelected ? whiteColor : violetColor);
        }

        // Month chip
        if (chipMonth != null) {
            boolean isSelected = "MONTH".equals(currentDateFilter);
            chipMonth.setCardBackgroundColor(
                    isSelected ? amberColor : ContextCompat.getColor(requireContext(), R.color.amber_50));
            TextView tv = chipMonth.findViewById(R.id.tvChipMonth);
            ImageView icon = chipMonth.findViewById(R.id.iconMonth);
            if (tv != null)
                tv.setTextColor(isSelected ? whiteColor : amberColor);
            if (icon != null)
                icon.setColorFilter(isSelected ? whiteColor : amberColor);
        }
    }

    private void setupCategoryChipsFromData() {
        if (categoryChipsContainer == null)
            return;

        // Keep only the "All" chip, remove others
        int childCount = categoryChipsContainer.getChildCount();
        for (int i = childCount - 1; i > 0; i--) {
            categoryChipsContainer.removeViewAt(i);
        }

        // Get unique categories from allExpenses
        Set<String> uniqueCategories = new HashSet<>();
        for (Expense expense : allExpenses) {
            if (expense.getCategory() != null && !expense.getCategory().isEmpty()) {
                uniqueCategories.add(expense.getCategory());
            }
        }

        // Setup "All" chip click listener
        if (chipCatAll != null) {
            chipCatAll.setOnClickListener(v -> {
                currentCategoryFilter = "ALL";
                currentPage = 1;
                updateCategoryChipsUI();
                filterExpenses();
            });
        }

        // Create chips for each category
        for (String category : uniqueCategories) {
            MaterialCardView chip = createCategoryChip(category);
            categoryChipsContainer.addView(chip);
        }

        updateCategoryChipsUI();
    }

    private MaterialCardView createCategoryChip(String category) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (int) (36 * getResources().getDisplayMetrics().density));
        params.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
        card.setLayoutParams(params);
        card.setRadius((int) (18 * getResources().getDisplayMetrics().density));
        card.setCardElevation(0);
        card.setStrokeWidth((int) (1.5f * getResources().getDisplayMetrics().density));

        int categoryColor = getCategoryColor(category);
        int categoryBgColor = getCategoryBgColor(category);

        card.setCardBackgroundColor(categoryBgColor);
        card.setStrokeColor(categoryColor);

        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(android.view.Gravity.CENTER);
        int padding = (int) (14 * getResources().getDisplayMetrics().density);
        inner.setPadding(padding, 0, padding, 0);
        inner.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        ImageView icon = new ImageView(requireContext());
        int iconSize = (int) (16 * getResources().getDisplayMetrics().density);
        icon.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
        icon.setImageResource(getCategoryIcon(category));
        icon.setColorFilter(categoryColor);

        TextView tv = new TextView(requireContext());
        LinearLayout.LayoutParams tvParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        tvParams.setMarginStart((int) (6 * getResources().getDisplayMetrics().density));
        tv.setLayoutParams(tvParams);
        tv.setText(category);
        tv.setTextColor(categoryColor);
        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);

        inner.addView(icon);
        inner.addView(tv);
        card.addView(inner);

        card.setTag(category);
        card.setOnClickListener(v -> {
            currentCategoryFilter = category;
            currentPage = 1;
            updateCategoryChipsUI();
            filterExpenses();
        });

        return card;
    }

    private void updateCategoryChipsUI() {
        if (categoryChipsContainer == null)
            return;

        int whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white);
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);

        // Update "All" chip
        if (chipCatAll != null) {
            boolean isSelected = "ALL".equals(currentCategoryFilter);
            chipCatAll.setCardBackgroundColor(isSelected ? primaryColor : whiteColor);
            TextView tv = chipCatAll.findViewById(R.id.tvCatAll);
            ImageView icon = chipCatAll.findViewById(R.id.iconCatAll);
            if (tv != null)
                tv.setTextColor(isSelected ? whiteColor : primaryColor);
            if (icon != null)
                icon.setColorFilter(isSelected ? whiteColor : primaryColor);
        }

        // Update category chips
        for (int i = 1; i < categoryChipsContainer.getChildCount(); i++) {
            View child = categoryChipsContainer.getChildAt(i);
            if (child instanceof MaterialCardView && child.getTag() != null) {
                MaterialCardView card = (MaterialCardView) child;
                String category = (String) card.getTag();
                boolean isSelected = category.equals(currentCategoryFilter);

                int categoryColor = getCategoryColor(category);
                int categoryBgColor = getCategoryBgColor(category);

                card.setCardBackgroundColor(isSelected ? categoryColor : categoryBgColor);

                LinearLayout inner = (LinearLayout) card.getChildAt(0);
                if (inner != null && inner.getChildCount() >= 2) {
                    ImageView icon = (ImageView) inner.getChildAt(0);
                    TextView tv = (TextView) inner.getChildAt(1);
                    if (icon != null)
                        icon.setColorFilter(isSelected ? whiteColor : categoryColor);
                    if (tv != null)
                        tv.setTextColor(isSelected ? whiteColor : categoryColor);
                }
            }
        }
    }

    private int getCategoryColor(String category) {
        switch (category.toLowerCase()) {
            case "food":
                return ContextCompat.getColor(requireContext(), R.color.category_food);
            case "transport":
                return ContextCompat.getColor(requireContext(), R.color.category_transport);
            case "shopping":
                return ContextCompat.getColor(requireContext(), R.color.category_shopping);
            case "entertainment":
                return ContextCompat.getColor(requireContext(), R.color.category_entertainment);
            case "health":
                return ContextCompat.getColor(requireContext(), R.color.category_health);
            case "bills":
                return ContextCompat.getColor(requireContext(), R.color.category_bills);
            case "education":
                return ContextCompat.getColor(requireContext(), R.color.category_education);
            case "salary":
                return ContextCompat.getColor(requireContext(), R.color.category_salary);
            case "freelance":
                return ContextCompat.getColor(requireContext(), R.color.category_freelance);
            case "investment":
                return ContextCompat.getColor(requireContext(), R.color.category_investment);
            case "gift":
                return ContextCompat.getColor(requireContext(), R.color.category_gift);
            default:
                return ContextCompat.getColor(requireContext(), R.color.category_other);
        }
    }

    private int getCategoryBgColor(String category) {
        switch (category.toLowerCase()) {
            case "food":
                return ContextCompat.getColor(requireContext(), R.color.category_food_bg);
            case "transport":
                return ContextCompat.getColor(requireContext(), R.color.category_transport_bg);
            case "shopping":
                return ContextCompat.getColor(requireContext(), R.color.category_shopping_bg);
            case "entertainment":
                return ContextCompat.getColor(requireContext(), R.color.category_entertainment_bg);
            case "health":
                return ContextCompat.getColor(requireContext(), R.color.category_health_bg);
            case "bills":
                return ContextCompat.getColor(requireContext(), R.color.category_bills_bg);
            case "education":
                return ContextCompat.getColor(requireContext(), R.color.category_education_bg);
            case "salary":
                return ContextCompat.getColor(requireContext(), R.color.category_salary_bg);
            case "freelance":
                return ContextCompat.getColor(requireContext(), R.color.category_freelance_bg);
            case "investment":
                return ContextCompat.getColor(requireContext(), R.color.category_investment_bg);
            case "gift":
                return ContextCompat.getColor(requireContext(), R.color.category_gift_bg);
            default:
                return ContextCompat.getColor(requireContext(), R.color.category_other_bg);
        }
    }

    private int getCategoryIcon(String category) {
        switch (category.toLowerCase()) {
            case "food":
                return R.drawable.ic_food;
            case "transport":
                return R.drawable.ic_transport;
            case "shopping":
                return R.drawable.ic_shopping;
            case "entertainment":
                return R.drawable.ic_entertainment;
            case "health":
                return R.drawable.ic_health;
            case "bills":
                return R.drawable.ic_bills;
            case "education":
                return R.drawable.ic_education;
            case "salary":
                return R.drawable.ic_salary;
            case "freelance":
                return R.drawable.ic_freelance;
            case "investment":
                return R.drawable.ic_investment;
            case "gift":
                return R.drawable.ic_gift;
            default:
                return R.drawable.ic_other;
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
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
        int incomeColor = ContextCompat.getColor(requireContext(), R.color.income_green);
        int expenseColor = ContextCompat.getColor(requireContext(), R.color.expense_red);
        int incomeBgColor = ContextCompat.getColor(requireContext(), R.color.income_green_light);
        int expenseBgColor = ContextCompat.getColor(requireContext(), R.color.expense_red_light);
        int whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white);

        // All type
        if (btnTypeAll != null) {
            boolean isSelected = "ALL".equals(currentTypeFilter);
            btnTypeAll.setCardBackgroundColor(isSelected ? primaryColor : whiteColor);
            btnTypeAll.setStrokeWidth(isSelected ? 0 : (int) (1.5f * getResources().getDisplayMetrics().density));
            btnTypeAll.setStrokeColor(primaryColor);
            TextView tv = btnTypeAll.findViewById(R.id.tvTypeAll);
            ImageView icon = btnTypeAll.findViewById(R.id.iconTypeAll);
            if (tv != null)
                tv.setTextColor(isSelected ? whiteColor : primaryColor);
            if (icon != null)
                icon.setColorFilter(isSelected ? whiteColor : primaryColor);
        }

        // Income type
        if (btnTypeIncome != null) {
            boolean isSelected = "INCOME".equals(currentTypeFilter);
            btnTypeIncome.setCardBackgroundColor(isSelected ? incomeColor : incomeBgColor);
            TextView tv = btnTypeIncome.findViewById(R.id.tvTypeIncome);
            ImageView icon = btnTypeIncome.findViewById(R.id.iconTypeIncome);
            if (tv != null)
                tv.setTextColor(isSelected ? whiteColor : incomeColor);
            if (icon != null)
                icon.setColorFilter(isSelected ? whiteColor : incomeColor);
        }

        // Expense type
        if (btnTypeExpense != null) {
            boolean isSelected = "EXPENSE".equals(currentTypeFilter);
            btnTypeExpense.setCardBackgroundColor(isSelected ? expenseColor : expenseBgColor);
            TextView tv = btnTypeExpense.findViewById(R.id.tvTypeExpense);
            ImageView icon = btnTypeExpense.findViewById(R.id.iconTypeExpense);
            if (tv != null)
                tv.setTextColor(isSelected ? whiteColor : expenseColor);
            if (icon != null)
                icon.setColorFilter(isSelected ? whiteColor : expenseColor);
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

            BeautifulNotification.showSuccess(requireActivity(), "Transaction updated successfully!");
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

                    BeautifulNotification.showSuccess(requireActivity(), "Transaction deleted successfully!");
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

    private void observeData() {
        viewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            allExpenses = expenses;
            currentPage = 1;

            // Setup category chips from actual data
            setupCategoryChipsFromData();

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

        // Date filter (using new currentDateFilter)
        if (!"ALL".equals(currentDateFilter)) {
            Calendar cal = Calendar.getInstance();

            if ("TODAY".equals(currentDateFilter)) {
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                long startOfDay = cal.getTimeInMillis();
                filteredExpenses = filteredExpenses.stream()
                        .filter(e -> e.getDate() >= startOfDay)
                        .collect(Collectors.toList());
            } else if ("WEEK".equals(currentDateFilter)) {
                cal.add(Calendar.DAY_OF_YEAR, -7);
                long weekAgo = cal.getTimeInMillis();
                filteredExpenses = filteredExpenses.stream()
                        .filter(e -> e.getDate() >= weekAgo)
                        .collect(Collectors.toList());
            } else if ("MONTH".equals(currentDateFilter)) {
                cal.set(Calendar.DAY_OF_MONTH, 1);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                long startOfMonth = cal.getTimeInMillis();
                filteredExpenses = filteredExpenses.stream()
                        .filter(e -> e.getDate() >= startOfMonth)
                        .collect(Collectors.toList());
            }
        }

        // Separate pinned and unpinned transactions
        pinnedExpenses = filteredExpenses.stream()
                .filter(Expense::isPinned)
                .sorted((e1, e2) -> Long.compare(e2.getDate(), e1.getDate()))
                .collect(Collectors.toList());

        // Get unpinned transactions (these go in main list)
        List<Expense> unpinnedExpenses = filteredExpenses.stream()
                .filter(e -> !e.isPinned())
                .sorted((e1, e2) -> Long.compare(e2.getDate(), e1.getDate()))
                .collect(Collectors.toList());

        // Replace filteredExpenses with unpinned only for pagination
        filteredExpenses = unpinnedExpenses;

        // Update transaction count (total = pinned + unpinned)
        if (tvTransactionCount != null) {
            int totalCount = pinnedExpenses.size() + filteredExpenses.size();
            tvTransactionCount.setText(totalCount + " transaction" + (totalCount != 1 ? "s" : ""));
        }

        displayPaginatedResults();
    }

    private void displayPaginatedResults() {
        // Display pinned transactions (always show all)
        if (pinnedSection != null) {
            if (!pinnedExpenses.isEmpty()) {
                pinnedSection.setVisibility(View.VISIBLE);
                pinnedAdapter.setExpenses(new ArrayList<>(pinnedExpenses));
                if (tvPinnedCount != null) {
                    tvPinnedCount.setText(pinnedExpenses.size() + " pinned");
                }
                // Show "All Transactions" header when there are pinned items
                if (allTransactionsHeader != null && !filteredExpenses.isEmpty()) {
                    allTransactionsHeader.setVisibility(View.VISIBLE);
                }
            } else {
                pinnedSection.setVisibility(View.GONE);
                if (allTransactionsHeader != null) {
                    allTransactionsHeader.setVisibility(View.GONE);
                }
            }
        }

        // Display unpinned transactions with pagination
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

        // Show/hide empty state (only when both lists are empty)
        boolean isEmpty = pinnedExpenses.isEmpty() && filteredExpenses.isEmpty();
        if (emptyState != null) {
            emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (tvEmpty != null) {
            tvEmpty.setVisibility(View.GONE); // Using emptyState instead
        }
        rvTransactions.setVisibility(filteredExpenses.isEmpty() ? View.GONE : View.VISIBLE);
    }
}
