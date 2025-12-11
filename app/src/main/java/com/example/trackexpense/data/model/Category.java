package com.example.trackexpense.data.model;

import android.graphics.Color;

import com.example.trackexpense.R;

/**
 * Model class representing a category stored in Firestore.
 * Categories can be either EXPENSE or INCOME type.
 * 
 * Firestore structure:
 * - name: String (e.g., "Investment")
 * - type: String ("EXPENSE" or "INCOME")
 * - iconName: String (e.g., "ic_investment")
 * - colorHex: String (e.g., "#3B82F6")
 * - isDefault: boolean
 * - createdAt: Long (timestamp)
 */
public class Category {

    private String id; // Firestore document ID
    private String name; // Category name (e.g., "Food", "Salary")
    private String type; // "EXPENSE" or "INCOME"
    private String iconName; // Icon identifier (e.g., "ic_food", "ic_investment")
    private String colorHex; // Hex color string (e.g., "#3B82F6")
    private int order; // Order for sorting
    private boolean isDefault; // Whether this is a default category
    private long createdAt; // Creation timestamp

    // Default constructor for Firestore
    public Category() {
        this.isDefault = false;
    }

    public Category(String name, String type) {
        this.name = name;
        this.type = type;
        this.iconName = "ic_" + name.toLowerCase().replace(" ", "_");
        this.isDefault = false;
        this.order = 0;
    }

    public Category(String name, String type, String iconName, String colorHex) {
        this.name = name;
        this.type = type;
        this.iconName = iconName;
        this.colorHex = colorHex;
        this.isDefault = false;
        this.order = 0;
    }

    public Category(String name, String type, String iconName, String colorHex, int order, boolean isDefault) {
        this.name = name;
        this.type = type;
        this.iconName = iconName;
        this.colorHex = colorHex;
        this.order = order;
        this.isDefault = isDefault;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets the color as an integer from the hex string.
     * Returns a default color if parsing fails.
     */
    public int getColorInt() {
        if (colorHex == null || colorHex.isEmpty()) {
            return Color.parseColor("#64748B"); // Default gray
        }
        try {
            return Color.parseColor(colorHex);
        } catch (Exception e) {
            return Color.parseColor("#64748B");
        }
    }

    /**
     * Gets the drawable resource ID for this category's icon based on iconName.
     */
    public int getIconResource() {
        if (iconName == null)
            return R.drawable.ic_other;

        // Remove "ic_" prefix if present for matching
        String iconKey = iconName.toLowerCase().replace("ic_", "");

        switch (iconKey) {
            case "food":
                return R.drawable.ic_food;
            case "transport":
                return R.drawable.ic_transport;
            case "shopping":
                return R.drawable.ic_shopping;
            case "entertainment":
                return R.drawable.ic_entertainment;
            case "health":
                return R.drawable.ic_health;
            case "bills":
            case "utilities":
                return R.drawable.ic_bills;
            case "education":
                return R.drawable.ic_education;
            case "travel":
                return R.drawable.ic_travel;
            case "groceries":
                return R.drawable.ic_groceries;
            case "subscription":
                return R.drawable.ic_subscription;
            case "salary":
            case "bonus":
                return R.drawable.ic_salary;
            case "freelance":
                return R.drawable.ic_freelance;
            case "investment":
                return R.drawable.ic_investment;
            case "refund":
                return R.drawable.ic_investment;
            case "gift":
                return R.drawable.ic_gift;
            case "rent":
            case "insurance":
            case "rental_income":
                return R.drawable.ic_bills;
            case "other":
                return R.drawable.ic_other;
            default:
                return R.drawable.ic_other;
        }
    }

    /**
     * Gets the color resource ID for this category.
     * Fallback when colorHex is not available.
     */
    public int getColorResource() {
        if (iconName == null)
            return R.color.category_other;

        String iconKey = iconName.toLowerCase().replace("ic_", "");

        switch (iconKey) {
            case "food":
                return R.color.category_food;
            case "transport":
                return R.color.category_transport;
            case "shopping":
                return R.color.category_shopping;
            case "entertainment":
                return R.color.category_entertainment;
            case "health":
                return R.color.category_health;
            case "bills":
            case "utilities":
                return R.color.category_bills;
            case "education":
                return R.color.category_education;
            case "travel":
                return R.color.category_travel;
            case "groceries":
                return R.color.category_groceries;
            case "subscription":
                return R.color.category_subscription;
            case "salary":
            case "bonus":
                return R.color.category_salary;
            case "freelance":
                return R.color.category_freelance;
            case "investment":
            case "refund":
                return R.color.category_investment;
            case "gift":
                return R.color.category_gift;
            case "rent":
            case "insurance":
            case "rental_income":
                return R.color.category_bills;
            default:
                return R.color.category_other;
        }
    }

    /**
     * Gets the background color resource ID for this category.
     */
    public int getBackgroundColorResource() {
        if (iconName == null)
            return R.color.category_other_bg;

        String iconKey = iconName.toLowerCase().replace("ic_", "");

        switch (iconKey) {
            case "food":
                return R.color.category_food_bg;
            case "transport":
                return R.color.category_transport_bg;
            case "shopping":
                return R.color.category_shopping_bg;
            case "entertainment":
                return R.color.category_entertainment_bg;
            case "health":
                return R.color.category_health_bg;
            case "bills":
            case "utilities":
                return R.color.category_bills_bg;
            case "education":
                return R.color.category_education_bg;
            case "travel":
                return R.color.category_travel_bg;
            case "groceries":
                return R.color.category_groceries_bg;
            case "subscription":
                return R.color.category_subscription_bg;
            case "salary":
            case "bonus":
                return R.color.category_salary_bg;
            case "freelance":
                return R.color.category_freelance_bg;
            case "investment":
            case "refund":
                return R.color.category_investment_bg;
            case "gift":
                return R.color.category_gift_bg;
            case "rent":
            case "insurance":
            case "rental_income":
                return R.color.category_bills_bg;
            default:
                return R.color.category_other_bg;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Category category = (Category) o;
        return name != null && name.equals(category.name) &&
                type != null && type.equals(category.type);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Category{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", iconName='" + iconName + '\'' +
                ", colorHex='" + colorHex + '\'' +
                ", order=" + order +
                ", isDefault=" + isDefault +
                '}';
    }
}
