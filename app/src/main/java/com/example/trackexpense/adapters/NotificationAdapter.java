package com.example.trackexpense.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackexpense.R;
import com.example.trackexpense.data.local.Expense;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Expense> notifications = new ArrayList<>();
    private OnNotificationDeleteListener deleteListener;
    private String currencySymbol = "$";

    // Category to icon mapping
    private static final Map<String, Integer> EXPENSE_CATEGORY_ICONS = new HashMap<>();
    private static final Map<String, Integer> INCOME_CATEGORY_ICONS = new HashMap<>();
    private static final Map<String, Integer> CATEGORY_COLORS = new HashMap<>();
    private static final Map<String, Integer> CATEGORY_BG_COLORS = new HashMap<>();

    static {
        // Expense category icons
        EXPENSE_CATEGORY_ICONS.put("Food", R.drawable.ic_food);
        EXPENSE_CATEGORY_ICONS.put("Transport", R.drawable.ic_transport);
        EXPENSE_CATEGORY_ICONS.put("Shopping", R.drawable.ic_shopping);
        EXPENSE_CATEGORY_ICONS.put("Entertainment", R.drawable.ic_entertainment);
        EXPENSE_CATEGORY_ICONS.put("Health", R.drawable.ic_health);
        EXPENSE_CATEGORY_ICONS.put("Bills", R.drawable.ic_bills);
        EXPENSE_CATEGORY_ICONS.put("Education", R.drawable.ic_education);
        EXPENSE_CATEGORY_ICONS.put("Travel", R.drawable.ic_travel);
        EXPENSE_CATEGORY_ICONS.put("Groceries", R.drawable.ic_groceries);
        EXPENSE_CATEGORY_ICONS.put("Subscription", R.drawable.ic_subscription);
        EXPENSE_CATEGORY_ICONS.put("Other", R.drawable.ic_other);

        // Income category icons
        INCOME_CATEGORY_ICONS.put("Salary", R.drawable.ic_salary);
        INCOME_CATEGORY_ICONS.put("Freelance", R.drawable.ic_freelance);
        INCOME_CATEGORY_ICONS.put("Investment", R.drawable.ic_investment);
        INCOME_CATEGORY_ICONS.put("Gift", R.drawable.ic_gift);
        INCOME_CATEGORY_ICONS.put("Other", R.drawable.ic_other);

        // Category colors
        CATEGORY_COLORS.put("Food", R.color.category_food);
        CATEGORY_COLORS.put("Transport", R.color.category_transport);
        CATEGORY_COLORS.put("Shopping", R.color.category_shopping);
        CATEGORY_COLORS.put("Entertainment", R.color.category_entertainment);
        CATEGORY_COLORS.put("Health", R.color.category_health);
        CATEGORY_COLORS.put("Bills", R.color.category_bills);
        CATEGORY_COLORS.put("Education", R.color.category_education);
        CATEGORY_COLORS.put("Travel", R.color.category_travel);
        CATEGORY_COLORS.put("Groceries", R.color.category_groceries);
        CATEGORY_COLORS.put("Subscription", R.color.category_subscription);
        CATEGORY_COLORS.put("Salary", R.color.category_salary);
        CATEGORY_COLORS.put("Freelance", R.color.category_freelance);
        CATEGORY_COLORS.put("Investment", R.color.category_investment);
        CATEGORY_COLORS.put("Gift", R.color.category_gift);
        CATEGORY_COLORS.put("Other", R.color.category_other);

        // Category background colors
        CATEGORY_BG_COLORS.put("Food", R.color.category_food_bg);
        CATEGORY_BG_COLORS.put("Transport", R.color.category_transport_bg);
        CATEGORY_BG_COLORS.put("Shopping", R.color.category_shopping_bg);
        CATEGORY_BG_COLORS.put("Entertainment", R.color.category_entertainment_bg);
        CATEGORY_BG_COLORS.put("Health", R.color.category_health_bg);
        CATEGORY_BG_COLORS.put("Bills", R.color.category_bills_bg);
        CATEGORY_BG_COLORS.put("Education", R.color.category_education_bg);
        CATEGORY_BG_COLORS.put("Travel", R.color.category_travel_bg);
        CATEGORY_BG_COLORS.put("Groceries", R.color.category_groceries_bg);
        CATEGORY_BG_COLORS.put("Subscription", R.color.category_subscription_bg);
        CATEGORY_BG_COLORS.put("Salary", R.color.category_salary_bg);
        CATEGORY_BG_COLORS.put("Freelance", R.color.category_freelance_bg);
        CATEGORY_BG_COLORS.put("Investment", R.color.category_investment_bg);
        CATEGORY_BG_COLORS.put("Gift", R.color.category_gift_bg);
        CATEGORY_BG_COLORS.put("Other", R.color.category_other_bg);
    }

    public interface OnNotificationDeleteListener {
        void onDelete(Expense expense, int position);
    }

    public void setOnNotificationDeleteListener(OnNotificationDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setCurrencySymbol(String symbol) {
        this.currencySymbol = symbol;
    }

    public void setNotifications(List<Expense> notifications) {
        this.notifications = new ArrayList<>(notifications);
        notifyDataSetChanged();
    }

    public void removeNotification(int position) {
        if (position >= 0 && position < notifications.size()) {
            notifications.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void clearAll() {
        int size = notifications.size();
        notifications.clear();
        notifyItemRangeRemoved(0, size);
    }

    public int getNotificationCount() {
        return notifications.size();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Expense expense = notifications.get(position);
        holder.bind(expense, position);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDescription;
        private final TextView tvTime;
        private final TextView tvAmount;
        private final TextView tvTypeIndicator;
        private final ImageView ivIcon;
        private final ImageView btnDelete;
        private final View iconBackground;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDescription = itemView.findViewById(R.id.tvNotificationDescription);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            tvAmount = itemView.findViewById(R.id.tvNotificationAmount);
            tvTypeIndicator = itemView.findViewById(R.id.tvTypeIndicator);
            ivIcon = itemView.findViewById(R.id.ivTransactionIcon);
            btnDelete = itemView.findViewById(R.id.btnDeleteNotification);
            iconBackground = itemView.findViewById(R.id.iconBackground);
        }

        void bind(Expense expense, int position) {
            boolean isExpense = "EXPENSE".equals(expense.getType());
            String category = expense.getCategory();

            // Set category as main text
            tvDescription.setText(category);

            // Set type indicator
            tvTypeIndicator.setText(isExpense ? "Expense" : "Income");
            tvTypeIndicator.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    isExpense ? R.color.expense_red : R.color.income_green));

            // Set type indicator background
            android.graphics.drawable.GradientDrawable indicatorBg = new android.graphics.drawable.GradientDrawable();
            indicatorBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            indicatorBg.setCornerRadius(dpToPx(8));
            indicatorBg.setColor(ContextCompat.getColor(itemView.getContext(),
                    isExpense ? R.color.category_travel_bg : R.color.category_health_bg));
            tvTypeIndicator.setBackground(indicatorBg);

            // Set amount with sign (no decimals)
            String amount = String.format(Locale.getDefault(), "%s%s%,.0f",
                    isExpense ? "-" : "+",
                    currencySymbol,
                    expense.getAmount());
            tvAmount.setText(amount);
            tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    isExpense ? R.color.expense_red : R.color.income_green));

            // Set time
            tvTime.setText(getTimeAgo(expense.getDate()));

            // Get category-specific resources
            int iconRes;
            int colorRes;
            int bgColorRes;

            if (isExpense) {
                iconRes = EXPENSE_CATEGORY_ICONS.getOrDefault(category, R.drawable.ic_other);
                colorRes = CATEGORY_COLORS.getOrDefault(category, R.color.expense_red);
                bgColorRes = CATEGORY_BG_COLORS.getOrDefault(category, R.color.category_other_bg);
            } else {
                iconRes = INCOME_CATEGORY_ICONS.getOrDefault(category, R.drawable.ic_salary);
                colorRes = CATEGORY_COLORS.getOrDefault(category, R.color.income_green);
                bgColorRes = CATEGORY_BG_COLORS.getOrDefault(category, R.color.category_salary_bg);
            }

            // Set icon and colors
            ivIcon.setImageResource(iconRes);
            ivIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), colorRes));

            // Set icon background color
            android.graphics.drawable.GradientDrawable bgDrawable = new android.graphics.drawable.GradientDrawable();
            bgDrawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            bgDrawable.setCornerRadius(dpToPx(14));
            bgDrawable.setColor(ContextCompat.getColor(itemView.getContext(), bgColorRes));
            iconBackground.setBackground(bgDrawable);

            // Delete button
            btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(expense, position);
                }
            });

        }

        private int dpToPx(int dp) {
            float density = itemView.getContext().getResources().getDisplayMetrics().density;
            return Math.round(dp * density);
        }

        private String getTimeAgo(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            long days = TimeUnit.MILLISECONDS.toDays(diff);

            if (minutes < 1) {
                return "Just now";
            } else if (minutes < 60) {
                return minutes + " min ago";
            } else if (hours < 24) {
                return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            } else if (days < 7) {
                return days + " day" + (days > 1 ? "s" : "") + " ago";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
                return sdf.format(new Date(timestamp));
            }
        }
    }
}
