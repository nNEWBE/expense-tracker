package com.example.trackexpense.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ExpenseDao {

    @Insert
    void insert(Expense expense);

    @Update
    void update(Expense expense);

    @Delete
    void delete(Expense expense);

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    LiveData<List<Expense>> getAllExpenses();

    @Query("SELECT * FROM expenses WHERE type = 'EXPENSE'")
    LiveData<List<Expense>> getAllExpenseEntries();

    @Query("SELECT * FROM expenses WHERE type = 'INCOME'")
    LiveData<List<Expense>> getAllIncomeEntries();

    @Query("SELECT SUM(amount) FROM expenses WHERE type = 'EXPENSE'")
    LiveData<Double> getTotalExpense();

    @Query("SELECT SUM(amount) FROM expenses WHERE type = 'INCOME'")
    LiveData<Double> getTotalIncome();

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    LiveData<List<Expense>> getExpensesByDateRange(long startDate, long endDate);
}
