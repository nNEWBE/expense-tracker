package com.example.trackexpense.ui.auth;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ScrollView;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trackexpense.MainActivity;
import com.example.trackexpense.R;
import com.example.trackexpense.utils.BeautifulNotification;
import com.example.trackexpense.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private MaterialButton btnRegister;
    private CircularProgressIndicator progressBar;
    private FirebaseAuth mAuth;

    // Animation views
    private MaterialCardView formCard;
    private View sphere1, sphere2, sphere3;
    private View tvTitle, tvNameLabel, nameContainer, tvEmailLabel, emailContainer;
    private View tvPasswordLabel, passwordContainer;
    private View loginContainer, dividerContainer, btnGuest, tvBack;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        startEntranceAnimations();
        setupListeners();
    }

    private void initViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);

        // Animation views
        formCard = findViewById(R.id.formCard);
        sphere1 = findViewById(R.id.sphere1);
        sphere2 = findViewById(R.id.sphere2);
        sphere3 = findViewById(R.id.sphere3);
        tvTitle = findViewById(R.id.tvTitle);
        tvNameLabel = findViewById(R.id.tvNameLabel);
        nameContainer = findViewById(R.id.nameContainer);
        tvEmailLabel = findViewById(R.id.tvEmailLabel);
        emailContainer = findViewById(R.id.emailContainer);
        tvPasswordLabel = findViewById(R.id.tvPasswordLabel);
        passwordContainer = findViewById(R.id.passwordContainer);
        loginContainer = findViewById(R.id.loginContainer);
        dividerContainer = findViewById(R.id.dividerContainer);
        btnGuest = findViewById(R.id.btnGuest);
        tvBack = findViewById(R.id.tvBack);
        scrollView = findViewById(R.id.scrollView);

        // Setup auto-scroll when fields get focus
        setupAutoScrollOnFocus();
    }

    private void setupAutoScrollOnFocus() {
        View.OnFocusChangeListener scrollToFocusedView = (v, hasFocus) -> {
            if (hasFocus && scrollView != null) {
                // Post to ensure view is laid out
                scrollView.post(() -> {
                    // Scroll the view into visible area
                    scrollView.smoothScrollTo(0, v.getBottom());
                });
            }
        };

        etName.setOnFocusChangeListener(scrollToFocusedView);
        etEmail.setOnFocusChangeListener(scrollToFocusedView);
        etPassword.setOnFocusChangeListener(scrollToFocusedView);
    }

    private void startEntranceAnimations() {
        // Animate spheres
        animateSpheres();

        // Form card slides up from bottom
        ObjectAnimator cardTransY = ObjectAnimator.ofFloat(formCard, "translationY", 100f, 0f);
        ObjectAnimator cardAlpha = ObjectAnimator.ofFloat(formCard, "alpha", 0f, 1f);

        AnimatorSet cardAnim = new AnimatorSet();
        cardAnim.playTogether(cardTransY, cardAlpha);
        cardAnim.setDuration(600);
        cardAnim.setInterpolator(new DecelerateInterpolator(1.5f));
        cardAnim.setStartDelay(200);
        cardAnim.start();

        // Title bounces in
        animateViewWithBounce(tvTitle, 400, 50f);

        // Form fields cascade animation with faster stagger
        animateViewSlideUp(tvNameLabel, 480, 25f);
        animateViewSlideUp(nameContainer, 520, 25f);
        animateViewSlideUp(tvEmailLabel, 560, 25f);
        animateViewSlideUp(emailContainer, 600, 25f);
        animateViewSlideUp(tvPasswordLabel, 640, 25f);
        animateViewSlideUp(passwordContainer, 680, 25f);

        // Button with scale effect
        btnRegister.setScaleX(0.8f);
        btnRegister.setScaleY(0.8f);
        ObjectAnimator btnScaleX = ObjectAnimator.ofFloat(btnRegister, "scaleX", 0.8f, 1f);
        ObjectAnimator btnScaleY = ObjectAnimator.ofFloat(btnRegister, "scaleY", 0.8f, 1f);
        ObjectAnimator btnAlpha = ObjectAnimator.ofFloat(btnRegister, "alpha", 0f, 1f);

        AnimatorSet btnAnim = new AnimatorSet();
        btnAnim.playTogether(btnScaleX, btnScaleY, btnAlpha);
        btnAnim.setDuration(400);
        btnAnim.setInterpolator(new OvershootInterpolator(1.2f));
        btnAnim.setStartDelay(750);
        btnAnim.start();

        // Bottom elements fade in
        animateViewFadeIn(loginContainer, 820);
        animateViewFadeIn(dividerContainer, 860);
        animateViewFadeIn(btnGuest, 900);
        animateViewFadeIn(tvBack, 940);

        // Start floating spheres
        new Handler(Looper.getMainLooper()).postDelayed(this::startFloatingAnimations, 600);
    }

    private void animateSpheres() {
        View[] spheres = { sphere1, sphere2, sphere3 };
        int[] delays = { 100, 180, 140 };

        for (int i = 0; i < spheres.length; i++) {
            View sphere = spheres[i];
            sphere.setScaleX(0.4f);
            sphere.setScaleY(0.4f);

            AnimatorSet sphereAnim = new AnimatorSet();
            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(sphere, "alpha", 0f, 1f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(sphere, "scaleX", 0.4f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(sphere, "scaleY", 0.4f, 1f);

            sphereAnim.playTogether(fadeIn, scaleX, scaleY);
            sphereAnim.setDuration(500);
            sphereAnim.setInterpolator(new OvershootInterpolator(1.2f));
            sphereAnim.setStartDelay(delays[i]);
            sphereAnim.start();
        }
    }

    private void animateViewWithBounce(View view, int delay, float startOffset) {
        view.setTranslationY(startOffset);
        ObjectAnimator transY = ObjectAnimator.ofFloat(view, "translationY", startOffset, 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(transY, alpha);
        animSet.setDuration(500);
        animSet.setInterpolator(new OvershootInterpolator(1.0f));
        animSet.setStartDelay(delay);
        animSet.start();
    }

    private void animateViewSlideUp(View view, int delay, float startOffset) {
        view.setTranslationY(startOffset);
        ObjectAnimator transY = ObjectAnimator.ofFloat(view, "translationY", startOffset, 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);

        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(transY, alpha);
        animSet.setDuration(350);
        animSet.setInterpolator(new DecelerateInterpolator());
        animSet.setStartDelay(delay);
        animSet.start();
    }

    private void animateViewFadeIn(View view, int delay) {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        alpha.setDuration(300);
        alpha.setStartDelay(delay);
        alpha.start();
    }

    private void startFloatingAnimations() {
        View[] spheres = { sphere1, sphere2, sphere3 };
        int[] durations = { 2800, 2400, 2600 };
        float[] amplitudes = { 10f, 12f, 8f };

        for (int i = 0; i < spheres.length; i++) {
            ObjectAnimator floatUp = ObjectAnimator.ofFloat(spheres[i], "translationY", 0f, -amplitudes[i], 0f);
            floatUp.setDuration(durations[i]);
            floatUp.setInterpolator(new AccelerateDecelerateInterpolator());
            floatUp.setRepeatCount(ValueAnimator.INFINITE);
            floatUp.start();
        }
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> registerUser());

        findViewById(R.id.tvLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });

        findViewById(R.id.tvBack).setOnClickListener(v -> {
            startActivity(new Intent(this, WelcomeActivity.class));
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });

        findViewById(R.id.btnGuest).setOnClickListener(v -> {
            new PreferenceManager(this).setGuestMode(true);
            goToMain();
        });

        // Password visibility toggle
        findViewById(R.id.ivTogglePassword).setOnClickListener(v -> {
            if (etPassword.getInputType() == (android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                etPassword.setInputType(
                        android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            etPassword.setSelection(etPassword.getText().length());
        });
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation
        if (name.isEmpty()) {
            etName.setError("Name required");
            etName.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            etEmail.setError("Email required");
            etEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password required");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        showLoading(true);

        // Create user with email and password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // User created successfully
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Send verification email
                            user.sendEmailVerification()
                                    .addOnCompleteListener(emailTask -> {
                                        showLoading(false);
                                        if (emailTask.isSuccessful()) {
                                            // Navigate to email verification screen
                                            navigateToEmailVerification(name);
                                        } else {
                                            BeautifulNotification.showError(RegisterActivity.this,
                                                    "Failed to send verification email. Please try again.");
                                        }
                                    });
                        }
                    } else {
                        showLoading(false);
                        String errorMessage = "Registration failed";
                        if (task.getException() != null) {
                            String exMessage = task.getException().getMessage();
                            if (exMessage != null) {
                                if (exMessage.contains("email address is already in use")) {
                                    errorMessage = "This email is already registered. Please login instead.";
                                } else if (exMessage.contains("badly formatted")) {
                                    errorMessage = "Invalid email format";
                                } else if (exMessage.contains("weak password")) {
                                    errorMessage = "Password is too weak. Please use a stronger password.";
                                } else {
                                    errorMessage = exMessage;
                                }
                            }
                        }
                        BeautifulNotification.showError(RegisterActivity.this, errorMessage);
                    }
                });
    }

    private void navigateToEmailVerification(String name) {
        Intent intent = new Intent(this, EmailVerificationActivity.class);
        intent.putExtra("USER_NAME", name);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        if (show) {
            btnRegister.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            btnRegister.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, WelcomeActivity.class));
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        finish();
    }
}
