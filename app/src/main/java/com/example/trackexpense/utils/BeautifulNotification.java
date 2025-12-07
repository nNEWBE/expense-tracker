package com.example.trackexpense.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.trackexpense.R;
import com.google.android.material.card.MaterialCardView;

public class BeautifulNotification {

    public enum Type {
        SUCCESS,
        ERROR,
        INFO,
        WARNING
    }

    private static final int NOTIFICATION_DURATION = 3000; // 3 seconds

    public static void show(Activity activity, String title, String message, Type type) {
        if (activity == null || activity.isFinishing())
            return;

        // Inflate the notification layout
        View notificationView = LayoutInflater.from(activity)
                .inflate(R.layout.custom_notification, null);

        // Get references
        MaterialCardView card = notificationView.findViewById(R.id.notificationCard);
        View iconBackground = notificationView.findViewById(R.id.iconBackground);
        ImageView ivIcon = notificationView.findViewById(R.id.ivIcon);
        TextView tvTitle = notificationView.findViewById(R.id.tvTitle);
        TextView tvMessage = notificationView.findViewById(R.id.tvMessage);
        ImageView ivClose = notificationView.findViewById(R.id.ivClose);
        View progressBar = notificationView.findViewById(R.id.progressBar);

        // Set content
        tvTitle.setText(title);
        tvMessage.setText(message);

        // Apply type-specific styling
        applyTypeStyle(activity, type, iconBackground, ivIcon, progressBar, tvTitle);

        // Add to window
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.TOP;
        params.topMargin = getStatusBarHeight(activity) + dpToPx(activity, 16);
        params.leftMargin = dpToPx(activity, 16);
        params.rightMargin = dpToPx(activity, 16);

        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        decorView.addView(notificationView, params);

        // Entrance animation
        animateEntrance(notificationView, card, iconBackground, ivIcon);

        // Progress bar countdown animation
        animateProgressBar(progressBar, NOTIFICATION_DURATION);

        // Close button listener
        ivClose.setOnClickListener(v -> animateExit(notificationView, decorView));

        // Auto dismiss after duration
        notificationView.postDelayed(() -> {
            if (notificationView.getParent() != null) {
                animateExit(notificationView, decorView);
            }
        }, NOTIFICATION_DURATION);
    }

    private static void applyTypeStyle(Activity activity, Type type, View iconBg, ImageView icon, View progress,
            TextView title) {
        int bgColor, iconRes, progressBg;
        String titleColor;

        switch (type) {
            case SUCCESS:
                bgColor = R.drawable.notification_icon_bg_success;
                iconRes = R.drawable.ic_check_circle;
                progressBg = R.drawable.notification_progress_success;
                titleColor = "#1A3A4A"; // App teal theme color
                break;
            case ERROR:
                bgColor = R.drawable.notification_icon_bg_error;
                iconRes = R.drawable.ic_error;
                progressBg = R.drawable.notification_progress_error;
                titleColor = "#EF4444";
                break;
            case WARNING:
                bgColor = R.drawable.notification_icon_bg_warning;
                iconRes = R.drawable.ic_warning;
                progressBg = R.drawable.notification_progress_warning;
                titleColor = "#F59E0B";
                break;
            case INFO:
            default:
                bgColor = R.drawable.notification_icon_bg_info;
                iconRes = R.drawable.ic_info;
                progressBg = R.drawable.notification_progress_info;
                titleColor = "#3B82F6";
                break;
        }

        iconBg.setBackgroundResource(bgColor);
        icon.setImageResource(iconRes);
        progress.setBackgroundResource(progressBg);
        title.setTextColor(android.graphics.Color.parseColor(titleColor));
    }

    private static void animateEntrance(View container, MaterialCardView card, View iconBg, ImageView icon) {
        // Initial state
        container.setTranslationY(-200f);
        container.setAlpha(0f);
        iconBg.setScaleX(0f);
        iconBg.setScaleY(0f);
        icon.setScaleX(0f);
        icon.setScaleY(0f);
        icon.setRotation(-90f);

        // Slide down animation
        ObjectAnimator slideDown = ObjectAnimator.ofFloat(container, "translationY", -200f, 0f);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(container, "alpha", 0f, 1f);

        AnimatorSet containerAnim = new AnimatorSet();
        containerAnim.playTogether(slideDown, fadeIn);
        containerAnim.setDuration(500);
        containerAnim.setInterpolator(new DecelerateInterpolator(1.5f));
        containerAnim.start();

        // Icon bounce animation (delayed)
        ObjectAnimator iconBgScaleX = ObjectAnimator.ofFloat(iconBg, "scaleX", 0f, 1f);
        ObjectAnimator iconBgScaleY = ObjectAnimator.ofFloat(iconBg, "scaleY", 0f, 1f);

        AnimatorSet iconBgAnim = new AnimatorSet();
        iconBgAnim.playTogether(iconBgScaleX, iconBgScaleY);
        iconBgAnim.setDuration(400);
        iconBgAnim.setInterpolator(new OvershootInterpolator(2f));
        iconBgAnim.setStartDelay(200);
        iconBgAnim.start();

        // Checkmark animation
        ObjectAnimator iconScaleX = ObjectAnimator.ofFloat(icon, "scaleX", 0f, 1f);
        ObjectAnimator iconScaleY = ObjectAnimator.ofFloat(icon, "scaleY", 0f, 1f);
        ObjectAnimator iconRotate = ObjectAnimator.ofFloat(icon, "rotation", -90f, 0f);

        AnimatorSet iconAnim = new AnimatorSet();
        iconAnim.playTogether(iconScaleX, iconScaleY, iconRotate);
        iconAnim.setDuration(400);
        iconAnim.setInterpolator(new OvershootInterpolator(1.5f));
        iconAnim.setStartDelay(400);
        iconAnim.start();

        // Card subtle scale
        card.setScaleX(0.95f);
        card.setScaleY(0.95f);
        ObjectAnimator cardScaleX = ObjectAnimator.ofFloat(card, "scaleX", 0.95f, 1f);
        ObjectAnimator cardScaleY = ObjectAnimator.ofFloat(card, "scaleY", 0.95f, 1f);

        AnimatorSet cardAnim = new AnimatorSet();
        cardAnim.playTogether(cardScaleX, cardScaleY);
        cardAnim.setDuration(300);
        cardAnim.setInterpolator(new DecelerateInterpolator());
        cardAnim.setStartDelay(100);
        cardAnim.start();
    }

    private static void animateProgressBar(View progressBar, int duration) {
        ValueAnimator progressAnim = ValueAnimator.ofFloat(1f, 0f);
        progressAnim.setDuration(duration);
        progressAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        progressAnim.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            progressBar.setScaleX(value);
            progressBar.setPivotX(0);
        });
        progressAnim.start();
    }

    private static void animateExit(View container, ViewGroup parent) {
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(container, "translationY", 0f, -100f);
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(container, "alpha", 1f, 0f);

        AnimatorSet exitAnim = new AnimatorSet();
        exitAnim.playTogether(slideUp, fadeOut);
        exitAnim.setDuration(300);
        exitAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        exitAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                parent.removeView(container);
            }
        });
        exitAnim.start();
    }

    private static int getStatusBarHeight(Activity activity) {
        int result = 0;
        int resourceId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = activity.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private static int dpToPx(Activity activity, int dp) {
        float density = activity.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // Convenience methods
    public static void showSuccess(Activity activity, String message) {
        show(activity, "Success!", message, Type.SUCCESS);
    }

    public static void showError(Activity activity, String message) {
        show(activity, "Error", message, Type.ERROR);
    }

    public static void showInfo(Activity activity, String message) {
        show(activity, "Info", message, Type.INFO);
    }

    public static void showWarning(Activity activity, String message) {
        show(activity, "Warning", message, Type.WARNING);
    }
}
