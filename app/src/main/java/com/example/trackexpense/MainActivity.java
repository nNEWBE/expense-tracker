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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    // File picker launcher for import
    private ActivityResultLauncher<String[]> filePickerLauncher;

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
        setupFilePicker();
    }

    private void setupFilePicker() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        processImportFile(uri);
                    }
                });
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
        } else if (itemId == R.id.nav_import) {
            showImportDialog();
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

    // ==================== IMPORT DATA ====================
    private void showImportDialog() {
        // Check if guest mode
        if (preferenceManager.isGuestMode()) {
            BeautifulNotification.showWarning(this, "Please log in to import data");
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            BeautifulNotification.showWarning(this, "Please log in to import data");
            return;
        }

        new MaterialAlertDialogBuilder(this, R.style.Theme_TrackExpense_Dialog)
                .setTitle("Import Data")
                .setMessage(
                        "Select a JSON or CSV file containing your transactions.\n\nRequired format:\n• JSON: Array of objects with amount, category, type, notes, date\n• CSV: Headers: amount,category,type,notes,date")
                .setPositiveButton("Select File", (dialog, which) -> {
                    filePickerLauncher.launch(new String[] {
                            "application/json",
                            "text/csv",
                            "text/comma-separated-values",
                            "*/*"
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void processImportFile(Uri uri) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            BeautifulNotification.showError(this, "Please log in to import data");
            return;
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    runOnUiThread(() -> BeautifulNotification.showError(this, "Could not read file"));
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                reader.close();
                inputStream.close();

                String fileContent = content.toString().trim();
                String fileName = getFileNameFromUri(uri);

                // Detect format and parse
                java.util.List<java.util.Map<String, Object>> transactions;
                if (fileName != null && fileName.toLowerCase().endsWith(".csv")) {
                    transactions = parseCSV(fileContent);
                } else {
                    transactions = parseJSON(fileContent);
                }

                if (transactions == null || transactions.isEmpty()) {
                    runOnUiThread(() -> BeautifulNotification.showError(this, "No valid transactions found in file"));
                    return;
                }

                // Import to Firestore
                importTransactionsToFirestore(currentUser.getUid(), transactions);

            } catch (Exception e) {
                android.util.Log.e("Import", "Error importing file", e);
                runOnUiThread(() -> BeautifulNotification.showError(this, "Import failed: " + e.getMessage()));
            }
        });
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private java.util.List<java.util.Map<String, Object>> parseJSON(String content) {
        java.util.List<java.util.Map<String, Object>> transactions = new java.util.ArrayList<>();

        try {
            org.json.JSONArray jsonArray = new org.json.JSONArray(content);

            for (int i = 0; i < jsonArray.length(); i++) {
                org.json.JSONObject obj = jsonArray.getJSONObject(i);
                java.util.Map<String, Object> transaction = new java.util.HashMap<>();

                // Required: amount
                if (!obj.has("amount"))
                    continue;
                double amount = obj.getDouble("amount");
                transaction.put("amount", amount);

                // Required: category
                String category = obj.optString("category", "Other");
                transaction.put("category", category);

                // Required: type (INCOME or EXPENSE)
                String type = obj.optString("type", "EXPENSE").toUpperCase();
                if (!type.equals("INCOME") && !type.equals("EXPENSE")) {
                    type = "EXPENSE";
                }
                transaction.put("type", type);

                // Optional: notes
                String notes = obj.optString("notes", "");
                transaction.put("notes", notes);

                // Optional: date
                String dateStr = obj.optString("date", "");
                long timestamp = parseDate(dateStr);
                transaction.put("date", timestamp);

                transactions.add(transaction);
            }
        } catch (org.json.JSONException e) {
            android.util.Log.e("Import", "JSON parse error", e);
            return null;
        }

        return transactions;
    }

    private java.util.List<java.util.Map<String, Object>> parseCSV(String content) {
        java.util.List<java.util.Map<String, Object>> transactions = new java.util.ArrayList<>();

        String[] lines = content.split("\n");
        if (lines.length < 2)
            return transactions; // Need header + at least 1 row

        // Parse header to find column indices
        String[] headers = lines[0].toLowerCase().split(",");
        int amountIdx = -1, categoryIdx = -1, typeIdx = -1, notesIdx = -1, dateIdx = -1;

        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].trim();
            if (h.equals("amount"))
                amountIdx = i;
            else if (h.equals("category"))
                categoryIdx = i;
            else if (h.equals("type"))
                typeIdx = i;
            else if (h.equals("notes") || h.equals("note"))
                notesIdx = i;
            else if (h.equals("date"))
                dateIdx = i;
        }

        if (amountIdx == -1)
            return transactions; // Amount is required

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty())
                continue;

            String[] values = line.split(",");
            java.util.Map<String, Object> transaction = new java.util.HashMap<>();

            try {
                // Amount
                if (amountIdx >= values.length)
                    continue;
                double amount = Double.parseDouble(values[amountIdx].trim());
                transaction.put("amount", amount);

                // Category
                String category = categoryIdx >= 0 && categoryIdx < values.length ? values[categoryIdx].trim()
                        : "Other";
                if (category.isEmpty())
                    category = "Other";
                transaction.put("category", category);

                // Type
                String type = typeIdx >= 0 && typeIdx < values.length ? values[typeIdx].trim().toUpperCase()
                        : "EXPENSE";
                if (!type.equals("INCOME") && !type.equals("EXPENSE")) {
                    type = "EXPENSE";
                }
                transaction.put("type", type);

                // Notes
                String notes = notesIdx >= 0 && notesIdx < values.length ? values[notesIdx].trim() : "";
                transaction.put("notes", notes);

                // Date
                String dateStr = dateIdx >= 0 && dateIdx < values.length ? values[dateIdx].trim() : "";
                long timestamp = parseDate(dateStr);
                transaction.put("date", timestamp);

                transactions.add(transaction);
            } catch (NumberFormatException e) {
                // Skip invalid rows
            }
        }

        return transactions;
    }

    private long parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return System.currentTimeMillis();
        }

        String[] formats = {
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "MM/dd/yyyy",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss"
        };

        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
                Date date = sdf.parse(dateStr);
                if (date != null)
                    return date.getTime();
            } catch (Exception ignored) {
            }
        }

        return System.currentTimeMillis();
    }

    private void importTransactionsToFirestore(String userId,
            java.util.List<java.util.Map<String, Object>> transactions) {
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore
                .getInstance();

        // Step 1: Check daily import count from user document in Firestore
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    String todayDate = getTodayDateString();
                    int todayImportCount = 0;

                    // Get stored import data
                    String lastImportDate = userDoc.getString("lastImportDate");
                    Long storedCount = userDoc.getLong("dailyImportCount");

                    // Check if it's the same day
                    if (todayDate.equals(lastImportDate) && storedCount != null) {
                        todayImportCount = storedCount.intValue();
                    }

                    // Check daily limit
                    int remainingLimit = 50 - todayImportCount;
                    if (remainingLimit <= 0) {
                        runOnUiThread(() -> BeautifulNotification.showWarning(this,
                                "Daily import limit reached (50/day). Try again tomorrow."));
                        return;
                    }

                    final int currentDayCount = todayImportCount;

                    // Step 2: Fetch ALL existing transactions for duplicate check
                    fetchAllTransactionsForDuplicateCheck(db, userId, transactions, remainingLimit, currentDayCount);
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> BeautifulNotification.showError(this,
                            "Failed to check import limits: " + e.getMessage()));
                });
    }

    private void fetchAllTransactionsForDuplicateCheck(
            com.google.firebase.firestore.FirebaseFirestore db,
            String userId,
            java.util.List<java.util.Map<String, Object>> transactions,
            int remainingLimit,
            int currentDayCount) {

        db.collection("users").document(userId).collection("expenses")
                .get()
                .addOnSuccessListener(allSnapshot -> {
                    // Build complete hash set of all existing transactions
                    java.util.Set<String> existingHashes = new java.util.HashSet<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : allSnapshot) {
                        String hash = buildTransactionHash(doc);
                        if (hash != null) {
                            existingHashes.add(hash);
                        }
                    }

                    // Filter out duplicates
                    java.util.List<java.util.Map<String, Object>> uniqueTransactions = new java.util.ArrayList<>();
                    int duplicateCount = 0;

                    for (java.util.Map<String, Object> transaction : transactions) {
                        String hash = buildTransactionHashFromMap(transaction);
                        if (!existingHashes.contains(hash)) {
                            uniqueTransactions.add(transaction);
                            existingHashes.add(hash); // Prevent duplicates within same import
                        } else {
                            duplicateCount++;
                        }
                    }

                    if (uniqueTransactions.isEmpty()) {
                        final int skipped = duplicateCount;
                        runOnUiThread(() -> BeautifulNotification.showWarning(this,
                                "All " + skipped + " transactions already exist. No new data to import."));
                        return;
                    }

                    // Apply daily limit
                    int toImport = Math.min(uniqueTransactions.size(), remainingLimit);
                    java.util.List<java.util.Map<String, Object>> finalTransactions = uniqueTransactions.subList(0,
                            toImport);

                    final int skippedDuplicates = duplicateCount;
                    final int skippedLimit = uniqueTransactions.size() - toImport;

                    // Proceed with import
                    performBatchImport(db, userId, finalTransactions, skippedDuplicates, skippedLimit, currentDayCount);
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> BeautifulNotification.showError(this,
                            "Failed to check for duplicates: " + e.getMessage()));
                });
    }

    private void performBatchImport(
            com.google.firebase.firestore.FirebaseFirestore db,
            String userId,
            java.util.List<java.util.Map<String, Object>> transactions,
            int skippedDuplicates,
            int skippedLimit,
            int currentDayCount) {

        com.google.firebase.firestore.WriteBatch batch = db.batch();
        long now = System.currentTimeMillis();

        for (java.util.Map<String, Object> transaction : transactions) {
            transaction.put("userId", userId);
            transaction.put("createdAt", now);
            transaction.put("importedAt", now); // Mark as imported

            com.google.firebase.firestore.DocumentReference docRef = db.collection("users").document(userId)
                    .collection("expenses").document();
            batch.set(docRef, transaction);
        }

        final int importedCount = transactions.size();

        batch.commit()
                .addOnSuccessListener(v -> {
                    // Update daily import count in Firestore user document
                    updateDailyImportCount(db, userId, currentDayCount + importedCount);

                    runOnUiThread(() -> {
                        // Build detailed message
                        StringBuilder message = new StringBuilder();
                        message.append("Imported ").append(importedCount).append(" transactions!");

                        if (skippedDuplicates > 0) {
                            message.append("\n").append(skippedDuplicates).append(" duplicates skipped.");
                        }
                        if (skippedLimit > 0) {
                            message.append("\n").append(skippedLimit).append(" skipped (daily limit).");
                        }

                        BeautifulNotification.showSuccess(this, message.toString());
                        sendImportNotification(userId, importedCount);
                    });
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> BeautifulNotification.showError(this,
                            "Import failed: " + e.getMessage()));
                });
    }

    private void updateDailyImportCount(com.google.firebase.firestore.FirebaseFirestore db,
            String userId, int newCount) {
        String todayDate = getTodayDateString();

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("lastImportDate", todayDate);
        updates.put("dailyImportCount", newCount);

        db.collection("users").document(userId)
                .update(updates)
                .addOnFailureListener(e -> {
                    // If update fails (field doesn't exist), try set with merge
                    db.collection("users").document(userId)
                            .set(updates, com.google.firebase.firestore.SetOptions.merge());
                });
    }

    private String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String buildTransactionHash(com.google.firebase.firestore.QueryDocumentSnapshot doc) {
        try {
            Double amount = doc.getDouble("amount");
            String category = doc.getString("category");
            String type = doc.getString("type");
            Long date = doc.getLong("date");
            String notes = doc.getString("notes");

            if (amount == null)
                return null;

            return String.format(Locale.US, "%.2f|%s|%s|%d|%s",
                    amount,
                    category != null ? category : "",
                    type != null ? type : "",
                    date != null ? date : 0L,
                    notes != null ? notes.trim().toLowerCase() : "");
        } catch (Exception e) {
            return null;
        }
    }

    private String buildTransactionHashFromMap(java.util.Map<String, Object> transaction) {
        try {
            Object amountObj = transaction.get("amount");
            double amount = amountObj instanceof Number ? ((Number) amountObj).doubleValue() : 0;
            String category = (String) transaction.getOrDefault("category", "");
            String type = (String) transaction.getOrDefault("type", "");
            Object dateObj = transaction.get("date");
            long date = dateObj instanceof Number ? ((Number) dateObj).longValue() : 0L;
            String notes = (String) transaction.getOrDefault("notes", "");

            return String.format(Locale.US, "%.2f|%s|%s|%d|%s",
                    amount, category, type, date,
                    notes != null ? notes.trim().toLowerCase() : "");
        } catch (Exception e) {
            return "";
        }
    }

    private void sendImportNotification(String userId, int count) {
        java.util.Map<String, Object> notification = new java.util.HashMap<>();
        notification.put("userId", userId);
        notification.put("type", "DATA_IMPORT");
        notification.put("title", "Data Import Complete");
        notification.put("message", "Successfully imported " + count + " transactions from file.");
        notification.put("amount", 0);
        notification.put("category", "");
        notification.put("transactionType", "");
        notification.put("isRead", false);
        notification.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("notifications")
                .add(notification)
                .addOnSuccessListener(docRef -> {
                    android.util.Log.d("Import", "Import notification sent");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("Import", "Failed to send notification", e);
                });
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

        // Check if user is admin - show request button only for non-admin
        // cardRequest is hidden by default in XML
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        Boolean isAdmin = documentSnapshot.getBoolean("isAdmin");
                        if (isAdmin == null || !isAdmin) {
                            // Show request card for non-admin users
                            cardRequest.setVisibility(View.VISIBLE);
                        }
                        // For admin, card stays GONE (default in XML)
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

        // Check if user already has a pending request
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("category_requests")
                .whereEqualTo("userId", user.getUid())
                .whereEqualTo("status", "PENDING")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // User already has a pending request
                        BeautifulNotification.showWarning(this,
                                "You already have a pending request. Please wait for admin review.");
                        return;
                    }

                    // No pending request, proceed with submission
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
                                android.util.Log.d("CategoryRequest",
                                        "Request saved with ID: " + documentReference.getId());
                                BeautifulNotification.showSuccess(this, "Request submitted! Admin will review it.");
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("CategoryRequest", "Failed to save request", e);
                                BeautifulNotification.showError(this, "Failed to submit request: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    BeautifulNotification.showError(this, "Failed to check pending requests");
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