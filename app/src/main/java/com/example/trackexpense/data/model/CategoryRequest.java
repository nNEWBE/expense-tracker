package com.example.trackexpense.data.model;

/**
 * Model for category request that users can submit to admin.
 * Stored in Firestore under: category_requests/{requestId}
 */
public class CategoryRequest {
    private String id;
    private String userId;
    private String userName;
    private String userEmail;
    private String categoryName;
    private String categoryType; // "EXPENSE" or "INCOME"
    private String reason;
    private String status; // "PENDING", "APPROVED", "REJECTED"
    private long createdAt;
    private long updatedAt;

    public CategoryRequest() {
        // Required for Firestore
    }

    public CategoryRequest(String userId, String userName, String userEmail,
            String categoryName, String categoryType, String reason) {
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.categoryName = categoryName;
        this.categoryType = categoryType;
        this.reason = reason;
        this.status = "PENDING";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryType() {
        return categoryType;
    }

    public void setCategoryType(String categoryType) {
        this.categoryType = categoryType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
