package com.example.trackexpense.data.remote;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.data.model.CategoryModel;
import com.example.trackexpense.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminService {

    private static final String TAG = "AdminService";
    private static AdminService instance;

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final MutableLiveData<List<User>> allUsers = new MutableLiveData<>();
    private final MutableLiveData<List<CategoryModel>> allCategories = new MutableLiveData<>();
    private final MutableLiveData<List<Expense>> allTransactions = new MutableLiveData<>();

    public interface OnCompleteListener {
        void onSuccess();

        void onFailure(Exception e);
    }

    private AdminService() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized AdminService getInstance() {
        if (instance == null) {
            instance = new AdminService();
        }
        return instance;
    }

    // Check if current user is admin
    public void checkAdminStatus(OnAdminCheckListener listener) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            listener.onResult(false);
            return;
        }

        db.collection("admins").document(userId).get()
                .addOnSuccessListener(doc -> {
                    boolean isAdmin = doc.exists();
                    Log.d(TAG, "Admin status: " + isAdmin);
                    listener.onResult(isAdmin);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking admin status", e);
                    listener.onResult(false);
                });
    }

    public interface OnAdminCheckListener {
        void onResult(boolean isAdmin);
    }

    // ==================== USER MANAGEMENT ====================

    public LiveData<List<User>> getAllUsers() {
        Log.d(TAG, "getAllUsers: Fetching all users from Firestore...");

        // Fetch all users without orderBy (createdAt may not exist in all documents)
        db.collection("users")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching users: " + error.getMessage(), error);
                        allUsers.setValue(new ArrayList<>());
                        return;
                    }

                    List<User> users = new ArrayList<>();
                    if (value != null) {
                        Log.d(TAG, "getAllUsers: Found " + value.size() + " documents");
                        for (var doc : value.getDocuments()) {
                            try {
                                User user = new User();
                                user.setId(doc.getId());
                                user.setEmail(doc.getString("email"));
                                user.setDisplayName(doc.getString("displayName"));
                                user.setPhotoUrl(doc.getString("photoUrl"));
                                user.setBlocked(Boolean.TRUE.equals(doc.getBoolean("isBlocked")));
                                user.setAdmin(Boolean.TRUE.equals(doc.getBoolean("isAdmin")));

                                // Check verified status - multiple ways:
                                // 1. isVerified field
                                // 2. emailVerified field (Firebase Auth)
                                // 3. Has photoUrl (usually means Google/social sign-in = verified)
                                boolean isVerified = Boolean.TRUE.equals(doc.getBoolean("isVerified")) ||
                                        Boolean.TRUE.equals(doc.getBoolean("emailVerified")) ||
                                        (doc.getString("photoUrl") != null && !doc.getString("photoUrl").isEmpty());
                                user.setVerified(isVerified);

                                Long createdAt = doc.getLong("createdAt");
                                user.setCreatedAt(createdAt != null ? createdAt : 0);

                                Long lastLoginAt = doc.getLong("lastLoginAt");
                                user.setLastLoginAt(lastLoginAt != null ? lastLoginAt : 0);

                                users.add(user);
                                Log.d(TAG, "getAllUsers: Added user " + user.getEmail() + ", verified=" + isVerified);
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing user document: " + doc.getId(), e);
                            }
                        }
                    } else {
                        Log.w(TAG, "getAllUsers: value is null");
                    }
                    allUsers.setValue(users);
                    Log.d(TAG, "getAllUsers: Total users fetched: " + users.size());
                });

        return allUsers;
    }

    public void blockUser(String userId, boolean block, OnCompleteListener listener) {
        db.collection("users").document(userId)
                .update("isBlocked", block)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User " + (block ? "blocked" : "unblocked"));
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error blocking user", e);
                    listener.onFailure(e);
                });
    }

    public void deleteUser(String userId, OnCompleteListener listener) {
        // First delete all user's expenses
        db.collection("users").document(userId).collection("expenses")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Delete each expense
                    for (var doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }

                    // Then delete the user document
                    db.collection("users").document(userId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "User deleted");
                                listener.onSuccess();
                            })
                            .addOnFailureListener(listener::onFailure);
                })
                .addOnFailureListener(listener::onFailure);
    }

    public void updateUser(User user, OnCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", user.getDisplayName());
        updates.put("email", user.getEmail());
        updates.put("isBlocked", user.isBlocked());
        updates.put("isAdmin", user.isAdmin());

        db.collection("users").document(user.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User updated");
                    listener.onSuccess();
                })
                .addOnFailureListener(listener::onFailure);
    }

    // ==================== TRANSACTION MANAGEMENT ====================

    public LiveData<List<Expense>> getUserTransactions(String userId) {
        MutableLiveData<List<Expense>> userTransactions = new MutableLiveData<>();

        db.collection("users").document(userId).collection("expenses")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching transactions", error);
                        return;
                    }

                    List<Expense> expenses = new ArrayList<>();
                    if (value != null) {
                        value.getDocuments().forEach(doc -> {
                            Expense expense = new Expense();
                            expense.setFirestoreId(doc.getId());
                            expense.setAmount(doc.getDouble("amount") != null ? doc.getDouble("amount") : 0);
                            expense.setCategory(doc.getString("category"));
                            expense.setDate(doc.getLong("date") != null ? doc.getLong("date") : 0);
                            expense.setNotes(doc.getString("notes"));
                            expense.setType(doc.getString("type"));
                            expenses.add(expense);
                        });
                    }
                    userTransactions.setValue(expenses);
                });

        return userTransactions;
    }

    public void deleteTransaction(String userId, String transactionId, OnCompleteListener listener) {
        db.collection("users").document(userId).collection("expenses").document(transactionId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Transaction deleted");
                    listener.onSuccess();
                })
                .addOnFailureListener(listener::onFailure);
    }

    public void updateTransaction(String userId, Expense expense, OnCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("amount", expense.getAmount());
        updates.put("category", expense.getCategory());
        updates.put("date", expense.getDate());
        updates.put("notes", expense.getNotes());
        updates.put("type", expense.getType());

        db.collection("users").document(userId).collection("expenses").document(expense.getFirestoreId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Transaction updated");
                    listener.onSuccess();
                })
                .addOnFailureListener(listener::onFailure);
    }

    public void addTransaction(String userId, double amount, String category, String notes,
            String type, OnCompleteListener listener) {
        Map<String, Object> data = new HashMap<>();
        data.put("amount", amount);
        data.put("category", category);
        data.put("notes", notes);
        data.put("type", type);
        data.put("date", System.currentTimeMillis());
        data.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(userId).collection("expenses")
                .add(data)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Transaction added: " + docRef.getId());
                    listener.onSuccess();
                })
                .addOnFailureListener(listener::onFailure);
    }

    // ==================== CATEGORY MANAGEMENT ====================

    public LiveData<List<CategoryModel>> getAllCategories() {
        db.collection("categories")
                .orderBy("name")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching categories", error);
                        return;
                    }

                    List<CategoryModel> categories = new ArrayList<>();
                    if (value != null) {
                        value.getDocuments().forEach(doc -> {
                            CategoryModel category = new CategoryModel();
                            category.setId(doc.getId());
                            category.setName(doc.getString("name"));
                            category.setIconName(doc.getString("iconName"));
                            category.setColorHex(doc.getString("colorHex"));
                            category.setType(doc.getString("type"));
                            category.setDefault(Boolean.TRUE.equals(doc.getBoolean("isDefault")));
                            categories.add(category);
                        });
                    }
                    allCategories.setValue(categories);
                    Log.d(TAG, "Fetched " + categories.size() + " categories");
                });

        return allCategories;
    }

    public void addCategory(CategoryModel category, OnCompleteListener listener) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", category.getName());
        data.put("iconName", category.getIconName());
        data.put("colorHex", category.getColorHex());
        data.put("type", category.getType());
        data.put("isDefault", category.isDefault());
        data.put("createdAt", System.currentTimeMillis());

        db.collection("categories")
                .add(data)
                .addOnSuccessListener(doc -> {
                    Log.d(TAG, "Category added: " + doc.getId());
                    listener.onSuccess();
                })
                .addOnFailureListener(listener::onFailure);
    }

    public void updateCategory(CategoryModel category, OnCompleteListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", category.getName());
        updates.put("iconName", category.getIconName());
        updates.put("colorHex", category.getColorHex());
        updates.put("type", category.getType());
        updates.put("isDefault", category.isDefault());

        db.collection("categories").document(category.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Category updated");
                    listener.onSuccess();
                })
                .addOnFailureListener(listener::onFailure);
    }

    public void deleteCategory(String categoryId, OnCompleteListener listener) {
        db.collection("categories").document(categoryId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Category deleted");
                    listener.onSuccess();
                })
                .addOnFailureListener(listener::onFailure);
    }
}
