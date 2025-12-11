package com.example.trackexpense.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
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
 * Adapter for displaying Firebase-backed app notifications.
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

    public void setNotifications(List<AppNotification> newNotifications) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return notifications.size();
            }

            @Override
            public int getNewListSize() {
                return newNotifications.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                String oldId = notifications.get(oldItemPosition).getId();
                String newId = newNotifications.get(newItemPosition).getId();
                return oldId != null && oldId.equals(newId);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return notifications.get(oldItemPosition).equals(newNotifications.get(newItemPosition));
            }
        });

        this.notifications = new ArrayList<>(newNotifications);
        diffResult.dispatchUpdatesTo(this);
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
        private final TextView tvType;
        private final ImageView ivIcon;
        private final ImageView btnDelete;
        private final View iconBackground;
        private final View unreadIndicator;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            tvType = itemView.findViewById(R.id.tvNotificationType);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
            btnDelete = itemView.findViewById(R.id.btnDeleteNotification);
            iconBackground = itemView.findViewById(R.id.iconBackground);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }

        void bind(AppNotification notification, int position) {
            // Set title and message
            tvTitle.setText(notification.getTitle());
            tvMessage.setText(notification.getMessage());

            // Set time
            Date createdAt = notification.getCreatedAt();
            if (createdAt != null) {
                tvTime.setText(getTimeAgo(createdAt.getTime()));
            } else {
                tvTime.setText("Just now");
            }

            // Set type badge
            String typeText = getTypeDisplayText(notification.getType());
            tvType.setText(typeText);
            tvType.setTextColor(ContextCompat.getColor(itemView.getContext(), notification.getColorResource()));

            // Set type badge background
            android.graphics.drawable.GradientDrawable typeBg = new android.graphics.drawable.GradientDrawable();
            typeBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            typeBg.setCornerRadius(dpToPx(8));
            typeBg.setColor(getTypeBackgroundColor(notification.getType()));
            tvType.setBackground(typeBg);

            // Set icon
            ivIcon.setImageResource(notification.getIconResource());
            ivIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), notification.getColorResource()));

            // Set icon background
            android.graphics.drawable.GradientDrawable iconBg = new android.graphics.drawable.GradientDrawable();
            iconBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            iconBg.setCornerRadius(dpToPx(14));
            iconBg.setColor(getTypeBackgroundColor(notification.getType()));
            iconBackground.setBackground(iconBg);

            // Show/hide unread indicator
            if (unreadIndicator != null) {
                unreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
            }

            // Delete button click
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDelete(notification, position);
                }
            });

            // Item click
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onClick(notification);
                }
            });
        }

        private String getTypeDisplayText(String type) {
            switch (type) {
                case AppNotification.TYPE_TRANSACTION_CREATED:
                    return "Created";
                case AppNotification.TYPE_TRANSACTION_UPDATED:
                    return "Updated";
                case AppNotification.TYPE_TRANSACTION_DELETED:
                    return "Deleted";
                case AppNotification.TYPE_BUDGET_EXCEEDED:
                    return "Alert";
                case AppNotification.TYPE_BUDGET_WARNING:
                    return "Warning";
                default:
                    return "Info";
            }
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
