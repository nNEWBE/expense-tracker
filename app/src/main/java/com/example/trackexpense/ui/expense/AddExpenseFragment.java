package com.example.trackexpense.ui.expense;

import android.app.DatePickerDialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

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
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

public class AddExpenseFragment extends Fragment {

    private ExpenseViewModel viewModel;
    private PreferenceManager preferenceManager;
    private NotificationHelper notificationHelper;
    private Calendar selectedDate = Calendar.getInstance();
    private String selectedType = "EXPENSE";
    private String selectedCategory = null;

    private EditText etAmount;
    private TextInputEditText etDate, etNotes;
    private MaterialButton btnSave;
    private MaterialCardView cardIncome, cardExpense;
    private View incomeIndicator, expenseIndicator;
    private RecyclerView rvCategories;
    private CategoryAdapter categoryAdapter;
    private ImageView btnBack;

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
        setupSaveButton();
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        etAmount = view.findViewById(R.id.etAmount);
        etDate = view.findViewById(R.id.etDate);
        etNotes = view.findViewById(R.id.etNotes);
        btnSave = view.findViewById(R.id.btnSave);
        cardIncome = view.findViewById(R.id.cardIncome);
        cardExpense = view.findViewById(R.id.cardExpense);
        incomeIndicator = view.findViewById(R.id.incomeIndicator);
        expenseIndicator = view.findViewById(R.id.expenseIndicator);
        rvCategories = view.findViewById(R.id.rvCategories);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                try {
                    NavHostFragment.findNavController(this).popBackStack();
                } catch (Exception e) {
                    // Ignore navigation errors
                }
            });
        }

        // Set indicator colors
        if (incomeIndicator != null) {
            setIndicatorColor(incomeIndicator, R.color.income_green);
        }
        if (expenseIndicator != null) {
            setIndicatorColor(expenseIndicator, R.color.expense_red);
        }
    }

    private void setIndicatorColor(View indicator, int colorRes) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(ContextCompat.getColor(requireContext(), colorRes));
        indicator.setBackground(shape);
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

        if ("INCOME".equals(selectedType)) {
            cardIncome.setStrokeWidth(4);
            cardIncome.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.income_green));
            cardExpense.setStrokeWidth(0);
        } else {
            cardExpense.setStrokeWidth(4);
            cardExpense.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
            cardIncome.setStrokeWidth(0);
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
        if (etDate != null) {
            etDate.setOnClickListener(v -> {
                DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                        (view, year, month, dayOfMonth) -> {
                            selectedDate.set(Calendar.YEAR, year);
                            selectedDate.set(Calendar.MONTH, month);
                            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                            updateDateDisplay();
                        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH),
                        selectedDate.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show();
            });
        }
    }

    private void updateDateDisplay() {
        if (etDate != null) {
            etDate.setText(DateFormat.format("MMM dd, yyyy", selectedDate));
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
