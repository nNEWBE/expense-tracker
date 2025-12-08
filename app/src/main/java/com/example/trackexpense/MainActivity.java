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

                androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                        .setEnterAnim(R.anim.nav_fade_in)
                        .setExitAnim(R.anim.nav_fade_out)
                        .setPopEnterAnim(R.anim.nav_pop_enter)
                        .setPopExitAnim(R.anim.nav_pop_exit)
                        .build();
                navController.navigate(R.id.addExpenseFragment, null, navOptions);
            });

            // Set up click listeners for custom nav items - only navigate if not already
            // there, with beautiful page transition animations
            navDashboard.setOnClickListener(v -> {
                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() != R.id.dashboardFragment) {
                    androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                            .setEnterAnim(R.anim.nav_fade_in)
                            .setExitAnim(R.anim.nav_fade_out)
                            .setPopEnterAnim(R.anim.nav_pop_enter)
                            .setPopExitAnim(R.anim.nav_pop_exit)
                            .setPopUpTo(R.id.dashboardFragment, true)
                            .build();
                    navController.navigate(R.id.dashboardFragment, null, navOptions);
                }
            });

            navTransaction.setOnClickListener(v -> {
                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() != R.id.transactionsFragment) {
                    androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                            .setEnterAnim(R.anim.nav_fade_in)
                            .setExitAnim(R.anim.nav_fade_out)
                            .setPopEnterAnim(R.anim.nav_pop_enter)
                            .setPopExitAnim(R.anim.nav_pop_exit)
                            .build();
                    navController.navigate(R.id.transactionsFragment, null, navOptions);
                }
            });

            navAnalytics.setOnClickListener(v -> {
                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() != R.id.analyticsFragment) {
                    androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                            .setEnterAnim(R.anim.nav_fade_in)
                            .setExitAnim(R.anim.nav_fade_out)
                            .setPopEnterAnim(R.anim.nav_pop_enter)
                            .setPopExitAnim(R.anim.nav_pop_exit)
                            .build();
                    navController.navigate(R.id.analyticsFragment, null, navOptions);
                }
            });

            navProfile.setOnClickListener(v -> {
                if (navController.getCurrentDestination() != null &&
                        navController.getCurrentDestination().getId() != R.id.profileFragment) {
                    androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                            .setEnterAnim(R.anim.nav_fade_in)
                            .setExitAnim(R.anim.nav_fade_out)
                            .setPopEnterAnim(R.anim.nav_pop_enter)
                            .setPopExitAnim(R.anim.nav_pop_exit)
                            .build();
                    navController.navigate(R.id.profileFragment, null, navOptions);
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

        if (itemId == R.id.nav_dashboard) {
            navController.navigate(R.id.dashboardFragment);
        } else if (itemId == R.id.nav_analytics) {
            navController.navigate(R.id.transactionsFragment);
        } else if (itemId == R.id.nav_budget) {
            showBudgetDialog();
        } else if (itemId == R.id.nav_export) {
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
        String[] options = { "Export as CSV", "Export as JSON" };

        new MaterialAlertDialogBuilder(this, R.style.Theme_TrackExpense_Dialog)
                .setTitle("📊 Export Data")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        exportAsCSV();
                    } else {
                        exportAsJSON();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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
        String[] categories = {
                "🍔 Food & Dining",
                "🚗 Transportation",
                "🛒 Shopping",
                "🎬 Entertainment",
                "🏥 Health & Fitness",
                "📱 Bills & Utilities",
                "📚 Education",
                "✈️ Travel",
                "🛒 Groceries",
                "📺 Subscriptions",
                "💼 Salary (Income)",
                "💻 Freelance (Income)",
                "📈 Investment (Income)",
                "🎁 Gift (Income)"
        };

        new MaterialAlertDialogBuilder(this, R.style.Theme_TrackExpense_Dialog)
                .setTitle("📂 Categories")
                .setItems(categories, null)
                .setPositiveButton("OK", null)
                .setNeutralButton("Add Custom", (dialog, which) -> {
                    BeautifulNotification.showInfo(this, "Custom categories coming soon!");
                })
                .show();
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
        boolean notificationsEnabled = preferenceManager.isNotificationsEnabled();
        String[] options = { "Daily expense reminder", "Budget alerts", "Weekly summary" };
        boolean[] checked = { notificationsEnabled, true, true };

        new MaterialAlertDialogBuilder(this, R.style.Theme_TrackExpense_Dialog)
                .setTitle("🔔 Notifications")
                .setMultiChoiceItems(options, checked, (dialog, which, isChecked) -> {
                    if (which == 0) {
                        preferenceManager.setNotificationsEnabled(isChecked);
                    }
                })
                .setPositiveButton("Save", (dialog, which) -> {
                    BeautifulNotification.showSuccess(this, "Notification settings saved!");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ==================== THEME ====================
    private void showThemeDialog() {
        String[] themes = { "☀️ Light Mode", "🌙 Dark Mode", "📱 System Default" };
        int currentTheme = preferenceManager.getThemeMode();
        int checkedItem = 2; // default to system

        if (currentTheme == AppCompatDelegate.MODE_NIGHT_NO) {
            checkedItem = 0;
        } else if (currentTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            checkedItem = 1;
        } else if (currentTheme == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            checkedItem = 2;
        }

        new MaterialAlertDialogBuilder(this, R.style.Theme_TrackExpense_Dialog)
                .setTitle("🎨 Choose Theme")
                .setSingleChoiceItems(themes, checkedItem, (dialog, which) -> {
                    int mode;
                    switch (which) {
                        case 0:
                            mode = AppCompatDelegate.MODE_NIGHT_NO;
                            break;
                        case 1:
                            mode = AppCompatDelegate.MODE_NIGHT_YES;
                            break;
                        default:
                            mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                            break;
                    }
                    preferenceManager.setThemeMode(mode);
                    AppCompatDelegate.setDefaultNightMode(mode);
                    dialog.dismiss();
                    BeautifulNotification.showSuccess(this, "Theme updated!");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ==================== BACKUP & RESTORE ====================
    private void showBackupDialog() {
        long lastBackup = preferenceManager.getLastBackupTime();
        String lastBackupText = lastBackup > 0
                ? new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date(lastBackup))
                : "Never";

        new MaterialAlertDialogBuilder(this, R.style.Theme_TrackExpense_Dialog)
                .setTitle("☁️ Backup & Restore")
                .setMessage("Last backup: " + lastBackupText
                        + "\n\nBackup your data to prevent loss. You can restore your data on any device.")
                .setPositiveButton("Backup Now", (dialog, which) -> {
                    // Trigger export as backup
                    exportAsJSON();
                    preferenceManager.setLastBackupTime(System.currentTimeMillis());
                })
                .setNeutralButton("Restore", (dialog, which) -> {
                    BeautifulNotification.showInfo(this, "Restore feature coming soon!");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ==================== HELP & FAQ ====================
    private void showHelpDialog() {
        String helpText = "📌 Quick Tips:\n\n" +
                "• Tap the + button to add expenses or income\n" +
                "• Swipe left on transactions to delete\n" +
                "• Set budget goals to track spending\n" +
                "• Enable notifications for daily reminders\n" +
                "• Export your data regularly for backup\n\n" +
                "📧 Need more help? Contact support!";

        new MaterialAlertDialogBuilder(this, R.style.Theme_TrackExpense_Dialog)
                .setTitle("❓ Help & FAQ")
                .setMessage(helpText)
                .setPositiveButton("Got it!", null)
                .setNeutralButton("Contact Support", (dialog, which) -> sendFeedback())
                .show();
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