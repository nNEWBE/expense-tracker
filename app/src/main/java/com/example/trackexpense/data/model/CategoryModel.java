package com.example.trackexpense.data.model;

public class CategoryModel {
    private String id;
    private String name;
    private String iconName;
    private String colorHex;
    private String type; // "EXPENSE" or "INCOME"
    private boolean isDefault;
    private long createdAt;

    public CategoryModel() {
    }

    public CategoryModel(String name, String iconName, String colorHex, String type) {
        this.name = name;
        this.iconName = iconName;
        this.colorHex = colorHex;
        this.type = type;
        this.isDefault = false;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
