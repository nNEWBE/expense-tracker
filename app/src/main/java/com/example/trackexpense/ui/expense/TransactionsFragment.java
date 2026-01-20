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
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.trackexpense.utils.CategoryHelper;
import com.example.trackexpense.data.model.Category;
import com.example.trackexpense.data.remote.FirestoreService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
import androidx.core.graphics.ColorUtils;
import androidx.core.content.ContextCompat;
import java.util.Map;
import java.util.HashMap;

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
    private Map<String, Category> categoryMap = new HashMap<>();
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

        // Use activity-scoped ViewModel for shared data caching across fragments
        viewModel = new ViewModelProvider(requireActivity()).get(ExpenseViewModel.class);
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
                .setDuration(150)
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

        // RecyclerView performance optimizations
        // Note: NOT using setHasFixedSize(true) because items can expand
        rvTransactions.setItemViewCacheSize(20);
        rvTransactions.setDrawingCacheEnabled(true);
        rvTransactions.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
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
            // Note: NOT using setHasFixedSize(true) because items can expand
            rvPinnedTransactions.setItemViewCacheSize(10);
            rvPinnedTransactions.setNestedScrollingEnabled(false);
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
        viewModel.updatePinStatus(expense);

        // Use simple toast for faster, smoother feedback
        String message = expense.isPinned() ? "📌 Transaction pinned" : "📍 Transaction unpinned";
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show();
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

        // Populate Category Map from Cache for accurate Icons/Colors
        categoryMap.clear();
        List<Category> expenses = loadCachedCategories("EXPENSE");
        List<Category> incomes = loadCachedCategories("INCOME");
        for (Category c : expenses)
            categoryMap.put(c.getName(), c);
        for (Category c : incomes)
            categoryMap.put(c.getName(), c);

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

    private int getCategoryColor(String categoryName) {
        if (categoryMap.containsKey(categoryName)) {
            return categoryMap.get(categoryName).getColorInt();
        }
        return ContextCompat.getColor(requireContext(), CategoryHelper.getCategoryInfo(categoryName).colorRes);
    }

    private int getCategoryBgColor(String categoryName) {
        if (categoryMap.containsKey(categoryName)) {
            int color = categoryMap.get(categoryName).getColorInt();
            return ColorUtils.setAlphaComponent(color, 40); // 15% opacity
        }
        return ContextCompat.getColor(requireContext(), CategoryHelper.getCategoryInfo(categoryName).bgColorRes);
    }

    private int getCategoryIcon(String categoryName) {
        if (categoryMap.containsKey(categoryName)) {
            return categoryMap.get(categoryName).getIconResource();
        }
        return CategoryHelper.getCategoryInfo(categoryName).iconRes;
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
                .inflate(R.layout.dialog_form_transaction, null);

        // Header views
        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvDialogSubtitle = dialogView.findViewById(R.id.tvDialogSubtitle);
        ImageView ivHeaderIcon = dialogView.findViewById(R.id.ivHeaderIcon);

        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        RecyclerView rvCategories = dialogView.findViewById(R.id.rvCategories);
        TextInputEditText etNotes = dialogView.findViewById(R.id.etNotes);
        MaterialButtonToggleGroup toggleGroup = dialogView.findViewById(R.id.toggleButton);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        // Set edit mode header
        tvDialogTitle.setText("Edit Transaction");
        tvDialogSubtitle.setText("Update your transaction details");
        ivHeaderIcon.setImageResource(R.drawable.ic_edit);
        btnSave.setText("Update");
        btnSave.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_check));

        // Populate fields
        // Handle integer vs double display
        if (expense.getAmount() == (long) expense.getAmount()) {
            etAmount.setText(String.format("%d", (long) expense.getAmount()));
        } else {
            etAmount.setText(String.valueOf(expense.getAmount()));
        }
        etNotes.setText(expense.getNotes());

        // Setup Category Adapter (start with empty)
        CategoryAdapter categoryAdapter = new CategoryAdapter(new String[] {});
        rvCategories.setLayoutManager(new GridLayoutManager(requireContext(), 4)); // Span 4 for compact look
        rvCategories.setAdapter(categoryAdapter);
        rvCategories.setHasFixedSize(false);
        rvCategories.setNestedScrollingEnabled(false);
        // Add spacing item decoration if needed, or rely on padding

        // State variables
        final String[] currentType = { expense.getType() };
        final String initialCategory = expense.getCategory();

        // Loading Logic
        Runnable updateCategories = () -> {
            boolean isIncome = "INCOME".equals(currentType[0]);
            String type = isIncome ? "INCOME" : "EXPENSE";

            // 1. Try to load from Cache first
            List<Category> cachedList = loadCachedCategories(type);
            if (!cachedList.isEmpty()) {
                categoryAdapter.setCategories(cachedList);
                rvCategories.requestLayout();
                if (expense.getType().equals(currentType[0])) {
                    categoryAdapter.setSelectedCategory(initialCategory);
                }
            }

            // 2. Fetch from Firestore
            FirestoreService.getInstance().fetchCategoriesOnce(type, new FirestoreService.OnCategoriesLoadedListener() {
                @Override
                public void onSuccess(List<Category> remoteCategories) {
                    if (!isAdded())
                        return;

                    if (remoteCategories != null && !remoteCategories.isEmpty()) {
                        cacheCategoriesForType(remoteCategories, type);
                        List<Category> sortedList = sortCategoriesWithOtherLast(remoteCategories);

                        rvCategories.post(() -> {
                            if (currentType[0].equals(type)) { // Check if type hasn't changed
                                categoryAdapter.setCategories(sortedList);
                                rvCategories.requestLayout();
                                if (expense.getType().equals(currentType[0])) {
                                    categoryAdapter.setSelectedCategory(initialCategory);
                                } else {
                                    categoryAdapter.setSelectedCategory(null);
                                }
                            }
                        });
                    } else if (cachedList.isEmpty()) {
                        // Fallback to legacy
                        rvCategories.post(() -> {
                            String[] defaults = isIncome ? CategoryHelper.INCOME_CATEGORIES
                                    : CategoryHelper.EXPENSE_CATEGORIES;
                            categoryAdapter.setCategories(defaults);
                            if (expense.getType().equals(currentType[0])) {
                                categoryAdapter.setSelectedCategory(initialCategory);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    if (cachedList.isEmpty() && isAdded()) {
                        rvCategories.post(() -> {
                            String[] defaults = isIncome ? CategoryHelper.INCOME_CATEGORIES
                                    : CategoryHelper.EXPENSE_CATEGORIES;
                            categoryAdapter.setCategories(defaults);
                            if (expense.getType().equals(currentType[0])) {
                                categoryAdapter.setSelectedCategory(initialCategory);
                            }
                        });
                    }
                }
            });
        };

        // Set initial toggle
        if ("INCOME".equals(expense.getType())) {
            toggleGroup.check(R.id.btnIncome);
        } else {
            toggleGroup.check(R.id.btnExpense);
        }

        // Initial Load
        updateCategories.run();

        // Type Toggle Listener
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                String newType = (checkedId == R.id.btnIncome) ? "INCOME" : "EXPENSE";
                if (!newType.equals(currentType[0])) {
                    currentType[0] = newType;
                    updateCategories.run();
                }
            }
        });

        // Create dialog with transparent background for rounded corners
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setBackground(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText() != null ? etAmount.getText().toString() : "";
            if (amountStr.isEmpty()) {
                etAmount.setError("Required");
                return;
            }

            String selectedCategory = categoryAdapter.getSelectedCategory();
            if (selectedCategory == null) {
                BeautifulNotification.showError(requireActivity(), "Please select a category");
                return;
            }

            try {
                expense.setAmount(Double.parseDouble(amountStr));
            } catch (NumberFormatException e) {
                etAmount.setError("Invalid amount");
                return;
            }

            expense.setCategory(selectedCategory);
            expense.setNotes(etNotes.getText() != null ? etNotes.getText().toString() : "");
            expense.setType(currentType[0]);

            viewModel.update(expense);
            dialog.dismiss();

            BeautifulNotification.showSuccess(requireActivity(), "Transaction updated successfully!");
        });

        dialog.show();
    }

    private void confirmDelete(Expense expense) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_delete_transaction, null);

        // Setup dialog views
        TextView tvCategory = dialogView.findViewById(R.id.tvCategory);
        TextView tvDate = dialogView.findViewById(R.id.tvDate);
        TextView tvAmount = dialogView.findViewById(R.id.tvAmount);
        ImageView ivCategoryIcon = dialogView.findViewById(R.id.ivCategoryIcon);
        View iconBg = dialogView.findViewById(R.id.iconBg);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnDelete = dialogView.findViewById(R.id.btnDelete);

        // Set transaction details
        tvCategory.setText(expense.getCategory());

        // Format date
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
        tvDate.setText(sdf.format(new java.util.Date(expense.getDate())));

        // Set amount with proper formatting
        String symbol = preferenceManager.getCurrencySymbol();
        if ("INCOME".equals(expense.getType())) {
            tvAmount.setText(String.format("+%s%,.0f", symbol, expense.getAmount()));
            tvAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        } else {
            tvAmount.setText(String.format("-%s%,.0f", symbol, expense.getAmount()));
            tvAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        }

        // Set category icon
        com.example.trackexpense.utils.CategoryHelper.CategoryInfo categoryInfo = com.example.trackexpense.utils.CategoryHelper
                .getCategoryInfo(expense.getCategory());
        ivCategoryIcon.setImageResource(categoryInfo.iconRes);
        int categoryColor = ContextCompat.getColor(requireContext(), categoryInfo.colorRes);
        ivCategoryIcon.setColorFilter(categoryColor);

        // Set icon background
        android.graphics.drawable.GradientDrawable bgShape = new android.graphics.drawable.GradientDrawable();
        bgShape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        int lowOpacityColor = androidx.core.graphics.ColorUtils.setAlphaComponent(categoryColor, 38);
        bgShape.setColor(lowOpacityColor);
        iconBg.setBackground(bgShape);

        // Create dialog
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setBackground(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                .create();

        // Button click listeners
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            viewModel.delete(expense);
            dialog.dismiss();

            notificationHelper.showTransactionDeletedNotification(
                    expense.getCategory(), symbol, expense.getAmount());

            BeautifulNotification.showSuccess(requireActivity(), "Transaction deleted successfully!");
        });

        dialog.show();
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

            // Setup category chips from actual data (deferred to avoid blocking)
            requireView().post(() -> setupCategoryChipsFromData());

            if (isFirstLoad && skeletonView != null) {
                // Hide skeleton immediately when data arrives - no artificial delay
                hideSkeletonLoading(() -> filterExpenses());
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
                cal.set(Calendar.MILLISECOND, 0);
                long startOfDay = cal.getTimeInMillis();

                Calendar endCal = (Calendar) cal.clone();
                endCal.add(Calendar.DAY_OF_YEAR, 1);
                long endOfDay = endCal.getTimeInMillis();

                filteredExpenses = filteredExpenses.stream()
                        .filter(e -> e.getDate() >= startOfDay && e.getDate() < endOfDay)
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

    private List<Category> loadCachedCategories(String type) {
        String cachedData = "INCOME".equals(type)
                ? preferenceManager.getCachedIncomeCategories()
                : preferenceManager.getCachedExpenseCategories();

        List<Category> categories = new ArrayList<>();
        if (cachedData == null || cachedData.isEmpty()) {
            return categories;
        }

        try {
            String[] items = cachedData.split(";");
            for (int i = 0; i < items.length; i++) {
                String[] parts = items[i].split("\\|");
                if (parts.length >= 3) {
                    Category cat = new Category(parts[0], type, parts[1], parts[2], i, true);
                    categories.add(cat);
                }
            }
        } catch (Exception e) {
        }
        return sortCategoriesWithOtherLast(categories);
    }

    private void cacheCategoriesForType(List<Category> categories, String type) {
        if (categories == null || categories.isEmpty())
            return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            sb.append(cat.getName()).append("|")
                    .append(cat.getIconName() != null ? cat.getIconName() : "ic_other").append("|")
                    .append(cat.getColorHex() != null ? cat.getColorHex() : "#64748B");
            if (i < categories.size() - 1)
                sb.append(";");
        }
        if ("EXPENSE".equals(type))
            preferenceManager.cacheExpenseCategories(sb.toString());
        else
            preferenceManager.cacheIncomeCategories(sb.toString());
    }

    private List<Category> sortCategoriesWithOtherLast(List<Category> categories) {
        if (categories == null)
            return new ArrayList<>();
        Collections.sort(categories, (c1, c2) -> {
            if ("Other".equalsIgnoreCase(c1.getName()))
                return 1;
            if ("Other".equalsIgnoreCase(c2.getName()))
                return -1;
            return Integer.compare(c1.getOrder(), c2.getOrder());
        });
        return categories;
    }
}
