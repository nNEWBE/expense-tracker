package com.example.trackexpense.ui.analytics;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.trackexpense.MainActivity;
import com.example.trackexpense.R;
import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.utils.CategoryHelper;
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
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnalyticsFragment extends Fragment {

    private ExpenseViewModel expenseViewModel;
    private PreferenceManager preferenceManager;
    private PieChart pieChart;
    private BarChart barChart;
    private LineChart lineChart;

    // New views
    private TextView tvTotalIncome, tvTotalExpense, tvBalance;
    private TextView tvIncomePercent, tvExpensePercent;
    private LinearLayout categoryProgressContainer;
    private FrameLayout headerLayout;
    private MaterialCardView cardDonut, cardCategories, cardWeekly, cardMonthly;
    private ImageView btnMenu;

    // Skeleton loading
    private View skeletonView;
    private boolean isFirstLoad = true;

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

        // Check if data is already available (cached)
        List<com.example.trackexpense.data.local.Expense> cachedData = expenseViewModel.getAllExpenses().getValue();
        if (cachedData == null || cachedData.isEmpty()) {
            isFirstLoad = true;
            showSkeletonLoading(view);
        } else {
            isFirstLoad = false;
        }

        initViews(view);
        setupClickListeners();
        observeData();
    }

    /**
     * Show skeleton loading placeholder while data loads.
     */
    /**
     * Show skeleton loading placeholder while data loads.
     */
    private void showSkeletonLoading(View rootView) {
        if (getActivity() == null)
            return;

        if (rootView instanceof ViewGroup) {
            skeletonView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.skeleton_analytics, (ViewGroup) rootView, false);

            // Ensure skeleton overlaps everything
            skeletonView.setElevation(100f);

            ((ViewGroup) rootView).addView(skeletonView);
        }
    }

    /**
     * Hide skeleton loading with smooth fade animation.
     */
    /**
     * Hide skeleton loading with smooth fade animation.
     */
    private void hideSkeletonLoading(Runnable onAnimationEndAction) {
        if (skeletonView == null) {
            if (onAnimationEndAction != null) {
                onAnimationEndAction.run();
            }
            return;
        }

        skeletonView.animate()
                .alpha(0f)
                .setDuration(400)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .setListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        if (skeletonView != null && skeletonView.getParent() != null) {
                            ((ViewGroup) skeletonView.getParent()).removeView(skeletonView);
                            skeletonView = null;
                        }
                        if (onAnimationEndAction != null) {
                            onAnimationEndAction.run();
                        }
                    }
                })
                .start();
    }

    private void initViews(View view) {
        pieChart = view.findViewById(R.id.pieChart);
        barChart = view.findViewById(R.id.barChart);
        lineChart = view.findViewById(R.id.lineChart);

        // Header views
        headerLayout = view.findViewById(R.id.headerLayout);
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome);
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
        tvBalance = view.findViewById(R.id.tvBalance);
        btnMenu = view.findViewById(R.id.btnMenu);

        // Donut card views
        cardDonut = view.findViewById(R.id.cardDonut);
        tvIncomePercent = view.findViewById(R.id.tvIncomePercent);
        tvExpensePercent = view.findViewById(R.id.tvExpensePercent);

        // Other cards
        cardCategories = view.findViewById(R.id.cardCategories);
        cardWeekly = view.findViewById(R.id.cardWeekly);
        cardMonthly = view.findViewById(R.id.cardMonthly);
        categoryProgressContainer = view.findViewById(R.id.categoryProgressContainer);
    }

    private void setupClickListeners() {
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openDrawer();
                }
            });
        }
    }

    private void observeData() {
        expenseViewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses != null) {
                if (isFirstLoad && skeletonView != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        hideSkeletonLoading(() -> {
                            updateSummary(expenses);
                            updateCharts(expenses);
                            updateCategoryProgress(expenses);
                        });
                    }, 500);
                } else {
                    updateSummary(expenses);
                    updateCharts(expenses);
                    updateCategoryProgress(expenses);
                }
            }
        });
    }

    private void updateSummary(List<Expense> expenses) {
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
        String symbol = preferenceManager.getCurrencySymbol();

        // Animate the counter values
        animateCounter(tvTotalIncome, 0, totalIncome, symbol);
        animateCounter(tvTotalExpense, 0, totalExpense, symbol);
        animateCounter(tvBalance, 0, balance, symbol);

        // Update percentages with animation
        double total = totalIncome + totalExpense;
        if (total > 0) {
            int incomePercent = (int) ((totalIncome / total) * 100);
            int expensePercent = 100 - incomePercent;

            animatePercentage(tvIncomePercent, 0, incomePercent);
            animatePercentage(tvExpensePercent, 0, expensePercent);
        }
    }

    private void animateCounter(TextView textView, double start, double end, String symbol) {
        if (textView == null)
            return;

        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat((float) start, (float) end);
        animator.setDuration(1500);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());

        animator.addUpdateListener(animation -> {
            if (textView != null && isAdded()) {
                float value = (float) animation.getAnimatedValue();
                textView.setText(symbol + formatAmount(value));
            }
        });

        animator.start();
    }

    private void animatePercentage(TextView textView, int start, int end) {
        if (textView == null)
            return;

        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(start, end);
        animator.setDuration(1200);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());

        animator.addUpdateListener(animation -> {
            if (textView != null && isAdded()) {
                int value = (int) animation.getAnimatedValue();
                textView.setText(value + "%");
            }
        });

        animator.start();
    }

    private String formatAmount(double amount) {
        if (amount >= 1000000) {
            return String.format("%.1fM", amount / 1000000);
        } else if (amount >= 1000) {
            return String.format("%.1fK", amount / 1000);
        } else {
            return String.format("%.0f", amount);
        }
    }

    private void updateCategoryProgress(List<Expense> expenses) {
        if (categoryProgressContainer == null)
            return;
        categoryProgressContainer.removeAllViews();

        // Calculate category totals for expenses only
        Map<String, Double> categoryMap = new HashMap<>();
        double totalExpense = 0;

        for (Expense e : expenses) {
            if ("EXPENSE".equals(e.getType())) {
                String cat = e.getCategory();
                double current = categoryMap.getOrDefault(cat, 0.0);
                categoryMap.put(cat, current + e.getAmount());
                totalExpense += e.getAmount();
            }
        }

        if (categoryMap.isEmpty())
            return;

        // Sort by amount descending
        List<Map.Entry<String, Double>> sortedCategories = new ArrayList<>(categoryMap.entrySet());
        Collections.sort(sortedCategories, (a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Show top 5 categories
        int count = Math.min(5, sortedCategories.size());
        for (int i = 0; i < count; i++) {
            Map.Entry<String, Double> entry = sortedCategories.get(i);
            addCategoryProgressBar(entry.getKey(), entry.getValue(), totalExpense);
        }
    }

    private void addCategoryProgressBar(String category, double amount, double total) {
        // Main item container with card-like styling
        LinearLayout itemLayout = new LinearLayout(requireContext());
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        itemParams.bottomMargin = dpToPx(14);
        itemLayout.setLayoutParams(itemParams);

        // Top row: icon, category name, amount and percentage
        LinearLayout topRow = new LinearLayout(requireContext());
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        // Category icon with background
        FrameLayout iconContainer = new FrameLayout(requireContext());
        int iconContainerSize = dpToPx(38);
        LinearLayout.LayoutParams iconContainerParams = new LinearLayout.LayoutParams(iconContainerSize,
                iconContainerSize);
        iconContainer.setLayoutParams(iconContainerParams);

        CategoryHelper.CategoryInfo info = CategoryHelper.getCategoryInfo(category);
        int categoryColor = ContextCompat.getColor(requireContext(), info.colorRes);

        // Icon background
        View iconBg = new View(requireContext());
        iconBg.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        GradientDrawable iconBgDrawable = new GradientDrawable();
        iconBgDrawable.setShape(GradientDrawable.OVAL);
        iconBgDrawable.setColor(categoryColor);
        iconBg.setBackground(iconBgDrawable);
        iconContainer.addView(iconBg);

        // Category icon
        ImageView icon = new ImageView(requireContext());
        int iconSize = dpToPx(18);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(iconSize, iconSize);
        iconParams.gravity = Gravity.CENTER;
        icon.setLayoutParams(iconParams);
        icon.setImageResource(info.iconRes);
        icon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.white));
        iconContainer.addView(icon);

        // Text container
        LinearLayout textContainer = new LinearLayout(requireContext());
        textContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMarginStart(dpToPx(12));
        textContainer.setLayoutParams(textParams);

        // Category name
        TextView tvName = new TextView(requireContext());
        tvName.setText(category);
        tvName.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_background_light));
        tvName.setTextSize(14);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);

        // Amount
        String symbol = preferenceManager.getCurrencySymbol();
        TextView tvAmount = new TextView(requireContext());
        tvAmount.setText(symbol + formatAmount(amount));
        tvAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_500));
        tvAmount.setTextSize(12);

        textContainer.addView(tvName);
        textContainer.addView(tvAmount);

        // Percentage badge
        int percent = (int) ((amount / total) * 100);
        TextView tvPercent = new TextView(requireContext());
        tvPercent.setText(percent + "%");
        tvPercent.setTextColor(categoryColor);
        tvPercent.setTextSize(15);
        tvPercent.setTypeface(null, android.graphics.Typeface.BOLD);

        topRow.addView(iconContainer);
        topRow.addView(textContainer);
        topRow.addView(tvPercent);

        // Progress bar with animation
        ProgressBar progressBar = new ProgressBar(requireContext(), null,
                android.R.attr.progressBarStyleHorizontal);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(8));
        progressParams.topMargin = dpToPx(10);
        progressBar.setLayoutParams(progressParams);
        progressBar.setMax(100);
        progressBar.setProgress(0); // Start at 0 for animation
        progressBar.setProgressDrawable(createProgressDrawable(info.colorRes));

        itemLayout.addView(topRow);
        itemLayout.addView(progressBar);

        categoryProgressContainer.addView(itemLayout);

        // Animate progress bar
        android.animation.ObjectAnimator progressAnimator = android.animation.ObjectAnimator.ofInt(
                progressBar, "progress", 0, percent);
        progressAnimator.setDuration(1000);
        progressAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        progressAnimator.setStartDelay(categoryProgressContainer.getChildCount() * 100L); // Stagger animation
        progressAnimator.start();
    }

    private android.graphics.drawable.Drawable createProgressDrawable(int colorRes) {
        android.graphics.drawable.LayerDrawable layerDrawable = new android.graphics.drawable.LayerDrawable(
                new android.graphics.drawable.Drawable[] {
                        createRoundedDrawable(Color.parseColor("#F3F4F6")),
                        createRoundedDrawable(ContextCompat.getColor(requireContext(), colorRes))
                });
        layerDrawable.setId(0, android.R.id.background);
        layerDrawable.setId(1, android.R.id.progress);

        android.graphics.drawable.ClipDrawable clip = new android.graphics.drawable.ClipDrawable(
                createRoundedDrawable(ContextCompat.getColor(requireContext(), colorRes)),
                Gravity.START, android.graphics.drawable.ClipDrawable.HORIZONTAL);

        android.graphics.drawable.LayerDrawable result = new android.graphics.drawable.LayerDrawable(
                new android.graphics.drawable.Drawable[] {
                        createRoundedDrawable(Color.parseColor("#F3F4F6")),
                        clip
                });
        result.setId(0, android.R.id.background);
        result.setId(1, android.R.id.progress);

        return result;
    }

    private GradientDrawable createRoundedDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(dpToPx(6)); // More rounded for modern look
        drawable.setColor(color);
        return drawable;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void updateCharts(List<Expense> expenses) {
        updatePieChart(expenses);
        updateBarChart(expenses);
        updateLineChart(expenses);
    }

    private void updatePieChart(List<Expense> expenses) {
        double totalIncome = 0;
        double totalExpense = 0;

        for (Expense e : expenses) {
            if ("INCOME".equals(e.getType())) {
                totalIncome += e.getAmount();
            } else {
                totalExpense += e.getAmount();
            }
        }

        List<PieEntry> pieEntries = new ArrayList<>();
        if (totalIncome > 0) {
            pieEntries.add(new PieEntry((float) totalIncome, "Income"));
        }
        if (totalExpense > 0) {
            pieEntries.add(new PieEntry((float) totalExpense, "Expense"));
        }

        if (pieEntries.isEmpty()) {
            pieChart.setNoDataText("No data yet");
            pieChart.setNoDataTextColor(Color.GRAY);
            pieChart.invalidate();
            return;
        }

        int[] colors = {
                ContextCompat.getColor(requireContext(), R.color.income_green),
                ContextCompat.getColor(requireContext(), R.color.expense_red)
        };

        PieDataSet dataSet = new PieDataSet(pieEntries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(0f);
        dataSet.setSliceSpace(4f); // Slightly more spacing for modern look
        dataSet.setSelectionShift(8f); // Enhanced selection effect
        dataSet.setDrawValues(false);

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(68f); // Slightly adjusted for better proportion
        pieChart.setTransparentCircleRadius(73f);
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(80); // More visible ring
        pieChart.setDrawCenterText(false);
        pieChart.setEntryLabelColor(Color.TRANSPARENT);
        pieChart.getLegend().setEnabled(false);
        pieChart.setDrawEntryLabels(false);
        pieChart.setRotationEnabled(false);
        pieChart.setHighlightPerTapEnabled(true); // Enable highlighting
        pieChart.animateY(1200); // Slightly longer animation
        pieChart.invalidate();
    }

    private void updateBarChart(List<Expense> expenses) {
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

        List<BarEntry> expenseEntries = new ArrayList<>();
        List<BarEntry> incomeEntries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            expenseEntries.add(new BarEntry(i, expenseAmounts[i]));
            incomeEntries.add(new BarEntry(i, incomeAmounts[i]));
        }

        BarDataSet expenseSet = new BarDataSet(expenseEntries, "Expense");
        expenseSet.setColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        expenseSet.setDrawValues(false);

        BarDataSet incomeSet = new BarDataSet(incomeEntries, "Income");
        incomeSet.setColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        incomeSet.setDrawValues(false);

        BarData barData = new BarData(incomeSet, expenseSet);
        float groupSpace = 0.3f;
        float barSpace = 0.05f;
        float barWidth = 0.3f;
        barData.setBarWidth(barWidth);

        barChart.setData(barData);
        barChart.getDescription().setEnabled(false);
        barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(days));
        barChart.getXAxis().setGranularity(1f);
        barChart.getXAxis().setDrawGridLines(false);
        barChart.getXAxis().setTextColor(Color.GRAY);
        barChart.getXAxis().setCenterAxisLabels(true);
        barChart.getAxisLeft().setDrawGridLines(true);
        barChart.getAxisLeft().setGridColor(Color.parseColor("#F3F4F6"));
        barChart.getAxisLeft().setTextColor(Color.GRAY);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setFitBars(false);
        barChart.setDrawGridBackground(false);
        barChart.groupBars(0f, groupSpace, barSpace);
        barChart.animateY(1000);
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

            String[] dayNames = { "S", "M", "T", "W", "T", "F", "S" };
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
        expenseSet.setLineWidth(2.5f);
        expenseSet.setCircleRadius(4f);
        expenseSet.setDrawValues(false);
        expenseSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        expenseSet.setDrawFilled(true);
        expenseSet.setFillColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        expenseSet.setFillAlpha(20);

        LineDataSet incomeSet = new LineDataSet(incomeEntries, "Income");
        incomeSet.setColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        incomeSet.setCircleColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        incomeSet.setLineWidth(2.5f);
        incomeSet.setCircleRadius(4f);
        incomeSet.setDrawValues(false);
        incomeSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        incomeSet.setDrawFilled(true);
        incomeSet.setFillColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        incomeSet.setFillAlpha(20);

        LineData lineData = new LineData(incomeSet, expenseSet);
        lineChart.setData(lineData);

        lineChart.getDescription().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(days));
        lineChart.getXAxis().setGranularity(1f);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getXAxis().setTextColor(Color.GRAY);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.parseColor("#F3F4F6"));
        lineChart.getAxisLeft().setTextColor(Color.GRAY);
        lineChart.getAxisLeft().setAxisMinimum(0f);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(false);
        lineChart.animateY(1000);
        lineChart.invalidate();
    }
}
