package com.example.trackexpense.ui.auth;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trackexpense.MainActivity;
import com.example.trackexpense.R;
import com.example.trackexpense.utils.BeautifulNotification;
import com.example.trackexpense.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

public class WelcomeActivity extends AppCompatActivity {

    private MaterialCardView logoCard;
    private View tvWelcome, tvSubtitle, buttonsContainer, btnGuest;
    private View sphere1, sphere2, sphere3, sphere4, sphere5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if already logged in
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_welcome);

        initViews();
        startEntranceAnimations();
        setupListeners();
        checkForLogoutMessage();
    }

    private void checkForLogoutMessage() {
        if (getIntent().getBooleanExtra("SHOW_LOGOUT_MESSAGE", false)) {
            // Show logout notification after a short delay for animations to settle
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                BeautifulNotification.showSuccess(this, "You have been successfully logged out. See you soon!");
            }, 800);
        }
    }

    private void initViews() {
        logoCard = findViewById(R.id.logoCard);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        buttonsContainer = findViewById(R.id.buttonsContainer);
        btnGuest = findViewById(R.id.btnGuest);

        sphere1 = findViewById(R.id.sphere1);
        sphere2 = findViewById(R.id.sphere2);
        sphere3 = findViewById(R.id.sphere3);
        sphere4 = findViewById(R.id.sphere4);
        sphere5 = findViewById(R.id.sphere5);
    }

    private void setupListeners() {
        MaterialButton btnLogin = findViewById(R.id.btnLogin);
        MaterialButton btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        ((TextView) btnGuest).setOnClickListener(v -> {
            // Set guest mode and go to main
            new PreferenceManager(this).setGuestMode(true);
            goToMain();
        });
    }

    private void startEntranceAnimations() {
        // Animate spheres floating in with staggered delays
        animateSpheres();

        // Logo entrance with bounce
        logoCard.setScaleX(0f);
        logoCard.setScaleY(0f);
        logoCard.setRotation(-20f);

        AnimatorSet logoAnimSet = new AnimatorSet();
        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(logoCard, "scaleX", 0f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logoCard, "scaleY", 0f, 1f);
        ObjectAnimator logoRotate = ObjectAnimator.ofFloat(logoCard, "rotation", -20f, 0f);
        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(logoCard, "alpha", 0f, 1f);

        logoAnimSet.playTogether(logoScaleX, logoScaleY, logoRotate, logoAlpha);
        logoAnimSet.setDuration(600);
        logoAnimSet.setInterpolator(new OvershootInterpolator(1.5f));
        logoAnimSet.setStartDelay(200);
        logoAnimSet.start();

        // Welcome text slide in from left
        tvWelcome.setTranslationX(-100f);
        ObjectAnimator welcomeTransX = ObjectAnimator.ofFloat(tvWelcome, "translationX", -100f, 0f);
        ObjectAnimator welcomeAlpha = ObjectAnimator.ofFloat(tvWelcome, "alpha", 0f, 1f);

        AnimatorSet welcomeAnim = new AnimatorSet();
        welcomeAnim.playTogether(welcomeTransX, welcomeAlpha);
        welcomeAnim.setDuration(500);
        welcomeAnim.setInterpolator(new DecelerateInterpolator());
        welcomeAnim.setStartDelay(500);
        welcomeAnim.start();

        // Subtitle fade in with slide up
        tvSubtitle.setTranslationY(30f);
        ObjectAnimator subtitleTransY = ObjectAnimator.ofFloat(tvSubtitle, "translationY", 30f, 0f);
        ObjectAnimator subtitleAlpha = ObjectAnimator.ofFloat(tvSubtitle, "alpha", 0f, 1f);

        AnimatorSet subtitleAnim = new AnimatorSet();
        subtitleAnim.playTogether(subtitleTransY, subtitleAlpha);
        subtitleAnim.setDuration(500);
        subtitleAnim.setInterpolator(new DecelerateInterpolator());
        subtitleAnim.setStartDelay(700);
        subtitleAnim.start();

        // Buttons container slide up from bottom with overshoot
        buttonsContainer.setTranslationY(80f);
        ObjectAnimator buttonsTransY = ObjectAnimator.ofFloat(buttonsContainer, "translationY", 80f, 0f);
        ObjectAnimator buttonsAlpha = ObjectAnimator.ofFloat(buttonsContainer, "alpha", 0f, 1f);

        AnimatorSet buttonsAnim = new AnimatorSet();
        buttonsAnim.playTogether(buttonsTransY, buttonsAlpha);
        buttonsAnim.setDuration(600);
        buttonsAnim.setInterpolator(new OvershootInterpolator(0.8f));
        buttonsAnim.setStartDelay(900);
        buttonsAnim.start();

        // Guest button fade in
        ObjectAnimator guestAlpha = ObjectAnimator.ofFloat(btnGuest, "alpha", 0f, 1f);
        guestAlpha.setDuration(400);
        guestAlpha.setStartDelay(1200);
        guestAlpha.start();

        // Start continuous floating animation for spheres after entrance
        new Handler(Looper.getMainLooper()).postDelayed(this::startFloatingAnimations, 800);
    }

    private void animateSpheres() {
        View[] spheres = { sphere1, sphere2, sphere3, sphere4, sphere5 };
        int[] delays = { 100, 200, 150, 300, 250 };
        float[] startScales = { 0.3f, 0.4f, 0.5f, 0.3f, 0.4f };

        for (int i = 0; i < spheres.length; i++) {
            View sphere = spheres[i];
            float startScale = startScales[i];

            sphere.setScaleX(startScale);
            sphere.setScaleY(startScale);

            AnimatorSet sphereAnim = new AnimatorSet();
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(sphere, "alpha", 0f, 1f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(sphere, "scaleX", startScale, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(sphere, "scaleY", startScale, 1f);

            sphereAnim.playTogether(fadeIn, scaleX, scaleY);
            sphereAnim.setDuration(600);
            sphereAnim.setInterpolator(new OvershootInterpolator(1.2f));
            sphereAnim.setStartDelay(delays[i]);
            sphereAnim.start();
        }
    }

    private void startFloatingAnimations() {
        View[] spheres = { sphere1, sphere2, sphere3, sphere4, sphere5 };
        int[] durations = { 3000, 2500, 2800, 3200, 2600 };
        float[] amplitudes = { 12f, 15f, 10f, 18f, 14f };

        for (int i = 0; i < spheres.length; i++) {
            startFloatingAnimation(spheres[i], durations[i], amplitudes[i]);
        }
    }

    private void startFloatingAnimation(View sphere, int duration, float amplitude) {
        ObjectAnimator floatUp = ObjectAnimator.ofFloat(sphere, "translationY", 0f, -amplitude, 0f);
        floatUp.setDuration(duration);
        floatUp.setInterpolator(new AccelerateDecelerateInterpolator());
        floatUp.setRepeatCount(ValueAnimator.INFINITE);
        floatUp.start();

        // Also add subtle horizontal movement
        ObjectAnimator floatSide = ObjectAnimator.ofFloat(sphere, "translationX", 0f, amplitude * 0.3f, 0f);
        floatSide.setDuration((int) (duration * 1.2f));
        floatSide.setInterpolator(new AccelerateDecelerateInterpolator());
        floatSide.setRepeatCount(ValueAnimator.INFINITE);
        floatSide.start();
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
