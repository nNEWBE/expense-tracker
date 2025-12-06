package com.example.trackexpense.data;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.trackexpense.data.local.AppDatabase;
import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.data.local.ExpenseDao;
import com.example.trackexpense.data.remote.FirestoreService;

import java.util.List;

public class ExpenseRepository {

    private final ExpenseDao expenseDao;
    private final FirestoreService firestoreService;
    private final LiveData<List<Expense>> allExpenses;
    private final MediatorLiveData<List<Expense>> mergedExpenses = new MediatorLiveData<>();

    public ExpenseRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        expenseDao = db.expenseDao();
        firestoreService = FirestoreService.getInstance();
        allExpenses = expenseDao.getAllExpenses();

        setupMergedExpenses();
    }

    private void setupMergedExpenses() {
        // Add Room data as source
        mergedExpenses.addSource(allExpenses, localExpenses -> {
            if (!firestoreService.isUserLoggedIn()) {
                // Guest mode - use only local data
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
        if (firestoreService.isUserLoggedIn()) {
            // For logged-in users, prefer Firestore data
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
        // Always save to local Room database first
        AppDatabase.databaseWriteExecutor.execute(() -> {
            expenseDao.insert(expense);
        });

        // If user is logged in, also save to Firestore
        if (firestoreService.isUserLoggedIn()) {
            firestoreService.saveExpense(expense);
        }
    }

    public void delete(Expense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            expenseDao.delete(expense);
        });

        // Note: For Firestore deletion, we would need the Firestore document ID
        // This is a simplified implementation
    }

    public void update(Expense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            expenseDao.update(expense);
        });

        // Note: For Firestore update, we would need the Firestore document ID
        // This is a simplified implementation
    }

    public void syncLocalToCloud() {
        if (firestoreService.isUserLoggedIn()) {
            // Get all local expenses and sync to Firestore
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
