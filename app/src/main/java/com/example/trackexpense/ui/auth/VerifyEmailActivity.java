package com.example.trackexpense.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
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

    private TextView tvPhone, tvResend, tvTimer;
    private MaterialButton btnVerify;
    private CircularProgressIndicator progressBar;
    private ImageView btnBack;

    private EditText etOtp1, etOtp2, etOtp3, etOtp4, etOtp5, etOtp6;
    private EditText[] otpFields;

    private CountDownTimer resendTimer;
    private boolean canResend = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        initViews();
        setupOtpInputs();
        setupListeners();
        displayUserEmail();
        startResendTimer();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvPhone = findViewById(R.id.tvPhone);
        tvResend = findViewById(R.id.tvResend);
        tvTimer = findViewById(R.id.tvTimer);
        btnVerify = findViewById(R.id.btnVerify);
        progressBar = findViewById(R.id.progressBar);

        etOtp1 = findViewById(R.id.etOtp1);
        etOtp2 = findViewById(R.id.etOtp2);
        etOtp3 = findViewById(R.id.etOtp3);
        etOtp4 = findViewById(R.id.etOtp4);
        etOtp5 = findViewById(R.id.etOtp5);
        etOtp6 = findViewById(R.id.etOtp6);

        otpFields = new EditText[] { etOtp1, etOtp2, etOtp3, etOtp4, etOtp5, etOtp6 };
    }

    private void setupOtpInputs() {
        for (int i = 0; i < otpFields.length; i++) {
            final int index = i;

            otpFields[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() == 1 && index < otpFields.length - 1) {
                        // Move to next field
                        otpFields[index + 1].requestFocus();
                    } else if (s.length() == 0 && index > 0) {
                        // Move to previous field on delete
                        otpFields[index - 1].requestFocus();
                    }

                    // Auto verify when all fields are filled
                    if (isOtpComplete()) {
                        verifyOtp();
                    }
                }
            });

            // Handle backspace on empty field
            otpFields[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (otpFields[index].getText().toString().isEmpty() && index > 0) {
                        otpFields[index - 1].requestFocus();
                        otpFields[index - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }

        // Focus first field
        etOtp1.requestFocus();
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

        btnVerify.setOnClickListener(v -> verifyOtp());
    }

    private void displayUserEmail() {
        if (currentUser != null && currentUser.getEmail() != null) {
            // Mask the email for display
            String email = currentUser.getEmail();
            tvPhone.setText(email);
        }
    }

    private boolean isOtpComplete() {
        for (EditText field : otpFields) {
            if (field.getText().toString().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getOtpCode() {
        StringBuilder otp = new StringBuilder();
        for (EditText field : otpFields) {
            otp.append(field.getText().toString());
        }
        return otp.toString();
    }

    private void clearOtp() {
        for (EditText field : otpFields) {
            field.setText("");
        }
        etOtp1.requestFocus();
    }

    private void verifyOtp() {
        if (!isOtpComplete()) {
            Toast.makeText(this, "Please enter the complete OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // Since Firebase email auth uses links not OTP codes,
        // we'll check if the user has verified their email via the link
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
                            Toast.makeText(this, "Verification successful!", Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        } else {
                            Toast.makeText(this, "Please verify your email first by clicking the link we sent.",
                                    Toast.LENGTH_LONG).show();
                            clearOtp();
                        }
                    } else {
                        Toast.makeText(this, "Verification failed. Please try again.",
                                Toast.LENGTH_SHORT).show();
                        clearOtp();
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
                        Toast.makeText(this, "Verification email sent!", Toast.LENGTH_SHORT).show();
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
