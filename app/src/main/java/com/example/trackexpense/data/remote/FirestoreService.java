package com.example.trackexpense.data.remote;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.trackexpense.data.local.Expense;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
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

    public interface OnExpenseSavedListener {
        void onSuccess(String firestoreId);

        void onFailure(Exception e);
    }

    private FirestoreService() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        Log.d(TAG, "FirestoreService initialized");
    }

    public static synchronized FirestoreService getInstance() {
        if (instance == null) {
            instance = new FirestoreService();
        }
        return instance;
    }

    public boolean isUserLoggedIn() {
        FirebaseUser user = auth.getCurrentUser();
        boolean isLoggedIn = user != null;
        Log.d(TAG, "isUserLoggedIn: " + isLoggedIn);
        return isLoggedIn;
    }

    public String getUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    private CollectionReference getExpensesCollection() {
        String userId = getUserId();
        if (userId == null) {
            Log.w(TAG, "getExpensesCollection: userId is null");
            return null;
        }
        return db.collection("users").document(userId).collection("expenses");
    }

    public void saveExpense(Expense expense, OnExpenseSavedListener listener) {
        Log.d(TAG, "saveExpense: " + expense.getCategory() + ", amount: " + expense.getAmount());

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "saveExpense: User not logged in");
            if (listener != null)
                listener.onFailure(new Exception("User not logged in"));
            return;
        }

        String userId = currentUser.getUid();
        Map<String, Object> expenseData = new HashMap<>();
        expenseData.put("amount", expense.getAmount());
        expenseData.put("category", expense.getCategory());
        expenseData.put("date", expense.getDate());
        expenseData.put("notes", expense.getNotes() != null ? expense.getNotes() : "");
        expenseData.put("type", expense.getType());
        expenseData.put("createdAt", System.currentTimeMillis());
        expenseData.put("localId", expense.getId());

        db.collection("users")
                .document(userId)
                .collection("expenses")
                .add(expenseData)
                .addOnSuccessListener(documentReference -> {
                    String firestoreId = documentReference.getId();
                    Log.d(TAG, "SUCCESS: Saved with ID: " + firestoreId);
                    if (listener != null)
                        listener.onSuccess(firestoreId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "FAILURE: " + e.getMessage(), e);
                    if (listener != null)
                        listener.onFailure(e);
                });
    }

    public void updateExpense(Expense expense) {
        String firestoreId = expense.getFirestoreId();
        if (firestoreId == null || firestoreId.isEmpty()) {
            Log.w(TAG, "updateExpense: No firestoreId, cannot update");
            return;
        }

        CollectionReference expensesRef = getExpensesCollection();
        if (expensesRef == null)
            return;

        Map<String, Object> expenseData = new HashMap<>();
        expenseData.put("amount", expense.getAmount());
        expenseData.put("category", expense.getCategory());
        expenseData.put("date", expense.getDate());
        expenseData.put("notes", expense.getNotes() != null ? expense.getNotes() : "");
        expenseData.put("type", expense.getType());
        expenseData.put("updatedAt", System.currentTimeMillis());

        expensesRef.document(firestoreId)
                .update(expenseData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "updateExpense: SUCCESS"))
                .addOnFailureListener(e -> Log.e(TAG, "updateExpense: FAILURE", e));
    }

    public void deleteExpense(Expense expense) {
        String firestoreId = expense.getFirestoreId();
        if (firestoreId == null || firestoreId.isEmpty()) {
            Log.w(TAG, "deleteExpense: No firestoreId, cannot delete from Firestore");
            return;
        }

        CollectionReference expensesRef = getExpensesCollection();
        if (expensesRef == null)
            return;

        expensesRef.document(firestoreId)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "deleteExpense: SUCCESS"))
                .addOnFailureListener(e -> Log.e(TAG, "deleteExpense: FAILURE", e));
    }

    public LiveData<List<Expense>> getExpenses() {
        if (!isUserLoggedIn()) {
            Log.w(TAG, "getExpenses: User not logged in");
            remoteExpenses.setValue(new ArrayList<>());
            return remoteExpenses;
        }

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
                            expense.setFirestoreId(doc.getId()); // Store Firestore ID
                            expense.setAmount(doc.getDouble("amount") != null ? doc.getDouble("amount") : 0);
                            expense.setCategory(doc.getString("category"));
                            expense.setDate(doc.getLong("date") != null ? doc.getLong("date") : 0);
                            expense.setNotes(doc.getString("notes"));
                            expense.setType(doc.getString("type"));

                            // Get local ID if exists
                            Long localId = doc.getLong("localId");
                            if (localId != null) {
                                expense.setId(localId.intValue());
                            }

                            expenses.add(expense);
                        });
                    }
                    remoteExpenses.setValue(expenses);
                    Log.d(TAG, "Fetched " + expenses.size() + " expenses from Firestore");
                });

        return remoteExpenses;
    }

    public void syncLocalToFirestore(List<Expense> localExpenses) {
        if (!isUserLoggedIn()) {
            Log.w(TAG, "syncLocalToFirestore: User not logged in");
            return;
        }

        Log.d(TAG, "syncLocalToFirestore: Syncing " + localExpenses.size() + " expenses");
        for (Expense expense : localExpenses) {
            saveExpense(expense, null);
        }
    }
}
