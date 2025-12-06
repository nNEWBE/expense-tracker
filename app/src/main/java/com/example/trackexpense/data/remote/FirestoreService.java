package com.example.trackexpense.data.remote;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.trackexpense.data.local.Expense;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirestoreService {

    private static final String TAG = "FirestoreService";
    private static FirestoreService instance;

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final MutableLiveData<List<Expense>> remoteExpenses = new MutableLiveData<>();

    private FirestoreService() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized FirestoreService getInstance() {
        if (instance == null) {
            instance = new FirestoreService();
        }
        return instance;
    }

    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public String getUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    private CollectionReference getExpensesCollection() {
        String userId = getUserId();
        if (userId == null)
            return null;
        return db.collection("users").document(userId).collection("expenses");
    }

    public void saveExpense(Expense expense) {
        CollectionReference expensesRef = getExpensesCollection();
        if (expensesRef == null) {
            Log.w(TAG, "User not logged in, skipping Firestore save");
            return;
        }

        Map<String, Object> expenseData = new HashMap<>();
        expenseData.put("amount", expense.getAmount());
        expenseData.put("category", expense.getCategory());
        expenseData.put("date", expense.getDate());
        expenseData.put("notes", expense.getNotes());
        expenseData.put("type", expense.getType());
        expenseData.put("localId", expense.getId());

        expensesRef.add(expenseData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Expense saved to Firestore: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving expense to Firestore", e);
                });
    }

    public void updateExpense(String firestoreId, Expense expense) {
        CollectionReference expensesRef = getExpensesCollection();
        if (expensesRef == null)
            return;

        Map<String, Object> expenseData = new HashMap<>();
        expenseData.put("amount", expense.getAmount());
        expenseData.put("category", expense.getCategory());
        expenseData.put("date", expense.getDate());
        expenseData.put("notes", expense.getNotes());
        expenseData.put("type", expense.getType());

        expensesRef.document(firestoreId)
                .update(expenseData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Expense updated in Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating expense", e));
    }

    public void deleteExpense(String firestoreId) {
        CollectionReference expensesRef = getExpensesCollection();
        if (expensesRef == null)
            return;

        expensesRef.document(firestoreId)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Expense deleted from Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting expense", e));
    }

    public LiveData<List<Expense>> getExpenses() {
        CollectionReference expensesRef = getExpensesCollection();
        if (expensesRef == null) {
            remoteExpenses.setValue(new ArrayList<>());
            return remoteExpenses;
        }

        expensesRef.orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to expenses", error);
                        return;
                    }

                    List<Expense> expenses = new ArrayList<>();
                    if (value != null) {
                        value.getDocuments().forEach(doc -> {
                            Expense expense = new Expense();
                            expense.setAmount(doc.getDouble("amount") != null ? doc.getDouble("amount") : 0);
                            expense.setCategory(doc.getString("category"));
                            expense.setDate(doc.getLong("date") != null ? doc.getLong("date") : 0);
                            expense.setNotes(doc.getString("notes"));
                            expense.setType(doc.getString("type"));
                            expenses.add(expense);
                        });
                    }
                    remoteExpenses.setValue(expenses);
                    Log.d(TAG, "Fetched " + expenses.size() + " expenses from Firestore");
                });

        return remoteExpenses;
    }

    public void syncLocalToFirestore(List<Expense> localExpenses) {
        CollectionReference expensesRef = getExpensesCollection();
        if (expensesRef == null)
            return;

        for (Expense expense : localExpenses) {
            saveExpense(expense);
        }
        Log.d(TAG, "Synced " + localExpenses.size() + " local expenses to Firestore");
    }
}
