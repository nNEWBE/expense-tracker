package com.example.trackexpense.ui.expense;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackexpense.R;
import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.data.model.Category;
import com.example.trackexpense.data.remote.FirestoreService;
import com.example.trackexpense.utils.CategoryHelper;
import com.example.trackexpense.utils.NotificationHelper;
import com.example.trackexpense.utils.PreferenceManager;
import com.example.trackexpense.viewmodel.ExpenseViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class AddExpenseFragment extends Fragment {

    private static final String TAG = "AddExpenseFragment";

    private ExpenseViewModel viewModel;
    private PreferenceManager preferenceManager;
    private NotificationHelper notificationHelper;
    private Calendar selectedDate = Calendar.getInstance();
    private String selectedType = "EXPENSE";
    private String selectedCategory = null;

    private EditText etAmount, etNotes;
    private TextView tvDate, tvCurrencySymbol;
    private MaterialButton btnSave;
    private MaterialCardView cardIncome, cardExpense;
    private RecyclerView rvCategories;
    private CategoryAdapter categoryAdapter;
    private View btnBack;
    private LinearLayout datePickerRow;
    private TextView chip100, chip500, chip1000, chip5000;
    private FrameLayout btnClearAmount;

    // Views for type toggle backgrounds
    private LinearLayout incomeToggleBg, expenseToggleBg;

    // Views for animation
    private FrameLayout headerLayout;
    private MaterialCardView amountCard;
    private View formSection;

    // Skeleton Loading
    private View skeletonView;
    private boolean isFirstLoad = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_expense, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);
        preferenceManager = new PreferenceManager(requireContext());
        notificationHelper = new NotificationHelper(requireContext());

        initViews(view);
        setupTypeToggle();
        setupDatePicker();
        setupQuickAmountChips();
        setupClearButton();
        setupSaveButton();

        // Initial check for cached categories to decide on skeleton
        List<Category> cachedCategories = loadCachedCategories();
        if (cachedCategories.isEmpty()) {
            isFirstLoad = true;
            showSkeletonLoading(view);
        } else {
            isFirstLoad = false;
        }

        setupCategoryGrid();

        // Pre-fetch and cache both expense and income categories
        prefetchAllCategories();
    }

    /**
     * Pre-fetch and cache both expense and income categories from Firestore.
     * This ensures guest users have access to all categories.
     */
    private void prefetchAllCategories() {
        FirestoreService firestoreService = FirestoreService.getInstance();

        // Fetch and cache expense categories
        firestoreService.fetchCategoriesOnce("EXPENSE", new FirestoreService.OnCategoriesLoadedListener() {
            @Override
            public void onSuccess(List<Category> categories) {
                if (!isAdded() || categories.isEmpty())
                    return;
                cacheCategoriesForType(categories, "EXPENSE");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to prefetch expense categories", e);
            }
        });

        // Fetch and cache income categories
        firestoreService.fetchCategoriesOnce("INCOME", new FirestoreService.OnCategoriesLoadedListener() {
            @Override
            public void onSuccess(List<Category> categories) {
                if (!isAdded() || categories.isEmpty())
                    return;
                cacheCategoriesForType(categories, "INCOME");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to prefetch income categories", e);
            }
        });
    }

    /**
     * Cache categories for a specific type.
     */
    private void cacheCategoriesForType(List<Category> categories, String type) {
        if (categories == null || categories.isEmpty())
            return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            sb.append(cat.getName())
                    .append("|")
                    .append(cat.getIconName() != null ? cat.getIconName() : "ic_other")
                    .append("|")
                    .append(cat.getColorHex() != null ? cat.getColorHex() : "#64748B");
            if (i < categories.size() - 1) {
                sb.append(";");
            }
        }

        String data = sb.toString();
        if ("EXPENSE".equals(type)) {
            preferenceManager.cacheExpenseCategories(data);
            Log.d(TAG, "Prefetched and cached " + categories.size() + " expense categories");
        } else {
            preferenceManager.cacheIncomeCategories(data);
            Log.d(TAG, "Prefetched and cached " + categories.size() + " income categories");
        }
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        etAmount = view.findViewById(R.id.etAmount);
        etNotes = view.findViewById(R.id.etNotes);
        tvDate = view.findViewById(R.id.tvDate);
        tvCurrencySymbol = view.findViewById(R.id.tvCurrencySymbol);
        btnSave = view.findViewById(R.id.btnSave);
        cardIncome = view.findViewById(R.id.cardIncome);
        cardExpense = view.findViewById(R.id.cardExpense);
        rvCategories = view.findViewById(R.id.rvCategories);
        datePickerRow = view.findViewById(R.id.datePickerRow);
        btnClearAmount = view.findViewById(R.id.btnClearAmount);

        // Get the inner LinearLayouts for background changes
        if (cardIncome != null) {
            incomeToggleBg = (LinearLayout) cardIncome.getChildAt(0);
        }
        if (cardExpense != null) {
            expenseToggleBg = (LinearLayout) cardExpense.getChildAt(0);
        }

        // Animation views
        headerLayout = view.findViewById(R.id.headerLayout);
        amountCard = view.findViewById(R.id.amountCard);
        formSection = view.findViewById(R.id.formSection);

        // Quick amount chips
        chip100 = view.findViewById(R.id.chip100);
        chip500 = view.findViewById(R.id.chip500);
        chip1000 = view.findViewById(R.id.chip1000);
        chip5000 = view.findViewById(R.id.chip5000);

        // Set currency symbol
        if (tvCurrencySymbol != null) {
            tvCurrencySymbol.setText(preferenceManager.getCurrencySymbol());
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                try {
                    NavHostFragment.findNavController(this).popBackStack();
                } catch (Exception e) {
                    // Ignore navigation errors
                }
            });
        }

        // Set initial date display
        updateDateDisplay();
    }

    private void setupClearButton() {
        if (btnClearAmount == null || etAmount == null)
            return;

        // Add text change listener to show/hide clear button
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Show clear button if there's text, hide if empty
                if (s != null && s.length() > 0) {
                    btnClearAmount.setVisibility(View.VISIBLE);
                } else {
                    btnClearAmount.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Clear button click listener
        btnClearAmount.setOnClickListener(v -> {
            if (etAmount != null) {
                etAmount.setText("");
                etAmount.requestFocus();

                // Show a subtle animation on clear
                // Animation removed

            }
        });
    }

    private void setupQuickAmountChips() {
        if (chip100 != null) {
            chip100.setOnClickListener(v -> addToAmount(100));
        }
        if (chip500 != null) {
            chip500.setOnClickListener(v -> addToAmount(500));
        }
        if (chip1000 != null) {
            chip1000.setOnClickListener(v -> addToAmount(1000));
        }
        if (chip5000 != null) {
            chip5000.setOnClickListener(v -> addToAmount(5000));
        }
    }

    private void addToAmount(int value) {
        if (etAmount == null)
            return;

        String currentText = etAmount.getText().toString();
        double currentAmount = 0;

        if (!currentText.isEmpty()) {
            try {
                currentAmount = Double.parseDouble(currentText);
            } catch (NumberFormatException e) {
                currentAmount = 0;
            }
        }

        double newAmount = currentAmount + value;
        etAmount.setText(String.valueOf((int) newAmount));

        // Move cursor to end
        etAmount.setSelection(etAmount.getText().length());
    }

    private void setupTypeToggle() {
        updateTypeSelection();

        if (cardIncome != null) {
            cardIncome.setOnClickListener(v -> {
                selectedType = "INCOME";
                updateTypeSelection();
                updateCategoriesForType();
            });
        }

        if (cardExpense != null) {
            cardExpense.setOnClickListener(v -> {
                selectedType = "EXPENSE";
                updateTypeSelection();
                updateCategoriesForType();
            });
        }
    }

    private void updateTypeSelection() {
        if (incomeToggleBg == null || expenseToggleBg == null)
            return;

        try {
            if ("INCOME".equals(selectedType)) {
                // Income selected
                incomeToggleBg.setBackgroundResource(R.drawable.bg_type_toggle_selected);
                expenseToggleBg.setBackgroundResource(R.drawable.bg_type_toggle_unselected);
            } else {
                // Expense selected
                expenseToggleBg.setBackgroundResource(R.drawable.bg_type_toggle_selected);
                incomeToggleBg.setBackgroundResource(R.drawable.bg_type_toggle_unselected);
            }
        } catch (Exception e) {
            // Ignore styling errors
        }
    }

    private void setupCategoryGrid() {
        if (rvCategories != null) {
            rvCategories.setLayoutManager(new GridLayoutManager(requireContext(), 4));
            updateCategoriesForType();
        }
    }

    private void updateCategoriesForType() {
        if (rvCategories == null)
            return;

        // Reset selected category when type changes
        selectedCategory = null;

        // Initialize adapter with empty list first
        if (categoryAdapter == null) {
            categoryAdapter = new CategoryAdapter(new ArrayList<>());
            categoryAdapter.setOnCategorySelectedListener(category -> selectedCategory = category);
            rvCategories.setAdapter(categoryAdapter);
        }

        // Try to load cached categories first for instant display
        List<Category> cachedCategories = loadCachedCategories();
        if (!cachedCategories.isEmpty()) {
            Log.d(TAG, "Using cached categories for instant display");
            categoryAdapter.setCategories(cachedCategories);
        }

        // Fetch categories from Firestore (categories are stored globally)
        FirestoreService firestoreService = FirestoreService.getInstance();

        Log.d(TAG, "Fetching " + selectedType + " categories from Firestore");

        firestoreService.fetchCategoriesOnce(selectedType, new FirestoreService.OnCategoriesLoadedListener() {
            @Override
            public void onSuccess(List<Category> categories) {
                if (!isAdded())
                    return;

                Log.d(TAG, "Loaded " + categories.size() + " categories from Firestore");

                Runnable updateAction = () -> {
                    if (categories.isEmpty()) {
                        // No categories found in Firestore, use defaults or cached
                        Log.d(TAG, "No categories found in Firestore");
                        if (cachedCategories.isEmpty()) {
                            requireActivity().runOnUiThread(() -> loadDefaultCategories());
                        }
                    } else {
                        // Cache the categories for guest users
                        cacheCategories(categories);

                        requireActivity().runOnUiThread(() -> {
                            if (categoryAdapter != null) {
                                categoryAdapter.setCategories(categories);
                            }
                        });
                    }
                };

                if (isFirstLoad && skeletonView != null) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> hideSkeletonLoading(updateAction), 500);
                } else {
                    updateAction.run();
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (!isAdded())
                    return;

                Log.e(TAG, "Error loading categories from Firestore: " + e.getMessage());

                Runnable errorAction = () -> {
                    // If we already have cached categories displayed, don't overwrite
                    if (cachedCategories.isEmpty()) {
                        requireActivity().runOnUiThread(() -> loadDefaultCategories());
                    }
                };

                if (isFirstLoad && skeletonView != null) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> hideSkeletonLoading(errorAction), 500);
                } else {
                    errorAction.run();
                }
            }
        });
    }

    /**
     * Cache categories to SharedPreferences for guest users.
     */
    private void cacheCategories(List<Category> categories) {
        if (categories == null || categories.isEmpty())
            return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < categories.size(); i++) {
            Category cat = categories.get(i);
            sb.append(cat.getName())
                    .append("|")
                    .append(cat.getIconName() != null ? cat.getIconName() : "ic_other")
                    .append("|")
                    .append(cat.getColorHex() != null ? cat.getColorHex() : "#64748B");
            if (i < categories.size() - 1) {
                sb.append(";");
            }
        }

        String data = sb.toString();
        if ("EXPENSE".equals(selectedType)) {
            preferenceManager.cacheExpenseCategories(data);
            Log.d(TAG, "Cached " + categories.size() + " expense categories");
        } else {
            preferenceManager.cacheIncomeCategories(data);
            Log.d(TAG, "Cached " + categories.size() + " income categories");
        }
    }

    /**
     * Load categories from cache.
     */
    private List<Category> loadCachedCategories() {
        String cachedData = "INCOME".equals(selectedType)
                ? preferenceManager.getCachedIncomeCategories()
                : preferenceManager.getCachedExpenseCategories();

        List<Category> categories = new ArrayList<>();

        if (cachedData == null || cachedData.isEmpty()) {
            return categories;
        }

        String[] items = cachedData.split(";");
        for (int i = 0; i < items.length; i++) {
            String[] parts = items[i].split("\\|");
            if (parts.length >= 3) {
                Category cat = new Category(parts[0], selectedType, parts[1], parts[2], i, true);
                categories.add(cat);
            }
        }

        Log.d(TAG, "Loaded " + categories.size() + " cached " + selectedType + " categories");
        return categories;
    }

    /**
     * Load default categories as fallback when Firestore is unavailable.
     * These categories match the structure stored in Firestore so users
     * see consistent categories whether logged in or not.
     */
    private void loadDefaultCategories() {
        List<Category> defaultCategories = "INCOME".equals(selectedType)
                ? getDefaultIncomeCategories()
                : getDefaultExpenseCategories();

        if (categoryAdapter != null) {
            categoryAdapter.setCategories(defaultCategories);
        }
    }

    /**
     * Get default expense categories matching Firestore structure.
     */
    private List<Category> getDefaultExpenseCategories() {
        List<Category> categories = new ArrayList<>();
        // Format: {name, iconName, colorHex}
        String[][] defaults = {
                { "Food", "ic_food", "#F97316" },
                { "Transport", "ic_transport", "#3B82F6" },
                { "Shopping", "ic_shopping", "#EC4899" },
                { "Entertainment", "ic_entertainment", "#8B5CF6" },
                { "Health", "ic_health", "#10B981" },
                { "Bills", "ic_bills", "#EAB308" },
                { "Education", "ic_education", "#06B6D4" },
                { "Travel", "ic_travel", "#F43F5E" },
                { "Groceries", "ic_groceries", "#22C55E" },
                { "Subscription", "ic_subscription", "#A855F7" },
                { "Rent", "ic_bills", "#64748B" },
                { "Insurance", "ic_bills", "#64748B" },
                { "Utilities", "ic_bills", "#EAB308" },
                { "Other", "ic_other", "#64748B" }
        };

        for (int i = 0; i < defaults.length; i++) {
            categories.add(new Category(defaults[i][0], "EXPENSE", defaults[i][1], defaults[i][2], i, true));
        }
        return categories;
    }

    /**
     * Get default income categories matching Firestore structure.
     */
    private List<Category> getDefaultIncomeCategories() {
        List<Category> categories = new ArrayList<>();
        // Format: {name, iconName, colorHex}
        String[][] defaults = {
                { "Salary", "ic_salary", "#14B8A6" },
                { "Freelance", "ic_freelance", "#F59E0B" },
                { "Investment", "ic_investment", "#3B82F6" },
                { "Gift", "ic_gift", "#DB2777" },
                { "Bonus", "ic_salary", "#14B8A6" },
                { "Refund", "ic_investment", "#6366F1" },
                { "Rental Income", "ic_bills", "#64748B" },
                { "Other", "ic_other", "#64748B" }
        };

        for (int i = 0; i < defaults.length; i++) {
            categories.add(new Category(defaults[i][0], "INCOME", defaults[i][1], defaults[i][2], i, true));
        }
        return categories;
    }

    private void setupDatePicker() {
        updateDateDisplay();

        // Handle click on date row
        if (datePickerRow != null) {
            datePickerRow.setOnClickListener(v -> showMaterialDatePicker());
        }
    }

    private void showSkeletonLoading(View view) {
        if (skeletonView != null)
            return;

        // Inflate skeleton layout
        LayoutInflater inflater = LayoutInflater.from(view.getContext());
        skeletonView = inflater.inflate(R.layout.skeleton_add_expense, (ViewGroup) view, false);

        // Set elevation to ensure it's on top
        skeletonView.setElevation(100f);
        skeletonView.setClickable(true);
        skeletonView.setFocusable(true);

        // Add to root view (FrameLayout)
        if (view instanceof android.widget.FrameLayout) {
            ((android.widget.FrameLayout) view).addView(skeletonView);
        }
    }

    private void hideSkeletonLoading(Runnable onAnimationEndAction) {
        if (skeletonView == null) {
            if (onAnimationEndAction != null)
                onAnimationEndAction.run();
            isFirstLoad = false; // Ensure flag is reset even if skeleton wasn't shown
            return;
        }

        skeletonView.animate()
                .alpha(0f)
                .setDuration(400)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {
                    if (skeletonView != null && skeletonView.getParent() instanceof ViewGroup) {
                        ((ViewGroup) skeletonView.getParent()).removeView(skeletonView);
                    }
                    skeletonView = null;
                    if (onAnimationEndAction != null) {
                        onAnimationEndAction.run();
                    }
                    isFirstLoad = false; // Reset flag after animation
                })
                .start();
    }

    private void showMaterialDatePicker() {
        try {
            // Build Material Date Picker
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date")
                    .setSelection(selectedDate.getTimeInMillis())
                    .setTheme(R.style.ThemeOverlay_App_DatePicker)
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                // Convert UTC to local time zone
                Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                utc.setTimeInMillis(selection);

                selectedDate.set(Calendar.YEAR, utc.get(Calendar.YEAR));
                selectedDate.set(Calendar.MONTH, utc.get(Calendar.MONTH));
                selectedDate.set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH));
                updateDateDisplay();
            });

            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        } catch (Exception e) {
            // Fallback to standard date picker if Material fails
            showFallbackDatePicker();
        }
    }

    private void showFallbackDatePicker() {
        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateDisplay();
                }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void updateDateDisplay() {
        if (tvDate != null) {
            // Check if it's today
            Calendar today = Calendar.getInstance();
            if (selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    selectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                tvDate.setText("Today");
            } else {
                tvDate.setText(DateFormat.format("MMM dd, yyyy", selectedDate));
            }
        }
    }

    private void setupSaveButton() {
        if (btnSave == null)
            return;

        btnSave.setOnClickListener(v -> {
            try {
                String amountStr = etAmount != null ? etAmount.getText().toString() : "";
                if (amountStr.isEmpty()) {
                    Snackbar.make(v, "Please enter an amount", Snackbar.LENGTH_SHORT).show();
                    if (etAmount != null)
                        etAmount.requestFocus();
                    return;
                }

                if (selectedCategory == null) {
                    Snackbar.make(v, "Please select a category", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                double amount;
                try {
                    amount = Double.parseDouble(amountStr);
                } catch (NumberFormatException e) {
                    Snackbar.make(v, "Invalid amount", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                String notes = etNotes != null && etNotes.getText() != null
                        ? etNotes.getText().toString()
                        : "";

                Expense expense = new Expense(amount, selectedCategory, selectedDate.getTimeInMillis(), notes,
                        selectedType);
                viewModel.insert(expense);

                // Show notification (wrapped in try-catch)
                try {
                    String symbol = preferenceManager.getCurrencySymbol();
                    notificationHelper.showTransactionAddedNotification(selectedType, amount, selectedCategory, symbol);
                } catch (Exception e) {
                    // Ignore notification errors
                }

                Snackbar.make(v, "Transaction saved!", Snackbar.LENGTH_SHORT).show();

                // Navigate back
                try {
                    NavHostFragment.findNavController(this).popBackStack();
                } catch (Exception e) {
                    // Fragment might be detached
                }

            } catch (Exception e) {
                Snackbar.make(v, "Error saving transaction", Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}
