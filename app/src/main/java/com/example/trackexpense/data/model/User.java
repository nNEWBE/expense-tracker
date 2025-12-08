package com.example.trackexpense.data.model;

public class User {
    private String id;
    private String email;
    private String displayName;
    private String photoUrl;
    private boolean isBlocked;
    private boolean isAdmin;
    private boolean isVerified;
    private long createdAt;
    private long lastLoginAt;
    private int transactionCount;

    public User() {
    }

    public User(String id, String email, String displayName) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.isBlocked = false;
        this.isAdmin = false;
        this.isVerified = false;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(long lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }
}
