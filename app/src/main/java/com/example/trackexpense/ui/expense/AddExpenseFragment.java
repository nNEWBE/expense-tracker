package com.example.trackexpense.ui.expense;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackexpense.R;
import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.utils.CategoryHelper;
import com.example.trackexpense.utils.NotificationHelper;
import com.example.trackexpense.utils.PreferenceManager;
import com.example.trackexpense.viewmodel.ExpenseViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;
import java.util.TimeZone;

public class AddExpenseFragment extends Fragment {

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
    private View incomeIndicator, expenseIndicator;
    private RecyclerView rvCategories;
    private CategoryAdapter categoryAdapter;
    private ImageView btnBack;
    private LinearLayout datePickerRow;
    private TextView chip100, chip500, chip1000, chip5000;

    // Views for animation
    private FrameLayout headerLayout;
    private MaterialCardView amountCard;
    private View formSection;

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
        setupCategoryGrid();
        setupDatePicker();
        setupQuickAmountChips();
        setupSaveButton();

        // Run entrance animations
        runEntranceAnimations();
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
        incomeIndicator = view.findViewById(R.id.incomeIndicator);
        expenseIndicator = view.findViewById(R.id.expenseIndicator);
        rvCategories = view.findViewById(R.id.rvCategories);
        datePickerRow = view.findViewById(R.id.datePickerRow);

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

    private void runEntranceAnimations() {
        try {
            // Load animations
            Animation slideDown = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_down);
            Animation slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up);
            Animation scaleUp = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_up);

            // Header slide down
            if (headerLayout != null) {
                headerLayout.startAnimation(slideDown);
            }

            // Amount card slide up with delay
            if (amountCard != null) {
                amountCard.setVisibility(View.INVISIBLE);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (amountCard != null && isAdded()) {
                        amountCard.setVisibility(View.VISIBLE);
                        amountCard.startAnimation(slideUp);
                    }
                }, 150);
            }

            // Form section slide up with more delay
            if (formSection != null) {
                formSection.setVisibility(View.INVISIBLE);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (formSection != null && isAdded()) {
                        formSection.setVisibility(View.VISIBLE);
                        Animation formSlide = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up);
                        formSection.startAnimation(formSlide);
                    }
                }, 250);
            }

            // Save button scale up with delay
            if (btnSave != null) {
                btnSave.setVisibility(View.INVISIBLE);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (btnSave != null && isAdded()) {
                        btnSave.setVisibility(View.VISIBLE);
                        btnSave.startAnimation(scaleUp);
                    }
                }, 400);
            }
        } catch (Exception e) {
            // Make all views visible in case of animation error
            if (headerLayout != null)
                headerLayout.setVisibility(View.VISIBLE);
            if (amountCard != null)
                amountCard.setVisibility(View.VISIBLE);
            if (formSection != null)
                formSection.setVisibility(View.VISIBLE);
            if (btnSave != null)
                btnSave.setVisibility(View.VISIBLE);
        }
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
        if (cardIncome == null || cardExpense == null)
            return;

        int whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white);
        int transparentColor = android.graphics.Color.TRANSPARENT;

        if ("INCOME".equals(selectedType)) {
            // Income selected
            cardIncome.setCardBackgroundColor(0x40FFFFFF); // 25% white
            cardIncome.setStrokeWidth(4);
            cardIncome.setStrokeColor(whiteColor);

            cardExpense.setCardBackgroundColor(0x20FFFFFF); // 12% white
            cardExpense.setStrokeWidth(0);
            cardExpense.setStrokeColor(transparentColor);
        } else {
            // Expense selected
            cardExpense.setCardBackgroundColor(0x40FFFFFF); // 25% white
            cardExpense.setStrokeWidth(4);
            cardExpense.setStrokeColor(whiteColor);

            cardIncome.setCardBackgroundColor(0x20FFFFFF); // 12% white
            cardIncome.setStrokeWidth(0);
            cardIncome.setStrokeColor(transparentColor);
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

        String[] categories = "INCOME".equals(selectedType)
                ? CategoryHelper.INCOME_CATEGORIES
                : CategoryHelper.EXPENSE_CATEGORIES;

        categoryAdapter = new CategoryAdapter(categories);
        categoryAdapter.setOnCategorySelectedListener(category -> selectedCategory = category);
        rvCategories.setAdapter(categoryAdapter);
    }

    private void setupDatePicker() {
        updateDateDisplay();

        // Handle click on date row
        if (datePickerRow != null) {
            datePickerRow.setOnClickListener(v -> showMaterialDatePicker());
        }
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
