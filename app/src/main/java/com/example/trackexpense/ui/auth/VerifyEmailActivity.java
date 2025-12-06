package com.example.trackexpense.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trackexpense.MainActivity;
import com.example.trackexpense.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerifyEmailActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private TextView tvEmail, tvResend, tvTimer;
    private MaterialButton btnVerify;
    private CircularProgressIndicator progressBar;
    private ImageView btnBack;

    private CountDownTimer resendTimer;
    private boolean canResend = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        initViews();
        setupListeners();
        displayUserEmail();
        startResendTimer();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvEmail = findViewById(R.id.tvEmail);
        tvResend = findViewById(R.id.tvResend);
        tvTimer = findViewById(R.id.tvTimer);
        btnVerify = findViewById(R.id.btnVerify);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            mAuth.signOut();
            navigateToLogin();
        });

        tvResend.setOnClickListener(v -> {
            if (canResend) {
                resendVerificationEmail();
            }
        });

        btnVerify.setOnClickListener(v -> checkVerificationStatus());
    }

    private void displayUserEmail() {
        if (currentUser != null && currentUser.getEmail() != null) {
            tvEmail.setText(currentUser.getEmail());
        }
    }

    private void checkVerificationStatus() {
        showLoading(true);

        if (currentUser == null) {
            showLoading(false);
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        // Reload user to get latest verification status
        currentUser.reload()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        currentUser = mAuth.getCurrentUser();
                        if (currentUser != null && currentUser.isEmailVerified()) {
                            // Email verified! Save to Firestore and proceed
                            saveUserToFirestore();
                            Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        } else {
                            Toast.makeText(this,
                                    "Email not verified yet. Please check your inbox and click the verification link.",
                                    Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "Failed to check verification status. Please try again.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resendVerificationEmail() {
        if (currentUser == null)
            return;

        showLoading(true);
        currentUser.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Verification email sent! Check your inbox.", Toast.LENGTH_SHORT).show();
                        startResendTimer();
                    } else {
                        Toast.makeText(this, "Failed to send email. Try again later.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startResendTimer() {
        canResend = false;
        tvResend.setAlpha(0.5f);
        tvTimer.setVisibility(View.VISIBLE);

        if (resendTimer != null) {
            resendTimer.cancel();
        }

        resendTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvTimer.setText("Resend available in " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                canResend = true;
                tvResend.setAlpha(1f);
                tvTimer.setVisibility(View.GONE);
            }
        }.start();
    }

    private void saveUserToFirestore() {
        if (currentUser == null)
            return;

        String userId = currentUser.getUid();
        String email = currentUser.getEmail();
        String displayName = currentUser.getDisplayName();

        java.util.Map<String, Object> userData = new java.util.HashMap<>();
        userData.put("email", email);
        userData.put("displayName", displayName != null ? displayName : "User");
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("isBlocked", false);
        userData.put("isAdmin", false);

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .set(userData, com.google.firebase.firestore.SetOptions.merge());
    }

    private void navigateToLogin() {
        mAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        if (show) {
            btnVerify.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            btnVerify.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }

    @Override
    public void onBackPressed() {
        mAuth.signOut();
        navigateToLogin();
    }
}
