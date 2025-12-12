package com.example.trackexpense.data.remote;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.data.model.Category;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
    private final MutableLiveData<List<Category>> expenseCategories = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> incomeCategories = new MutableLiveData<>();

    public interface OnExpenseSavedListener {
        void onSuccess(String firestoreId);

        void onFailure(Exception e);
    }

    public interface OnTotalLoadedListener {
        void onLoaded(double total);

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
        expenseData.put("isPinned", expense.isPinned());
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
        expenseData.put("isPinned", expense.isPinned());
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

                            // Get isPinned field
                            Boolean isPinned = doc.getBoolean("isPinned");
                            expense.setPinned(isPinned != null ? isPinned : false);

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

    public void getCurrentMonthTotal(long startDate, OnTotalLoadedListener listener) {
        if (!isUserLoggedIn()) {
            if (listener != null)
                listener.onLoaded(0);
            return;
        }

        CollectionReference expensesRef = getExpensesCollection();
        if (expensesRef == null) {
            if (listener != null)
                listener.onLoaded(0);
            return;
        }

        expensesRef.whereGreaterThanOrEqualTo("date", startDate)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    double total = 0;
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String type = doc.getString("type");
                        if ("EXPENSE".equals(type)) {
                            Double amount = doc.getDouble("amount");
                            if (amount != null) {
                                total += amount;
                            }
                        }
                    }
                    if (listener != null)
                        listener.onLoaded(total);
                })
                .addOnFailureListener(e -> {
                    if (listener != null)
                        listener.onFailure(e);
                });
    }

    // ==================== CATEGORY MANAGEMENT ====================

    /**
     * Get the top-level categories collection.
     * Categories are stored globally, not per user.
     */
    private CollectionReference getCategoriesCollection() {
        return db.collection("categories");
    }

    /**
     * Listener interface for category operations.
     */
    public interface OnCategoriesLoadedListener {
        void onSuccess(List<Category> categories);

        void onFailure(Exception e);
    }

    /**
     * Fetch expense categories from Firestore.
     * Returns a LiveData that updates when categories change.
     */
    public LiveData<List<Category>> getExpenseCategories() {
        fetchCategoriesByType("EXPENSE", expenseCategories);
        return expenseCategories;
    }

    /**
     * Fetch income categories from Firestore.
     * Returns a LiveData that updates when categories change.
     */
    public LiveData<List<Category>> getIncomeCategories() {
        fetchCategoriesByType("INCOME", incomeCategories);
        return incomeCategories;
    }

    /**
     * Fetch categories by type with one-time read.
     */
    public void fetchCategoriesOnce(String type, OnCategoriesLoadedListener listener) {
        CollectionReference categoriesRef = getCategoriesCollection();

        Log.d(TAG, "Fetching categories of type: " + type);

        categoriesRef
                .whereEqualTo("type", type)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Category> categories = new ArrayList<>();
                    Log.d(TAG, "Query returned " + querySnapshot.size() + " documents");

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Log.d(TAG, "Processing doc: " + doc.getId() + ", data: " + doc.getData());
                        Category category = documentToCategory(doc);
                        if (category != null) {
                            categories.add(category);
                            Log.d(TAG, "Added category: " + category.getName());
                        }
                    }

                    Log.d(TAG, "Fetched " + categories.size() + " " + type + " categories");
                    if (listener != null)
                        listener.onSuccess(categories);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching " + type + " categories", e);
                    if (listener != null)
                        listener.onFailure(e);
                });
    }

    /**
     * Fetch categories by type with real-time updates.
     */
    private void fetchCategoriesByType(String type, MutableLiveData<List<Category>> liveData) {
        if (!isUserLoggedIn()) {
            Log.w(TAG, "fetchCategoriesByType: User not logged in");
            liveData.setValue(new ArrayList<>());
            return;
        }

        CollectionReference categoriesRef = getCategoriesCollection();
        if (categoriesRef == null) {
            liveData.setValue(new ArrayList<>());
            return;
        }

        categoriesRef
                .whereEqualTo("type", type)
                .whereEqualTo("isActive", true)
                .orderBy("order", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to " + type + " categories", error);
                        return;
                    }

                    List<Category> categories = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Category category = documentToCategory(doc);
                            if (category != null) {
                                categories.add(category);
                            }
                        }
                    }

                    // If no categories found, initialize defaults
                    if (categories.isEmpty()) {
                        Log.d(TAG, "No " + type + " categories found, initializing defaults");
                        initializeDefaultCategories(type, null);
                    } else {
                        liveData.setValue(categories);
                        Log.d(TAG, "Fetched " + categories.size() + " " + type + " categories");
                    }
                });
    }

    /**
     * Convert a Firestore document to a Category object.
     * Matches the Firestore structure: name, type, iconName, colorHex, isDefault,
     * createdAt
     */
    private Category documentToCategory(DocumentSnapshot doc) {
        try {
            Category category = new Category();
            category.setId(doc.getId());
            category.setName(doc.getString("name"));
            category.setType(doc.getString("type"));
            category.setIconName(doc.getString("iconName"));
            category.setColorHex(doc.getString("colorHex"));

            Boolean isDefault = doc.getBoolean("isDefault");
            category.setDefault(isDefault != null ? isDefault : false);

            Long createdAt = doc.getLong("createdAt");
            category.setCreatedAt(createdAt != null ? createdAt : 0);

            return category;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing category document: " + doc.getId(), e);
            return null;
        }
    }

    /**
     * Initialize default categories for a user.
     */
    public void initializeDefaultCategories(String type, OnCategoriesLoadedListener listener) {
        CollectionReference categoriesRef = getCategoriesCollection();
        if (categoriesRef == null) {
            if (listener != null)
                listener.onFailure(new Exception("Could not get categories collection"));
            return;
        }

        List<Category> defaults = type.equals("EXPENSE")
                ? getDefaultExpenseCategories()
                : getDefaultIncomeCategories();

        List<Category> resultCategories = new ArrayList<>();
        final int[] completedCount = { 0 };
        final boolean[] hasError = { false };

        for (Category category : defaults) {
            Map<String, Object> categoryData = categoryToMap(category);

            categoriesRef.add(categoryData)
                    .addOnSuccessListener(docRef -> {
                        category.setId(docRef.getId());
                        resultCategories.add(category);
                        completedCount[0]++;

                        if (completedCount[0] == defaults.size() && listener != null) {
                            listener.onSuccess(resultCategories);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error saving default category: " + category.getName(), e);
                        if (!hasError[0]) {
                            hasError[0] = true;
                            if (listener != null)
                                listener.onFailure(e);
                        }
                    });
        }
    }

    /**
     * Save a new category to Firestore.
     */
    public void saveCategory(Category category, OnCategoriesLoadedListener listener) {
        CollectionReference categoriesRef = getCategoriesCollection();
        if (categoriesRef == null) {
            if (listener != null)
                listener.onFailure(new Exception("Could not get categories collection"));
            return;
        }

        Map<String, Object> categoryData = categoryToMap(category);

        categoriesRef.add(categoryData)
                .addOnSuccessListener(docRef -> {
                    category.setId(docRef.getId());
                    Log.d(TAG, "Saved category: " + category.getName() + " with ID: " + docRef.getId());

                    List<Category> result = new ArrayList<>();
                    result.add(category);
                    if (listener != null)
                        listener.onSuccess(result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving category: " + category.getName(), e);
                    if (listener != null)
                        listener.onFailure(e);
                });
    }

    /**
     * Update an existing category in Firestore.
     */
    public void updateCategory(Category category) {
        if (category.getId() == null || category.getId().isEmpty()) {
            Log.w(TAG, "updateCategory: No category ID");
            return;
        }

        CollectionReference categoriesRef = getCategoriesCollection();
        if (categoriesRef == null)
            return;

        Map<String, Object> categoryData = categoryToMap(category);
        categoryData.put("updatedAt", System.currentTimeMillis());

        categoriesRef.document(category.getId())
                .update(categoryData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Updated category: " + category.getName()))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating category", e));
    }

    /**
     * Delete a category (soft delete by setting isActive to false).
     */
    public void deleteCategory(Category category) {
        if (category.getId() == null || category.getId().isEmpty()) {
            Log.w(TAG, "deleteCategory: No category ID");
            return;
        }

        CollectionReference categoriesRef = getCategoriesCollection();
        if (categoriesRef == null)
            return;

        // Soft delete - just mark as inactive
        categoriesRef.document(category.getId())
                .update("isActive", false, "updatedAt", System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Deleted category: " + category.getName()))
                .addOnFailureListener(e -> Log.e(TAG, "Error deleting category", e));
    }

    /**
     * Convert a Category object to a Firestore-compatible map.
     */
    private Map<String, Object> categoryToMap(Category category) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", category.getName());
        map.put("type", category.getType());
        map.put("iconName", category.getIconName());
        map.put("colorHex", category.getColorHex());
        map.put("isDefault", category.isDefault());
        map.put("createdAt", System.currentTimeMillis());
        return map;
    }

    /**
     * Get default expense categories.
     */
    private List<Category> getDefaultExpenseCategories() {
        List<Category> categories = new ArrayList<>();
        // Format: {name, iconName, colorHex}
        String[][] defaults = {
                { "Food", "ic_food", "#F97316" },
                { "Transport", "ic_transport", "#3B82F6" },
                { "Shopping", "ic_shopping", "#EC4899" },
                { "Entertainment", "ic_entertainment", "#8B5CF6" },
                { "Health", "ic_health", "#10B981" },
                { "Bills", "ic_bills", "#EAB308" },
                { "Education", "ic_education", "#06B6D4" },
                { "Travel", "ic_travel", "#F43F5E" },
                { "Groceries", "ic_groceries", "#22C55E" },
                { "Subscription", "ic_subscription", "#A855F7" },
                { "Rent", "ic_bills", "#64748B" },
                { "Insurance", "ic_bills", "#64748B" },
                { "Utilities", "ic_bills", "#EAB308" },
                { "Other", "ic_other", "#64748B" }
        };

        for (int i = 0; i < defaults.length; i++) {
            categories.add(new Category(defaults[i][0], "EXPENSE", defaults[i][1], defaults[i][2], i, true));
        }
        return categories;
    }

    /**
     * Get default income categories.
     */
    private List<Category> getDefaultIncomeCategories() {
        List<Category> categories = new ArrayList<>();
        // Format: {name, iconName, colorHex}
        String[][] defaults = {
                { "Salary", "ic_salary", "#14B8A6" },
                { "Freelance", "ic_freelance", "#F59E0B" },
                { "Investment", "ic_investment", "#3B82F6" },
                { "Gift", "ic_gift", "#DB2777" },
                { "Bonus", "ic_salary", "#14B8A6" },
                { "Refund", "ic_investment", "#6366F1" },
                { "Rental Income", "ic_bills", "#64748B" },
                { "Other", "ic_other", "#64748B" }
        };

        for (int i = 0; i < defaults.length; i++) {
            categories.add(new Category(defaults[i][0], "INCOME", defaults[i][1], defaults[i][2], i, true));
        }
        return categories;
    }

    /**
     * Check if default categories have been initialized for this user.
     * If not, initialize them.
     */
    public void ensureDefaultCategoriesExist() {
        if (!isUserLoggedIn())
            return;

        CollectionReference categoriesRef = getCategoriesCollection();
        if (categoriesRef == null)
            return;

        // Check if any categories exist
        categoriesRef.limit(1).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "No categories found, initializing defaults");
                        initializeDefaultCategories("EXPENSE", null);
                        initializeDefaultCategories("INCOME", null);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking categories", e));
    }
}
