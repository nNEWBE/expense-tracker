package com.example.trackexpense.data;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.trackexpense.data.local.AppDatabase;
import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.data.local.ExpenseDao;
import com.example.trackexpense.data.remote.FirestoreService;
import com.example.trackexpense.data.repository.NotificationRepository;
import com.example.trackexpense.utils.PreferenceManager;

import java.util.Calendar;
import java.util.Calendar;
import java.util.List;

public class ExpenseRepository {

    private static final String TAG = "ExpenseRepository";

    private final ExpenseDao expenseDao;
    private final FirestoreService firestoreService;
    private final NotificationRepository notificationRepository;
    private final PreferenceManager preferenceManager;
    private final LiveData<List<Expense>> allExpenses;
    private final MediatorLiveData<List<Expense>> mergedExpenses = new MediatorLiveData<>();

    public ExpenseRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        expenseDao = db.expenseDao();
        firestoreService = FirestoreService.getInstance();
        notificationRepository = NotificationRepository.getInstance();
        preferenceManager = new PreferenceManager(application);
        allExpenses = expenseDao.getAllExpenses();

        Log.d(TAG, "ExpenseRepository initialized, user logged in: " + firestoreService.isUserLoggedIn());
        setupMergedExpenses();
    }

    private void setupMergedExpenses() {
        mergedExpenses.addSource(allExpenses, localExpenses -> {
            if (!firestoreService.isUserLoggedIn()) {
                mergedExpenses.setValue(localExpenses);
            }
        });

        if (firestoreService.isUserLoggedIn()) {
            mergedExpenses.addSource(firestoreService.getExpenses(), remoteExpenses -> {
                if (remoteExpenses != null) {
                    mergedExpenses.setValue(remoteExpenses);
                }
            });
        }
    }

    public LiveData<List<Expense>> getAllExpenses() {
        boolean loggedIn = firestoreService.isUserLoggedIn();
        Log.d(TAG, "getAllExpenses: user logged in = " + loggedIn);

        if (loggedIn) {
            return firestoreService.getExpenses();
        }
        return allExpenses;
    }

    public LiveData<Double> getTotalExpense() {
        return expenseDao.getTotalExpense();
    }

    public LiveData<Double> getTotalIncome() {
        return expenseDao.getTotalIncome();
    }

    public void insert(Expense expense) {
        Log.d(TAG, "insert: " + expense.getCategory() + ", Amount: " + expense.getAmount());

        // Always save to local Room database first
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                long id = expenseDao.insertAndGetId(expense);
                expense.setId((int) id);
                Log.d(TAG, "insert: Saved to Room with ID: " + id);

                // If user is logged in, also save to Firestore
                if (firestoreService.isUserLoggedIn()) {
                    firestoreService.saveExpense(expense, new FirestoreService.OnExpenseSavedListener() {
                        @Override
                        public void onSuccess(String firestoreId) {
                            // Update local expense with Firestore ID - must run on executor
                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                try {
                                    expense.setFirestoreId(firestoreId);
                                    expenseDao.update(expense);
                                    Log.d(TAG, "insert: Updated Room with firestoreId: " + firestoreId);
                                } catch (Exception e) {
                                    Log.e(TAG, "insert: Failed to update Room with firestoreId", e);
                                }
                            });
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "insert: Failed to save to Firestore", e);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "insert: Failed to save to Room", e);
            }
        });

        // Create notification for transaction created
        String currencySymbol = preferenceManager.getCurrencySymbol();
        if (firestoreService.isUserLoggedIn()) {
            notificationRepository.notifyTransactionCreated(
                    expense.getCategory(),
                    expense.getAmount(),
                    expense.getType(),
                    currencySymbol);
        } else {
            // Guest user - store notification locally
            String type = expense.getType();
            String title = "EXPENSE".equals(type) ? "Expense Added" : "Income Added";
            String message = String.format("Added %s%,.0f to %s", currencySymbol, expense.getAmount(),
                    expense.getCategory());
            preferenceManager.addGuestNotification(type, title, message);
            Log.d(TAG, "insert: Created local notification for guest user");
        }
    }

    public void delete(Expense expense) {
        Log.d(TAG, "delete: " + expense.getCategory() + ", firestoreId: " + expense.getFirestoreId());

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                expenseDao.delete(expense);
            } catch (Exception e) {
                Log.e(TAG, "delete: Failed to delete from Room", e);
            }
        });

        // Delete from Firestore if user is logged in
        if (firestoreService.isUserLoggedIn()) {
            try {
                firestoreService.deleteExpense(expense);
            } catch (Exception e) {
                Log.e(TAG, "delete: Failed to delete from Firestore", e);
            }

            // Create notification for transaction deleted
            String currencySymbol = preferenceManager.getCurrencySymbol();
            notificationRepository.notifyTransactionDeleted(
                    expense.getCategory(),
                    expense.getAmount(),
                    expense.getType(),
                    currencySymbol);
        }
    }

    public void update(Expense expense) {
        Log.d(TAG, "update: " + expense.getCategory() + ", firestoreId: " + expense.getFirestoreId());

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                expenseDao.update(expense);
            } catch (Exception e) {
                Log.e(TAG, "update: Failed to update Room", e);
            }
        });

        // Update Firestore if user is logged in
        if (firestoreService.isUserLoggedIn()) {
            try {
                firestoreService.updateExpense(expense);
            } catch (Exception e) {
                Log.e(TAG, "update: Failed to update Firestore", e);
            }

            // Create notification for transaction updated
            String currencySymbol = preferenceManager.getCurrencySymbol();
            notificationRepository.notifyTransactionUpdated(
                    expense.getCategory(),
                    expense.getAmount(),
                    expense.getType(),
                    currencySymbol);
        }
    }

    /**
     * Update expense pin status without triggering notifications.
     * Used for lightweight pin/unpin operations.
     */
    public void updatePinStatus(Expense expense) {
        Log.d(TAG, "updatePinStatus: " + expense.getCategory() + ", isPinned: " + expense.isPinned());

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                expenseDao.update(expense);
            } catch (Exception e) {
                Log.e(TAG, "updatePinStatus: Failed to update Room", e);
            }
        });

        // Update Firestore if user is logged in (no notification)
        if (firestoreService.isUserLoggedIn()) {
            try {
                firestoreService.updateExpense(expense);
            } catch (Exception e) {
                Log.e(TAG, "updatePinStatus: Failed to update Firestore", e);
            }
        }
    }

    public void syncLocalToCloud() {
        if (firestoreService.isUserLoggedIn()) {
            List<Expense> localExpenses = allExpenses.getValue();
            if (localExpenses != null && !localExpenses.isEmpty()) {
                firestoreService.syncLocalToFirestore(localExpenses);
            }
        }
    }

    public boolean isUserLoggedIn() {
        return firestoreService.isUserLoggedIn();
    }

    /**
     * Get count of local expenses (for checking if guest has data).
     */
    public void getLocalExpenseCount(OnCountLoadedListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                int count = expenseDao.getLocalExpenseCount();
                listener.onCount(count);
            } catch (Exception e) {
                Log.e(TAG, "getLocalExpenseCount: Failed", e);
                listener.onCount(0);
            }
        });
    }

    /**
     * Delete all local expenses.
     */
    public void deleteAllLocalExpenses(OnCompleteListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                expenseDao.deleteAll();
                Log.d(TAG, "deleteAllLocalExpenses: All local expenses deleted");
                if (listener != null)
                    listener.onSuccess();
            } catch (Exception e) {
                Log.e(TAG, "deleteAllLocalExpenses: Failed", e);
                if (listener != null)
                    listener.onError(e.getMessage());
            }
        });
    }

    /**
     * Sync guest data to Firestore and optionally delete local data.
     */
    public void syncGuestDataToCloud(boolean deleteLocalAfterSync, OnSyncCompleteListener listener) {
        if (!firestoreService.isUserLoggedIn()) {
            if (listener != null)
                listener.onError("User not logged in");
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                List<Expense> localExpenses = expenseDao.getAllExpensesSync();

                if (localExpenses == null || localExpenses.isEmpty()) {
                    if (listener != null)
                        listener.onSuccess(0);
                    return;
                }

                int totalCount = localExpenses.size();
                Log.d(TAG, "syncGuestDataToCloud: Syncing " + totalCount + " expenses");

                // Sync each expense to Firestore
                for (Expense expense : localExpenses) {
                    firestoreService.saveExpense(expense, new FirestoreService.OnExpenseSavedListener() {
                        @Override
                        public void onSuccess(String firestoreId) {
                            Log.d(TAG, "syncGuestDataToCloud: Synced expense " + expense.getCategory());
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e(TAG, "syncGuestDataToCloud: Failed to sync expense", e);
                        }
                    });
                }

                // Delete local data if requested
                if (deleteLocalAfterSync) {
                    expenseDao.deleteAll();
                    Log.d(TAG, "syncGuestDataToCloud: Deleted local data after sync");
                }

                if (listener != null)
                    listener.onSuccess(totalCount);
            } catch (Exception e) {
                Log.e(TAG, "syncGuestDataToCloud: Failed", e);
                if (listener != null)
                    listener.onError(e.getMessage());
            }
        });
    }

    public void getCurrentMonthTotal(FirestoreService.OnTotalLoadedListener listener) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfMonth = calendar.getTimeInMillis();
        long now = System.currentTimeMillis();

        if (firestoreService.isUserLoggedIn()) {
            firestoreService.getCurrentMonthTotal(startOfMonth, listener);
        } else {
            // Guest mode - query Room on background
            AppDatabase.databaseWriteExecutor.execute(() -> {
                Double total = expenseDao.getTotalExpenseBetweenSync(startOfMonth, now);
                if (listener != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper())
                            .post(() -> listener.onLoaded(total != null ? total : 0));
                }
            });
        }
    }

    // Callback interfaces
    public interface OnCountLoadedListener {
        void onCount(int count);
    }

    public interface OnCompleteListener {
        void onSuccess();

        void onError(String error);
    }

    public interface OnSyncCompleteListener {
        void onSuccess(int syncedCount);

        void onError(String error);
    }
}
