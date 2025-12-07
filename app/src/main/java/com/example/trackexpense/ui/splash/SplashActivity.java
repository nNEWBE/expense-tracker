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
    private View tvAppName, tvTagline, waveContainer, tvLoading;
    private View bar1, bar2, bar3, bar4, bar5;
    private View sphere1, sphere2, sphere3, sphere4, sphere5;
    private boolean isAnimating = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        splashScreen.setKeepOnScreenCondition(() -> false);

        initViews();
        startAnimations();
    }

    private void initViews() {
        logoCard = findViewById(R.id.logoCard);
        tvAppName = findViewById(R.id.tvAppName);
        tvTagline = findViewById(R.id.tvTagline);
        waveContainer = findViewById(R.id.waveContainer);
        tvLoading = findViewById(R.id.tvLoading);

        // Wave bars
        bar1 = findViewById(R.id.bar1);
        bar2 = findViewById(R.id.bar2);
        bar3 = findViewById(R.id.bar3);
        bar4 = findViewById(R.id.bar4);
        bar5 = findViewById(R.id.bar5);

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

        // Fade in wave container and loading text
        ObjectAnimator waveAlpha = ObjectAnimator.ofFloat(waveContainer, "alpha", 0f, 1f);
        waveAlpha.setDuration(400);
        waveAlpha.setStartDelay(1100);
        waveAlpha.start();

        ObjectAnimator loadingTextAlpha = ObjectAnimator.ofFloat(tvLoading, "alpha", 0f, 1f);
        loadingTextAlpha.setDuration(300);
        loadingTextAlpha.setStartDelay(1200);
        loadingTextAlpha.start();

        // Start wave animation
        new Handler(Looper.getMainLooper()).postDelayed(this::startWaveAnimation, 1100);

        // Start logo pulse after initial animation
        new Handler(Looper.getMainLooper()).postDelayed(this::startLogoPulse, 1000);

        // Navigate after animations complete
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToNextScreen, 3000);
    }

    private void startWaveAnimation() {
        View[] bars = { bar1, bar2, bar3, bar4, bar5 };
        int[] delays = { 0, 80, 160, 240, 320 };

        for (int i = 0; i < bars.length; i++) {
            animateWaveBar(bars[i], delays[i], i);
        }
    }

    private void animateWaveBar(View bar, int delay, int index) {
        ObjectAnimator scaleAnim = ObjectAnimator.ofFloat(bar, "scaleY", 0.4f, 1.8f, 0.4f);
        scaleAnim.setDuration(600 + (index * 50));
        scaleAnim.setStartDelay(delay);
        scaleAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleAnim.setRepeatCount(ValueAnimator.INFINITE);
        scaleAnim.start();

        // Alpha pulse effect
        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(bar, "alpha", 0.6f, 1f, 0.6f);
        alphaAnim.setDuration(600 + (index * 50));
        alphaAnim.setStartDelay(delay);
        alphaAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        alphaAnim.setRepeatCount(ValueAnimator.INFINITE);
        alphaAnim.start();
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
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
