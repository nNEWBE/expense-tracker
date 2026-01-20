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
    private MaterialCardView btnMenu, btnNotification, btnCategoryRequests, cardBalance, cardBudget;
    private MaterialCardView btnFilterAll, btnFilterIncome, btnFilterExpense;
    private View cardContainer;

    private List<Expense> allExpenses = new ArrayList<>();
    private String currentFilter = "ALL";

    // Notification panel views
    private View notificationOverlay, notificationDimBackground, notificationPanel;
    private View emptyNotifications;
    private RecyclerView rvNotifications;
    private AppNotificationAdapter appNotificationAdapter;
    private TextView tvNotificationBadge, tvNotificationCount, tvPanelTitle, tvNotificationSubtitle;
    private List<AppNotification> notificationsList = new ArrayList<>();
    private NotificationRepository notificationRepository;

    // Category Request Panel
    private View categoryRequestsOverlay, categoryRequestsDimBackground, categoryRequestsPanel;
    private View emptyCategoryRequests;
    private RecyclerView rvCategoryRequests;
    private AppNotificationAdapter categoryRequestsAdapter;
    private TextView tvCategoryRequestsBadge, tvCategoryRequestCount, tvCategoryPanelTitle, tvCategoryPanelSubtitle;
    private List<AppNotification> categoryRequestsList = new ArrayList<>();

    // State
    private boolean isUserAdmin = false;
    private boolean isNotificationPanelOpen = false;
    private boolean isCategoryPanelOpen = false;

    // Admin status cache
    private boolean isAdminStatusLoaded = false;
    private long adminStatusCacheTime = 0;
    private static final long ADMIN_STATUS_CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

    // Handler for notification animation
    private android.os.Handler notificationHandler;
    private Runnable notificationAnimationRunnable;

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

        // Use activity-scoped ViewModel for shared data caching across fragments
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
                .setDuration(150)
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

        // Cleanup real-time notification listener
        if (notificationRepository != null) {
            notificationRepository.removeListener();
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
        btnCategoryRequests = view.findViewById(R.id.btnCategoryRequests);
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
        tvPanelTitle = view.findViewById(R.id.tvPanelTitle);
        tvNotificationSubtitle = view.findViewById(R.id.tvNotificationSubtitle);

        // Category Request Panel Views
        categoryRequestsOverlay = view.findViewById(R.id.categoryRequestsOverlay);
        categoryRequestsDimBackground = view.findViewById(R.id.categoryRequestsDimBackground);
        categoryRequestsPanel = view.findViewById(R.id.categoryRequestsPanel);
        rvCategoryRequests = view.findViewById(R.id.rvCategoryRequests);
        emptyCategoryRequests = view.findViewById(R.id.emptyCategoryRequests);
        tvCategoryRequestsBadge = view.findViewById(R.id.tvCategoryRequestsBadge);
        tvCategoryRequestCount = view.findViewById(R.id.tvCategoryRequestCount);
        tvCategoryPanelTitle = view.findViewById(R.id.tvCategoryPanelTitle);
        tvCategoryPanelSubtitle = view.findViewById(R.id.tvCategoryPanelSubtitle);

        // Notification tabs removed

        // Initialize notification repository
        notificationRepository = NotificationRepository.getInstance();

        // Setup swipe gesture on card container (critical for UI)
        setupCardSwipeGesture();

        // Setup notification and category panels
        setupPanels(view);

        // DEFER non-critical operations to after view is rendered for instant
        // navigation
        view.post(() -> {
            // Check if user is admin (background operation)
            checkAdminStatus();

            // Load notification counts (background operation)
            loadAllNotificationCounts();

            // Setup real-time listener for notification updates (background operation)
            setupNotificationListener();
        });
    }

    private com.google.firebase.firestore.ListenerRegistration notificationListenerRegistration;

    /**
     * Setup real-time listener for notifications to update badge immediately
     */
    private void setupNotificationListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || notificationRepository == null)
            return;

        // Listen for real-time updates
        notificationRepository.listenToNotifications(new NotificationRepository.OnNotificationsLoadedListener() {
            @Override
            public void onLoaded(List<AppNotification> notifications) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        // Create a map of current local read status to preserve
                        java.util.Map<String, Boolean> localReadStatus = new java.util.HashMap<>();
                        for (AppNotification n : notificationsList) {
                            if (n.getId() != null && n.isRead()) {
                                localReadStatus.put(n.getId(), true);
                            }
                        }

                        // Merge: if locally marked as read, preserve that status
                        for (AppNotification n : notifications) {
                            if (n.getId() != null && localReadStatus.containsKey(n.getId())) {
                                n.setRead(true); // Preserve local read status
                            }
                        }

                        // Update notification count in badge
                        int unreadCount = 0;
                        for (AppNotification n : notifications) {
                            if (!n.isRead())
                                unreadCount++;
                        }
                        updateBadgeDisplay(unreadCount);

                        // Always update our local list with merged data
                        notificationsList = new ArrayList<>(notifications);

                        // If panel show alerts, update adapter
                        if (isNotificationPanelOpen) {
                            if (appNotificationAdapter != null) {
                                appNotificationAdapter.setNotifications(notificationsList);
                            }
                            updateNotificationCount();
                            checkEmptyState();
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("DashboardFragment", "Notification listener error: " + error);
            }
        });
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
            // Guest mode - load from local (instant)
            int count = preferenceManager.getGuestUnreadNotificationCount();
            updateBadgeDisplay(count);
            return;
        }

        if (notificationRepository == null)
            return;

        // OPTIMIZATION: Show cached count IMMEDIATELY for instant UI feedback
        int cachedCount = notificationRepository.getCachedUnreadCountSync();
        if (cachedCount >= 0) {
            updateBadgeDisplay(cachedCount); // Instant display
        }

        // Then fetch fresh count in background
        notificationRepository.getUnreadCount(alertsCount -> {
            // Load category requests count
            loadCategoryRequestsCount(user.getUid(), requestsCount -> {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        updateBadgeDisplay(alertsCount);

                        // Update badges
                        if (tvCategoryRequestsBadge != null) {
                            if (requestsCount > 0) {
                                tvCategoryRequestsBadge.setVisibility(View.VISIBLE);
                                tvCategoryRequestsBadge
                                        .setText(requestsCount > 99 ? "99+" : String.valueOf(requestsCount));
                            } else {
                                tvCategoryRequestsBadge.setVisibility(View.GONE);
                            }
                        }
                    });
                }
            });
        });
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
            // Hide badge for guest users since they can't access notifications
            if (preferenceManager.isGuestMode()) {
                tvNotificationBadge.setVisibility(View.GONE);
                return;
            }
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
        cardBalance.setScaleX(0.9f);
        cardBalance.setScaleY(0.9f);

        // Fast entrance animation - no 3D flip for speed
        android.animation.AnimatorSet animatorSet = new android.animation.AnimatorSet();

        // Fade in
        android.animation.ObjectAnimator fadeIn = android.animation.ObjectAnimator.ofFloat(cardBalance, "alpha", 0f,
                1f);
        fadeIn.setDuration(150);

        // Scale up
        android.animation.ObjectAnimator scaleX = android.animation.ObjectAnimator.ofFloat(cardBalance, "scaleX", 0.9f,
                1f);
        android.animation.ObjectAnimator scaleY = android.animation.ObjectAnimator.ofFloat(cardBalance, "scaleY", 0.9f,
                1f);
        scaleX.setDuration(200);
        scaleY.setDuration(200);

        // Play all together - no delay
        animatorSet.playTogether(fadeIn, scaleX, scaleY);
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

        // RecyclerView performance optimizations
        rvRecentTransactions.setHasFixedSize(true);
        rvRecentTransactions.setItemViewCacheSize(20);
        rvRecentTransactions.setDrawingCacheEnabled(true);
        rvRecentTransactions.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
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
                    // Hide skeleton immediately when data arrives - no artificial delay
                    hideSkeletonLoading(() -> {
                        updateSummary(expenses);
                        updateRecentTransactions();
                        animateBalanceCard();
                    });
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
        animator.setDuration(500);
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
        animator.setDuration(400);
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
        if (tvAll != null)
            tvAll.setTextColor(allSelected ? whiteColor : primaryColor);
        if (iconAll != null)
            iconAll.setColorFilter(allSelected ? whiteColor : primaryColor);

        // Income filter
        boolean incomeSelected = "INCOME".equals(currentFilter);
        btnFilterIncome.setCardBackgroundColor(incomeSelected ? incomeColor : incomeBgColor);
        TextView tvIncome = btnFilterIncome.findViewById(R.id.tvFilterIncome);
        android.widget.ImageView iconIncome = btnFilterIncome.findViewById(R.id.iconFilterIncome);
        if (tvIncome != null)
            tvIncome.setTextColor(incomeSelected ? whiteColor : incomeColor);
        if (iconIncome != null)
            iconIncome.setColorFilter(incomeSelected ? whiteColor : incomeColor);

        // Expense filter
        boolean expenseSelected = "EXPENSE".equals(currentFilter);
        btnFilterExpense.setCardBackgroundColor(expenseSelected ? expenseColor : expenseBgColor);
        TextView tvExpense = btnFilterExpense.findViewById(R.id.tvFilterExpense);
        android.widget.ImageView iconExpense = btnFilterExpense.findViewById(R.id.iconFilterExpense);
        if (tvExpense != null)
            tvExpense.setTextColor(expenseSelected ? whiteColor : expenseColor);
        if (iconExpense != null)
            iconExpense.setColorFilter(expenseSelected ? whiteColor : expenseColor);
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // ===================== NOTIFICATION PANEL METHODS =====================

    private void setupPanels(View view) {
        setupNotificationPanelForAlerts(view);

        // Hide category requests button and panel for guest users
        if (preferenceManager.isGuestMode()) {
            if (btnCategoryRequests != null) {
                btnCategoryRequests.setVisibility(View.GONE);
            }
            if (categoryRequestsOverlay != null) {
                categoryRequestsOverlay.setVisibility(View.GONE);
            }
            // Don't setup category panel for guests
        } else {
            setupCategoryRequestsPanel(view);
        }
    }

    private void setupNotificationPanelForAlerts(View view) {
        appNotificationAdapter = new AppNotificationAdapter();
        appNotificationAdapter.setCurrencySymbol(preferenceManager.getCurrencySymbol());
        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNotifications.setAdapter(appNotificationAdapter);

        appNotificationAdapter
                .setOnNotificationActionListener(new AppNotificationAdapter.OnNotificationActionListener() {
                    @Override
                    public void onDelete(AppNotification notification, int position) {
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser != null) {
                            notificationRepository.deleteNotification(notification.getId(),
                                    new NotificationRepository.OnCompleteListener() {
                                        @Override
                                        public void onSuccess() {
                                            notificationsList.remove(notification);
                                            appNotificationAdapter.removeNotification(position);
                                            updateNotificationCount();
                                            checkEmptyState();
                                            loadAllNotificationCounts();
                                        }

                                        @Override
                                        public void onError(String error) {
                                            if (isAdded())
                                                android.widget.Toast.makeText(requireContext(), "Failed to delete",
                                                        android.widget.Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            preferenceManager.deleteGuestNotification(notification.getId());
                            appNotificationAdapter.removeNotification(position);
                            updateNotificationCount();
                            checkEmptyState();
                            updateNotificationBadge();
                        }
                    }

                    @Override
                    public void onClick(AppNotification notification) {
                        if (!notification.isRead()) {
                            notification.setRead(true);
                            appNotificationAdapter.notifyDataSetChanged();
                            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                            if (currentUser != null) {
                                notificationRepository.markAsRead(notification.getId(), null);
                            } else {
                                preferenceManager.markGuestNotificationRead(notification.getId());
                            }
                            loadAllNotificationCounts();
                        }
                        showNotificationDetailsDialog(notification);
                    }
                });

        View closeBtn = notificationOverlay.findViewById(R.id.btnCloseNotificationsCard);
        if (closeBtn != null)
            closeBtn.setOnClickListener(v -> hidePanels());
        notificationDimBackground.setOnClickListener(v -> hidePanels());
        btnNotification.setOnClickListener(v -> {
            // Guest users cannot access notifications - show login toast
            if (preferenceManager.isGuestMode()) {
                com.example.trackexpense.utils.BeautifulNotification.showInfo(requireActivity(),
                        "Please log in to access Notifications feature.");
                return;
            }
            showNotificationPanel();
        });

        View markAllReadBtn = view.findViewById(R.id.tvMarkAllRead);
        if (markAllReadBtn != null) {
            markAllReadBtn.setOnClickListener(v -> {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    for (AppNotification notification : notificationsList)
                        notification.setRead(true);
                    appNotificationAdapter.notifyDataSetChanged();
                    updateBadgeDisplay(0);
                    notificationRepository.markAllAsRead(null);
                } else {
                    for (AppNotification notification : notificationsList) {
                        notification.setRead(true);
                        preferenceManager.markGuestNotificationRead(notification.getId());
                    }
                    appNotificationAdapter.notifyDataSetChanged();
                    updateBadgeDisplay(0);
                }
            });
        }

        View clearAllBtn = view.findViewById(R.id.btnClearAll);
        if (clearAllBtn != null) {
            clearAllBtn.setOnClickListener(v -> {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    notificationsList.clear();
                    appNotificationAdapter.setNotifications(notificationsList);
                    updateNotificationCount();
                    checkEmptyState();
                    updateBadgeDisplay(0);
                    notificationRepository.deleteAllNotifications(new NotificationRepository.OnCompleteListener() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onError(String error) {
                        }
                    });
                } else {
                    preferenceManager.clearGuestNotifications();
                    notificationsList.clear();
                    appNotificationAdapter.setNotifications(notificationsList);
                    updateNotificationCount();
                    checkEmptyState();
                    updateBadgeDisplay(0);
                }
            });
        }

        loadNotificationsFromFirebase();
    }

    private void setupCategoryRequestsPanel(View view) {
        categoryRequestsAdapter = new AppNotificationAdapter();
        rvCategoryRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCategoryRequests.setAdapter(categoryRequestsAdapter);

        categoryRequestsAdapter
                .setOnNotificationActionListener(new AppNotificationAdapter.OnNotificationActionListener() {
                    @Override
                    public void onDelete(AppNotification notification, int position) {
                        String requestId = notification.getId();
                        if (requestId != null) {
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("category_requests")
                                    .document(requestId)
                                    .delete()
                                    .addOnSuccessListener(v -> {
                                        if (isAdded()) {
                                            categoryRequestsList.remove(position);
                                            categoryRequestsAdapter.removeNotification(position);
                                            int count = categoryRequestsList.size();
                                            if (tvCategoryRequestCount != null) {
                                                tvCategoryRequestCount
                                                        .setText(count + (count == 1 ? " request" : " requests"));
                                            }
                                            checkCategoryEmptyState();
                                            loadAllNotificationCounts();
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onClick(AppNotification notification) {
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
                        showCategoryRequestReviewDialog(notification.getId(), notification.getTitle(), categoryType,
                                userName,
                                reason, status);
                    }
                });

        View closeBtn = categoryRequestsOverlay.findViewById(R.id.btnCloseCategoryRequestCard);
        if (closeBtn != null)
            closeBtn.setOnClickListener(v -> hidePanels());
        categoryRequestsDimBackground.setOnClickListener(v -> hidePanels());

        if (btnCategoryRequests != null) {
            btnCategoryRequests.setOnClickListener(v -> {
                // Guest users cannot access category requests - show login toast
                if (preferenceManager.isGuestMode()) {
                    com.example.trackexpense.utils.BeautifulNotification.showInfo(requireActivity(),
                            "Please log in to access Category Requests feature.");
                    return;
                }
                showCategoryRequestsPanel();
            });
        }
    }

    private void hidePanels() {
        if (isNotificationPanelOpen) {
            isNotificationPanelOpen = false;
            notificationOverlay.animate().alpha(0f).setDuration(200)
                    .withEndAction(() -> notificationOverlay.setVisibility(View.GONE)).start();
            notificationPanel.animate().translationX(notificationPanel.getWidth() + 100).setDuration(200).start();
        }
        if (isCategoryPanelOpen) {
            isCategoryPanelOpen = false;
            categoryRequestsOverlay.animate().alpha(0f).setDuration(200)
                    .withEndAction(() -> categoryRequestsOverlay.setVisibility(View.GONE)).start();
            categoryRequestsPanel.animate().translationX(categoryRequestsPanel.getWidth() + 100).setDuration(200)
                    .start();
        }
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
        if (isNotificationPanelOpen || isCategoryPanelOpen)
            return;
        isNotificationPanelOpen = true;

        notificationOverlay.setVisibility(View.VISIBLE);
        notificationOverlay.setAlpha(0f);
        notificationOverlay.animate().alpha(1f).setDuration(300).start();

        notificationPanel.setTranslationX(1000);
        notificationPanel.animate().translationX(0).setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();

        appNotificationAdapter.setNotifications(notificationsList);
        updateNotificationCount();
        checkEmptyState();

        refreshNotificationsFromFirebase();
    }

    private void showCategoryRequestsPanel() {
        if (isNotificationPanelOpen || isCategoryPanelOpen)
            return;
        isCategoryPanelOpen = true;

        categoryRequestsOverlay.setVisibility(View.VISIBLE);
        categoryRequestsOverlay.setAlpha(0f);
        categoryRequestsOverlay.animate().alpha(1f).setDuration(300).start();

        categoryRequestsPanel.setTranslationX(1000);
        categoryRequestsPanel.animate().translationX(0).setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();

        categoryRequestsAdapter.setNotifications(categoryRequestsList);
        int count = categoryRequestsList.size();
        if (tvCategoryRequestCount != null) {
            tvCategoryRequestCount.setText(count + (count == 1 ? " request" : " requests"));
        }
        checkCategoryEmptyState();

        // Refresh requests
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null)
            loadCategoryRequestsAsNotifications(user.getUid());
    }

    private void checkCategoryEmptyState() {
        if (categoryRequestsList.isEmpty()) {
            if (emptyCategoryRequests != null)
                emptyCategoryRequests.setVisibility(View.VISIBLE);
            if (rvCategoryRequests != null)
                rvCategoryRequests.setVisibility(View.GONE);
        } else {
            if (emptyCategoryRequests != null)
                emptyCategoryRequests.setVisibility(View.GONE);
            if (rvCategoryRequests != null)
                rvCategoryRequests.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Show notification details in a modern dialog.
     * Called when a notification item is clicked.
     */
    private void showNotificationDetailsDialog(AppNotification notification) {
        if (!isAdded())
            return;

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_notification_details, null);
        builder.setView(dialogView);

        android.app.AlertDialog dialog = builder.create();

        // Get views
        android.widget.ImageView ivIcon = dialogView.findViewById(R.id.ivDialogIcon);
        View iconBackground = dialogView.findViewById(R.id.iconBackground);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvType = dialogView.findViewById(R.id.tvDialogType);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        TextView tvAmount = dialogView.findViewById(R.id.tvDialogAmount);
        TextView tvCategory = dialogView.findViewById(R.id.tvDialogCategory);
        TextView tvTime = dialogView.findViewById(R.id.tvDialogTime);
        View amountContainer = dialogView.findViewById(R.id.amountContainer);
        View categoryContainer = dialogView.findViewById(R.id.categoryContainer);
        android.widget.ImageView btnClose = dialogView.findViewById(R.id.btnCloseDialog);
        com.google.android.material.button.MaterialButton btnDelete = dialogView
                .findViewById(R.id.btnDeleteNotification);
        com.google.android.material.button.MaterialButton btnDismiss = dialogView.findViewById(R.id.btnDismissDialog);

        // Set icon and colors
        int colorRes = notification.getColorResource();
        int color = androidx.core.content.ContextCompat.getColor(requireContext(), colorRes);
        if (ivIcon != null) {
            ivIcon.setImageResource(notification.getIconResource());
            ivIcon.setColorFilter(color);
        }
        if (iconBackground != null) {
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            bg.setColor(getNotificationTypeBackgroundColor(notification.getType()));
            iconBackground.setBackground(bg);
        }

        // Set texts
        if (tvTitle != null)
            tvTitle.setText(notification.getTitle());
        if (tvType != null)
            tvType.setText(getNotificationTypeDisplayName(notification.getType()));
        if (tvMessage != null)
            tvMessage.setText(notification.getMessage());

        // Set time
        if (tvTime != null && notification.getCreatedAt() != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy • h:mm a",
                    java.util.Locale.getDefault());
            tvTime.setText(sdf.format(notification.getCreatedAt()));
        }

        // Show amount if present
        if (notification.getAmount() > 0 && amountContainer != null) {
            amountContainer.setVisibility(View.VISIBLE);
            if (tvAmount != null) {
                String symbol = preferenceManager.getCurrencySymbol();
                tvAmount.setText(String.format("%s%,.0f", symbol, notification.getAmount()));
            }
        }

        // Show category if present
        if (notification.getCategory() != null && !notification.getCategory().isEmpty() && categoryContainer != null) {
            categoryContainer.setVisibility(View.VISIBLE);
            if (tvCategory != null)
                tvCategory.setText(notification.getCategory());
        }

        // Close button actions
        if (btnClose != null)
            btnClose.setOnClickListener(v -> dialog.dismiss());
        if (btnDismiss != null)
            btnDismiss.setOnClickListener(v -> dialog.dismiss());

        // Delete button - delete and dismiss
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                dialog.dismiss();
                // Find position and delete
                int position = notificationsList.indexOf(notification);
                if (position >= 0) {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        notificationRepository.deleteNotification(notification.getId(),
                                new NotificationRepository.OnCompleteListener() {
                                    @Override
                                    public void onSuccess() {
                                        if (isAdded()) {
                                            notificationsList.remove(notification);
                                            appNotificationAdapter.setNotifications(notificationsList);
                                            updateNotificationCount();
                                            checkEmptyState();
                                            loadAllNotificationCounts();
                                        }
                                    }

                                    @Override
                                    public void onError(String error) {
                                    }
                                });
                    } else {
                        preferenceManager.deleteGuestNotification(notification.getId());
                        notificationsList.remove(notification);
                        appNotificationAdapter.setNotifications(notificationsList);
                        updateNotificationCount();
                        checkEmptyState();
                        updateNotificationBadge();
                    }
                }
            });
        }

        // Show dialog
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private String getNotificationTypeDisplayName(String type) {
        if (type == null)
            return "Notification";
        switch (type) {
            case AppNotification.TYPE_TRANSACTION_CREATED:
                return "New Transaction";
            case AppNotification.TYPE_TRANSACTION_UPDATED:
                return "Transaction Updated";
            case AppNotification.TYPE_TRANSACTION_DELETED:
                return "Transaction Deleted";
            case AppNotification.TYPE_BUDGET_EXCEEDED:
                return "Budget Alert";
            case AppNotification.TYPE_BUDGET_WARNING:
                return "Budget Warning";
            default:
                return "Notification";
        }
    }

    private int getNotificationTypeBackgroundColor(String type) {
        int colorRes;
        if (type == null) {
            colorRes = R.color.category_other_bg;
        } else {
            switch (type) {
                case AppNotification.TYPE_TRANSACTION_CREATED:
                    colorRes = R.color.category_health_bg;
                    break;
                case AppNotification.TYPE_TRANSACTION_UPDATED:
                    colorRes = R.color.category_transport_bg;
                    break;
                case AppNotification.TYPE_TRANSACTION_DELETED:
                case AppNotification.TYPE_BUDGET_EXCEEDED:
                    colorRes = R.color.category_travel_bg;
                    break;
                case AppNotification.TYPE_BUDGET_WARNING:
                    colorRes = R.color.category_bills_bg;
                    break;
                default:
                    colorRes = R.color.category_other_bg;
            }
        }
        return androidx.core.content.ContextCompat.getColor(requireContext(), colorRes);
    }

    /**
     * Refresh notifications from Firebase and update the UI.
     * Called when the notification panel is opened.
     * 
     * OPTIMIZED: Shows cached data IMMEDIATELY, then refreshes in background.
     * Uses parallel loading for notifications and category requests.
     */
    private void refreshNotificationsFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null && notificationRepository != null) {
            String userId = currentUser.getUid();

            // STEP 1: Show cached notifications IMMEDIATELY
            List<AppNotification> cachedData = notificationRepository.getCachedNotificationsSync();
            if (cachedData != null && !cachedData.isEmpty()) {
                notificationsList = new ArrayList<>(cachedData);
                if (isNotificationPanelOpen && appNotificationAdapter != null) {
                    appNotificationAdapter.setNotifications(notificationsList);
                    updateNotificationCount();
                }
            }

            // STEP 2: Load fresh notifications and category requests in PARALLEL
            final int[] completedLoads = { 0 };
            final int totalLoads = 2;

            Runnable checkAllLoaded = () -> {
                if (completedLoads[0] >= totalLoads) {
                    if (isAdded()) {
                        updateUIAfterLoad();
                    }
                }
            };

            // Load 1: Get notifications
            notificationRepository.getNotifications(new NotificationRepository.OnNotificationsLoadedListener() {
                @Override
                public void onLoaded(List<AppNotification> notifications) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            notificationsList = new ArrayList<>(notifications);
                            completedLoads[0]++;
                            checkAllLoaded.run();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    android.util.Log.e("DashboardFragment", "Error refreshing notifications: " + error);
                    completedLoads[0]++;
                    if (isAdded()) {
                        requireActivity().runOnUiThread(checkAllLoaded);
                    }
                }
            });

            // Load 2: Get category requests
            loadCategoryRequestsAsNotificationsParallel(userId, () -> {
                completedLoads[0]++;
                if (isAdded()) {
                    requireActivity().runOnUiThread(checkAllLoaded);
                }
            });
        } else {
            // Guest user
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
        loadCategoryRequestsAsNotificationsParallel(userId, null);
    }

    /**
     * Load category requests with callback support for parallel loading.
     * Uses cached admin status to avoid repeated Firestore calls.
     * 
     * @param userId     User ID to load requests for
     * @param onComplete Optional callback when loading completes
     */
    private void loadCategoryRequestsAsNotificationsParallel(String userId, Runnable onComplete) {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore
                .getInstance();

        // OPTIMIZATION: Check if admin status is already cached and valid
        boolean isAdminCacheValid = isAdminStatusLoaded &&
                (System.currentTimeMillis() - adminStatusCacheTime) < ADMIN_STATUS_CACHE_DURATION;

        if (isAdminCacheValid) {
            // Use cached admin status - skip the Firestore call for admin check
            android.util.Log.d("DashboardFragment", "Using cached admin status: " + isUserAdmin);
            if (isUserAdmin) {
                loadAdminCategoryRequestsWithCallback(db, onComplete);
            } else {
                loadUserCategoryRequestsWithCallback(db, userId, onComplete);
            }
        } else {
            // First check if user is admin (cache for future use)
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(userDoc -> {
                        Boolean isAdmin = userDoc.getBoolean("isAdmin");
                        isUserAdmin = isAdmin != null && isAdmin;
                        isAdminStatusLoaded = true;
                        adminStatusCacheTime = System.currentTimeMillis();

                        android.util.Log.d("DashboardFragment", "Cached admin status: " + isUserAdmin);

                        if (isUserAdmin) {
                            loadAdminCategoryRequestsWithCallback(db, onComplete);
                        } else {
                            loadUserCategoryRequestsWithCallback(db, userId, onComplete);
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("DashboardFragment", "Error checking admin status", e);
                        if (onComplete != null)
                            onComplete.run();
                    });
        }
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
            appNotificationAdapter.setNotifications(notificationsList);
            updateNotificationCount();
            checkEmptyState();
        }

        if (isCategoryPanelOpen && categoryRequestsAdapter != null) {
            categoryRequestsAdapter.setNotifications(categoryRequestsList);
            int count = categoryRequestsList.size();
            if (tvCategoryRequestCount != null) {
                tvCategoryRequestCount.setText(count + (count == 1 ? " request" : " requests"));
            }
            checkCategoryEmptyState();
        }
    }

    /**
     * Load admin category requests with callback support for parallel loading.
     */
    private void loadAdminCategoryRequestsWithCallback(
            com.google.firebase.firestore.FirebaseFirestore db, Runnable onComplete) {
        android.util.Log.d("CategoryRequests", "Loading admin category requests (parallel)...");
        categoryRequestsList.clear();

        db.collection("category_requests")
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) {
                        if (onComplete != null)
                            onComplete.run();
                        return;
                    }

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
                        requestNotification.setId(requestId);
                        requestNotification.setRead(false);

                        requestNotification.setExtraData(userName + "|" + categoryType + "|" +
                                (reason != null ? reason : "") + "|" + status);

                        categoryRequestsList.add(requestNotification);
                    }

                    if (onComplete != null)
                        onComplete.run();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CategoryRequests", "FAILED to load admin requests: " + e.getMessage(), e);
                    if (onComplete != null)
                        onComplete.run();
                });
    }

    /**
     * Load user category requests with callback support for parallel loading.
     */
    private void loadUserCategoryRequestsWithCallback(
            com.google.firebase.firestore.FirebaseFirestore db, String userId, Runnable onComplete) {
        android.util.Log.d("CategoryRequests", "Loading user category requests (parallel) for: " + userId);
        categoryRequestsList.clear();

        db.collection("category_requests")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded()) {
                        if (onComplete != null)
                            onComplete.run();
                        return;
                    }

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
                        requestNotification.setRead(!"PENDING".equals(status));

                        requestNotification.setExtraData(userName + "|" + categoryType + "|" +
                                (reason != null ? reason : "") + "|" + status);

                        categoryRequestsList.add(requestNotification);
                    }

                    if (onComplete != null)
                        onComplete.run();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CategoryRequests", "FAILED to load user requests: " + e.getMessage(), e);
                    if (onComplete != null)
                        onComplete.run();
                });
    }
}
