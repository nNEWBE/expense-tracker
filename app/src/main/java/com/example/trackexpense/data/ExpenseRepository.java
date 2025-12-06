package com.example.trackexpense.data;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.trackexpense.data.local.AppDatabase;
import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.data.local.ExpenseDao;
import com.example.trackexpense.data.remote.FirestoreService;

import java.util.List;

public class ExpenseRepository {

    private static final String TAG = "ExpenseRepository";

    private final ExpenseDao expenseDao;
    private final FirestoreService firestoreService;
    private final LiveData<List<Expense>> allExpenses;
    private final MediatorLiveData<List<Expense>> mergedExpenses = new MediatorLiveData<>();

    public ExpenseRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        expenseDao = db.expenseDao();
        firestoreService = FirestoreService.getInstance();
        allExpenses = expenseDao.getAllExpenses();

        Log.d(TAG, "ExpenseRepository initialized, user logged in: " + firestoreService.isUserLoggedIn());
        setupMergedExpenses();
    }

    private void setupMergedExpenses() {
        // Add Room data as source
        mergedExpenses.addSource(allExpenses, localExpenses -> {
            if (!firestoreService.isUserLoggedIn()) {
                mergedExpenses.setValue(localExpenses);
            }
        });

        // If user is logged in, also listen to Firestore
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
        Log.d(TAG, "insert: Adding expense - Category: " + expense.getCategory() +
                ", Amount: " + expense.getAmount() + ", Type: " + expense.getType());

        // Always save to local Room database first
        AppDatabase.databaseWriteExecutor.execute(() -> {
            expenseDao.insert(expense);
            Log.d(TAG, "insert: Saved to Room database");
        });

        // If user is logged in, also save to Firestore
        boolean isLoggedIn = firestoreService.isUserLoggedIn();
        Log.d(TAG, "insert: User logged in = " + isLoggedIn);

        if (isLoggedIn) {
            Log.d(TAG, "insert: Calling firestoreService.saveExpense()");
            firestoreService.saveExpense(expense);
        } else {
            Log.d(TAG, "insert: Skipping Firestore (user not logged in)");
        }
    }

    public void delete(Expense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            expenseDao.delete(expense);
        });
    }

    public void update(Expense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            expenseDao.update(expense);
        });
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
