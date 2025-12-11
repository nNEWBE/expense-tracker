package com.example.trackexpense.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Model class for app notifications stored in Firebase.
 * Notification types:
 * - TRANSACTION_CREATED: When a new transaction is added
 * - TRANSACTION_UPDATED: When a transaction is modified
 * - TRANSACTION_DELETED: When a transaction is removed
 * - BUDGET_EXCEEDED: When monthly budget limit is exceeded
 * - BUDGET_WARNING: When approaching budget limit (80%)
 */
public class AppNotification {

    public static final String TYPE_TRANSACTION_CREATED = "TRANSACTION_CREATED";
    public static final String TYPE_TRANSACTION_UPDATED = "TRANSACTION_UPDATED";
    public static final String TYPE_TRANSACTION_DELETED = "TRANSACTION_DELETED";
    public static final String TYPE_BUDGET_EXCEEDED = "BUDGET_EXCEEDED";
    public static final String TYPE_BUDGET_WARNING = "BUDGET_WARNING";

    @DocumentId
    private String id;
    private String userId;
    private String type;
    private String title;
    private String message;
    private double amount;
    private String category;
    private String transactionType; // INCOME or EXPENSE
    private boolean isRead;
    @ServerTimestamp
    private Date createdAt;

    // Required empty constructor for Firestore
    public AppNotification() {
    }

    public AppNotification(String userId, String type, String title, String message) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.isRead = false;
    }

    // Builder pattern for easier construction
    public static class Builder {
        private AppNotification notification;

        public Builder(String userId, String type) {
            notification = new AppNotification();
            notification.userId = userId;
            notification.type = type;
            notification.isRead = false;
        }

        public Builder title(String title) {
            notification.title = title;
            return this;
        }

        public Builder message(String message) {
            notification.message = message;
            return this;
        }

        public Builder amount(double amount) {
            notification.amount = amount;
            return this;
        }

        public Builder category(String category) {
            notification.category = category;
            return this;
        }

        public Builder transactionType(String transactionType) {
            notification.transactionType = transactionType;
            return this;
        }

        public AppNotification build() {
            return notification;
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Get the icon resource for this notification type
     */
    public int getIconResource() {
        switch (type) {
            case TYPE_TRANSACTION_CREATED:
                return com.example.trackexpense.R.drawable.ic_add;
            case TYPE_TRANSACTION_UPDATED:
                return com.example.trackexpense.R.drawable.ic_edit;
            case TYPE_TRANSACTION_DELETED:
                return com.example.trackexpense.R.drawable.ic_delete;
            case TYPE_BUDGET_EXCEEDED:
            case TYPE_BUDGET_WARNING:
                return com.example.trackexpense.R.drawable.ic_warning;
            default:
                return com.example.trackexpense.R.drawable.ic_nav_notifications;
        }
    }

    /**
     * Get the color resource for this notification type
     */
    public int getColorResource() {
        switch (type) {
            case TYPE_TRANSACTION_CREATED:
                return com.example.trackexpense.R.color.income_green;
            case TYPE_TRANSACTION_UPDATED:
                return com.example.trackexpense.R.color.info_blue;
            case TYPE_TRANSACTION_DELETED:
                return com.example.trackexpense.R.color.expense_red;
            case TYPE_BUDGET_EXCEEDED:
                return com.example.trackexpense.R.color.expense_red;
            case TYPE_BUDGET_WARNING:
                return com.example.trackexpense.R.color.warning_yellow;
            default:
                return com.example.trackexpense.R.color.primary;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AppNotification that = (AppNotification) o;

        if (Double.compare(that.amount, amount) != 0)
            return false;
        if (isRead != that.isRead)
            return false;
        if (id != null ? !id.equals(that.id) : that.id != null)
            return false;
        if (userId != null ? !userId.equals(that.userId) : that.userId != null)
            return false;
        if (type != null ? !type.equals(that.type) : that.type != null)
            return false;
        if (title != null ? !title.equals(that.title) : that.title != null)
            return false;
        if (message != null ? !message.equals(that.message) : that.message != null)
            return false;
        if (category != null ? !category.equals(that.category) : that.category != null)
            return false;
        if (transactionType != null ? !transactionType.equals(that.transactionType) : that.transactionType != null)
            return false;
        return createdAt != null ? createdAt.equals(that.createdAt) : that.createdAt == null;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = id != null ? id.hashCode() : 0;
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        temp = Double.doubleToLongBits(amount);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (category != null ? category.hashCode() : 0);
        result = 31 * result + (transactionType != null ? transactionType.hashCode() : 0);
        result = 31 * result + (isRead ? 1 : 0);
        result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
        return result;
    }
}
