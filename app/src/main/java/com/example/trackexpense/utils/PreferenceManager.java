package com.example.trackexpense.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {

    private static final String PREF_NAME = "expense_tracker_prefs";
    private static final String KEY_CURRENCY = "currency";
    private static final String KEY_MONTHLY_BUDGET = "monthly_budget";
    private static final String KEY_WEEKLY_BUDGET = "weekly_budget";
    private static final String KEY_GUEST_MODE = "guest_mode";
    private static final String KEY_APP_LOCK_ENABLED = "app_lock_enabled";
    private static final String KEY_APP_PIN = "app_pin";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_THEME_MODE = "theme_mode";
    private static final String KEY_FIRST_LAUNCH = "first_launch";

    private final SharedPreferences sharedPreferences;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Currency
    public void setCurrency(String currency) {
        sharedPreferences.edit().putString(KEY_CURRENCY, currency).apply();
    }

    public String getCurrency() {
        return sharedPreferences.getString(KEY_CURRENCY, "BDT");
    }

    public String getCurrencySymbol() {
        String currency = getCurrency();
        switch (currency) {
            case "BDT":
                return "৳";
            case "INR":
                return "₹";
            case "EUR":
                return "€";
            case "GBP":
                return "£";
            default:
                return "$";
        }
    }

    // Budget
    public void setMonthlyBudget(double budget) {
        sharedPreferences.edit().putFloat(KEY_MONTHLY_BUDGET, (float) budget).apply();
    }

    public double getMonthlyBudget() {
        return sharedPreferences.getFloat(KEY_MONTHLY_BUDGET, 0f);
    }

    public void setWeeklyBudget(double budget) {
        sharedPreferences.edit().putFloat(KEY_WEEKLY_BUDGET, (float) budget).apply();
    }

    public double getWeeklyBudget() {
        return sharedPreferences.getFloat(KEY_WEEKLY_BUDGET, 0f);
    }

    // Guest Mode
    public void setGuestMode(boolean isGuest) {
        sharedPreferences.edit().putBoolean(KEY_GUEST_MODE, isGuest).apply();
    }

    public boolean isGuestMode() {
        return sharedPreferences.getBoolean(KEY_GUEST_MODE, false);
    }

    // App Lock
    public void setAppLockEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply();
    }

    public boolean isAppLockEnabled() {
        return sharedPreferences.getBoolean(KEY_APP_LOCK_ENABLED, false);
    }

    public void setAppPin(String pin) {
        sharedPreferences.edit().putString(KEY_APP_PIN, pin).apply();
    }

    public String getAppPin() {
        return sharedPreferences.getString(KEY_APP_PIN, "");
    }

    public void setBiometricEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public boolean isBiometricEnabled() {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    // Theme
    public void setThemeMode(int mode) {
        sharedPreferences.edit().putInt(KEY_THEME_MODE, mode).apply();
    }

    public int getThemeMode() {
        return sharedPreferences.getInt(KEY_THEME_MODE, -1); // -1 = system default
    }

    // First Launch
    public void setFirstLaunch(boolean isFirst) {
        sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, isFirst).apply();
    }

    public boolean isFirstLaunch() {
        return sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    // Notifications
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_DAILY_REMINDER_TIME = "daily_reminder_time";

    public void setNotificationsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    public boolean isNotificationsEnabled() {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    public void setDailyReminderTime(String time) {
        sharedPreferences.edit().putString(KEY_DAILY_REMINDER_TIME, time).apply();
    }

    public String getDailyReminderTime() {
        return sharedPreferences.getString(KEY_DAILY_REMINDER_TIME, "20:00");
    }

    // Last Backup
    private static final String KEY_LAST_BACKUP = "last_backup";

    public void setLastBackupTime(long time) {
        sharedPreferences.edit().putLong(KEY_LAST_BACKUP, time).apply();
    }

    public long getLastBackupTime() {
        return sharedPreferences.getLong(KEY_LAST_BACKUP, 0);
    }

    // Category Caching for Guest Users
    private static final String KEY_CACHED_EXPENSE_CATEGORIES = "cached_expense_categories";
    private static final String KEY_CACHED_INCOME_CATEGORIES = "cached_income_categories";
    private static final String KEY_CATEGORIES_CACHE_TIME = "categories_cache_time";

    /**
     * Cache expense categories as JSON string.
     * Format: "name1|iconName1|colorHex1;name2|iconName2|colorHex2;..."
     */
    public void cacheExpenseCategories(String categoriesData) {
        sharedPreferences.edit()
                .putString(KEY_CACHED_EXPENSE_CATEGORIES, categoriesData)
                .putLong(KEY_CATEGORIES_CACHE_TIME, System.currentTimeMillis())
                .apply();
    }

    /**
     * Get cached expense categories.
     */
    public String getCachedExpenseCategories() {
        return sharedPreferences.getString(KEY_CACHED_EXPENSE_CATEGORIES, "");
    }

    /**
     * Cache income categories as JSON string.
     */
    public void cacheIncomeCategories(String categoriesData) {
        sharedPreferences.edit()
                .putString(KEY_CACHED_INCOME_CATEGORIES, categoriesData)
                .putLong(KEY_CATEGORIES_CACHE_TIME, System.currentTimeMillis())
                .apply();
    }

    /**
     * Get cached income categories.
     */
    public String getCachedIncomeCategories() {
        return sharedPreferences.getString(KEY_CACHED_INCOME_CATEGORIES, "");
    }

    /**
     * Check if categories cache is valid (less than 24 hours old).
     */
    public boolean isCategoriesCacheValid() {
        long cacheTime = sharedPreferences.getLong(KEY_CATEGORIES_CACHE_TIME, 0);
        long oneDayMs = 24 * 60 * 60 * 1000; // 24 hours
        return (System.currentTimeMillis() - cacheTime) < oneDayMs;
    }

    /**
     * Check if categories are cached.
     */
    public boolean hasCachedCategories() {
        String expense = getCachedExpenseCategories();
        String income = getCachedIncomeCategories();
        return !expense.isEmpty() && !income.isEmpty();
    }
}
