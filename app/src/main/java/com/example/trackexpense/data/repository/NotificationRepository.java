package com.example.trackexpense.data.repository;

import android.util.Log;

import com.example.trackexpense.data.model.AppNotification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing app notifications in Firebase Firestore.
 * Notifications are stored under: users/{userId}/notifications
 * Handles CRUD operations for notifications.
 */
public class NotificationRepository {

    private static final String TAG = "NotificationRepository";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private static NotificationRepository instance;

    private NotificationRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized NotificationRepository getInstance() {
        if (instance == null) {
            instance = new NotificationRepository();
        }
        return instance;
    }

    /**
     * Get the current user ID
     */
    private String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /**
     * Get the notifications collection reference for the current user
     */
    private CollectionReference getNotificationsCollection() {
        String userId = getCurrentUserId();
        if (userId == null)
            return null;
        return db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_NOTIFICATIONS);
    }

    /**
     * Save a new notification to Firebase
     */
    public void saveNotification(AppNotification notification, OnCompleteListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (listener != null)
                listener.onError("User not logged in");
            return;
        }

        notification.setUserId(userId);

        CollectionReference notificationsRef = getNotificationsCollection();
        if (notificationsRef == null) {
            if (listener != null)
                listener.onError("Cannot get notifications collection");
            return;
        }

        notificationsRef.add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Notification saved with ID: " + documentReference.getId());
                    if (listener != null)
                        listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving notification", e);
                    if (listener != null)
                        listener.onError(e.getMessage());
                });
    }

    /**
     * Get all notifications for the current user
     */
    public void getNotifications(OnNotificationsLoadedListener listener) {
        CollectionReference notificationsRef = getNotificationsCollection();
        if (notificationsRef == null) {
            if (listener != null)
                listener.onLoaded(new ArrayList<>()); // Return empty list instead of error
            return;
        }

        // Simple query without ordering (avoids need for composite index)
        notificationsRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<AppNotification> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            AppNotification notification = doc.toObject(AppNotification.class);
                            notification.setId(doc.getId());
                            notifications.add(notification);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing notification: " + doc.getId(), e);
                        }
                    }

                    // Sort locally by createdAt (newest first)
                    notifications.sort((a, b) -> {
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null)
                            return 0;
                        if (a.getCreatedAt() == null)
                            return 1;
                        if (b.getCreatedAt() == null)
                            return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    });

                    Log.d(TAG, "Loaded " + notifications.size() + " notifications");
                    if (listener != null)
                        listener.onLoaded(notifications);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting notifications", e);
                    if (listener != null)
                        listener.onLoaded(new ArrayList<>()); // Return empty on error
                });
    }

    /**
     * Listen to real-time notification updates
     */
    public void listenToNotifications(OnNotificationsLoadedListener listener) {
        CollectionReference notificationsRef = getNotificationsCollection();
        if (notificationsRef == null) {
            if (listener != null)
                listener.onError("User not logged in");
            return;
        }

        notificationsRef
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed", error);
                        if (listener != null)
                            listener.onError(error.getMessage());
                        return;
                    }

                    if (value != null) {
                        List<AppNotification> notifications = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            AppNotification notification = doc.toObject(AppNotification.class);
                            notification.setId(doc.getId());
                            notifications.add(notification);
                        }
                        if (listener != null)
                            listener.onLoaded(notifications);
                    }
                });
    }

    /**
     * Delete a notification permanently
     */
    public void deleteNotification(String notificationId, OnCompleteListener listener) {
        if (notificationId == null || notificationId.isEmpty()) {
            if (listener != null)
                listener.onError("Invalid notification ID");
            return;
        }

        CollectionReference notificationsRef = getNotificationsCollection();
        if (notificationsRef == null) {
            if (listener != null)
                listener.onError("User not logged in");
            return;
        }

        notificationsRef.document(notificationId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification deleted: " + notificationId);
                    if (listener != null)
                        listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting notification", e);
                    if (listener != null)
                        listener.onError(e.getMessage());
                });
    }

    /**
     * Delete all notifications for the current user
     */
    public void deleteAllNotifications(OnCompleteListener listener) {
        CollectionReference notificationsRef = getNotificationsCollection();
        if (notificationsRef == null) {
            if (listener != null)
                listener.onError("User not logged in");
            return;
        }

        notificationsRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        if (listener != null)
                            listener.onSuccess();
                        return;
                    }

                    // Use batched writes for efficiency
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "All notifications deleted");
                                if (listener != null)
                                    listener.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error deleting all notifications", e);
                                if (listener != null)
                                    listener.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching notifications for deletion", e);
                    if (listener != null)
                        listener.onError(e.getMessage());
                });
    }

    /**
     * Mark a notification as read
     */
    public void markAsRead(String notificationId, OnCompleteListener listener) {
        if (notificationId == null || notificationId.isEmpty()) {
            if (listener != null)
                listener.onError("Invalid notification ID");
            return;
        }

        CollectionReference notificationsRef = getNotificationsCollection();
        if (notificationsRef == null) {
            if (listener != null)
                listener.onError("User not logged in");
            return;
        }

        notificationsRef.document(notificationId)
                .update("isRead", true)
                .addOnSuccessListener(aVoid -> {
                    if (listener != null)
                        listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error marking notification as read", e);
                    if (listener != null)
                        listener.onError(e.getMessage());
                });
    }

    /**
     * Mark all notifications as read
     */
    public void markAllAsRead(OnCompleteListener listener) {
        CollectionReference notificationsRef = getNotificationsCollection();
        if (notificationsRef == null) {
            if (listener != null)
                listener.onError("User not logged in");
            return;
        }

        notificationsRef.whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        if (listener != null)
                            listener.onSuccess();
                        return;
                    }

                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        batch.update(doc.getReference(), "isRead", true);
                    }
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "All notifications marked as read");
                                if (listener != null)
                                    listener.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error marking all as read", e);
                                if (listener != null)
                                    listener.onError(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching unread notifications", e);
                    if (listener != null)
                        listener.onError(e.getMessage());
                });
    }

    /**
     * Get unread notification count
     */
    public void getUnreadCount(OnCountListener listener) {
        CollectionReference notificationsRef = getNotificationsCollection();
        if (notificationsRef == null) {
            if (listener != null)
                listener.onCount(0);
            return;
        }

        // Get all notifications and count unread locally
        notificationsRef.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int unreadCount = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Boolean isRead = doc.getBoolean("isRead");
                        if (isRead == null || !isRead) {
                            unreadCount++;
                        }
                    }
                    Log.d(TAG, "Unread count: " + unreadCount);
                    if (listener != null)
                        listener.onCount(unreadCount);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting unread count", e);
                    if (listener != null)
                        listener.onCount(0);
                });
    }

    // Callback interfaces
    public interface OnCompleteListener {
        void onSuccess();

        void onError(String error);
    }

    public interface OnNotificationsLoadedListener {
        void onLoaded(List<AppNotification> notifications);

        void onError(String error);
    }

    public interface OnCountListener {
        void onCount(int count);
    }

    // Helper methods to create notifications

    /**
     * Create notification for transaction created
     */
    public void notifyTransactionCreated(String category, double amount, String transactionType,
            String currencySymbol) {
        String userId = getCurrentUserId();
        if (userId == null)
            return;

        boolean isExpense = "EXPENSE".equals(transactionType);
        String title = isExpense ? "New Expense Added" : "New Income Added";
        String message = String.format("%s%,.0f added in %s", currencySymbol, amount, category);

        AppNotification notification = new AppNotification.Builder(userId, AppNotification.TYPE_TRANSACTION_CREATED)
                .title(title)
                .message(message)
                .amount(amount)
                .category(category)
                .transactionType(transactionType)
                .build();

        saveNotification(notification, null);
    }

    /**
     * Create notification for transaction updated
     */
    public void notifyTransactionUpdated(String category, double amount, String transactionType,
            String currencySymbol) {
        String userId = getCurrentUserId();
        if (userId == null)
            return;

        boolean isExpense = "EXPENSE".equals(transactionType);
        String title = isExpense ? "Expense Updated" : "Income Updated";
        String message = String.format("%s %s of %s%,.0f updated", category,
                isExpense ? "expense" : "income", currencySymbol, amount);

        AppNotification notification = new AppNotification.Builder(userId, AppNotification.TYPE_TRANSACTION_UPDATED)
                .title(title)
                .message(message)
                .amount(amount)
                .category(category)
                .transactionType(transactionType)
                .build();

        saveNotification(notification, null);
    }

    /**
     * Create notification for transaction deleted
     */
    public void notifyTransactionDeleted(String category, double amount, String transactionType,
            String currencySymbol) {
        String userId = getCurrentUserId();
        if (userId == null)
            return;

        boolean isExpense = "EXPENSE".equals(transactionType);
        String title = isExpense ? "Expense Deleted" : "Income Deleted";
        String message = String.format("%s %s of %s%,.0f removed", category,
                isExpense ? "expense" : "income", currencySymbol, amount);

        AppNotification notification = new AppNotification.Builder(userId, AppNotification.TYPE_TRANSACTION_DELETED)
                .title(title)
                .message(message)
                .amount(amount)
                .category(category)
                .transactionType(transactionType)
                .build();

        saveNotification(notification, null);
    }

    /**
     * Create notification for budget exceeded
     */
    public void notifyBudgetExceeded(double spent, double budget, String currencySymbol) {
        String userId = getCurrentUserId();
        if (userId == null)
            return;

        String title = "⚠️ Budget Exceeded!";
        String message = String.format("You've spent %s%,.0f, exceeding your monthly budget of %s%,.0f",
                currencySymbol, spent, currencySymbol, budget);

        AppNotification notification = new AppNotification.Builder(userId, AppNotification.TYPE_BUDGET_EXCEEDED)
                .title(title)
                .message(message)
                .amount(spent)
                .build();

        saveNotification(notification, null);
    }

    /**
     * Create notification for budget warning (80% reached)
     */
    public void notifyBudgetWarning(double spent, double budget, String currencySymbol) {
        String userId = getCurrentUserId();
        if (userId == null)
            return;

        int percentage = (int) ((spent / budget) * 100);
        String title = "⚡ Budget Warning";
        String message = String.format("You've used %d%% of your monthly budget (%s%,.0f of %s%,.0f)",
                percentage, currencySymbol, spent, currencySymbol, budget);

        AppNotification notification = new AppNotification.Builder(userId, AppNotification.TYPE_BUDGET_WARNING)
                .title(title)
                .message(message)
                .amount(spent)
                .build();

        saveNotification(notification, null);
    }

    /**
     * Check budget status and create appropriate notification
     * Call this when a new expense is added
     */
    public void checkBudgetAndNotify(double monthlyExpense, double monthlyBudget, String currencySymbol) {
        if (monthlyBudget <= 0)
            return; // No budget set

        double percentage = (monthlyExpense / monthlyBudget) * 100;

        if (percentage >= 100) {
            notifyBudgetExceeded(monthlyExpense, monthlyBudget, currencySymbol);
        } else if (percentage >= 80 && percentage < 100) {
            // Only notify once when reaching 80% threshold
            // You might want to add logic to prevent duplicate warnings
            notifyBudgetWarning(monthlyExpense, monthlyBudget, currencySymbol);
        }
    }
}
