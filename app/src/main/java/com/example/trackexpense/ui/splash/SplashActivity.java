package com.example.trackexpense.ui.splash;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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
    private View tvAppName, tvTagline, progressIndicator;
    private View sphere1, sphere2, sphere3, sphere4, sphere5;

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
        progressIndicator = findViewById(R.id.progressIndicator);
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

        // Progress indicator fade in
        ObjectAnimator progressAlpha = ObjectAnimator.ofFloat(progressIndicator, "alpha", 0f, 1f);
        progressAlpha.setDuration(300);
        progressAlpha.setStartDelay(1100);
        progressAlpha.start();

        // Start logo pulse after initial animation
        new Handler(Looper.getMainLooper()).postDelayed(this::startLogoPulse, 1000);

        // Navigate after animations complete
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToNextScreen, 2500);
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
        }
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
                pulseSet.start(); // Loop the pulse
            }
        });
        pulseSet.start();
    }

    private void navigateToNextScreen() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        Intent intent;

        if (auth.getCurrentUser() != null) {
            intent = new Intent(this, MainActivity.class);
        } else {
            intent = new Intent(this, WelcomeActivity.class);
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
