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
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.trackexpense.MainActivity;
import com.example.trackexpense.R;
import com.example.trackexpense.ui.auth.WelcomeActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Random;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private MaterialCardView logoCard;
    private View tvAppName, tvTagline, loadingContainer, tvLoading;
    private ImageView arc1, arc2, arc3;
    private View centerOrb, outerGlow;
    private View sparkle1, sparkle2, sparkle3;
    private View sphere1, sphere2, sphere3, sphere4, sphere5;
    private boolean isAnimating = true;
    private Random random = new Random();

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
        loadingContainer = findViewById(R.id.loadingContainer);
        tvLoading = findViewById(R.id.tvLoading);

        // Arc spinner elements
        arc1 = findViewById(R.id.arc1);
        arc2 = findViewById(R.id.arc2);
        arc3 = findViewById(R.id.arc3);
        centerOrb = findViewById(R.id.centerOrb);
        outerGlow = findViewById(R.id.outerGlow);

        // Sparkles
        sparkle1 = findViewById(R.id.sparkle1);
        sparkle2 = findViewById(R.id.sparkle2);
        sparkle3 = findViewById(R.id.sparkle3);

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

        // Fade in loading container with scale
        loadingContainer.setScaleX(0.5f);
        loadingContainer.setScaleY(0.5f);

        AnimatorSet loadingAppear = new AnimatorSet();
        ObjectAnimator loadingAlpha = ObjectAnimator.ofFloat(loadingContainer, "alpha", 0f, 1f);
        ObjectAnimator loadingScaleX = ObjectAnimator.ofFloat(loadingContainer, "scaleX", 0.5f, 1f);
        ObjectAnimator loadingScaleY = ObjectAnimator.ofFloat(loadingContainer, "scaleY", 0.5f, 1f);
        loadingAppear.playTogether(loadingAlpha, loadingScaleX, loadingScaleY);
        loadingAppear.setDuration(500);
        loadingAppear.setStartDelay(1100);
        loadingAppear.setInterpolator(new OvershootInterpolator(1.5f));
        loadingAppear.start();

        // Fade in loading text
        ObjectAnimator loadingTextAlpha = ObjectAnimator.ofFloat(tvLoading, "alpha", 0f, 1f);
        loadingTextAlpha.setDuration(300);
        loadingTextAlpha.setStartDelay(1300);
        loadingTextAlpha.start();

        // Start the awesome triple arc spinner
        new Handler(Looper.getMainLooper()).postDelayed(this::startTripleArcSpinner, 1100);

        // Start logo pulse after initial animation
        new Handler(Looper.getMainLooper()).postDelayed(this::startLogoPulse, 1000);

        // Navigate after animations complete
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToNextScreen, 3000);
    }

    private void startTripleArcSpinner() {
        // Rotate arc 1 - Slowest, clockwise
        startArcRotation(arc1, 2400, true);

        // Rotate arc 2 - Medium speed, counter-clockwise
        startArcRotation(arc2, 1800, false);

        // Rotate arc 3 - Fastest, clockwise
        startArcRotation(arc3, 1200, true);

        // Pulse the center orb
        startCenterOrbPulse();

        // Pulse the outer glow
        startOuterGlowPulse();

        // Start sparkle animations
        startSparkleAnimations();
    }

    private void startArcRotation(ImageView arc, int duration, boolean clockwise) {
        float startRotation = arc.getRotation();
        float endRotation = clockwise ? startRotation + 360f : startRotation - 360f;

        ObjectAnimator rotation = ObjectAnimator.ofFloat(arc, "rotation", startRotation, endRotation);
        rotation.setDuration(duration);
        rotation.setInterpolator(new LinearInterpolator());
        rotation.setRepeatCount(ValueAnimator.INFINITE);
        rotation.start();
    }

    private void startCenterOrbPulse() {
        // Scale pulse
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(centerOrb, "scaleX", 1f, 1.3f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(centerOrb, "scaleY", 1f, 1.3f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(centerOrb, "alpha", 1f, 0.7f, 1f);

        AnimatorSet pulseSet = new AnimatorSet();
        pulseSet.playTogether(scaleX, scaleY, alpha);
        pulseSet.setDuration(1200);
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

    private void startOuterGlowPulse() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(outerGlow, "scaleX", 1f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(outerGlow, "scaleY", 1f, 1.2f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(outerGlow, "alpha", 0.4f, 0.7f, 0.4f);

        AnimatorSet glowSet = new AnimatorSet();
        glowSet.playTogether(scaleX, scaleY, alpha);
        glowSet.setDuration(1600);
        glowSet.setInterpolator(new AccelerateDecelerateInterpolator());
        glowSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isAnimating) {
                    glowSet.start();
                }
            }
        });
        glowSet.start();
    }

    private void startSparkleAnimations() {
        View[] sparkles = { sparkle1, sparkle2, sparkle3 };

        for (View sparkle : sparkles) {
            animateSparkle(sparkle);
        }
    }

    private void animateSparkle(View sparkle) {
        float density = getResources().getDisplayMetrics().density;

        // Random starting position around the center
        float angle = random.nextFloat() * 360f;
        float startRadius = 15f * density;
        float endRadius = 45f * density;

        double radians = Math.toRadians(angle);
        float startX = (float) (Math.cos(radians) * startRadius);
        float startY = (float) (Math.sin(radians) * startRadius);
        float endX = (float) (Math.cos(radians) * endRadius);
        float endY = (float) (Math.sin(radians) * endRadius);

        sparkle.setTranslationX(startX);
        sparkle.setTranslationY(startY);
        sparkle.setAlpha(0f);
        sparkle.setScaleX(0.3f);
        sparkle.setScaleY(0.3f);

        AnimatorSet sparkleAnim = new AnimatorSet();

        ObjectAnimator moveX = ObjectAnimator.ofFloat(sparkle, "translationX", startX, endX);
        ObjectAnimator moveY = ObjectAnimator.ofFloat(sparkle, "translationY", startY, endY);
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(sparkle, "alpha", 0f, 1f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(sparkle, "scaleX", 0.3f, 1f, 0.3f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(sparkle, "scaleY", 0.3f, 1f, 0.3f);

        sparkleAnim.playTogether(moveX, moveY, alphaAnim, scaleX, scaleY);
        sparkleAnim.setDuration(800 + random.nextInt(400));
        sparkleAnim.setStartDelay(random.nextInt(500));
        sparkleAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        sparkleAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isAnimating) {
                    // Restart with new random angle
                    animateSparkle(sparkle);
                }
            }
        });
        sparkleAnim.start();
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
