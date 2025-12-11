package com.example.trackexpense.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import com.example.trackexpense.R;

/**
 * Helper class for managing skeleton loading screens.
 * Provides methods to show/hide skeleton layouts with smooth animations.
 */
public class SkeletonHelper {

    private static final int FADE_DURATION = 300;

    /**
     * Show skeleton layout in a container.
     * 
     * @param container         The container to add skeleton to
     * @param skeletonLayoutRes The skeleton layout resource ID
     * @return The inflated skeleton view (to keep reference for hiding)
     */
    public static View showSkeleton(ViewGroup container, int skeletonLayoutRes) {
        View skeleton = LayoutInflater.from(container.getContext())
                .inflate(skeletonLayoutRes, container, false);
        skeleton.setTag("skeleton_view");
        container.addView(skeleton, 0);

        // Fade in animation
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(FADE_DURATION);
        skeleton.startAnimation(fadeIn);

        return skeleton;
    }

    /**
     * Hide and remove skeleton from container.
     * 
     * @param container The container holding the skeleton
     */
    public static void hideSkeleton(ViewGroup container) {
        View skeleton = container.findViewWithTag("skeleton_view");
        if (skeleton != null) {
            // Fade out animation
            AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
            fadeOut.setDuration(FADE_DURATION);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    container.removeView(skeleton);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            skeleton.startAnimation(fadeOut);
        }
    }

    /**
     * Hide skeleton and show content with crossfade animation.
     * 
     * @param container   The container holding the skeleton
     * @param contentView The actual content view to show
     */
    public static void hideSkeletonShowContent(ViewGroup container, View contentView) {
        View skeleton = container.findViewWithTag("skeleton_view");

        // Show content with fade in
        if (contentView != null) {
            contentView.setVisibility(View.VISIBLE);
            AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
            fadeIn.setDuration(FADE_DURATION);
            contentView.startAnimation(fadeIn);
        }

        // Hide skeleton with fade out
        if (skeleton != null) {
            AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
            fadeOut.setDuration(FADE_DURATION);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    container.removeView(skeleton);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            skeleton.startAnimation(fadeOut);
        }
    }

    /**
     * Check if skeleton is currently showing.
     * 
     * @param container The container to check
     * @return true if skeleton is visible
     */
    public static boolean isSkeletonShowing(ViewGroup container) {
        View skeleton = container.findViewWithTag("skeleton_view");
        return skeleton != null && skeleton.getVisibility() == View.VISIBLE;
    }

    /**
     * Replace skeleton view with actual content smoothly.
     * 
     * @param skeletonView The skeleton view to replace
     * @param contentView  The actual content view
     */
    public static void replaceSkeleton(View skeletonView, View contentView) {
        if (skeletonView == null || contentView == null)
            return;

        ViewGroup parent = (ViewGroup) skeletonView.getParent();
        if (parent == null)
            return;

        int index = parent.indexOfChild(skeletonView);

        // Fade out skeleton
        AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
        fadeOut.setDuration(FADE_DURATION);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                parent.removeView(skeletonView);

                // Add content at same position
                parent.addView(contentView, index);

                // Fade in content
                AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
                fadeIn.setDuration(FADE_DURATION);
                contentView.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        skeletonView.startAnimation(fadeOut);
    }
}
