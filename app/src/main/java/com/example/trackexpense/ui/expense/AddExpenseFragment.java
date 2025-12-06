package com.example.trackexpense.ui.expense;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.trackexpense.R;
import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.viewmodel.ExpenseViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;
import java.util.Date;

public class AddExpenseFragment extends Fragment {

    private ExpenseViewModel viewModel;
    private Calendar selectedDate = Calendar.getInstance();
    private String selectedType = "EXPENSE";

    private TextInputEditText etDate, etAmount, etNotes;
    private AutoCompleteTextView etCategory;
    private MaterialButtonToggleGroup toggleGroup;
    private MaterialButton btnSave;

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

        etAmount = view.findViewById(R.id.etAmount);
        etCategory = view.findViewById(R.id.etCategory);
        etDate = view.findViewById(R.id.etDate);
        etNotes = view.findViewById(R.id.etNotes);
        toggleGroup = view.findViewById(R.id.toggleButton);
        btnSave = view.findViewById(R.id.btnSave);

        setupDatePicker();
        setupCategoryDropdown();
        setupToggle();
        setupSaveButton();
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
        String[] categories = new String[] { "Food", "Transport", "Shopping", "Entertainment", "Health", "Bills",
                "Education", "Other" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line,
                categories);
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

    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                etAmount.setError("Amount required");
                return;
            }

            double amount = Double.parseDouble(amountStr);
            String category = etCategory.getText().toString();
            String notes = etNotes.getText().toString();

            Expense expense = new Expense(amount, category, selectedDate.getTimeInMillis(), notes, selectedType);
            viewModel.insert(expense);

            Snackbar.make(v, "Transaction Saved", Snackbar.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).popBackStack();
        });
    }
}
