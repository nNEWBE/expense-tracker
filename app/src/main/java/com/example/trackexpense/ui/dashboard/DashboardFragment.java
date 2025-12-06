package com.example.trackexpense.ui.dashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackexpense.R;
import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.utils.PreferenceManager;
import com.example.trackexpense.viewmodel.ExpenseViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private ExpenseViewModel expenseViewModel;
    private PreferenceManager preferenceManager;

    private TextView tvTotalBalance, tvTotalIncome, tvTotalExpense;
    private TextView tvBudgetStatus, tvRemainingBudget, tvTodaySpend, tvSeeAll;
    private LinearProgressIndicator progressBudget;
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
        preferenceManager = new PreferenceManager(requireContext());

        initViews(view);
        setupRecyclerView();
        setupClickListeners(view);
        observeData();
    }

    private void initViews(View view) {
        tvTotalBalance = view.findViewById(R.id.tvTotalBalance);
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome);
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
        tvBudgetStatus = view.findViewById(R.id.tvBudgetStatus);
        tvRemainingBudget = view.findViewById(R.id.tvRemainingBudget);
        tvTodaySpend = view.findViewById(R.id.tvTodaySpend);
        tvSeeAll = view.findViewById(R.id.tvSeeAll);
        progressBudget = view.findViewById(R.id.progressBudget);
        pieChart = view.findViewById(R.id.pieChart);
        lineChart = view.findViewById(R.id.lineChart);
        rvRecentTransactions = view.findViewById(R.id.rvRecentTransactions);
    }

    private void setupRecyclerView() {
        expenseAdapter = new ExpenseAdapter();
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentTransactions.setAdapter(expenseAdapter);
    }

    private void setupClickListeners(View view) {
        FloatingActionButton fab = view.findViewById(R.id.fabAddExpense);
        fab.setOnClickListener(
                v -> Navigation.findNavController(view).navigate(R.id.action_dashboardFragment_to_addExpenseFragment));

        tvSeeAll.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.transactionsFragment));
    }

    private void observeData() {
        expenseViewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses != null) {
                updateSummary(expenses);
                updateCharts(expenses);
                updateRecentTransactions(expenses);
            }
        });
    }

    private void updateSummary(List<Expense> expenses) {
        String symbol = preferenceManager.getCurrencySymbol();
        double totalIncome = 0;
        double totalExpense = 0;
        double todaySpend = 0;

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        long startOfDay = today.getTimeInMillis();

        for (Expense e : expenses) {
            if ("INCOME".equals(e.getType())) {
                totalIncome += e.getAmount();
            } else {
                totalExpense += e.getAmount();
                if (e.getDate() >= startOfDay) {
                    todaySpend += e.getAmount();
                }
            }
        }

        double balance = totalIncome - totalExpense;
        tvTotalBalance.setText(String.format("%s%.2f", symbol, balance));
        tvTotalIncome.setText(String.format("%s%.2f", symbol, totalIncome));
        tvTotalExpense.setText(String.format("%s%.2f", symbol, totalExpense));
        tvTodaySpend.setText(String.format("%s%.2f", symbol, todaySpend));

        // Budget progress
        double budget = preferenceManager.getMonthlyBudget();
        if (budget > 0) {
            double remaining = budget - totalExpense;
            int progress = (int) ((totalExpense / budget) * 100);
            progress = Math.min(progress, 100);

            tvBudgetStatus.setText(String.format("%s%.0f / %s%.0f", symbol, totalExpense, symbol, budget));
            tvRemainingBudget.setText(String.format("%s%.2f remaining", symbol, Math.max(0, remaining)));
            progressBudget.setProgress(progress);

            // Color based on usage
            if (progress > 90) {
                progressBudget.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.budget_danger));
            } else if (progress > 70) {
                progressBudget.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.budget_warning));
            } else {
                progressBudget.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.budget_safe));
            }
        } else {
            tvBudgetStatus.setText("No budget set");
            tvRemainingBudget.setText("Tap to set budget");
            progressBudget.setProgress(0);
        }
    }

    private void updateCharts(List<Expense> expenses) {
        updatePieChart(expenses);
        updateLineChart(expenses);
    }

    private void updatePieChart(List<Expense> expenses) {
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

        if (pieEntries.isEmpty()) {
            pieChart.setNoDataText("No expense data");
            pieChart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(pieEntries, "");
        dataSet.setColors(new int[] {
                ContextCompat.getColor(requireContext(), R.color.chart_1),
                ContextCompat.getColor(requireContext(), R.color.chart_2),
                ContextCompat.getColor(requireContext(), R.color.chart_3),
                ContextCompat.getColor(requireContext(), R.color.chart_4),
                ContextCompat.getColor(requireContext(), R.color.chart_5),
                ContextCompat.getColor(requireContext(), R.color.chart_6)
        });
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);
        dataSet.setSliceSpace(2f);

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(50f);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setCenterText("Categories");
        pieChart.setCenterTextSize(14f);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void updateLineChart(List<Expense> expenses) {
        // Get last 7 days data
        Calendar cal = Calendar.getInstance();
        String[] days = new String[7];
        float[] amounts = new float[7];

        for (int i = 6; i >= 0; i--) {
            Calendar dayCal = (Calendar) cal.clone();
            dayCal.add(Calendar.DAY_OF_YEAR, i - 6);
            days[6 - i] = String.format("%d/%d", dayCal.get(Calendar.MONTH) + 1, dayCal.get(Calendar.DAY_OF_MONTH));

            dayCal.set(Calendar.HOUR_OF_DAY, 0);
            dayCal.set(Calendar.MINUTE, 0);
            dayCal.set(Calendar.SECOND, 0);
            long dayStart = dayCal.getTimeInMillis();
            dayCal.add(Calendar.DAY_OF_YEAR, 1);
            long dayEnd = dayCal.getTimeInMillis();

            for (Expense e : expenses) {
                if ("EXPENSE".equals(e.getType()) && e.getDate() >= dayStart && e.getDate() < dayEnd) {
                    amounts[6 - i] += e.getAmount();
                }
            }
        }

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            entries.add(new Entry(i, amounts[i]));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Daily Spending");
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.primary));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.primary));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(requireContext(), R.color.primary));
        dataSet.setFillAlpha(50);
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.getDescription().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(days));
        lineChart.getXAxis().setGranularity(1f);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.animateX(1000);
        lineChart.invalidate();
    }

    private void updateRecentTransactions(List<Expense> expenses) {
        // Show only last 5 transactions
        int count = Math.min(expenses.size(), 5);
        List<Expense> recent = expenses.subList(0, count);
        expenseAdapter.setExpenses(recent);
    }
}
