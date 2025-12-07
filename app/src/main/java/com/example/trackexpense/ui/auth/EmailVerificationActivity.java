package com.example.trackexpense.ui.auth;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trackexpense.MainActivity;
import com.example.trackexpense.R;
import com.example.trackexpense.utils.BeautifulNotification;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EmailVerificationActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private String userName;

    // UI elements
    private View illustrationContainer, decorCircle1, decorCircle2;
    private TextView tvTitle, tvDescription, tvEmail, tvStatus, tvTimer, tvResend, tvBackToLogin;
    private LinearLayout statusContainer, resendContainer;
    private MaterialButton btnProceed, btnOpenEmail;
    private CircularProgressIndicator progressBar;
    private ProgressBar statusProgress;
    private ImageView ivEmailIllustration;

    private CountDownTimer resendTimer;
    private boolean canResend = true;
    private Handler verificationCheckHandler;
    private Runnable verificationCheckRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        userName = getIntent().getStringExtra("USER_NAME");

        initViews();
        setupUserInfo();
        startEntranceAnimations();
        setupListeners();
        startAutoVerificationCheck();
    }

    private void initViews() {
        illustrationContainer = findViewById(R.id.illustrationContainer);
        decorCircle1 = findViewById(R.id.decorCircle1);
        decorCircle2 = findViewById(R.id.decorCircle2);
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvEmail = findViewById(R.id.tvEmail);
        tvStatus = findViewById(R.id.tvStatus);
        tvTimer = findViewById(R.id.tvTimer);
        tvResend = findViewById(R.id.tvResend);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        statusContainer = findViewById(R.id.statusContainer);
        resendContainer = findViewById(R.id.resendContainer);
        btnProceed = findViewById(R.id.btnProceed);
        btnOpenEmail = findViewById(R.id.btnOpenEmail);
        progressBar = findViewById(R.id.progressBar);
        statusProgress = findViewById(R.id.statusProgress);
        ivEmailIllustration = findViewById(R.id.ivEmailIllustration);
    }

    private void setupUserInfo() {
        if (currentUser != null) {
            tvEmail.setText(currentUser.getEmail());
        }
    }

    private void startEntranceAnimations() {
        // Background circles fade in
        animateFadeIn(decorCircle1, 100, 0.15f);
        animateFadeIn(decorCircle2, 200, 0.15f);

        // Illustration bounces in
        illustrationContainer.setScaleX(0.5f);
        illustrationContainer.setScaleY(0.5f);
        AnimatorSet illustrationAnim = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(illustrationContainer, "scaleX", 0.5f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(illustrationContainer, "scaleY", 0.5f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(illustrationContainer, "alpha", 0f, 1f);
        illustrationAnim.playTogether(scaleX, scaleY, alpha);
        illustrationAnim.setDuration(700);
        illustrationAnim.setInterpolator(new OvershootInterpolator(1.2f));
        illustrationAnim.setStartDelay(200);
        illustrationAnim.start();

        // Subtle rotation animation for illustration
        ObjectAnimator rotate = ObjectAnimator.ofFloat(ivEmailIllustration, "rotation", -5f, 5f, -5f);
        rotate.setDuration(3000);
        rotate.setRepeatCount(ObjectAnimator.INFINITE);
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());
        rotate.setStartDelay(1000);
        rotate.start();

        // Title slides up
        animateSlideUp(tvTitle, 400);

        // Description slides up
        animateSlideUp(tvDescription, 500);

        // Email slides up
        animateSlideUp(tvEmail, 600);

        // Status container fades in
        animateFadeIn(statusContainer, 700, 1f);

        // Resend container fades in
        animateFadeIn(resendContainer, 800, 1f);

        // Proceed button bounces in
        btnProceed.setScaleX(0.8f);
        btnProceed.setScaleY(0.8f);
        AnimatorSet btnAnim = new AnimatorSet();
        ObjectAnimator btnScaleX = ObjectAnimator.ofFloat(btnProceed, "scaleX", 0.8f, 1f);
        ObjectAnimator btnScaleY = ObjectAnimator.ofFloat(btnProceed, "scaleY", 0.8f, 1f);
        ObjectAnimator btnAlpha = ObjectAnimator.ofFloat(btnProceed, "alpha", 0f, 1f);
        btnAnim.playTogether(btnScaleX, btnScaleY, btnAlpha);
        btnAnim.setDuration(500);
        btnAnim.setInterpolator(new OvershootInterpolator(1.0f));
        btnAnim.setStartDelay(900);
        btnAnim.start();

        // Open email button fades in
        animateFadeIn(btnOpenEmail, 1000, 1f);

        // Back to login fades in
        animateFadeIn(tvBackToLogin, 1100, 1f);
    }

    private void animateFadeIn(View view, int delay, float targetAlpha) {
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, targetAlpha);
        alpha.setDuration(400);
        alpha.setStartDelay(delay);
        alpha.start();
    }

    private void animateSlideUp(View view, int delay) {
        view.setTranslationY(30f);
        AnimatorSet animSet = new AnimatorSet();
        ObjectAnimator transY = ObjectAnimator.ofFloat(view, "translationY", 30f, 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        animSet.playTogether(transY, alpha);
        animSet.setDuration(500);
        animSet.setInterpolator(new DecelerateInterpolator());
        animSet.setStartDelay(delay);
        animSet.start();
    }

    private void setupListeners() {
        btnProceed.setOnClickListener(v -> checkEmailVerification());

        btnOpenEmail.setOnClickListener(v -> openEmailApp());

        tvResend.setOnClickListener(v -> {
            if (canResend) {
                resendVerificationEmail();
            }
        });

        tvBackToLogin.setOnClickListener(v -> {
            // Sign out and go back to login
            mAuth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });
    }

    private void startAutoVerificationCheck() {
        verificationCheckHandler = new Handler(Looper.getMainLooper());
        verificationCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentUser != null) {
                    currentUser.reload().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && currentUser.isEmailVerified()) {
                            onEmailVerified();
                        } else {
                            // Check again in 3 seconds
                            verificationCheckHandler.postDelayed(this, 3000);
                        }
                    });
                }
            }
        };
        // Start checking after 3 seconds
        verificationCheckHandler.postDelayed(verificationCheckRunnable, 3000);
    }

    private void checkEmailVerification() {
        if (currentUser == null) {
            BeautifulNotification.showError(this, "User session expired. Please login again.");
            return;
        }

        showLoading(true);

        // Reload user to get latest verification status
        currentUser.reload().addOnCompleteListener(task -> {
            showLoading(false);
            if (task.isSuccessful()) {
                if (currentUser.isEmailVerified()) {
                    onEmailVerified();
                } else {
                    BeautifulNotification.showWarning(this,
                            "Email not verified yet. Please check your inbox and click the verification link.");
                }
            } else {
                BeautifulNotification.showError(this,
                        "Failed to check verification status. Please try again.");
            }
        });
    }

    private void onEmailVerified() {
        // Stop auto-check
        if (verificationCheckHandler != null && verificationCheckRunnable != null) {
            verificationCheckHandler.removeCallbacks(verificationCheckRunnable);
        }

        // Update status UI with animation
        updateStatusToVerified();

        // Save user data to Firestore
        saveUserToFirestore();
    }

    private void updateStatusToVerified() {
        statusProgress.setVisibility(View.GONE);
        tvStatus.setText("Email Verified ✓");
        tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        statusContainer.setBackground(getResources().getDrawable(R.drawable.status_bg_verified));

        // Animate the status change
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(statusContainer, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(statusContainer, "scaleY", 1f, 1.1f, 1f);
        AnimatorSet animSet = new AnimatorSet();
        animSet.playTogether(scaleX, scaleY);
        animSet.setDuration(400);
        animSet.start();
    }

    private void saveUserToFirestore() {
        if (currentUser == null)
            return;

        // Update display name
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(userName)
                .build();

        currentUser.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            // Save to Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> userData = new HashMap<>();
            userData.put("uid", currentUser.getUid());
            userData.put("name", userName);
            userData.put("email", currentUser.getEmail());
            userData.put("emailVerified", true);
            userData.put("createdAt", System.currentTimeMillis());

            db.collection("users")
                    .document(currentUser.getUid())
                    .set(userData)
                    .addOnSuccessListener(aVoid -> {
                        BeautifulNotification.showSuccess(EmailVerificationActivity.this,
                                "Account verified successfully!");

                        // Navigate to main after brief delay to show notification
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            navigateToMain();
                        }, 1500);
                    })
                    .addOnFailureListener(e -> {
                        // Even if Firestore save fails, proceed to main
                        // Data can be synced later
                        navigateToMain();
                    });
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    private void resendVerificationEmail() {
        if (currentUser == null)
            return;

        canResend = false;
        tvResend.setTextColor(getResources().getColor(android.R.color.darker_gray));

        currentUser.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        BeautifulNotification.showSuccess(this,
                                "Verification email sent! Please check your inbox.");
                        startResendTimer();
                    } else {
                        canResend = true;
                        tvResend.setTextColor(getResources().getColor(R.color.primary_dark));
                        BeautifulNotification.showError(this,
                                "Failed to send email. Please try again.");
                    }
                });
    }

    private void startResendTimer() {
        tvTimer.setVisibility(View.VISIBLE);
        tvTimer.setAlpha(0f);
        tvTimer.animate().alpha(1f).setDuration(200).start();

        resendTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                tvTimer.setText("Resend available in " + seconds + "s");
            }

            @Override
            public void onFinish() {
                canResend = true;
                tvResend.setTextColor(getResources().getColor(R.color.primary_dark));
                tvTimer.animate().alpha(0f).setDuration(200).withEndAction(() -> tvTimer.setVisibility(View.GONE))
                        .start();
            }
        }.start();
    }

    private void openEmailApp() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_EMAIL);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            BeautifulNotification.showInfo(this, "No email app found on this device.");
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            btnProceed.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            btnProceed.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) {
            resendTimer.cancel();
        }
        if (verificationCheckHandler != null && verificationCheckRunnable != null) {
            verificationCheckHandler.removeCallbacks(verificationCheckRunnable);
        }
    }

    @Override
    public void onBackPressed() {
        // Confirm before going back (signs out)
        new android.app.AlertDialog.Builder(this)
                .setTitle("Cancel Verification?")
                .setMessage("Your account will not be verified. You can verify later by logging in again.")
                .setPositiveButton("Stay", null)
                .setNegativeButton("Leave", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(this, WelcomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                    finish();
                })
                .show();
    }
}
