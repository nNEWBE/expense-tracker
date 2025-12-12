package com.example.trackexpense;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.data.local.AppDatabase;
import com.example.trackexpense.utils.BeautifulNotification;
import com.example.trackexpense.utils.PreferenceManager;
import com.example.trackexpense.utils.ReminderWorker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private NavController navController;
    private FloatingActionButton fabAdd;
    private View bottomNavContainer;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private PreferenceManager preferenceManager;

    // Custom bottom nav views
    private View navDashboard, navTransaction, navAnalytics, navProfile;
    private ImageView icDashboard, icTransaction, icAnalytics, icProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferenceManager = new PreferenceManager(this);

        // Apply theme
        applyTheme();

        // Enable immersive fullscreen
        enableImmersiveMode();

        setContentView(R.layout.activity_main);

        setupDrawer();
        setupNavigation();
        setupDrawerHeader();
        checkNotificationPermission();
        scheduleDailyReminder();
        setupBackPressHandler();
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Close drawer if open
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return;
                }

                // Check if we're on a main destination
                if (navController != null) {
                    int currentDest = navController.getCurrentDestination() != null
                            ? navController.getCurrentDestination().getId()
                            : 0;

                    // Main destinations where we should show exit dialog
                    if (currentDest == R.id.dashboardFragment ||
                            currentDest == R.id.transactionsFragment ||
                            currentDest == R.id.analyticsFragment ||
                            currentDest == R.id.profileFragment) {
                        showExitConfirmation();
                    } else {
                        // Navigate back normally for other screens
                        navController.popBackStack();
                    }
                }
            }
        });
    }

    private void showExitConfirmation() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(
                this, R.style.BottomSheetDialogTheme);

        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_exit, null);
        bottomSheetDialog.setContentView(sheetView);

        // Make bottom sheet background transparent
        if (bottomSheetDialog.getWindow() != null) {
            bottomSheetDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        com.google.android.material.button.MaterialButton btnCancel = sheetView.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnExit = sheetView.findViewById(R.id.btnExit);

        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        btnExit.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            finishAffinity();
        });

        bottomSheetDialog.show();
    }

    private void enableImmersiveMode() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableImmersiveMode();
        }
    }

    private void applyTheme() {
        int themeMode = preferenceManager.getThemeMode();
        if (themeMode != -1) {
            AppCompatDelegate.setDefaultNightMode(themeMode);
        }
    }

    private void setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Disable swipe gesture - drawer only opens via menu button
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        // Improve drawer scrolling performance
        drawerLayout.setDrawerElevation(0f);

        // Add smooth drawer listener to reduce frame drops
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                // Reduce overdraw during animation
                super.onDrawerSlide(drawerView, slideOffset);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                // Refresh header data when drawer opens
                setupDrawerHeader();
            }
        });
    }

    private void setupDrawerHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView tvUserName = headerView.findViewById(R.id.tvDrawerUserName);
        TextView tvUserEmail = headerView.findViewById(R.id.tvDrawerUserEmail);
        ImageView ivSettings = headerView.findViewById(R.id.ivDrawerUserSettings);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            String email = user.getEmail();

            tvUserName.setText(name != null && !name.isEmpty() ? name : "User");
            tvUserEmail.setText(email != null ? email : "");
        } else if (preferenceManager.isGuestMode()) {
            tvUserName.setText("Guest User");
            tvUserEmail.setText("Sign in for more features");
        }

        // Settings icon click
        ivSettings.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            navController.navigate(R.id.profileFragment);
        });
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            fabAdd = findViewById(R.id.fab_add);
            bottomNavContainer = findViewById(R.id.bottom_nav_container);

            // Initialize custom nav views
            navDashboard = findViewById(R.id.nav_dashboard);
            navTransaction = findViewById(R.id.nav_transaction);
            navAnalytics = findViewById(R.id.nav_analytics);
            navProfile = findViewById(R.id.nav_profile);

            icDashboard = findViewById(R.id.ic_dashboard);
            icTransaction = findViewById(R.id.ic_transaction);
            icAnalytics = findViewById(R.id.ic_analytics);
            icProfile = findViewById(R.id.ic_profile);

            // Handle center FAB click to navigate to add expense
            fabAdd.setOnClickListener(v -> {
                // Don't navigate if already on Add page
                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() == R.id.addExpenseFragment) {
                    return;
                }

                navController.navigate(R.id.addExpenseFragment);
            });

            // Set up click listeners for custom nav items - only navigate if not already
            // there
            navDashboard.setOnClickListener(v -> {
                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() != R.id.dashboardFragment) {
                    androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.dashboardFragment, true)
                            .build();
                    navController.navigate(R.id.dashboardFragment, null, navOptions);
                }
            });

            navTransaction.setOnClickListener(v -> {
                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() != R.id.transactionsFragment) {
                    navController.navigate(R.id.transactionsFragment);
                }
            });

            navAnalytics.setOnClickListener(v -> {
                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() != R.id.analyticsFragment) {
                    navController.navigate(R.id.analyticsFragment);
                }
            });

            navProfile.setOnClickListener(v -> {
                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() != R.id.profileFragment) {
                    navController.navigate(R.id.profileFragment);
                }
            });

            // Listen for destination changes to update indicators
            // Listen for destination changes to update indicators
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destId = destination.getId();
                // Bottom nav stays visible on all pages
                updateNavIndicators(destId);
            });

            // Set initial state
            updateNavIndicators(R.id.dashboardFragment);
        }
    }

    private void updateNavIndicators(int selectedId) {
        int activeColor = ContextCompat.getColor(this, R.color.nav_active_color);
        int inactiveColor = ContextCompat.getColor(this, R.color.nav_inactive_color);

        // Reset all icons to inactive color
        icDashboard.setColorFilter(inactiveColor);
        icTransaction.setColorFilter(inactiveColor);
        icAnalytics.setColorFilter(inactiveColor);
        icProfile.setColorFilter(inactiveColor);

        // Set active icon color based on selected destination
        if (selectedId == R.id.dashboardFragment) {
            icDashboard.setColorFilter(activeColor);
        } else if (selectedId == R.id.transactionsFragment) {
            icTransaction.setColorFilter(activeColor);
        } else if (selectedId == R.id.analyticsFragment) {
            icAnalytics.setColorFilter(activeColor);
        } else if (selectedId == R.id.profileFragment) {
            icProfile.setColorFilter(activeColor);
        }
    }

    public void openDrawer() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_export) {
            showExportDialog();
        } else if (itemId == R.id.nav_categories) {
            showCategoriesDialog();
        } else if (itemId == R.id.nav_recurring) {
            showRecurringDialog();
        } else if (itemId == R.id.nav_notifications) {
            showNotificationsDialog();
        } else if (itemId == R.id.nav_theme) {
            showThemeDialog();
        } else if (itemId == R.id.nav_backup) {
            showBackupDialog();
        } else if (itemId == R.id.nav_help) {
            showHelpDialog();
        } else if (itemId == R.id.nav_feedback) {
            sendFeedback();
        } else if (itemId == R.id.nav_rate) {
            rateApp();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // ==================== BUDGET GOALS ====================
    private void showBudgetDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_budget_goals, null);
        TextInputEditText etMonthlyBudget = dialogView.findViewById(R.id.etMonthlyBudget);
        TextInputEditText etWeeklyBudget = dialogView.findViewById(R.id.etWeeklyBudget);

        // Load existing values
        double monthlyBudget = preferenceManager.getMonthlyBudget();
        double weeklyBudget = preferenceManager.getWeeklyBudget();

        if (monthlyBudget > 0)
            etMonthlyBudget.setText(String.valueOf(monthlyBudget));
        if (weeklyBudget > 0)
            etWeeklyBudget.setText(String.valueOf(weeklyBudget));

        new MaterialAlertDialogBuilder(this, R.style.Theme_TrackExpense_Dialog)
                .setTitle("💰 Budget Goals")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        String monthlyStr = etMonthlyBudget.getText().toString();
                        String weeklyStr = etWeeklyBudget.getText().toString();

                        if (!monthlyStr.isEmpty()) {
                            preferenceManager.setMonthlyBudget(Double.parseDouble(monthlyStr));
                        }
                        if (!weeklyStr.isEmpty()) {
                            preferenceManager.setWeeklyBudget(Double.parseDouble(weeklyStr));
                        }
                        BeautifulNotification.showSuccess(this, "Budget goals saved!");
                    } catch (NumberFormatException e) {
                        BeautifulNotification.showError(this, "Invalid budget amount");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ==================== EXPORT DATA ====================
    private void showExportDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_export, null);

        com.google.android.material.card.MaterialCardView cardCSV = dialogView.findViewById(R.id.cardCSV);
        com.google.android.material.card.MaterialCardView cardJSON = dialogView.findViewById(R.id.cardJSON);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        cardCSV.setOnClickListener(v -> {
            dialog.dismiss();
            exportAsCSV();
        });

        cardJSON.setOnClickListener(v -> {
            dialog.dismiss();
            exportAsJSON();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private void exportAsCSV() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Expense> expenses = AppDatabase.getDatabase(this).expenseDao().getAllExpensesSync();

            StringBuilder csv = new StringBuilder();
            csv.append("Date,Type,Category,Amount,Note\n");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            for (Expense e : expenses) {
                csv.append(sdf.format(new Date(e.getDate()))).append(",");
                csv.append(e.getType()).append(",");
                csv.append(e.getCategory()).append(",");
                csv.append(e.getAmount()).append(",");
                csv.append(e.getNotes() != null ? e.getNotes().replace(",", ";") : "").append("\n");
            }

            saveToFile(csv.toString(), "expenses_" + System.currentTimeMillis() + ".csv");
        });
    }

    private void exportAsJSON() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Expense> expenses = AppDatabase.getDatabase(this).expenseDao().getAllExpensesSync();

            StringBuilder json = new StringBuilder();
            json.append("[\n");

            for (int i = 0; i < expenses.size(); i++) {
                Expense e = expenses.get(i);
                json.append("  {\n");
                json.append("    \"date\": ").append(e.getDate()).append(",\n");
                json.append("    \"type\": \"").append(e.getType()).append("\",\n");
                json.append("    \"category\": \"").append(e.getCategory()).append("\",\n");
                json.append("    \"amount\": ").append(e.getAmount()).append(",\n");
                json.append("    \"note\": \"").append(e.getNotes() != null ? e.getNotes() : "").append("\"\n");
                json.append("  }").append(i < expenses.size() - 1 ? "," : "").append("\n");
            }

            json.append("]");
            saveToFile(json.toString(), "expenses_" + System.currentTimeMillis() + ".json");
        });
    }

    private void saveToFile(String content, String filename) {
        try {
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadDir, filename);
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();

            runOnUiThread(() -> BeautifulNotification.showSuccess(this, "Exported to Downloads/" + filename));
        } catch (IOException e) {
            runOnUiThread(() -> BeautifulNotification.showError(this, "Export failed: " + e.getMessage()));
        }
    }

    // ==================== CATEGORIES ====================
    private void showCategoriesDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_categories, null);

        com.google.android.material.card.MaterialCardView tabExpense = dialogView.findViewById(R.id.tabExpense);
        com.google.android.material.card.MaterialCardView tabIncome = dialogView.findViewById(R.id.tabIncome);
        TextView tvExpenseTab = dialogView.findViewById(R.id.tvExpenseTab);
        TextView tvIncomeTab = dialogView.findViewById(R.id.tvIncomeTab);
        ImageView icExpenseTab = dialogView.findViewById(R.id.icExpenseTab);
        ImageView icIncomeTab = dialogView.findViewById(R.id.icIncomeTab);
        RecyclerView rvCategories = dialogView.findViewById(R.id.rvCategories);
        com.google.android.material.card.MaterialCardView cardRequest = dialogView.findViewById(R.id.cardRequest);
        com.google.android.material.button.MaterialButton btnClose = dialogView.findViewById(R.id.btnClose);

        // Setup RecyclerView
        com.example.trackexpense.ui.dialog.CategoryListAdapter adapter = new com.example.trackexpense.ui.dialog.CategoryListAdapter(
                this);
        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        rvCategories.setAdapter(adapter);

        // Initial state - show expense categories
        boolean[] isExpenseTab = { true };
        adapter.setCategories(java.util.Arrays.asList(
                com.example.trackexpense.utils.CategoryHelper.EXPENSE_CATEGORIES));

        // Tab click listeners
        tabExpense.setOnClickListener(v -> {
            if (!isExpenseTab[0]) {
                isExpenseTab[0] = true;
                // Update tab styles
                tabExpense.setCardBackgroundColor(ContextCompat.getColor(this, R.color.expense_red_light));
                tabExpense.setStrokeColor(ContextCompat.getColor(this, R.color.expense_red));
                tvExpenseTab.setTextColor(ContextCompat.getColor(this, R.color.expense_red));
                icExpenseTab.setColorFilter(ContextCompat.getColor(this, R.color.expense_red));

                tabIncome.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_50));
                tabIncome.setStrokeColor(ContextCompat.getColor(this, R.color.gray_200));
                tvIncomeTab.setTextColor(ContextCompat.getColor(this, R.color.gray_500));
                icIncomeTab.setColorFilter(ContextCompat.getColor(this, R.color.gray_500));

                adapter.setCategories(java.util.Arrays.asList(
                        com.example.trackexpense.utils.CategoryHelper.EXPENSE_CATEGORIES));
            }
        });

        tabIncome.setOnClickListener(v -> {
            if (isExpenseTab[0]) {
                isExpenseTab[0] = false;
                // Update tab styles
                tabIncome.setCardBackgroundColor(ContextCompat.getColor(this, R.color.green_50));
                tabIncome.setStrokeColor(ContextCompat.getColor(this, R.color.income_green));
                tvIncomeTab.setTextColor(ContextCompat.getColor(this, R.color.income_green));
                icIncomeTab.setColorFilter(ContextCompat.getColor(this, R.color.income_green));

                tabExpense.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_50));
                tabExpense.setStrokeColor(ContextCompat.getColor(this, R.color.gray_200));
                tvExpenseTab.setTextColor(ContextCompat.getColor(this, R.color.gray_500));
                icExpenseTab.setColorFilter(ContextCompat.getColor(this, R.color.gray_500));

                adapter.setCategories(java.util.Arrays.asList(
                        com.example.trackexpense.utils.CategoryHelper.INCOME_CATEGORIES));
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        // Check if user is admin - hide request button for admin
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Boolean isAdmin = documentSnapshot.getBoolean("isAdmin");
                        if (isAdmin != null && isAdmin) {
                            // Hide request card for admin
                            cardRequest.setVisibility(View.GONE);
                        }
                    });
        }

        // Request new category - check for guest mode
        cardRequest.setOnClickListener(v -> {
            if (preferenceManager.isGuestMode()) {
                // Guest users cannot submit requests
                BeautifulNotification.showWarning(this, "Please log in to request new categories");
            } else {
                dialog.dismiss();
                showRequestCategoryDialog();
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private void showRequestCategoryDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_request_category, null);

        com.google.android.material.textfield.TextInputLayout tilCategoryName = dialogView
                .findViewById(R.id.tilCategoryName);
        com.google.android.material.textfield.TextInputEditText etCategoryName = dialogView
                .findViewById(R.id.etCategoryName);
        com.google.android.material.textfield.TextInputEditText etReason = dialogView.findViewById(R.id.etReason);
        com.google.android.material.card.MaterialCardView cardTypeExpense = dialogView
                .findViewById(R.id.cardTypeExpense);
        com.google.android.material.card.MaterialCardView cardTypeIncome = dialogView.findViewById(R.id.cardTypeIncome);
        ImageView ivExpenseType = dialogView.findViewById(R.id.ivExpenseType);
        ImageView ivIncomeType = dialogView.findViewById(R.id.ivIncomeType);
        TextView tvExpenseType = dialogView.findViewById(R.id.tvExpenseType);
        TextView tvIncomeType = dialogView.findViewById(R.id.tvIncomeType);
        ImageView checkExpense = dialogView.findViewById(R.id.checkExpense);
        ImageView checkIncome = dialogView.findViewById(R.id.checkIncome);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnSubmit = dialogView.findViewById(R.id.btnSubmit);

        String[] selectedType = { "EXPENSE" };

        // Type selection listeners
        cardTypeExpense.setOnClickListener(v -> {
            selectedType[0] = "EXPENSE";
            cardTypeExpense.setCardBackgroundColor(ContextCompat.getColor(this, R.color.expense_red_light));
            cardTypeExpense.setStrokeColor(ContextCompat.getColor(this, R.color.expense_red));
            cardTypeExpense.setStrokeWidth(4);
            ivExpenseType.setColorFilter(ContextCompat.getColor(this, R.color.expense_red));
            tvExpenseType.setTextColor(ContextCompat.getColor(this, R.color.expense_red));
            checkExpense.setVisibility(View.VISIBLE);

            cardTypeIncome.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_50));
            cardTypeIncome.setStrokeColor(ContextCompat.getColor(this, R.color.gray_200));
            cardTypeIncome.setStrokeWidth(3);
            ivIncomeType.setColorFilter(ContextCompat.getColor(this, R.color.gray_400));
            tvIncomeType.setTextColor(ContextCompat.getColor(this, R.color.gray_500));
            checkIncome.setVisibility(View.GONE);
        });

        cardTypeIncome.setOnClickListener(v -> {
            selectedType[0] = "INCOME";
            cardTypeIncome.setCardBackgroundColor(ContextCompat.getColor(this, R.color.green_50));
            cardTypeIncome.setStrokeColor(ContextCompat.getColor(this, R.color.income_green));
            cardTypeIncome.setStrokeWidth(4);
            ivIncomeType.setColorFilter(ContextCompat.getColor(this, R.color.income_green));
            tvIncomeType.setTextColor(ContextCompat.getColor(this, R.color.income_green));
            checkIncome.setVisibility(View.VISIBLE);

            cardTypeExpense.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_50));
            cardTypeExpense.setStrokeColor(ContextCompat.getColor(this, R.color.gray_200));
            cardTypeExpense.setStrokeWidth(3);
            ivExpenseType.setColorFilter(ContextCompat.getColor(this, R.color.gray_400));
            tvExpenseType.setTextColor(ContextCompat.getColor(this, R.color.gray_500));
            checkExpense.setVisibility(View.GONE);
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String categoryName = etCategoryName.getText() != null ? etCategoryName.getText().toString().trim() : "";
            String reason = etReason.getText() != null ? etReason.getText().toString().trim() : "";

            if (categoryName.isEmpty()) {
                tilCategoryName.setError("Please enter a category name");
                return;
            }

            // Submit request to Firestore
            submitCategoryRequest(categoryName, selectedType[0], reason);
            dialog.dismiss();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private void submitCategoryRequest(String categoryName, String type, String reason) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            BeautifulNotification.showError(this, "Please log in to submit requests");
            return;
        }

        // Use HashMap for direct Firestore serialization
        java.util.Map<String, Object> request = new java.util.HashMap<>();
        request.put("userId", user.getUid());
        request.put("userName", user.getDisplayName() != null ? user.getDisplayName() : "User");
        request.put("userEmail", user.getEmail() != null ? user.getEmail() : "");
        request.put("categoryName", categoryName);
        request.put("categoryType", type);
        request.put("reason", reason);
        request.put("status", "PENDING");
        request.put("createdAt", System.currentTimeMillis());
        request.put("updatedAt", System.currentTimeMillis());

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("category_requests")
                .add(request)
                .addOnSuccessListener(documentReference -> {
                    android.util.Log.d("CategoryRequest", "Request saved with ID: " + documentReference.getId());
                    // Also create a notification for admin
                    sendAdminNotification(categoryName, type,
                            user.getDisplayName() != null ? user.getDisplayName() : "User");
                    BeautifulNotification.showSuccess(this, "Request submitted! Admin will review it.");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("CategoryRequest", "Failed to save request", e);
                    BeautifulNotification.showError(this, "Failed to submit request: " + e.getMessage());
                });
    }

    private void sendAdminNotification(String categoryName, String type, String userName) {
        // Query all admin users and send notification to their notifications
        // subcollection
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore
                .getInstance();

        db.collection("users")
                .whereEqualTo("isAdmin", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot adminDoc : querySnapshot) {
                        String adminId = adminDoc.getId();

                        // Create notification matching AppNotification model structure
                        java.util.Map<String, Object> notification = new java.util.HashMap<>();
                        notification.put("type", "CATEGORY_REQUEST");
                        notification.put("title", "📂 New Category Request");
                        notification.put("message", userName + " requested a new " + type.toLowerCase()
                                + " category: \"" + categoryName + "\"");
                        notification.put("isRead", false);
                        notification.put("read", false);
                        notification.put("createdAt", new java.util.Date());
                        notification.put("userId", adminId);
                        notification.put("iconResource", com.example.trackexpense.R.drawable.ic_nav_categories);
                        notification.put("colorResource", com.example.trackexpense.R.color.warning_yellow);

                        // Send to admin's notifications subcollection
                        db.collection("users")
                                .document(adminId)
                                .collection("notifications")
                                .add(notification)
                                .addOnSuccessListener(ref -> {
                                    android.util.Log.d("AdminNotification", "Notification sent to admin: " + adminId);
                                })
                                .addOnFailureListener(e -> {
                                    android.util.Log.e("AdminNotification",
                                            "Failed to send notification to admin: " + adminId, e);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("AdminNotification", "Failed to query admin users", e);
                });
    }

    // ==================== RECURRING EXPENSES ====================
    private void showRecurringDialog() {
        new MaterialAlertDialogBuilder(this, R.style.Theme_TrackExpense_Dialog)
                .setTitle("🔄 Recurring Expenses")
                .setMessage(
                        "Set up automatic recurring expenses like rent, subscriptions, and bills.\n\nThis feature will allow you to:\n• Add monthly recurring expenses\n• Add weekly recurring expenses\n• Get reminders before due dates")
                .setPositiveButton("Set Up", (dialog, which) -> {
                    BeautifulNotification.showInfo(this, "Recurring expenses feature coming soon!");
                })
                .setNegativeButton("Later", null)
                .show();
    }

    // ==================== NOTIFICATIONS ====================
    private void showNotificationsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_notifications, null);

        com.google.android.material.switchmaterial.SwitchMaterial switchDaily = dialogView
                .findViewById(R.id.switchDaily);
        com.google.android.material.switchmaterial.SwitchMaterial switchBudget = dialogView
                .findViewById(R.id.switchBudget);
        com.google.android.material.switchmaterial.SwitchMaterial switchWeekly = dialogView
                .findViewById(R.id.switchWeekly);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);

        // Load current preferences
        switchDaily.setChecked(preferenceManager.isNotificationsEnabled());

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            preferenceManager.setNotificationsEnabled(switchDaily.isChecked());
            BeautifulNotification.showSuccess(this, "Notification settings saved!");
            dialog.dismiss();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    // ==================== THEME ====================
    private void showThemeDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_theme, null);

        com.google.android.material.card.MaterialCardView cardLight = dialogView.findViewById(R.id.cardLight);
        com.google.android.material.card.MaterialCardView cardDark = dialogView.findViewById(R.id.cardDark);
        com.google.android.material.card.MaterialCardView cardSystem = dialogView.findViewById(R.id.cardSystem);
        ImageView checkLight = dialogView.findViewById(R.id.checkLight);
        ImageView checkDark = dialogView.findViewById(R.id.checkDark);
        ImageView checkSystem = dialogView.findViewById(R.id.checkSystem);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnApply = dialogView.findViewById(R.id.btnApply);

        int[] selectedTheme = { preferenceManager.getThemeMode() };

        Runnable updateSelection = () -> {
            int gray = ContextCompat.getColor(this, R.color.gray_200);
            int primary = ContextCompat.getColor(this, R.color.primary);

            cardLight.setStrokeColor(gray);
            cardDark.setStrokeColor(gray);
            cardSystem.setStrokeColor(gray);
            checkLight.setVisibility(View.GONE);
            checkDark.setVisibility(View.GONE);
            checkSystem.setVisibility(View.GONE);

            if (selectedTheme[0] == AppCompatDelegate.MODE_NIGHT_NO) {
                cardLight.setStrokeColor(primary);
                checkLight.setVisibility(View.VISIBLE);
            } else if (selectedTheme[0] == AppCompatDelegate.MODE_NIGHT_YES) {
                cardDark.setStrokeColor(primary);
                checkDark.setVisibility(View.VISIBLE);
            } else {
                cardSystem.setStrokeColor(primary);
                checkSystem.setVisibility(View.VISIBLE);
            }
        };
        updateSelection.run();

        cardLight.setOnClickListener(v -> {
            selectedTheme[0] = AppCompatDelegate.MODE_NIGHT_NO;
            updateSelection.run();
        });

        cardDark.setOnClickListener(v -> {
            selectedTheme[0] = AppCompatDelegate.MODE_NIGHT_YES;
            updateSelection.run();
        });

        cardSystem.setOnClickListener(v -> {
            selectedTheme[0] = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            updateSelection.run();
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnApply.setOnClickListener(v -> {
            preferenceManager.setThemeMode(selectedTheme[0]);
            AppCompatDelegate.setDefaultNightMode(selectedTheme[0]);
            BeautifulNotification.showSuccess(this, "Theme updated!");
            dialog.dismiss();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    // ==================== BACKUP & RESTORE ====================
    private void showBackupDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_backup, null);

        TextView tvLastBackup = dialogView.findViewById(R.id.tvLastBackup);
        com.google.android.material.card.MaterialCardView cardBackup = dialogView.findViewById(R.id.cardBackup);
        com.google.android.material.card.MaterialCardView cardRestore = dialogView.findViewById(R.id.cardRestore);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        long lastBackup = preferenceManager.getLastBackupTime();
        String lastBackupText = lastBackup > 0
                ? new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date(lastBackup))
                : "Never";
        tvLastBackup.setText(lastBackupText);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        cardBackup.setOnClickListener(v -> {
            dialog.dismiss();
            exportAsJSON();
            preferenceManager.setLastBackupTime(System.currentTimeMillis());
        });

        cardRestore.setOnClickListener(v -> {
            dialog.dismiss();
            BeautifulNotification.showInfo(this, "Restore feature coming soon!");
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    // ==================== HELP & FAQ ====================
    private void showHelpDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_help, null);

        com.google.android.material.button.MaterialButton btnClose = dialogView.findViewById(R.id.btnClose);
        com.google.android.material.button.MaterialButton btnContact = dialogView.findViewById(R.id.btnContact);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnContact.setOnClickListener(v -> {
            dialog.dismiss();
            sendFeedback();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    // ==================== FEEDBACK ====================
    private void sendFeedback() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:support@trackexpense.app"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "TrackExpense Feedback");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Hi TrackExpense Team,\n\n");

        try {
            startActivity(Intent.createChooser(emailIntent, "Send Feedback"));
        } catch (Exception e) {
            BeautifulNotification.showError(this, "No email app found");
        }
    }

    // ==================== RATE APP ====================
    private void rateApp() {
        new MaterialAlertDialogBuilder(this, R.style.Theme_TrackExpense_Dialog)
                .setTitle("⭐ Rate TrackExpense")
                .setMessage(
                        "Enjoying TrackExpense? Please take a moment to rate us on the Play Store. Your feedback helps us improve!")
                .setPositiveButton("Rate Now", (dialog, which) -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=" + getPackageName())));
                    } catch (Exception e) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
                    }
                })
                .setNeutralButton("Later", null)
                .setNegativeButton("No Thanks", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void scheduleDailyReminder() {
        if (!preferenceManager.isNotificationsEnabled())
            return;

        PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest.Builder(
                ReminderWorker.class, 24, TimeUnit.HOURS)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "daily_expense_reminder",
                ExistingPeriodicWorkPolicy.KEEP,
                reminderRequest);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.POST_NOTIFICATIONS }, 101);
            }
        }
    }
}