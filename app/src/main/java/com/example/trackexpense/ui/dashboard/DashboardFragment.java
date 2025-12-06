package com.example.trackexpense.ui.dashboard;

import android.graphics.Color;
import android.os.Bundle;
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
import com.example.trackexpense.viewmodel.ExpenseViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private ExpenseViewModel expenseViewModel;
    private TextView tvTotalExpense, tvRemainingBudget, tvTodaySpend;
    private PieChart pieChart;
    private LineChart lineChart;
    private RecyclerView rvRecentTransactions;
    private ExpenseAdapter expenseAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        expenseViewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);

        tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
        tvRemainingBudget = view.findViewById(R.id.tvRemainingBudget);
        tvTodaySpend = view.findViewById(R.id.tvTodaySpend);
        pieChart = view.findViewById(R.id.pieChart);
        lineChart = view.findViewById(R.id.lineChart);
        rvRecentTransactions = view.findViewById(R.id.rvRecentTransactions);

        setupRecyclerView();

        ExtendedFloatingActionButton fab = view.findViewById(R.id.fabAddExpense);
        fab.setOnClickListener(
                v -> Navigation.findNavController(view).navigate(R.id.action_dashboardFragment_to_addExpenseFragment));

        setupObservers();
    }

    private void setupRecyclerView() {
        expenseAdapter = new ExpenseAdapter();
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentTransactions.setAdapter(expenseAdapter);
    }

    private void setupObservers() {
        expenseViewModel.getTotalExpense().observe(getViewLifecycleOwner(), total -> {
            if (total != null) {
                tvTotalExpense.setText(String.format("$%.2f", total));
                // TODO: Calculate remaining budget (Budget - Total)
            } else {
                tvTotalExpense.setText("$0.00");
            }
        });

        expenseViewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            updateCharts(expenses);
            expenseAdapter.setExpenses(expenses);
        });
    }

    private void updateCharts(List<Expense> expenses) {
        // Pie Chart - Categories
        Map<String, Float> categoryMap = new HashMap<>();
        for (Expense e : expenses) {
            if ("EXPENSE".equals(e.getType())) {
                String cat = e.getCategory();
                float current = categoryMap.getOrDefault(cat, 0f);
                categoryMap.put(cat, current + (float) e.getAmount());
            }
        }

        List<PieEntry> pieEntries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : categoryMap.entrySet()) {
            pieEntries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(pieEntries, "Categories");
        dataSet.setColors(new int[] { Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA });
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.invalidate();
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Expenses");
        pieChart.animateY(1000);
    }
}
