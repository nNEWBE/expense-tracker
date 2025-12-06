package com.example.trackexpense.ui.expense;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.trackexpense.viewmodel.ExpenseViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionsFragment extends Fragment {

    private ExpenseViewModel viewModel;
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
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTransactions.setAdapter(adapter);
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
