package com.example.trackexpense.data.remote;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.trackexpense.data.local.Expense;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class FirestoreService {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final MutableLiveData<List<Expense>> remoteExpenses = new MutableLiveData<>();

    public FirestoreService() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public void saveExpense(Expense expense) {
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            db.collection("users").document(userId)
                    .collection("expenses")
                    .add(expense)
                    .addOnSuccessListener(documentReference -> {
                        // Log success
                    })
                    .addOnFailureListener(e -> {
                        // Log error
                    });
        }
    }

    public LiveData<List<Expense>> getExpenses() {
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            CollectionReference expenseRef = db.collection("users").document(userId).collection("expenses");

            expenseRef.orderBy("date", Query.Direction.DESCENDING)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            return;
                        }

                        List<Expense> expenses = new ArrayList<>();
                        if (value != null) {
                            expenses = value.toObjects(Expense.class);
                        }
                        remoteExpenses.setValue(expenses);
                    });
        }
        return remoteExpenses;
    }
}
