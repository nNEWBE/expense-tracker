package com.example.trackexpense.ui.splash;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.trackexpense.MainActivity;
import com.example.trackexpense.R;
import com.example.trackexpense.ui.auth.WelcomeActivity;
import com.example.trackexpense.utils.PreferenceManager;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private MaterialCardView logoCard;
    private View tvAppName, tvTagline, loadingContainer, tvLoading;
    private View ringOuter1, ringOuter2, ringMain, currencyContainer;
    private TextView tvCurrencySymbol;
    private View sphere1, sphere2, sphere3, sphere4, sphere5;
    private boolean isAnimating = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge fullscreen
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_splash);

        splashScreen.setKeepOnScreenCondition(() -> false);

        initViews();
        setupCurrencySymbol();
        startAnimations();
    }

    private void initViews() {
        logoCard = findViewById(R.id.logoCard);
        tvAppName = findViewById(R.id.tvAppName);
        tvTagline = findViewById(R.id.tvTagline);
        loadingContainer = findViewById(R.id.loadingContainer);
        tvLoading = findViewById(R.id.tvLoading);

        // Loading ring elements
        ringOuter1 = findViewById(R.id.ringOuter1);
        ringOuter2 = findViewById(R.id.ringOuter2);
        ringMain = findViewById(R.id.ringMain);
        currencyContainer = findViewById(R.id.currencyContainer);
        tvCurrencySymbol = findViewById(R.id.tvCurrencySymbol);

        // Spheres
        sphere1 = findViewById(R.id.sphere1);
        sphere2 = findViewById(R.id.sphere2);
        sphere3 = findViewById(R.id.sphere3);
        sphere4 = findViewById(R.id.sphere4);
        sphere5 = findViewById(R.id.sphere5);
    }

    private void setupCurrencySymbol() {
        // Use user's preferred currency symbol
        try {
            PreferenceManager preferenceManager = new PreferenceManager(this);
            String symbol = preferenceManager.getCurrencySymbol();
            if (tvCurrencySymbol != null && symbol != null) {
                tvCurrencySymbol.setText(symbol);
            }
        } catch (Exception e) {
            // Use default symbol if preference not available
        }
    }

    private void startAnimations() {
        // Animate spheres floating in
        animateSpheres();

        // Logo bounce in animation
        logoCard.setScaleX(0f);
        logoCard.setScaleY(0f);
        logoCard.setRotation(-30f);
        logoCard.setAlpha(0f);

        AnimatorSet logoAnimSet = new AnimatorSet();
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(logoCard, "scaleX", 0f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logoCard, "scaleY", 0f, 1f);
        ObjectAnimator logoRotate = ObjectAnimator.ofFloat(logoCard, "rotation", -30f, 0f);
        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(logoCard, "alpha", 0f, 1f);

        logoAnimSet.playTogether(logoScaleX, logoScaleY, logoRotate, logoAlpha);
        logoAnimSet.setDuration(700);
        logoAnimSet.setInterpolator(new OvershootInterpolator(1.2f));
        logoAnimSet.setStartDelay(200);
        logoAnimSet.start();

        // App name slide up
        tvAppName.setTranslationY(50f);
        tvAppName.setAlpha(0f);

        ObjectAnimator nameTransY = ObjectAnimator.ofFloat(tvAppName, "translationY", 50f, 0f);
        ObjectAnimator nameAlpha = ObjectAnimator.ofFloat(tvAppName, "alpha", 0f, 1f);

        AnimatorSet nameAnimSet = new AnimatorSet();
        nameAnimSet.playTogether(nameTransY, nameAlpha);
        nameAnimSet.setDuration(500);
        nameAnimSet.setInterpolator(new DecelerateInterpolator());
        nameAnimSet.setStartDelay(600);
        nameAnimSet.start();

        // Tagline fade in
        tvTagline.setTranslationY(30f);
        tvTagline.setAlpha(0f);

        ObjectAnimator tagTransY = ObjectAnimator.ofFloat(tvTagline, "translationY", 30f, 0f);
        ObjectAnimator tagAlpha = ObjectAnimator.ofFloat(tvTagline, "alpha", 0f, 0.8f);

        AnimatorSet tagAnimSet = new AnimatorSet();
        tagAnimSet.playTogether(tagTransY, tagAlpha);
        tagAnimSet.setDuration(400);
        tagAnimSet.setInterpolator(new DecelerateInterpolator());
        tagAnimSet.setStartDelay(900);
        tagAnimSet.start();

        // Fade in loading container
        ObjectAnimator loadingAlpha = ObjectAnimator.ofFloat(loadingContainer, "alpha", 0f, 1f);
        loadingAlpha.setDuration(400);
        loadingAlpha.setStartDelay(1100);
        loadingAlpha.start();

        // Fade in loading text
        ObjectAnimator loadingTextAlpha = ObjectAnimator.ofFloat(tvLoading, "alpha", 0f, 1f);
        loadingTextAlpha.setDuration(300);
        loadingTextAlpha.setStartDelay(1200);
        loadingTextAlpha.start();

        // Start currency pulse animation
        new Handler(Looper.getMainLooper()).postDelayed(this::startCurrencyPulseAnimation, 1100);

        // Start logo pulse after initial animation
        new Handler(Looper.getMainLooper()).postDelayed(this::startLogoPulse, 1000);

        // Navigate after animations complete
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToNextScreen, 3000);
    }

    private void startCurrencyPulseAnimation() {
        // Start the main ring breathing animation
        startRingBreathing();

        // Start the expanding ripple rings
        startRingRipple(ringOuter1, 0);
        startRingRipple(ringOuter2, 800);

        // Start currency symbol pulse
        startCurrencyPulse();
    }

    private void startRingBreathing() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ringMain, "scaleX", 1f, 1.15f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ringMain, "scaleY", 1f, 1.15f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(ringMain, "alpha", 1f, 0.7f, 1f);

        AnimatorSet breatheSet = new AnimatorSet();
        breatheSet.playTogether(scaleX, scaleY, alpha);
        breatheSet.setDuration(1600);
        breatheSet.setInterpolator(new AccelerateDecelerateInterpolator());
        breatheSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isAnimating) {
                    breatheSet.start();
                }
            }
        });
        breatheSet.start();
    }

    private void startRingRipple(View ring, int delay) {
        // Reset initial state
        ring.setScaleX(0.5f);
        ring.setScaleY(0.5f);
        ring.setAlpha(0f);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ring, "scaleX", 0.5f, 1.5f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ring, "scaleY", 0.5f, 1.5f);
        ObjectAnimator alphaIn = ObjectAnimator.ofFloat(ring, "alpha", 0f, 0.8f, 0f);

        AnimatorSet rippleSet = new AnimatorSet();
        rippleSet.playTogether(scaleX, scaleY, alphaIn);
        rippleSet.setDuration(1600);
        rippleSet.setStartDelay(delay);
        rippleSet.setInterpolator(new AccelerateInterpolator(0.5f));
        rippleSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isAnimating) {
                    // Reset and restart
                    ring.setScaleX(0.5f);
                    ring.setScaleY(0.5f);
                    ring.setAlpha(0f);
                    rippleSet.setStartDelay(0);
                    rippleSet.start();
                }
            }
        });
        rippleSet.start();
    }

    private void startCurrencyPulse() {
        // Currency container subtle pulse
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(currencyContainer, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(currencyContainer, "scaleY", 1f, 1.1f, 1f);

        AnimatorSet pulseSet = new AnimatorSet();
        pulseSet.playTogether(scaleX, scaleY);
        pulseSet.setDuration(1600);
        pulseSet.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isAnimating) {
                    pulseSet.start();
                }
            }
        });
        pulseSet.start();

        // Currency symbol subtle rotation wiggle
        ObjectAnimator rotation = ObjectAnimator.ofFloat(tvCurrencySymbol, "rotation", 0f, 5f, -5f, 0f);
        rotation.setDuration(2000);
        rotation.setInterpolator(new AccelerateDecelerateInterpolator());
        rotation.setRepeatCount(ValueAnimator.INFINITE);
        rotation.start();
    }

    private void animateSpheres() {
        View[] spheres = { sphere1, sphere2, sphere3, sphere4, sphere5 };
        int[] delays = { 100, 200, 150, 300, 250 };

        for (int i = 0; i < spheres.length; i++) {
            View sphere = spheres[i];
            sphere.setAlpha(0f);
            sphere.setScaleX(0.5f);
            sphere.setScaleY(0.5f);

            AnimatorSet sphereAnim = new AnimatorSet();
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(sphere, "alpha", 0f, 1f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(sphere, "scaleX", 0.5f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(sphere, "scaleY", 0.5f, 1f);

            sphereAnim.playTogether(fadeIn, scaleX, scaleY);
            sphereAnim.setDuration(500);
            sphereAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            sphereAnim.setStartDelay(delays[i]);
            sphereAnim.start();

            // Add floating animation
            startFloatingAnimation(sphere, i * 200);
        }
    }

    private void startFloatingAnimation(View sphere, int delay) {
        ObjectAnimator floatUp = ObjectAnimator.ofFloat(sphere, "translationY", 0f, -15f, 0f);
        floatUp.setDuration(2500 + delay);
        floatUp.setStartDelay(delay);
        floatUp.setInterpolator(new AccelerateDecelerateInterpolator());
        floatUp.setRepeatCount(ValueAnimator.INFINITE);
        floatUp.start();
    }

    private void startLogoPulse() {
        ObjectAnimator pulseX = ObjectAnimator.ofFloat(logoCard, "scaleX", 1f, 1.05f, 1f);
        ObjectAnimator pulseY = ObjectAnimator.ofFloat(logoCard, "scaleY", 1f, 1.05f, 1f);

        AnimatorSet pulseSet = new AnimatorSet();
        pulseSet.playTogether(pulseX, pulseY);
        pulseSet.setDuration(1500);
        pulseSet.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isAnimating) {
                    pulseSet.start();
                }
            }
        });
        pulseSet.start();
    }

    private void navigateToNextScreen() {
        isAnimating = false;
        FirebaseAuth auth = FirebaseAuth.getInstance();
        Intent intent;

        if (auth.getCurrentUser() != null) {
            // Check if email is verified
            if (auth.getCurrentUser().isEmailVerified()) {
                // User is logged in and email is verified - go to main app
                intent = new Intent(this, MainActivity.class);
            } else {
                // User is logged in but email is NOT verified - go to verification screen
                intent = new Intent(this, com.example.trackexpense.ui.auth.EmailVerificationActivity.class);
                intent.putExtra("USER_NAME", auth.getCurrentUser().getDisplayName());
            }
        } else {
            // No user logged in - go to welcome screen
            intent = new Intent(this, WelcomeActivity.class);
        }

        startActivity(intent);
        finish();
    }
}
