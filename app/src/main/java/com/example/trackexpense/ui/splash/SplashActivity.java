package com.example.trackexpense.ui.splash;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.trackexpense.MainActivity;
import com.example.trackexpense.R;
import com.example.trackexpense.ui.auth.WelcomeActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private MaterialCardView logoCard;
    private View tvAppName, tvTagline, orbitContainer, tvLoading;
    private View orbitDot1, orbitDot2, orbitDot3, centerGlow;
    private View sphere1, sphere2, sphere3, sphere4, sphere5;
    private boolean isAnimating = true;

    // Orbit parameters
    private static final float ORBIT_RADIUS_1 = 22f; // dp
    private static final float ORBIT_RADIUS_2 = 18f;
    private static final float ORBIT_RADIUS_3 = 14f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge fullscreen
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_splash);

        splashScreen.setKeepOnScreenCondition(() -> false);

        initViews();
        startAnimations();
    }

    private void initViews() {
        logoCard = findViewById(R.id.logoCard);
        tvAppName = findViewById(R.id.tvAppName);
        tvTagline = findViewById(R.id.tvTagline);
        orbitContainer = findViewById(R.id.orbitContainer);
        tvLoading = findViewById(R.id.tvLoading);

        // Orbit dots
        orbitDot1 = findViewById(R.id.orbitDot1);
        orbitDot2 = findViewById(R.id.orbitDot2);
        orbitDot3 = findViewById(R.id.orbitDot3);
        centerGlow = findViewById(R.id.centerGlow);

        // Spheres
        sphere1 = findViewById(R.id.sphere1);
        sphere2 = findViewById(R.id.sphere2);
        sphere3 = findViewById(R.id.sphere3);
        sphere4 = findViewById(R.id.sphere4);
        sphere5 = findViewById(R.id.sphere5);
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

        // Fade in orbit container and loading text
        ObjectAnimator orbitAlpha = ObjectAnimator.ofFloat(orbitContainer, "alpha", 0f, 1f);
        orbitAlpha.setDuration(400);
        orbitAlpha.setStartDelay(1100);
        orbitAlpha.start();

        ObjectAnimator loadingTextAlpha = ObjectAnimator.ofFloat(tvLoading, "alpha", 0f, 1f);
        loadingTextAlpha.setDuration(300);
        loadingTextAlpha.setStartDelay(1200);
        loadingTextAlpha.start();

        // Start orbiting animation
        new Handler(Looper.getMainLooper()).postDelayed(this::startOrbitAnimation, 1100);

        // Start logo pulse after initial animation
        new Handler(Looper.getMainLooper()).postDelayed(this::startLogoPulse, 1000);

        // Start center glow pulse
        new Handler(Looper.getMainLooper()).postDelayed(this::startCenterGlowPulse, 1100);

        // Navigate after animations complete
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToNextScreen, 3000);
    }

    private void startOrbitAnimation() {
        float density = getResources().getDisplayMetrics().density;

        // Orbit dot 1 - Outer orbit, slower
        startOrbitingDot(orbitDot1, ORBIT_RADIUS_1 * density, 1800, 0);

        // Orbit dot 2 - Middle orbit, medium speed, offset start
        startOrbitingDot(orbitDot2, ORBIT_RADIUS_2 * density, 1400, 120);

        // Orbit dot 3 - Inner orbit, faster, different offset
        startOrbitingDot(orbitDot3, ORBIT_RADIUS_3 * density, 1100, 240);
    }

    private void startOrbitingDot(View dot, float radius, int duration, float startAngle) {
        ValueAnimator orbitAnimator = ValueAnimator.ofFloat(startAngle, startAngle + 360f);
        orbitAnimator.setDuration(duration);
        orbitAnimator.setInterpolator(new LinearInterpolator());
        orbitAnimator.setRepeatCount(ValueAnimator.INFINITE);

        orbitAnimator.addUpdateListener(animation -> {
            if (!isAnimating)
                return;

            float angle = (float) animation.getAnimatedValue();
            double radians = Math.toRadians(angle);

            float x = (float) (Math.cos(radians) * radius);
            float y = (float) (Math.sin(radians) * radius);

            dot.setTranslationX(x);
            dot.setTranslationY(y);

            // Add subtle alpha variation based on position (fade when at top)
            float alphaMod = (float) ((Math.sin(radians) + 1) / 2 * 0.3 + 0.7);
            dot.setAlpha(alphaMod);

            // Add subtle scale variation (larger when closer/bottom)
            float scaleMod = (float) ((Math.sin(radians) + 1) / 2 * 0.2 + 0.9);
            dot.setScaleX(scaleMod);
            dot.setScaleY(scaleMod);
        });

        orbitAnimator.start();
    }

    private void startCenterGlowPulse() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(centerGlow, "scaleX", 1f, 1.5f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(centerGlow, "scaleY", 1f, 1.5f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(centerGlow, "alpha", 0.6f, 1f, 0.6f);

        AnimatorSet pulseSet = new AnimatorSet();
        pulseSet.playTogether(scaleX, scaleY, alpha);
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
