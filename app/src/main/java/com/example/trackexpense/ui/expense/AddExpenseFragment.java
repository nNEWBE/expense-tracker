package com.example.trackexpense.ui.expense;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.trackexpense.R;
import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.utils.CategoryDetector;
import com.example.trackexpense.utils.NotificationHelper;
import com.example.trackexpense.utils.PreferenceManager;
import com.example.trackexpense.viewmodel.ExpenseViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

public class AddExpenseFragment extends Fragment {

    private ExpenseViewModel viewModel;
    private PreferenceManager preferenceManager;
    private NotificationHelper notificationHelper;
    private Calendar selectedDate = Calendar.getInstance();
    private String selectedType = "EXPENSE";

    private EditText etAmount;
    private AutoCompleteTextView etCategory;
    private TextInputEditText etDate, etNotes;
    private MaterialButtonToggleGroup toggleGroup;
    private MaterialButton btnSave;
    private TextView tvSuggestedCategory;
    private MaterialToolbar toolbar;

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
        setupToolbar();
        setupDatePicker();
        setupCategoryDropdown();
        setupToggle();
        setupSmartCategoryDetection();
        setupSaveButton();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        etAmount = view.findViewById(R.id.etAmount);
        etCategory = view.findViewById(R.id.etCategory);
        etDate = view.findViewById(R.id.etDate);
        etNotes = view.findViewById(R.id.etNotes);
        toggleGroup = view.findViewById(R.id.toggleButton);
        btnSave = view.findViewById(R.id.btnSave);
        tvSuggestedCategory = view.findViewById(R.id.tvSuggestedCategory);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
    }

    private void setupDatePicker() {
        updateDateDisplay();
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

    private void updateDateDisplay() {
        etDate.setText(DateFormat.format("MMM dd, yyyy", selectedDate));
    }

    private void setupCategoryDropdown() {
        String[] categories = new String[] { "Food", "Transport", "Shopping", "Entertainment",
                "Health", "Bills", "Education", "Salary", "Freelance", "Investment", "Other" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, categories);
        etCategory.setAdapter(adapter);
    }

    private void setupToggle() {
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnExpense) {
                    selectedType = "EXPENSE";
                } else if (checkedId == R.id.btnIncome) {
                    selectedType = "INCOME";
                }
            }
        });
    }

    private void setupSmartCategoryDetection() {
        etNotes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String detected = CategoryDetector.detectCategory(s.toString());
                if (!"Other".equals(detected)) {
                    tvSuggestedCategory.setText("Suggested: " + detected);
                    tvSuggestedCategory.setVisibility(View.VISIBLE);
                    tvSuggestedCategory.setOnClickListener(v -> {
                        etCategory.setText(detected, false);
                        tvSuggestedCategory.setVisibility(View.GONE);
                    });
                } else {
                    tvSuggestedCategory.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                Snackbar.make(v, "Please enter an amount", Snackbar.LENGTH_SHORT).show();
                etAmount.requestFocus();
                return;
            }

            String category = etCategory.getText().toString();
            if (category.isEmpty()) {
                Snackbar.make(v, "Please select a category", Snackbar.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            String notes = etNotes.getText().toString();

            Expense expense = new Expense(amount, category, selectedDate.getTimeInMillis(), notes, selectedType);
            viewModel.insert(expense);

            // Show notification
            String symbol = preferenceManager.getCurrencySymbol();
            notificationHelper.showTransactionAddedNotification(selectedType, amount, category, symbol);

            // Check budget warning
            checkBudgetStatus(amount);

            Snackbar.make(v, "Transaction saved successfully!", Snackbar.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).popBackStack();
        });
    }

    private void checkBudgetStatus(double newAmount) {
        if ("EXPENSE".equals(selectedType)) {
            double budget = preferenceManager.getMonthlyBudget();
            if (budget > 0) {
                // Get total expenses and check if budget exceeded
                viewModel.getTotalExpense().observe(getViewLifecycleOwner(), totalExpense -> {
                    if (totalExpense != null) {
                        double total = totalExpense + newAmount;
                        String symbol = preferenceManager.getCurrencySymbol();
                        notificationHelper.showBudgetWarningNotification(total, budget, symbol);
                    }
                });
            }
        }
    }
}
