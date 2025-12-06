package com.example.trackexpense.data;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.trackexpense.data.local.AppDatabase;
import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.data.local.ExpenseDao;

import java.util.List;

public class ExpenseRepository {

    private final ExpenseDao expenseDao;
    private final LiveData<List<Expense>> allExpenses;

    public ExpenseRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        expenseDao = db.expenseDao();
        allExpenses = expenseDao.getAllExpenses();
    }

    public LiveData<List<Expense>> getAllExpenses() {
        return allExpenses;
    }

    public LiveData<Double> getTotalExpense() {
        return expenseDao.getTotalExpense();
    }

    public LiveData<Double> getTotalIncome() {
        return expenseDao.getTotalIncome();
    }

    public void insert(Expense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            expenseDao.insert(expense);
        });
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
}
