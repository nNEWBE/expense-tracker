package com.example.trackexpense.ui.analytics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.trackexpense.R;
import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.utils.PreferenceManager;
import com.example.trackexpense.viewmodel.ExpenseViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsFragment extends Fragment {

    private ExpenseViewModel expenseViewModel;
    private PreferenceManager preferenceManager;
    private PieChart pieChart;
    private BarChart barChart;
    private LineChart lineChart;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        expenseViewModel = new ViewModelProvider(requireActivity()).get(ExpenseViewModel.class);
        preferenceManager = new PreferenceManager(requireContext());

        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);
        lineChart = view.findViewById(R.id.lineChart);

        observeData();
    }

    private void observeData() {
        expenseViewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses != null) {
                updateCharts(expenses);
            }
        });
    }

    private void updateCharts(List<Expense> expenses) {
        updatePieChart(expenses);
        updateBarChart(expenses);
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
        dataSet.setValueTextSize(11f);
        dataSet.setSliceSpace(4f);
        dataSet.setSelectionShift(8f);
        dataSet.setValueFormatter(new PercentFormatter(pieChart));

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(62f);
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);
        pieChart.setCenterText("Spending");
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.primary_dark));
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setEntryLabelTextSize(10f);
        pieChart.getLegend().setEnabled(false);
        pieChart.setDrawEntryLabels(false);
        pieChart.animateY(1200);
        pieChart.invalidate();
    }

    private void updateBarChart(List<Expense> expenses) {
        Calendar cal = Calendar.getInstance();
        String[] days = new String[7];
        float[] expenseAmounts = new float[7];

        for (int i = 6; i >= 0; i--) {
            Calendar dayCal = (Calendar) cal.clone();
            dayCal.add(Calendar.DAY_OF_YEAR, i - 6);

            String[] dayNames = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
            days[6 - i] = dayNames[dayCal.get(Calendar.DAY_OF_WEEK) - 1];

            dayCal.set(Calendar.HOUR_OF_DAY, 0);
            dayCal.set(Calendar.MINUTE, 0);
            dayCal.set(Calendar.SECOND, 0);
            long dayStart = dayCal.getTimeInMillis();
            dayCal.add(Calendar.DAY_OF_YEAR, 1);
            long dayEnd = dayCal.getTimeInMillis();

            for (Expense e : expenses) {
                if (e.getDate() >= dayStart && e.getDate() < dayEnd) {
                    if ("EXPENSE".equals(e.getType())) {
                        expenseAmounts[6 - i] += e.getAmount();
                    }
                }
            }
        }

        List<BarEntry> barEntries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            barEntries.add(new BarEntry(i, expenseAmounts[i]));
        }

        BarDataSet dataSet = new BarDataSet(barEntries, "");
        int[] barColors = new int[7];
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
        int primaryLight = ContextCompat.getColor(requireContext(), R.color.primary_light);
        for (int i = 0; i < 7; i++) {
            barColors[i] = i == 6 ? primaryColor : primaryLight;
        }
        dataSet.setColors(barColors);
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(days));
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setTextColor(Color.GRAY);
        barChart.getAxisLeft().setDrawGridLines(true);
        barChart.getAxisLeft().setGridColor(Color.parseColor("#E0E0E0"));
        barChart.getAxisLeft().setTextColor(Color.GRAY);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setFitBars(true);
        barChart.setDrawGridBackground(false);
        barChart.animateY(1200);
        barChart.invalidate();
    }

    private void updateLineChart(List<Expense> expenses) {
        Calendar cal = Calendar.getInstance();
        String[] days = new String[7];
        float[] expenseAmounts = new float[7];
        float[] incomeAmounts = new float[7];

        for (int i = 6; i >= 0; i--) {
            Calendar dayCal = (Calendar) cal.clone();
            dayCal.add(Calendar.DAY_OF_YEAR, i - 6);

            String[] dayNames = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
            days[6 - i] = dayNames[dayCal.get(Calendar.DAY_OF_WEEK) - 1];

            dayCal.set(Calendar.HOUR_OF_DAY, 0);
            dayCal.set(Calendar.MINUTE, 0);
            dayCal.set(Calendar.SECOND, 0);
            long dayStart = dayCal.getTimeInMillis();
            dayCal.add(Calendar.DAY_OF_YEAR, 1);
            long dayEnd = dayCal.getTimeInMillis();

            for (Expense e : expenses) {
                if (e.getDate() >= dayStart && e.getDate() < dayEnd) {
                    if ("EXPENSE".equals(e.getType())) {
                        expenseAmounts[6 - i] += e.getAmount();
                    } else {
                        incomeAmounts[6 - i] += e.getAmount();
                    }
                }
            }
        }

        List<Entry> expenseEntries = new ArrayList<>();
        List<Entry> incomeEntries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            expenseEntries.add(new Entry(i, expenseAmounts[i]));
            incomeEntries.add(new Entry(i, incomeAmounts[i]));
        }

        LineDataSet expenseSet = new LineDataSet(expenseEntries, "Expense");
        expenseSet.setColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        expenseSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        expenseSet.setLineWidth(2f);
        expenseSet.setCircleRadius(4f);
        expenseSet.setDrawValues(false);
        expenseSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineDataSet incomeSet = new LineDataSet(incomeEntries, "Income");
        incomeSet.setColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        incomeSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        incomeSet.setLineWidth(2f);
        incomeSet.setCircleRadius(4f);
        incomeSet.setDrawValues(false);
        incomeSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(incomeSet, expenseSet);
        lineChart.setData(lineData);

        lineChart.getDescription().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(days));
        lineChart.getXAxis().setGranularity(1f);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getXAxis().setTextColor(Color.GRAY);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.parseColor("#E0E0E0"));
        lineChart.getAxisLeft().setTextColor(Color.GRAY);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setTextColor(Color.GRAY);
        lineChart.setDrawGridBackground(false);
        lineChart.animateY(1200);
        lineChart.invalidate();
    }
}
