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
        if (firestoreService.isUserLoggedIn()) {
            String currencySymbol = preferenceManager.getCurrencySymbol();
            notificationRepository.notifyTransactionCreated(
                    expense.getCategory(),
                    expense.getAmount(),
                    expense.getType(),
                    currencySymbol);
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
}
