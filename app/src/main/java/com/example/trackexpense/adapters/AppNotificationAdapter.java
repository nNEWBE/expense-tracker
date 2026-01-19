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
import com.example.trackexpense.data.model.AppNotification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Lightweight adapter for displaying notifications.
 * Optimized for fast rendering with minimal view binding complexity.
 */
public class AppNotificationAdapter extends RecyclerView.Adapter<AppNotificationAdapter.NotificationViewHolder> {

    private List<AppNotification> notifications = new ArrayList<>();
    private OnNotificationActionListener listener;
    private String currencySymbol = "৳";

    public interface OnNotificationActionListener {
        void onDelete(AppNotification notification, int position);

        void onClick(AppNotification notification);
    }

    public void setOnNotificationActionListener(OnNotificationActionListener listener) {
        this.listener = listener;
    }

    public void setCurrencySymbol(String symbol) {
        this.currencySymbol = symbol;
    }

    /**
     * Fast update method - directly replaces list without DiffUtil for speed
     */
    public void setNotifications(List<AppNotification> newNotifications) {
        this.notifications = new ArrayList<>(newNotifications);
        notifyDataSetChanged();
    }

    public void removeNotification(int position) {
        if (position >= 0 && position < notifications.size()) {
            notifications.remove(position);
            notifyItemRemoved(position);
        }
    }

    public int getNotificationCount() {
        return notifications.size();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(notifications.get(position), position);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvMessage;
        private final TextView tvTime;
        private final ImageView ivIcon;
        private final ImageView btnDelete;
        private final View iconBackground;
        private final View unreadIndicator;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
            btnDelete = itemView.findViewById(R.id.btnDeleteNotification);
            iconBackground = itemView.findViewById(R.id.iconBackground);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }

        void bind(AppNotification notification, int position) {
            // Set title and message
            tvTitle.setText(notification.getTitle());
            tvMessage.setText(notification.getMessage());

            // Set time - compact format
            Date createdAt = notification.getCreatedAt();
            if (createdAt != null) {
                tvTime.setText(getCompactTimeAgo(createdAt.getTime()));
            } else {
                tvTime.setText("Now");
            }

            // Set icon
            ivIcon.setImageResource(notification.getIconResource());
            int iconColor = ContextCompat.getColor(itemView.getContext(), notification.getColorResource());
            ivIcon.setColorFilter(iconColor);

            // Set icon background
            android.graphics.drawable.GradientDrawable iconBg = new android.graphics.drawable.GradientDrawable();
            iconBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            iconBg.setColor(getTypeBackgroundColor(notification.getType()));
            iconBackground.setBackground(iconBg);

            // Show/hide unread indicator
            if (unreadIndicator != null) {
                if (!notification.isRead()) {
                    unreadIndicator.setVisibility(View.VISIBLE);
                    unreadIndicator.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(iconColor));
                } else {
                    unreadIndicator.setVisibility(View.GONE);
                }
            }

            // Make unread items slightly bolder
            tvTitle.setAlpha(notification.isRead() ? 0.7f : 1.0f);
            tvMessage.setAlpha(notification.isRead() ? 0.6f : 0.8f);

            // Delete button click
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(notification, position);
                }
            });

            // Item click - opens details dialog
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(notification);
                }
            });
        }

        private int getTypeBackgroundColor(String type) {
            int colorRes;
            switch (type) {
                case AppNotification.TYPE_TRANSACTION_CREATED:
                    colorRes = R.color.category_health_bg;
                    break;
                case AppNotification.TYPE_TRANSACTION_UPDATED:
                    colorRes = R.color.category_transport_bg;
                    break;
                case AppNotification.TYPE_TRANSACTION_DELETED:
                case AppNotification.TYPE_BUDGET_EXCEEDED:
                    colorRes = R.color.category_travel_bg;
                    break;
                case AppNotification.TYPE_BUDGET_WARNING:
                    colorRes = R.color.category_bills_bg;
                    break;
                default:
                    colorRes = R.color.category_other_bg;
            }
            return ContextCompat.getColor(itemView.getContext(), colorRes);
        }

        /**
         * Compact time format: 2m, 3h, 1d, Dec 12
         */
        private String getCompactTimeAgo(long timestamp) {
            long now = System.currentTimeMillis();
            long diff = now - timestamp;

            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            long days = TimeUnit.MILLISECONDS.toDays(diff);

            if (minutes < 1) {
                return "Now";
            } else if (minutes < 60) {
                return minutes + "m";
            } else if (hours < 24) {
                return hours + "h";
            } else if (days < 7) {
                return days + "d";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
                return sdf.format(new Date(timestamp));
            }
        }
    }
}
