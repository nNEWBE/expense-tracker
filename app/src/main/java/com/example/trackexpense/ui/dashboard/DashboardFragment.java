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
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private ExpenseViewModel expenseViewModel;
    private PreferenceManager preferenceManager;

    private TextView tvTotalBalance, tvTotalIncome, tvTotalExpense, tvSeeAll;
    private PieChart pieChart;
    private BarChart barChart;
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
        tvSeeAll = view.findViewById(R.id.tvSeeAll);
        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);
        rvRecentTransactions = view.findViewById(R.id.rvRecentTransactions);
    }

    private void setupRecyclerView() {
        expenseAdapter = new ExpenseAdapter();
        expenseAdapter.setCurrencySymbol(preferenceManager.getCurrencySymbol());
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

        for (Expense e : expenses) {
            if ("INCOME".equals(e.getType())) {
                totalIncome += e.getAmount();
            } else {
                totalExpense += e.getAmount();
            }
        }

        double balance = totalIncome - totalExpense;
        tvTotalBalance.setText(String.format("%s%,.2f", symbol, balance));
        tvTotalIncome.setText(String.format("%s%,.2f", symbol, totalIncome));
        tvTotalExpense.setText(String.format("%s%,.2f", symbol, totalExpense));
    }

    private void updateCharts(List<Expense> expenses) {
        updatePieChart(expenses);
        updateBarChart(expenses);
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
            pieChart.setNoDataText("No expense data yet");
            pieChart.setNoDataTextColor(Color.GRAY);
            pieChart.invalidate();
            return;
        }

        int[] colors = {
                ContextCompat.getColor(requireContext(), R.color.category_food),
                ContextCompat.getColor(requireContext(), R.color.category_transport),
                ContextCompat.getColor(requireContext(), R.color.category_shopping),
                ContextCompat.getColor(requireContext(), R.color.category_entertainment),
                ContextCompat.getColor(requireContext(), R.color.category_health),
                ContextCompat.getColor(requireContext(), R.color.category_bills),
                ContextCompat.getColor(requireContext(), R.color.category_education),
                ContextCompat.getColor(requireContext(), R.color.category_travel)
        };

        PieDataSet dataSet = new PieDataSet(pieEntries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);
        dataSet.setSliceSpace(3f);
        dataSet.setValueFormatter(new PercentFormatter(pieChart));

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(55f);
        pieChart.setTransparentCircleRadius(60f);
        pieChart.setCenterText("Expenses");
        pieChart.setCenterTextSize(16f);
        pieChart.setCenterTextColor(Color.DKGRAY);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setEntryLabelTextSize(11f);
        pieChart.getLegend().setEnabled(false);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void updateBarChart(List<Expense> expenses) {
        // Get last 7 days data
        Calendar cal = Calendar.getInstance();
        String[] days = new String[7];
        float[] incomeAmounts = new float[7];
        float[] expenseAmounts = new float[7];

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
                if (e.getDate() >= dayStart && e.getDate() < dayEnd) {
                    if ("INCOME".equals(e.getType())) {
                        incomeAmounts[6 - i] += e.getAmount();
                    } else {
                        expenseAmounts[6 - i] += e.getAmount();
                    }
                }
            }
        }

        List<BarEntry> incomeEntries = new ArrayList<>();
        List<BarEntry> expenseEntries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            incomeEntries.add(new BarEntry(i, incomeAmounts[i]));
            expenseEntries.add(new BarEntry(i, expenseAmounts[i]));
        }

        BarDataSet incomeDataSet = new BarDataSet(incomeEntries, "Income");
        incomeDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.income_green));

        BarDataSet expenseDataSet = new BarDataSet(expenseEntries, "Expenses");
        expenseDataSet.setColor(ContextCompat.getColor(requireContext(), R.color.expense_red));

        float groupSpace = 0.2f;
        float barSpace = 0.05f;
        float barWidth = 0.35f;

        BarData barData = new BarData(incomeDataSet, expenseDataSet);
        barData.setBarWidth(barWidth);

        barChart.setData(barData);
        barChart.groupBars(-0.5f, groupSpace, barSpace);
        barChart.getDescription().setEnabled(false);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(days));
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setCenterAxisLabels(true);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(true);
        barChart.setFitBars(true);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void updateRecentTransactions(List<Expense> expenses) {
        int count = Math.min(expenses.size(), 5);
        List<Expense> recent = expenses.subList(0, count);
        expenseAdapter.setExpenses(recent);
    }
}
