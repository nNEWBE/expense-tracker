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

import com.example.trackexpense.MainActivity;
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
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.trackexpense.adapters.NotificationAdapter;

public class DashboardFragment extends Fragment {

    private ExpenseViewModel expenseViewModel;
    private PreferenceManager preferenceManager;

    private TextView tvTotalBalance, tvTotalIncome, tvTotalExpense, tvSeeAll;
    private TextView tvGreeting, tvUserName;
    private TextView tvBudgetRemaining, tvBudgetSpent, tvDaysLeft;
    // Charts removed
    private RecyclerView rvRecentTransactions;
    private ExpenseAdapter expenseAdapter;
    private MaterialCardView btnMenu, btnNotification, cardBalance, cardBudget;
    private com.google.android.material.button.MaterialButton btnFilterAll, btnFilterIncome, btnFilterExpense;
    private View cardContainer;

    private List<Expense> allExpenses = new ArrayList<>();
    private String currentFilter = "ALL";

    // Notification panel views
    private View notificationOverlay, notificationDimBackground, notificationPanel;
    private View emptyNotifications;
    private RecyclerView rvNotifications;
    private NotificationAdapter notificationAdapter;
    private TextView tvNotificationBadge;
    private List<com.example.trackexpense.data.local.Expense> notificationsList = new ArrayList<>();
    private java.util.Set<Integer> viewedNotificationIds = new java.util.HashSet<>();

    // Handler for notification animation
    private android.os.Handler notificationHandler;
    private Runnable notificationAnimationRunnable;
    private boolean isNotificationPanelOpen = false;

    private boolean isShowingBalance = true;

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
        setupUserInfo();
        setupRecyclerView();
        setupClickListeners(view);
        observeData();
        animateBalanceCard();
    }

    @Override
    public void onDestroyView() {
        // Cancel any pending animation callbacks to prevent crashes
        cancelNotificationAnimation();

        // Cancel any running animations
        if (notificationOverlay != null) {
            notificationOverlay.animate().cancel();
        }
        if (notificationPanel != null) {
            notificationPanel.animate().cancel();
        }
        if (cardBalance != null) {
            cardBalance.animate().cancel();
        }
        if (cardBudget != null) {
            cardBudget.animate().cancel();
        }

        super.onDestroyView();
    }

    private void initViews(View view) {
        tvTotalBalance = view.findViewById(R.id.tvTotalBalance);
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome);
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
        tvSeeAll = view.findViewById(R.id.tvSeeAll);
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvUserName = view.findViewById(R.id.tvUserName);
        // Charts removed
        rvRecentTransactions = view.findViewById(R.id.rvRecentTransactions);
        btnMenu = view.findViewById(R.id.btnMenu);
        btnNotification = view.findViewById(R.id.btnNotification);
        cardBalance = view.findViewById(R.id.cardBalance);
        cardBudget = view.findViewById(R.id.cardBudget);

        btnFilterAll = view.findViewById(R.id.btnFilterAll);
        btnFilterIncome = view.findViewById(R.id.btnFilterIncome);
        btnFilterExpense = view.findViewById(R.id.btnFilterExpense);

        // Budget card views
        tvBudgetRemaining = view.findViewById(R.id.tvBudgetRemaining);
        tvBudgetSpent = view.findViewById(R.id.tvBudgetSpent);
        tvDaysLeft = view.findViewById(R.id.tvDaysLeft);
        cardContainer = view.findViewById(R.id.cardContainer);

        // Notification panel views
        notificationOverlay = view.findViewById(R.id.notificationOverlay);
        notificationDimBackground = view.findViewById(R.id.notificationDimBackground);
        notificationPanel = view.findViewById(R.id.notificationPanel);
        rvNotifications = view.findViewById(R.id.rvNotifications);
        emptyNotifications = view.findViewById(R.id.emptyNotifications);
        tvNotificationBadge = view.findViewById(R.id.tvNotificationBadge);

        // Setup notification panel
        setupNotificationPanel(view);

        // Setup swipe gesture on card container
        setupCardSwipeGesture();
    }

    private void animateBalanceCard() {
        // Set camera distance for proper 3D effect
        float scale = getResources().getDisplayMetrics().density;
        cardBalance.setCameraDistance(8000 * scale);

        // Initial state - invisible and slightly scaled down
        cardBalance.setAlpha(0f);
        cardBalance.setScaleX(0.8f);
        cardBalance.setScaleY(0.8f);
        cardBalance.setRotationY(-90f);

        // Entrance animation with 3D flip
        android.animation.AnimatorSet animatorSet = new android.animation.AnimatorSet();

        // Fade in
        android.animation.ObjectAnimator fadeIn = android.animation.ObjectAnimator.ofFloat(cardBalance, "alpha", 0f,
                1f);
        fadeIn.setDuration(400);

        // Scale up
        android.animation.ObjectAnimator scaleX = android.animation.ObjectAnimator.ofFloat(cardBalance, "scaleX", 0.8f,
                1f);
        android.animation.ObjectAnimator scaleY = android.animation.ObjectAnimator.ofFloat(cardBalance, "scaleY", 0.8f,
                1f);
        scaleX.setDuration(600);
        scaleY.setDuration(600);

        // Flip rotation with overshoot for bounce effect
        android.animation.ObjectAnimator flipIn = android.animation.ObjectAnimator.ofFloat(cardBalance, "rotationY",
                -90f, 0f);
        flipIn.setDuration(800);
        flipIn.setInterpolator(new android.view.animation.OvershootInterpolator(0.8f));

        // Play all together
        animatorSet.playTogether(fadeIn, scaleX, scaleY, flipIn);
        animatorSet.setStartDelay(300);
        animatorSet.start();
    }

    private void setupCardSwipeGesture() {
        float scale = getResources().getDisplayMetrics().density;
        cardBalance.setCameraDistance(8000 * scale);
        cardBudget.setCameraDistance(8000 * scale);

        // Flip on tap/click
        cardBalance.setOnClickListener(v -> flipToCard(false));
        cardBudget.setOnClickListener(v -> flipToCard(true));
    }

    private void flipToCard(boolean showBalance) {
        if (isShowingBalance == showBalance)
            return; // Already showing the desired card

        isShowingBalance = showBalance;

        MaterialCardView cardOut = showBalance ? cardBudget : cardBalance;
        MaterialCardView cardIn = showBalance ? cardBalance : cardBudget;

        // Flip out animation for current card
        android.animation.ObjectAnimator flipOut = android.animation.ObjectAnimator.ofFloat(
                cardOut, "rotationY", 0f, showBalance ? 90f : -90f);
        flipOut.setDuration(250);
        flipOut.setInterpolator(new android.view.animation.AccelerateInterpolator());

        flipOut.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                cardOut.setVisibility(View.GONE);
                cardOut.setRotationY(0f);

                // Prepare incoming card
                cardIn.setVisibility(View.VISIBLE);
                cardIn.setRotationY(showBalance ? -90f : 90f);

                // Flip in animation for new card
                android.animation.ObjectAnimator flipIn = android.animation.ObjectAnimator.ofFloat(
                        cardIn, "rotationY", showBalance ? -90f : 90f, 0f);
                flipIn.setDuration(250);
                flipIn.setInterpolator(new android.view.animation.DecelerateInterpolator());
                flipIn.start();
            }
        });

        flipOut.start();
    }

    private void setupUserInfo() {
        // Set greeting based on time of day
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) {
            greeting = "Good Morning 🌞";
        } else if (hour < 17) {
            greeting = "Good Afternoon ☀️";
        } else {
            greeting = "Good Evening 🌙";
        }
        tvGreeting.setText(greeting);

        // Set user name
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            tvUserName.setText(user.getDisplayName());
        } else if (preferenceManager.isGuestMode()) {
            tvUserName.setText("Guest User");
        } else {
            tvUserName.setText("User");
        }
    }

    private void setupRecyclerView() {
        expenseAdapter = new ExpenseAdapter();
        expenseAdapter.setCurrencySymbol(preferenceManager.getCurrencySymbol());
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentTransactions.setAdapter(expenseAdapter);
    }

    private void setupClickListeners(View view) {
        // FAB removed - using bottom nav FAB instead

        tvSeeAll.setOnClickListener(v -> Navigation.findNavController(view).navigate(R.id.transactionsFragment));

        // Menu button opens drawer
        btnMenu.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openDrawer();
            }
        });

        // Notification button - handled in setupNotificationPanel()

        // Filter listeners
        btnFilterAll.setOnClickListener(v -> applyFilter("ALL"));
        btnFilterIncome.setOnClickListener(v -> applyFilter("INCOME"));
        btnFilterExpense.setOnClickListener(v -> applyFilter("EXPENSE"));

        // Initialize filter UI
        updateFilterUI();
    }

    private void observeData() {
        expenseViewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses != null) {
                allExpenses = expenses;
                updateSummary(expenses);
                updateRecentTransactions();
                updateNotifications(expenses);
            }
        });
    }

    private void updateSummary(List<Expense> expenses) {
        String symbol = preferenceManager.getCurrencySymbol();
        double totalIncome = 0;
        double totalExpense = 0;
        double monthlyExpense = 0;

        // Get current month start
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long monthStart = cal.getTimeInMillis();

        for (Expense e : expenses) {
            if ("INCOME".equals(e.getType())) {
                totalIncome += e.getAmount();
            } else {
                totalExpense += e.getAmount();
                // Calculate monthly expense
                if (e.getDate() >= monthStart) {
                    monthlyExpense += e.getAmount();
                }
            }
        }

        double balance = totalIncome - totalExpense;

        // Animate counters
        animateCounter(tvTotalBalance, 0, balance, symbol);
        animateCounter(tvTotalIncome, 0, totalIncome, symbol);
        animateCounter(tvTotalExpense, 0, totalExpense, symbol);

        // Update budget card
        updateBudgetCard(symbol, monthlyExpense);
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
                textView.setText(String.format("%s%,.0f", symbol, value));
            }
        });

        animator.start();
    }

    private void updateBudgetCard(String symbol, double monthlyExpense) {
        double monthlyBudget = preferenceManager.getMonthlyBudget();
        double remaining = monthlyBudget - monthlyExpense;

        if (remaining < 0)
            remaining = 0;

        // Animate budget values
        animateCounter(tvBudgetRemaining, 0, remaining, symbol);
        animateCounter(tvBudgetSpent, 0, monthlyExpense, symbol);

        // Calculate days left in month
        Calendar cal = Calendar.getInstance();
        int currentDay = cal.get(Calendar.DAY_OF_MONTH);
        int totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int daysLeft = totalDays - currentDay;

        // Animate days left
        animateDaysCounter(tvDaysLeft, 0, daysLeft);
    }

    private void animateDaysCounter(TextView textView, int start, int end) {
        if (textView == null)
            return;

        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(start, end);
        animator.setDuration(1200);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());

        animator.addUpdateListener(animation -> {
            if (textView != null && isAdded()) {
                int value = (int) animation.getAnimatedValue();
                textView.setText(String.valueOf(value));
            }
        });

        animator.start();
    }

    private void updateRecentTransactions() {
        List<Expense> filtered = new ArrayList<>();
        if (allExpenses != null) {
            for (Expense e : allExpenses) {
                if ("ALL".equals(currentFilter) || currentFilter.equals(e.getType())) {
                    filtered.add(e);
                }
            }
        }
        int count = Math.min(filtered.size(), 10); // Show up to 10

        // Apply slide-right animation
        android.view.animation.LayoutAnimationController animController = android.view.animation.AnimationUtils
                .loadLayoutAnimation(
                        requireContext(), R.anim.layout_animation_slide_right);
        rvRecentTransactions.setLayoutAnimation(animController);
        expenseAdapter.setExpenses(filtered.subList(0, count));
        rvRecentTransactions.scheduleLayoutAnimation();
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        updateFilterUI();
        updateRecentTransactions();
    }

    private void updateFilterUI() {
        if (btnFilterAll == null)
            return;
        updateButtonStyle(btnFilterAll, "ALL".equals(currentFilter), R.color.primary);
        updateButtonStyle(btnFilterIncome, "INCOME".equals(currentFilter), R.color.income_green);
        updateButtonStyle(btnFilterExpense, "EXPENSE".equals(currentFilter), R.color.expense_red);
    }

    private void updateButtonStyle(com.google.android.material.button.MaterialButton btn, boolean isActive,
            int colorRes) {
        int color = androidx.core.content.ContextCompat.getColor(requireContext(), colorRes);
        if (isActive) {
            // Modern tonal style: 20% opacity background, 100% opacity text/icon
            int fadedColor = androidx.core.graphics.ColorUtils.setAlphaComponent(color, 50);

            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(fadedColor));
            btn.setTextColor(color);
            btn.setIconTint(android.content.res.ColorStateList.valueOf(color));
            btn.setStrokeWidth(0);
            btn.setElevation(0);
        } else {
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
            btn.setTextColor(color);
            btn.setIconTint(android.content.res.ColorStateList.valueOf(color));
            btn.setStrokeColor(android.content.res.ColorStateList.valueOf(color));
            btn.setStrokeWidth(dpToPx(1));
            btn.setElevation(0);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // ===================== NOTIFICATION PANEL METHODS =====================

    private void setupNotificationPanel(View view) {
        // Setup RecyclerView for notifications
        notificationAdapter = new NotificationAdapter();
        String symbol = preferenceManager.getCurrencySymbol();
        notificationAdapter.setCurrencySymbol(symbol);

        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNotifications.setAdapter(notificationAdapter);

        // Setup delete listener
        notificationAdapter.setOnNotificationDeleteListener((expense, position) -> {
            notificationAdapter.removeNotification(position);
            updateNotificationBadge();
            checkEmptyState();
        });

        // Close button
        view.findViewById(R.id.btnCloseNotifications).setOnClickListener(v -> hideNotificationPanel());

        // Dim background click to close
        notificationDimBackground.setOnClickListener(v -> hideNotificationPanel());

        // Clear all button
        view.findViewById(R.id.btnClearAll).setOnClickListener(v -> {
            notificationAdapter.clearAll();
            updateNotificationBadge();
            checkEmptyState();
        });

        // Notification button click
        btnNotification.setOnClickListener(v -> showNotificationPanel());
    }

    private void showNotificationPanel() {
        // Prevent double-opening
        if (isNotificationPanelOpen)
            return;
        isNotificationPanelOpen = true;

        // Cancel any pending animation callbacks
        cancelNotificationAnimation();

        // Mark all current notifications as viewed
        for (Expense expense : notificationsList) {
            viewedNotificationIds.add(expense.getId());
        }
        updateNotificationBadge();

        // Clear adapter first
        if (rvNotifications != null) {
            rvNotifications.setLayoutAnimation(null);
        }
        if (notificationAdapter != null) {
            notificationAdapter.setNotifications(new java.util.ArrayList<>());
        }

        // Show overlay
        if (notificationOverlay != null) {
            notificationOverlay.setVisibility(View.VISIBLE);
            notificationOverlay.setAlpha(1f);
        }

        // Set panel to starting position and animate
        if (notificationPanel != null) {
            notificationPanel.setTranslationX(notificationPanel.getWidth());
            notificationPanel.animate()
                    .translationX(0)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }

        // Load data after a delay
        notificationHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        notificationAnimationRunnable = () -> {
            try {
                if (isAdded() && isNotificationPanelOpen && notificationAdapter != null && rvNotifications != null) {
                    if (notificationsList != null && notificationsList.size() > 0) {
                        android.view.animation.LayoutAnimationController animController = android.view.animation.AnimationUtils
                                .loadLayoutAnimation(
                                        requireContext(), R.anim.notification_layout_animation);
                        rvNotifications.setLayoutAnimation(animController);
                        notificationAdapter.setNotifications(notificationsList);
                    }
                    checkEmptyState();
                }
            } catch (Exception ignored) {
                // Ignore any errors if fragment is detached
            }
        };
        notificationHandler.postDelayed(notificationAnimationRunnable, 350);
    }

    private void hideNotificationPanel() {
        // Prevent double-closing
        if (!isNotificationPanelOpen)
            return;
        isNotificationPanelOpen = false;

        // Cancel any pending animation callbacks
        cancelNotificationAnimation();

        // Animate panel out
        if (notificationPanel != null) {
            notificationPanel.animate()
                    .translationX(notificationPanel.getWidth())
                    .setDuration(250)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .start();
        }

        // Hide overlay after delay
        if (notificationHandler == null) {
            notificationHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        }
        notificationHandler.postDelayed(() -> {
            try {
                if (notificationOverlay != null) {
                    notificationOverlay.setVisibility(View.GONE);
                }
            } catch (Exception ignored) {
            }
        }, 300);
    }

    private void cancelNotificationAnimation() {
        if (notificationHandler != null) {
            notificationHandler.removeCallbacksAndMessages(null);
        }
    }

    private void updateNotificationBadge() {
        // Count unviewed notifications
        int unviewedCount = 0;
        for (Expense expense : notificationsList) {
            if (!viewedNotificationIds.contains(expense.getId())) {
                unviewedCount++;
            }
        }

        if (unviewedCount > 0) {
            tvNotificationBadge.setVisibility(View.VISIBLE);
            tvNotificationBadge.setText(unviewedCount > 99 ? "99+" : String.valueOf(unviewedCount));
        } else {
            tvNotificationBadge.setVisibility(View.GONE);
        }
    }

    private void checkEmptyState() {
        if (notificationAdapter.getNotificationCount() == 0) {
            rvNotifications.setVisibility(View.GONE);
            emptyNotifications.setVisibility(View.VISIBLE);
        } else {
            rvNotifications.setVisibility(View.VISIBLE);
            emptyNotifications.setVisibility(View.GONE);
        }
    }

    private void updateNotifications(List<Expense> expenses) {
        // Get recent transactions as notifications (e.g., last 20)
        int count = Math.min(expenses.size(), 20);
        notificationsList = new ArrayList<>(expenses.subList(0, count));
        updateNotificationBadge();
    }
}
