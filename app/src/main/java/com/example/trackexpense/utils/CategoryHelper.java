package com.example.trackexpense.utils;

import com.example.trackexpense.R;

public class CategoryHelper {

    public static class CategoryInfo {
        public final int iconRes;
        public final int colorRes;
        public final int bgColorRes;

        public CategoryInfo(int iconRes, int colorRes, int bgColorRes) {
            this.iconRes = iconRes;
            this.colorRes = colorRes;
            this.bgColorRes = bgColorRes;
        }
    }

    public static final String[] EXPENSE_CATEGORIES = {
            "Food", "Transport", "Shopping", "Entertainment", "Health",
            "Bills", "Education", "Travel", "Groceries", "Subscription",
            "Rent", "Insurance", "Utilities", "Other"
    };

    public static final String[] INCOME_CATEGORIES = {
            "Salary", "Freelance", "Investment", "Gift", "Bonus",
            "Refund", "Rental Income", "Other"
    };

    public static final String[] ALL_CATEGORIES = {
            "Food", "Transport", "Shopping", "Entertainment", "Health",
            "Bills", "Education", "Travel", "Groceries", "Subscription",
            "Salary", "Freelance", "Investment", "Gift", "Rent",
            "Insurance", "Utilities", "Bonus", "Refund", "Other"
    };

    public static CategoryInfo getCategoryInfo(String category) {
        if (category == null) {
            return new CategoryInfo(R.drawable.ic_other, R.color.category_other, R.color.category_other_bg);
        }

        switch (category.toLowerCase()) {
            case "food":
                return new CategoryInfo(R.drawable.ic_food, R.color.category_food, R.color.category_food_bg);
            case "transport":
                return new CategoryInfo(R.drawable.ic_transport, R.color.category_transport,
                        R.color.category_transport_bg);
            case "shopping":
                return new CategoryInfo(R.drawable.ic_shopping, R.color.category_shopping,
                        R.color.category_shopping_bg);
            case "entertainment":
                return new CategoryInfo(R.drawable.ic_entertainment, R.color.category_entertainment,
                        R.color.category_entertainment_bg);
            case "health":
                return new CategoryInfo(R.drawable.ic_health, R.color.category_health, R.color.category_health_bg);
            case "bills":
            case "utilities":
                return new CategoryInfo(R.drawable.ic_bills, R.color.category_bills, R.color.category_bills_bg);
            case "education":
                return new CategoryInfo(R.drawable.ic_education, R.color.category_education,
                        R.color.category_education_bg);
            case "travel":
                return new CategoryInfo(R.drawable.ic_travel, R.color.category_travel, R.color.category_travel_bg);
            case "groceries":
                return new CategoryInfo(R.drawable.ic_groceries, R.color.category_groceries,
                        R.color.category_groceries_bg);
            case "subscription":
                return new CategoryInfo(R.drawable.ic_subscription, R.color.category_subscription,
                        R.color.category_subscription_bg);
            case "salary":
            case "bonus":
                return new CategoryInfo(R.drawable.ic_salary, R.color.category_salary, R.color.category_salary_bg);
            case "freelance":
                return new CategoryInfo(R.drawable.ic_freelance, R.color.category_freelance,
                        R.color.category_freelance_bg);
            case "investment":
            case "refund":
                return new CategoryInfo(R.drawable.ic_investment, R.color.category_investment,
                        R.color.category_investment_bg);
            case "gift":
                return new CategoryInfo(R.drawable.ic_gift, R.color.category_gift, R.color.category_gift_bg);
            case "rent":
            case "insurance":
            case "rental income":
                return new CategoryInfo(R.drawable.ic_bills, R.color.category_bills, R.color.category_bills_bg);
            default:
                return new CategoryInfo(R.drawable.ic_other, R.color.category_other, R.color.category_other_bg);
        }
    }
}
