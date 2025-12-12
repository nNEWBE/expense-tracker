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

import com.example.trackexpense.adapters.AppNotificationAdapter;
import com.example.trackexpense.data.model.AppNotification;
import com.example.trackexpense.data.repository.NotificationRepository;

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
    private MaterialCardView btnFilterAll, btnFilterIncome, btnFilterExpense;
    private View cardContainer;

    private List<Expense> allExpenses = new ArrayList<>();
    private String currentFilter = "ALL";

    // Notification panel views
    private View notificationOverlay, notificationDimBackground, notificationPanel;
    private View emptyNotifications;
    private RecyclerView rvNotifications;
    private AppNotificationAdapter appNotificationAdapter;
    private TextView tvNotificationBadge, tvNotificationCount;
    private List<AppNotification> notificationsList = new ArrayList<>();
    private NotificationRepository notificationRepository;

    // Notification tabs
    private MaterialCardView tabAlerts, tabRequests;
    private TextView tvAlertsBadge, tvRequestsBadge;
    private TextView tvTabAlerts, tvTabRequests;
    private android.widget.ImageView icTabAlerts, icTabRequests;
    private boolean isAlertsTabActive = true;

    // Category requests
    private List<AppNotification> categoryRequestsList = new ArrayList<>();
    private boolean isUserAdmin = false;

    // Handler for notification animation
    private android.os.Handler notificationHandler;
    private Runnable notificationAnimationRunnable;
    private boolean isNotificationPanelOpen = false;

    private boolean isShowingBalance = true;

    // Skeleton loading
    private View skeletonView;
    private boolean isFirstLoad = true;
    private boolean hasAnimatedBalanceCard = false;

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

        // Check if data is already available (cached)
        List<com.example.trackexpense.data.local.Expense> cachedData = expenseViewModel.getAllExpenses().getValue();
        if (cachedData == null || cachedData.isEmpty()) {
            isFirstLoad = true;
            showSkeletonLoading(view);
        } else {
            isFirstLoad = false;
        }

        initViews(view);
        setupUserInfo();
        setupRecyclerView();
        setupClickListeners(view);
        observeData();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh notification badge every time dashboard is shown
        updateNotificationBadge();
    }

    /**
     * Show skeleton loading placeholder while data loads.
     */
    private void showSkeletonLoading(View rootView) {
        if (rootView instanceof ViewGroup) {
            skeletonView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.skeleton_dashboard, (ViewGroup) rootView, false);
            ((ViewGroup) rootView).addView(skeletonView);

            // Ensure skeleton is above everything
            skeletonView.setElevation(100f);
        }
    }

    /**
     * Hide skeleton loading with smooth fade animation.
     */
    private void hideSkeletonLoading(Runnable onAnimationEndAction) {
        if (skeletonView == null) {
            if (onAnimationEndAction != null)
                onAnimationEndAction.run();
            return;
        }

        isFirstLoad = false;

        skeletonView.animate()
                .alpha(0f)
                .setDuration(400)
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
        tvNotificationCount = view.findViewById(R.id.tvNotificationCount);

        // Notification tabs
        tabAlerts = view.findViewById(R.id.tabAlerts);
        tabRequests = view.findViewById(R.id.tabRequests);
        tvAlertsBadge = view.findViewById(R.id.tvAlertsBadge);
        tvRequestsBadge = view.findViewById(R.id.tvRequestsBadge);
        tvTabAlerts = view.findViewById(R.id.tvTabAlerts);
        tvTabRequests = view.findViewById(R.id.tvTabRequests);
        icTabAlerts = view.findViewById(R.id.icTabAlerts);
        icTabRequests = view.findViewById(R.id.icTabRequests);

        // Initialize notification repository
        notificationRepository = NotificationRepository.getInstance();

        // Check if user is admin
        checkAdminStatus();

        // Setup notification panel
        setupNotificationPanel(view);

        // Setup swipe gesture on card container
        setupCardSwipeGesture();

        // Load notification counts immediately
        loadAllNotificationCounts();
    }

    private void checkAdminStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        Boolean admin = doc.getBoolean("isAdmin");
                        isUserAdmin = admin != null && admin;
                        android.util.Log.d("DashboardFragment", "User is admin: " + isUserAdmin);
                    });
        }
    }

    private void loadAllNotificationCounts() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // Guest mode - load from local
            int count = preferenceManager.getGuestUnreadNotificationCount();
            updateBadgeDisplay(count);
            return;
        }

        // Load regular notifications count
        if (notificationRepository != null) {
            notificationRepository.getUnreadCount(alertsCount -> {
                // Load category requests count
                loadCategoryRequestsCount(user.getUid(), requestsCount -> {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            int totalCount = alertsCount + requestsCount;
                            updateBadgeDisplay(totalCount);

                            // Update tab badges
                            if (tvAlertsBadge != null) {
                                if (alertsCount > 0) {
                                    tvAlertsBadge.setVisibility(View.VISIBLE);
                                    tvAlertsBadge.setText(alertsCount > 99 ? "99+" : String.valueOf(alertsCount));
                                } else {
                                    tvAlertsBadge.setVisibility(View.GONE);
                                }
                            }
                            if (tvRequestsBadge != null) {
                                if (requestsCount > 0) {
                                    tvRequestsBadge.setVisibility(View.VISIBLE);
                                    tvRequestsBadge.setText(requestsCount > 99 ? "99+" : String.valueOf(requestsCount));
                                } else {
                                    tvRequestsBadge.setVisibility(View.GONE);
                                }
                            }
                        });
                    }
                });
            });
        }
    }

    private interface CountCallback {
        void onCount(int count);
    }

    private void loadCategoryRequestsCount(String userId, CountCallback callback) {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore
                .getInstance();

        // First check if admin
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    Boolean admin = userDoc.getBoolean("isAdmin");
                    isUserAdmin = admin != null && admin;

                    if (isUserAdmin) {
                        // Admin: count all pending requests
                        db.collection("category_requests")
                                .whereEqualTo("status", "PENDING")
                                .get()
                                .addOnSuccessListener(snap -> callback.onCount(snap.size()))
                                .addOnFailureListener(e -> callback.onCount(0));
                    } else {
                        // User: count their pending requests
                        db.collection("category_requests")
                                .whereEqualTo("userId", userId)
                                .whereEqualTo("status", "PENDING")
                                .get()
                                .addOnSuccessListener(snap -> callback.onCount(snap.size()))
                                .addOnFailureListener(e -> callback.onCount(0));
                    }
                })
                .addOnFailureListener(e -> callback.onCount(0));
    }

    private void updateBadgeDisplay(int count) {
        if (tvNotificationBadge != null && isAdded()) {
            if (count > 0) {
                tvNotificationBadge.setVisibility(View.VISIBLE);
                tvNotificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
            } else {
                tvNotificationBadge.setVisibility(View.GONE);
            }
        }
    }

    private void animateBalanceCard() {
        // Only animate once
        if (hasAnimatedBalanceCard) {
            return;
        }
        hasAnimatedBalanceCard = true;

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
        expenseAdapter.setExpandableEnabled(false); // Disable expanding in dashboard
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

                if (isFirstLoad && skeletonView != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        hideSkeletonLoading(() -> {
                            updateSummary(expenses);
                            updateRecentTransactions();
                            animateBalanceCard();
                        });
                    }, 500);
                } else {
                    updateSummary(expenses);
                    updateRecentTransactions();
                }
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

        expenseAdapter.setExpenses(filtered.subList(0, count));
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        updateFilterUI();
        updateRecentTransactions();
    }

    private void updateFilterUI() {
        if (btnFilterAll == null)
            return;
        
        int whiteColor = androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.white);
        int primaryColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary);
        int incomeColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.income_green);
        int expenseColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.expense_red);
        int incomeBgColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.income_green_light);
        int expenseBgColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.expense_red_light);

        // All filter
        boolean allSelected = "ALL".equals(currentFilter);
        btnFilterAll.setCardBackgroundColor(allSelected ? primaryColor : whiteColor);
        btnFilterAll.setStrokeWidth(allSelected ? 0 : dpToPx(1.5f));
        btnFilterAll.setStrokeColor(primaryColor);
        TextView tvAll = btnFilterAll.findViewById(R.id.tvFilterAll);
        android.widget.ImageView iconAll = btnFilterAll.findViewById(R.id.iconFilterAll);
        if (tvAll != null) tvAll.setTextColor(allSelected ? whiteColor : primaryColor);
        if (iconAll != null) iconAll.setColorFilter(allSelected ? whiteColor : primaryColor);

        // Income filter
        boolean incomeSelected = "INCOME".equals(currentFilter);
        btnFilterIncome.setCardBackgroundColor(incomeSelected ? incomeColor : incomeBgColor);
        TextView tvIncome = btnFilterIncome.findViewById(R.id.tvFilterIncome);
        android.widget.ImageView iconIncome = btnFilterIncome.findViewById(R.id.iconFilterIncome);
        if (tvIncome != null) tvIncome.setTextColor(incomeSelected ? whiteColor : incomeColor);
        if (iconIncome != null) iconIncome.setColorFilter(incomeSelected ? whiteColor : incomeColor);

        // Expense filter
        boolean expenseSelected = "EXPENSE".equals(currentFilter);
        btnFilterExpense.setCardBackgroundColor(expenseSelected ? expenseColor : expenseBgColor);
        TextView tvExpense = btnFilterExpense.findViewById(R.id.tvFilterExpense);
        android.widget.ImageView iconExpense = btnFilterExpense.findViewById(R.id.iconFilterExpense);
        if (tvExpense != null) tvExpense.setTextColor(expenseSelected ? whiteColor : expenseColor);
        if (iconExpense != null) iconExpense.setColorFilter(expenseSelected ? whiteColor : expenseColor);
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // ===================== NOTIFICATION PANEL METHODS =====================

    private void setupNotificationPanel(View view) {
        // Setup RecyclerView for notifications
        appNotificationAdapter = new AppNotificationAdapter();
        String symbol = preferenceManager.getCurrencySymbol();
        appNotificationAdapter.setCurrencySymbol(symbol);

        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNotifications.setAdapter(appNotificationAdapter);

        // Setup action listener for notifications
        appNotificationAdapter
                .setOnNotificationActionListener(new AppNotificationAdapter.OnNotificationActionListener() {
                    @Override
                    public void onDelete(AppNotification notification, int position) {
                        String type = notification.getType();

                        // Check if this is a category request notification
                        if ("CATEGORY_REQUEST".equals(type) || "CATEGORY_REQUEST_STATUS".equals(type)) {
                            // Delete from category_requests collection
                            String requestId = notification.getId();
                            if (requestId != null) {
                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                        .collection("category_requests")
                                        .document(requestId)
                                        .delete()
                                        .addOnSuccessListener(v -> {
                                            if (isAdded()) {
                                                categoryRequestsList.remove(position);
                                                if (!isAlertsTabActive) {
                                                    appNotificationAdapter.removeNotification(position);
                                                    if (tvNotificationCount != null) {
                                                        int count = categoryRequestsList.size();
                                                        tvNotificationCount.setText(
                                                                count + (count == 1 ? " request" : " requests"));
                                                    }
                                                }
                                                checkEmptyState();
                                                loadAllNotificationCounts();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            if (isAdded()) {
                                                android.widget.Toast.makeText(requireContext(),
                                                        "Failed to delete request",
                                                        android.widget.Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                            return;
                        }

                        // Regular notification delete
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser != null) {
                            // Delete from Firebase permanently
                            notificationRepository.deleteNotification(notification.getId(),
                                    new NotificationRepository.OnCompleteListener() {
                                        @Override
                                        public void onSuccess() {
                                            notificationsList.remove(notification);
                                            if (isAlertsTabActive) {
                                                appNotificationAdapter.removeNotification(position);
                                                updateNotificationCount();
                                            }
                                            checkEmptyState();
                                            loadAllNotificationCounts();
                                        }

                                        @Override
                                        public void onError(String error) {
                                            // Show error toast
                                            if (isAdded()) {
                                                android.widget.Toast.makeText(requireContext(),
                                                        "Failed to delete notification",
                                                        android.widget.Toast.LENGTH_SHORT)
                                                        .show();
                                            }
                                        }
                                    });
                        } else {
                            // Guest: Delete locally
                            preferenceManager.deleteGuestNotification(notification.getId());
                            appNotificationAdapter.removeNotification(position);
                            updateNotificationCount();
                            checkEmptyState();
                            updateNotificationBadge();
                        }
                    }

                    @Override
                    public void onClick(AppNotification notification) {
                        // Check if this is a category request notification
                        String type = notification.getType();
                        if ("CATEGORY_REQUEST".equals(type) || "CATEGORY_REQUEST_STATUS".equals(type)) {
                            // Parse extra data: userName|categoryType|reason|status
                            String extraData = notification.getExtraData();
                            String userName = "";
                            String categoryType = "";
                            String reason = "";
                            String status = "";

                            if (extraData != null && !extraData.isEmpty()) {
                                String[] parts = extraData.split("\\|", -1);
                                if (parts.length >= 4) {
                                    userName = parts[0];
                                    categoryType = parts[1];
                                    reason = parts[2];
                                    status = parts[3];
                                }
                            }

                            // Get category name from title
                            String categoryName = notification.getTitle();

                            showCategoryRequestReviewDialog(
                                    notification.getId(),
                                    categoryName,
                                    categoryType,
                                    userName,
                                    reason,
                                    status);
                            return;
                        }

                        // Mark as read when clicked (for regular notifications)
                        if (!notification.isRead()) {
                            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                            if (currentUser != null) {
                                notificationRepository.markAsRead(notification.getId(), null);
                            } else {
                                preferenceManager.markGuestNotificationRead(notification.getId());
                                notification.setRead(true);
                                appNotificationAdapter.notifyDataSetChanged();
                                updateNotificationBadge();
                            }
                        }
                    }
                });

        // Close button
        view.findViewById(R.id.btnCloseNotifications).setOnClickListener(v -> hideNotificationPanel());

        // Dim background click to close
        notificationDimBackground.setOnClickListener(v -> hideNotificationPanel());

        // Clear all button - delete all from Firebase
        // Clear all button - delete all
        view.findViewById(R.id.btnClearAll).setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                // Logged-in: Delete from Firebase
                notificationRepository.deleteAllNotifications(new NotificationRepository.OnCompleteListener() {
                    @Override
                    public void onSuccess() {
                        notificationsList.clear();
                        appNotificationAdapter.setNotifications(notificationsList);
                        updateNotificationCount();
                        checkEmptyState();
                    }

                    @Override
                    public void onError(String error) {
                        if (isAdded()) {
                            android.widget.Toast.makeText(requireContext(), "Failed to clear notifications",
                                    android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                // Guest: Clear local notifications
                preferenceManager.clearGuestNotifications();
                notificationsList.clear();
                appNotificationAdapter.setNotifications(notificationsList);
                updateNotificationCount();
                checkEmptyState();
                updateNotificationBadge();
            }
        });

        // Notification button click
        btnNotification.setOnClickListener(v -> showNotificationPanel());

        // Setup tab click listeners
        if (tabAlerts != null) {
            tabAlerts.setOnClickListener(v -> switchToTab(true));
        }
        if (tabRequests != null) {
            tabRequests.setOnClickListener(v -> switchToTab(false));
        }

        // Load notifications from Firebase
        loadNotificationsFromFirebase();
    }

    private void switchToTab(boolean alertsTab) {
        isAlertsTabActive = alertsTab;

        int primaryColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary);
        int whiteAlpha = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.white_70_alpha);

        if (alertsTab) {
            // Alerts tab active
            if (tabAlerts != null) {
                tabAlerts.setCardBackgroundColor(android.graphics.Color.WHITE);
                tabAlerts.setCardElevation(dpToPx(2));
            }
            if (tabRequests != null) {
                tabRequests.setCardBackgroundColor(android.graphics.Color.TRANSPARENT);
                tabRequests.setCardElevation(0);
            }
            if (tvTabAlerts != null)
                tvTabAlerts.setTextColor(primaryColor);
            if (tvTabRequests != null)
                tvTabRequests.setTextColor(whiteAlpha);
            if (icTabAlerts != null)
                icTabAlerts.setColorFilter(primaryColor);
            if (icTabRequests != null)
                icTabRequests.setColorFilter(whiteAlpha);

            // Show alerts notifications
            appNotificationAdapter.setNotifications(notificationsList);
            updateNotificationCount();
        } else {
            // Requests tab active
            if (tabAlerts != null) {
                tabAlerts.setCardBackgroundColor(android.graphics.Color.TRANSPARENT);
                tabAlerts.setCardElevation(0);
            }
            if (tabRequests != null) {
                tabRequests.setCardBackgroundColor(android.graphics.Color.WHITE);
                tabRequests.setCardElevation(dpToPx(2));
            }
            if (tvTabAlerts != null)
                tvTabAlerts.setTextColor(whiteAlpha);
            if (tvTabRequests != null)
                tvTabRequests.setTextColor(primaryColor);
            if (icTabAlerts != null)
                icTabAlerts.setColorFilter(whiteAlpha);
            if (icTabRequests != null)
                icTabRequests.setColorFilter(primaryColor);

            // Show category requests
            appNotificationAdapter.setNotifications(categoryRequestsList);
            if (tvNotificationCount != null) {
                int count = categoryRequestsList.size();
                tvNotificationCount.setText(count + (count == 1 ? " request" : " requests"));
            }
        }

        checkEmptyState();
    }

    private void showCategoryRequestReviewDialog(String requestId, String categoryName,
            String categoryType, String userName, String reason, String status) {
        if (!isAdded())
            return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_category_request_review, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();

        // Get views
        TextView tvCategoryName = dialogView.findViewById(R.id.tvCategoryName);
        TextView tvCategoryType = dialogView.findViewById(R.id.tvCategoryType);
        TextView tvRequestedBy = dialogView.findViewById(R.id.tvRequestedBy);
        TextView tvReason = dialogView.findViewById(R.id.tvReason);
        TextView tvStatus = dialogView.findViewById(R.id.tvStatus);
        TextView tvStatusEmoji = dialogView.findViewById(R.id.tvStatusEmoji);
        TextView tvDialogSubtitle = dialogView.findViewById(R.id.tvDialogSubtitle);
        View reasonContainer = dialogView.findViewById(R.id.reasonContainer);
        View statusContainer = dialogView.findViewById(R.id.statusContainer);
        View adminActionsContainer = dialogView.findViewById(R.id.adminActionsContainer);
        com.google.android.material.button.MaterialButton btnApprove = dialogView.findViewById(R.id.btnApprove);
        com.google.android.material.button.MaterialButton btnReject = dialogView.findViewById(R.id.btnReject);
        com.google.android.material.button.MaterialButton btnClose = dialogView.findViewById(R.id.btnClose);

        // Set data
        if (tvCategoryName != null)
            tvCategoryName.setText(categoryName);
        if (tvRequestedBy != null)
            tvRequestedBy.setText(userName != null ? userName : "Unknown");

        // Set category type chip
        if (tvCategoryType != null) {
            tvCategoryType.setText(categoryType != null ? categoryType : "Unknown");
            if ("EXPENSE".equalsIgnoreCase(categoryType)) {
                tvCategoryType.setBackgroundResource(R.drawable.bg_chip_red);
                tvCategoryType.setTextColor(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.expense_red));
            }
        }

        // Set reason
        if (reason != null && !reason.isEmpty()) {
            if (tvReason != null)
                tvReason.setText(reason);
            if (reasonContainer != null)
                reasonContainer.setVisibility(View.VISIBLE);
        }

        // Set status - hide emoji view
        if (tvStatusEmoji != null)
            tvStatusEmoji.setVisibility(View.GONE);

        if ("APPROVED".equals(status)) {
            if (tvStatus != null)
                tvStatus.setText("APPROVED");
            if (statusContainer != null)
                statusContainer.setBackgroundResource(R.drawable.bg_chip_green);
            if (tvStatus != null)
                tvStatus.setTextColor(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.income_green));
        } else if ("REJECTED".equals(status)) {
            if (tvStatus != null)
                tvStatus.setText("REJECTED");
            if (statusContainer != null)
                statusContainer.setBackgroundResource(R.drawable.bg_chip_red);
            if (tvStatus != null)
                tvStatus.setTextColor(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.expense_red));
        }

        // Show admin actions only for admin and only for pending requests
        if (isUserAdmin && "PENDING".equals(status)) {
            if (adminActionsContainer != null)
                adminActionsContainer.setVisibility(View.VISIBLE);
            if (tvDialogSubtitle != null)
                tvDialogSubtitle.setText("Review and take action");

            if (btnApprove != null) {
                btnApprove.setOnClickListener(v -> {
                    updateCategoryRequestStatus(requestId, "APPROVED");
                    dialog.dismiss();
                });
            }
            if (btnReject != null) {
                btnReject.setOnClickListener(v -> {
                    updateCategoryRequestStatus(requestId, "REJECTED");
                    dialog.dismiss();
                });
            }
        } else {
            if (tvDialogSubtitle != null) {
                if ("PENDING".equals(status)) {
                    tvDialogSubtitle.setText("Waiting for admin review");
                } else if ("APPROVED".equals(status)) {
                    tvDialogSubtitle.setText("This category has been added");
                } else {
                    tvDialogSubtitle.setText("This request was rejected");
                }
            }
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private void updateCategoryRequestStatus(String requestId, String newStatus) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("category_requests")
                .document(requestId)
                .update("status", newStatus, "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(v -> {
                    if (isAdded()) {
                        String message = "APPROVED".equals(newStatus) ? "Request approved!" : "Request rejected";
                        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT)
                                .show();
                        // Reload requests
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            loadCategoryRequestsAsNotifications(user.getUid());
                            loadAllNotificationCounts();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        android.widget.Toast.makeText(requireContext(), "Failed to update: " + e.getMessage(),
                                android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showNotificationPanel() {
        // Prevent double-opening
        if (isNotificationPanelOpen)
            return;
        isNotificationPanelOpen = true;

        // Cancel any pending animation callbacks
        cancelNotificationAnimation();

        // Show overlay
        if (notificationOverlay != null) {
            notificationOverlay.setVisibility(View.VISIBLE);
            notificationOverlay.setAlpha(1f);
        }

        // Set panel to starting position and animate
        if (notificationPanel != null) {
            notificationPanel.setTranslationX(notificationPanel.getWidth() > 0 ? notificationPanel.getWidth() : 1000);
            notificationPanel.animate()
                    .translationX(0)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }

        // Always refresh notifications from Firebase when panel opens
        refreshNotificationsFromFirebase();
    }

    /**
     * Refresh notifications from Firebase and update the UI.
     * Called when the notification panel is opened.
     */
    private void refreshNotificationsFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null && notificationRepository != null) {
            // Logged-in user: Load from Firebase
            notificationRepository.getNotifications(new NotificationRepository.OnNotificationsLoadedListener() {
                @Override
                public void onLoaded(List<AppNotification> notifications) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            notificationsList = new ArrayList<>(notifications);

                            // Also load category requests
                            loadCategoryRequestsAsNotifications(currentUser.getUid());
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    android.util.Log.e("DashboardFragment", "Error refreshing notifications: " + error);
                }
            });
        } else {
            // Guest user: Load from local storage
            loadGuestNotifications();
        }
    }

    /**
     * Load guest notifications from local storage and display in panel.
     */
    private void loadGuestNotifications() {
        String rawData = preferenceManager.getGuestNotificationsRaw();
        notificationsList = new ArrayList<>();

        if (rawData != null && !rawData.isEmpty()) {
            String[] items = rawData.split(";");
            for (String item : items) {
                String[] parts = item.split("\\|");
                if (parts.length >= 6) {
                    try {
                        String id = parts[0];
                        String type = parts[1];
                        String title = parts[2];
                        String message = parts[3];
                        long timestamp = Long.parseLong(parts[4]);
                        boolean isRead = "true".equals(parts[5]);

                        AppNotification notification = new AppNotification("guest", type, title, message);
                        notification.setId(id);
                        notification.setRead(isRead);
                        notification.setCreatedAt(new java.util.Date(timestamp));

                        notificationsList.add(notification);
                    } catch (Exception e) {
                        android.util.Log.e("DashboardFragment", "Error parsing guest notification: " + e.getMessage());
                    }
                }
            }
        }

        if (isNotificationPanelOpen && appNotificationAdapter != null) {
            appNotificationAdapter.setNotifications(notificationsList);
            updateNotificationCount();
            checkEmptyState();
        }
        updateNotificationBadge();
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
        // Check if user is logged in or guest
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null && notificationRepository != null) {
            // Logged-in user: Get unread count from Firebase
            notificationRepository.getUnreadCount(count -> {
                if (isAdded() && tvNotificationBadge != null) {
                    requireActivity().runOnUiThread(() -> {
                        if (count > 0) {
                            tvNotificationBadge.setVisibility(View.VISIBLE);
                            tvNotificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                        } else {
                            tvNotificationBadge.setVisibility(View.GONE);
                        }
                    });
                }
            });
        } else {
            // Guest user: Get unread count from local storage
            int count = preferenceManager.getGuestUnreadNotificationCount();
            if (isAdded() && tvNotificationBadge != null) {
                if (count > 0) {
                    tvNotificationBadge.setVisibility(View.VISIBLE);
                    tvNotificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                } else {
                    tvNotificationBadge.setVisibility(View.GONE);
                }
            }
        }
    }

    private void updateNotificationCount() {
        if (tvNotificationCount != null) {
            int count = notificationsList.size();
            tvNotificationCount.setText(count + (count == 1 ? " notification" : " notifications"));
        }
    }

    private void checkEmptyState() {
        if (appNotificationAdapter != null) {
            if (appNotificationAdapter.getNotificationCount() == 0) {
                if (rvNotifications != null)
                    rvNotifications.setVisibility(View.GONE);
                if (emptyNotifications != null)
                    emptyNotifications.setVisibility(View.VISIBLE);
            } else {
                if (rvNotifications != null)
                    rvNotifications.setVisibility(View.VISIBLE);
                if (emptyNotifications != null)
                    emptyNotifications.setVisibility(View.GONE);
            }
        }
    }

    private void loadNotificationsFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null && notificationRepository != null) {
            // Logged-in user: Load from Firebase
            notificationRepository.getNotifications(new NotificationRepository.OnNotificationsLoadedListener() {
                @Override
                public void onLoaded(List<AppNotification> notifications) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            notificationsList = new ArrayList<>(notifications);

                            // Also load category requests
                            loadCategoryRequestsAsNotifications(currentUser.getUid());
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    android.util.Log.e("DashboardFragment", "Error loading notifications: " + error);
                }
            });
        } else {
            // Guest user: Load from local storage
            loadGuestNotifications();
        }
    }

    /**
     * Load category requests and display them in the Requests tab.
     * - For admin users: Show all pending requests
     * - For regular users: Show their own requests with status
     */
    private void loadCategoryRequestsAsNotifications(String userId) {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore
                .getInstance();

        // First check if user is admin
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    Boolean isAdmin = userDoc.getBoolean("isAdmin");
                    isUserAdmin = isAdmin != null && isAdmin;

                    if (isUserAdmin) {
                        // Admin: Load all pending requests
                        loadAdminCategoryRequests(db);
                    } else {
                        // Regular user: Load their own requests
                        loadUserCategoryRequests(db, userId);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DashboardFragment", "Error checking admin status", e);
                    updateUIAfterLoad();
                });
    }

    /**
     * Load all pending category requests for admin users.
     */
    private void loadAdminCategoryRequests(com.google.firebase.firestore.FirebaseFirestore db) {
        android.util.Log.d("CategoryRequests", "Loading admin category requests...");
        categoryRequestsList.clear();

        db.collection("category_requests")
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded())
                        return;

                    android.util.Log.d("CategoryRequests", "Found " + querySnapshot.size() + " pending requests");

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        String requestId = doc.getId();
                        String categoryName = doc.getString("categoryName");
                        String categoryType = doc.getString("categoryType");
                        String userName = doc.getString("userName");
                        String reason = doc.getString("reason");
                        String status = doc.getString("status");

                        // Create notification from request with metadata
                        AppNotification requestNotification = new AppNotification(
                                "admin",
                                "CATEGORY_REQUEST",
                                categoryName,
                                userName + " - " + (categoryType != null ? categoryType : "Unknown") + " category");
                        requestNotification.setId(requestId); // Store actual request ID
                        requestNotification.setRead(false);

                        // Store extra data in a custom way using the message field
                        // Format: userName|categoryType|reason|status
                        requestNotification.setExtraData(userName + "|" + categoryType + "|" +
                                (reason != null ? reason : "") + "|" + status);

                        categoryRequestsList.add(requestNotification);
                    }

                    updateUIAfterLoad();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CategoryRequests", "FAILED to load admin requests: " + e.getMessage(), e);
                    updateUIAfterLoad();
                });
    }

    /**
     * Load user's own category requests to show status updates.
     */
    private void loadUserCategoryRequests(com.google.firebase.firestore.FirebaseFirestore db, String userId) {
        android.util.Log.d("CategoryRequests", "Loading user category requests for: " + userId);
        categoryRequestsList.clear();

        db.collection("category_requests")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded())
                        return;

                    android.util.Log.d("CategoryRequests", "Found " + querySnapshot.size() + " user requests");

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        String requestId = doc.getId();
                        String categoryName = doc.getString("categoryName");
                        String categoryType = doc.getString("categoryType");
                        String userName = doc.getString("userName");
                        String reason = doc.getString("reason");
                        String status = doc.getString("status");

                        // Format status for display
                        String statusText = "Pending";
                        if ("APPROVED".equals(status)) {
                            statusText = "Approved";
                        } else if ("REJECTED".equals(status)) {
                            statusText = "Rejected";
                        }

                        // Create notification from request
                        AppNotification requestNotification = new AppNotification(
                                userId,
                                "CATEGORY_REQUEST_STATUS",
                                categoryName,
                                statusText + " - " + (categoryType != null ? categoryType : "Unknown") + " category");
                        requestNotification.setId(requestId);
                        requestNotification.setRead(!"PENDING".equals(status)); // Pending = unread

                        // Store extra data
                        requestNotification.setExtraData(userName + "|" + categoryType + "|" +
                                (reason != null ? reason : "") + "|" + status);

                        categoryRequestsList.add(requestNotification);
                    }

                    updateUIAfterLoad();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CategoryRequests", "FAILED to load user requests: " + e.getMessage(), e);
                    updateUIAfterLoad();
                });
    }

    private void updateUIAfterLoad() {
        loadAllNotificationCounts();

        if (isNotificationPanelOpen && appNotificationAdapter != null) {
            if (isAlertsTabActive) {
                appNotificationAdapter.setNotifications(notificationsList);
                updateNotificationCount();
            } else {
                appNotificationAdapter.setNotifications(categoryRequestsList);
                if (tvNotificationCount != null) {
                    int count = categoryRequestsList.size();
                    tvNotificationCount.setText(count + (count == 1 ? " request" : " requests"));
                }
            }
            checkEmptyState();
        }
    }
}
