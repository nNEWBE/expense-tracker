package com.example.trackexpense.data.repository;

import android.util.Log;

import com.example.trackexpense.data.model.AppNotification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository for managing app notifications in Firebase Firestore.
 * Notifications are stored under: users/{userId}/notifications
 * Handles CRUD operations for notifications.
 * 
 * Performance optimizations:
 * - Caching with configurable cache duration
 * - Parallel loading support
 * - Optimized unread count queries
 * - Limit support for faster initial loads
 */
public class NotificationRepository {

    private static final String TAG = "NotificationRepository";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";

    // Cache configuration - optimized for fast perceived loading
    private static final long CACHE_DURATION_MS = 15000; // 15 seconds cache - short for freshness
    private static final int DEFAULT_NOTIFICATION_LIMIT = 30; // Limit for faster initial load

    // Cached data
    private List<AppNotification> cachedNotifications = null;
    private long cacheTimestamp = 0;
    private int cachedUnreadCount = -1;
    private long unreadCountCacheTimestamp = 0;

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    private static NotificationRepository instance;

    private NotificationRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Enable Firestore offline persistence for faster loads
        db.getFirestoreSettings();
    }

    public static synchronized NotificationRepository getInstance() {
        if (instance == null) {
            instance = new NotificationRepository();
        }
        return instance;
    }

    /**
     * Invalidate all caches. Call this after modifications.
     */
    public void invalidateCache() {
        cachedNotifications = null;
        cacheTimestamp = 0;
        cachedUnreadCount = -1;
        unreadCountCacheTimestamp = 0;
    }

    /**
     * Check if cache is still valid
     */
    private boolean isCacheValid() {
        return cachedNotifications != null &&
                (System.currentTimeMillis() - cacheTimestamp) < CACHE_DURATION_MS;
    }

    /**
     * Check if unread count cache is still valid
     */
    private boolean isUnreadCountCacheValid() {
        return cachedUnreadCount >= 0 &&
                (System.currentTimeMillis() - unreadCountCacheTimestamp) < CACHE_DURATION_MS;
    }

    /**
     * Get cached unread count IMMEDIATELY (synchronously).
     * Returns -1 if no cached value is available.
     * Use this for instant UI updates, then call getUnreadCount() for async
     * refresh.
     */
    public int getCachedUnreadCountSync() {
        if (cachedUnreadCount >= 0) {
            return cachedUnreadCount;
        }
        return -1; // No cached value
    }

    /**
     * Get cached notifications IMMEDIATELY (synchronously).
     * Returns null if no cached list is available.
     * Use this for instant UI updates.
     */
    public List<AppNotification> getCachedNotificationsSync() {
        if (cachedNotifications != null) {
            return new ArrayList<>(cachedNotifications);
        }
        return null;
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
                    invalidateCache(); // Invalidate cache after adding new notification
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
     * Get all notifications for the current user.
     * Uses caching for faster subsequent loads.
     */
    public void getNotifications(OnNotificationsLoadedListener listener) {
        getNotifications(listener, false);
    }

    /**
     * Get notifications with option to force refresh from server.
     * 
     * @param listener     Callback for notifications
     * @param forceRefresh If true, bypasses cache and fetches from server
     */
    public void getNotifications(OnNotificationsLoadedListener listener, boolean forceRefresh) {
        getNotifications(listener, forceRefresh, DEFAULT_NOTIFICATION_LIMIT);
    }

    /**
     * Get notifications with caching and limit support for faster loading.
     * 
     * @param listener     Callback for notifications
     * @param forceRefresh If true, bypasses cache and fetches from server
     * @param limit        Maximum number of notifications to fetch (0 for
     *                     unlimited)
     */
    public void getNotifications(OnNotificationsLoadedListener listener, boolean forceRefresh, int limit) {
        // Return cached data immediately if valid and not forcing refresh
        if (!forceRefresh && isCacheValid()) {
            Log.d(TAG, "Returning " + cachedNotifications.size() + " cached notifications");
            if (listener != null) {
                listener.onLoaded(new ArrayList<>(cachedNotifications));
            }
            return;
        }

        CollectionReference notificationsRef = getNotificationsCollection();
        if (notificationsRef == null) {
            if (listener != null)
                listener.onLoaded(new ArrayList<>()); // Return empty list instead of error
            return;
        }

        // Build query with ordering and optional limit for faster loading
        Query query = notificationsRef.orderBy("createdAt", Query.Direction.DESCENDING);
        if (limit > 0) {
            query = query.limit(limit);
        }

        // Use cache first, then server for faster perceived loading
        Source source = forceRefresh ? Source.SERVER : Source.DEFAULT;

        query.get(source)
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

                    // Update cache
                    cachedNotifications = new ArrayList<>(notifications);
                    cacheTimestamp = System.currentTimeMillis();

                    // Update unread count cache from loaded data
                    int unreadCount = 0;
                    for (AppNotification n : notifications) {
                        if (!n.isRead())
                            unreadCount++;
                    }
                    cachedUnreadCount = unreadCount;
                    unreadCountCacheTimestamp = System.currentTimeMillis();

                    Log.d(TAG, "Loaded " + notifications.size() + " notifications from " +
                            (source == Source.SERVER ? "server" : "cache/server"));
                    if (listener != null)
                        listener.onLoaded(notifications);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting notifications", e);
                    // On failure, try to return cached data if available
                    if (cachedNotifications != null && !cachedNotifications.isEmpty()) {
                        Log.d(TAG, "Returning stale cached data due to error");
                        if (listener != null)
                            listener.onLoaded(new ArrayList<>(cachedNotifications));
                    } else if (listener != null) {
                        listener.onLoaded(new ArrayList<>()); // Return empty on error
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
                    invalidateCache(); // Invalidate cache after deletion
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

        // IMMEDIATELY clear cache for instant UI update
        cachedNotifications = new ArrayList<>();
        cachedUnreadCount = 0;
        cacheTimestamp = System.currentTimeMillis();
        unreadCountCacheTimestamp = System.currentTimeMillis();

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
                                Log.d(TAG, "All notifications deleted from Firebase");
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

        // IMMEDIATELY update cached notification for instant persistence
        if (cachedNotifications != null) {
            for (AppNotification n : cachedNotifications) {
                if (notificationId.equals(n.getId())) {
                    n.setRead(true);
                    break;
                }
            }
            // Also update cached unread count
            if (cachedUnreadCount > 0) {
                cachedUnreadCount--;
            }
        }

        notificationsRef.document(notificationId)
                .update("isRead", true)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Notification marked as read: " + notificationId);
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

        // IMMEDIATELY update cache for instant persistence
        if (cachedNotifications != null) {
            for (AppNotification n : cachedNotifications) {
                n.setRead(true);
            }
        }
        cachedUnreadCount = 0;

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
                                Log.d(TAG, "All notifications marked as read in Firebase");
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
     * Get unread notification count.
     * Optimized to use cached count when available and query only unread docs.
     */
    public void getUnreadCount(OnCountListener listener) {
        // Return cached count immediately if valid
        if (isUnreadCountCacheValid()) {
            Log.d(TAG, "Returning cached unread count: " + cachedUnreadCount);
            if (listener != null) {
                listener.onCount(cachedUnreadCount);
            }
            return;
        }

        CollectionReference notificationsRef = getNotificationsCollection();
        if (notificationsRef == null) {
            if (listener != null)
                listener.onCount(0);
            return;
        }

        // Optimized: Query only unread notifications instead of fetching all
        notificationsRef.whereEqualTo("isRead", false)
                .get(Source.DEFAULT) // Use cache first for speed
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int unreadCount = queryDocumentSnapshots.size();

                    // Update cache
                    cachedUnreadCount = unreadCount;
                    unreadCountCacheTimestamp = System.currentTimeMillis();

                    Log.d(TAG, "Unread count: " + unreadCount);
                    if (listener != null)
                        listener.onCount(unreadCount);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting unread count", e);
                    // Return cached value on error if available
                    if (cachedUnreadCount >= 0) {
                        if (listener != null)
                            listener.onCount(cachedUnreadCount);
                    } else if (listener != null) {
                        listener.onCount(0);
                    }
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

    // Real-time listener for notifications
    private com.google.firebase.firestore.ListenerRegistration activeListener;

    /**
     * Listen to real-time notification updates.
     * This enables instant badge updates when new notifications arrive.
     */
    public void listenToNotifications(OnNotificationsLoadedListener listener) {
        CollectionReference notificationsRef = getNotificationsCollection();
        if (notificationsRef == null) {
            if (listener != null)
                listener.onError("User not logged in");
            return;
        }

        // Remove any existing listener
        if (activeListener != null) {
            activeListener.remove();
        }

        // Add new real-time listener
        activeListener = notificationsRef
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(DEFAULT_NOTIFICATION_LIMIT)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Real-time listener error", e);
                        if (listener != null)
                            listener.onError(e.getMessage());
                        return;
                    }

                    if (queryDocumentSnapshots == null) {
                        if (listener != null)
                            listener.onLoaded(new ArrayList<>());
                        return;
                    }

                    List<AppNotification> notifications = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AppNotification notification = doc.toObject(AppNotification.class);
                        notification.setId(doc.getId());
                        notifications.add(notification);
                    }

                    // Update cache
                    cachedNotifications = new ArrayList<>(notifications);
                    cacheTimestamp = System.currentTimeMillis();

                    // Update unread count cache
                    int unreadCount = 0;
                    for (AppNotification n : notifications) {
                        if (!n.isRead())
                            unreadCount++;
                    }
                    cachedUnreadCount = unreadCount;
                    unreadCountCacheTimestamp = System.currentTimeMillis();

                    Log.d(TAG,
                            "Real-time update: " + notifications.size() + " notifications, " + unreadCount + " unread");

                    if (listener != null) {
                        listener.onLoaded(notifications);
                    }
                });
    }

    /**
     * Remove the real-time listener (call in onDestroy)
     */
    public void removeListener() {
        if (activeListener != null) {
            activeListener.remove();
            activeListener = null;
        }
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

        String title = "Budget Exceeded";
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
        String title = "Budget Warning";
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
