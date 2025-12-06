package com.example.trackexpense.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.trackexpense.MainActivity;
import com.example.trackexpense.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class VerifyEmailActivity extends AppCompatActivity {

    private static final String TAG = "VerifyEmailActivity";

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

    private String phoneNumber;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Get phone number from intent
        phoneNumber = getIntent().getStringExtra("phone_number");

        initViews();
        setupOtpInputs();
        setupListeners();
        displayPhoneNumber();

        // Start phone verification
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            sendOtp();
        }
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
                resendOtp();
            }
        });

        btnVerify.setOnClickListener(v -> verifyOtp());
    }

    private void displayPhoneNumber() {
        if (phoneNumber != null) {
            // Mask some digits for privacy
            String masked = phoneNumber;
            if (phoneNumber.length() > 6) {
                masked = phoneNumber.substring(0, 4) + " XXX XXX " + phoneNumber.substring(phoneNumber.length() - 3);
            }
            tvPhone.setText(masked);
        }
    }

    private void sendOtp() {
        showLoading(true);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        // Auto-verification (instant verification on some devices)
                        Log.d(TAG, "onVerificationCompleted: Auto verified");
                        showLoading(false);

                        // Auto-fill the OTP if available
                        String smsCode = credential.getSmsCode();
                        if (smsCode != null && smsCode.length() == 6) {
                            for (int i = 0; i < 6; i++) {
                                otpFields[i].setText(String.valueOf(smsCode.charAt(i)));
                            }
                        }

                        linkPhoneCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        showLoading(false);
                        Log.e(TAG, "onVerificationFailed: " + e.getMessage());
                        Toast.makeText(VerifyEmailActivity.this,
                                "Verification failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verId,
                            @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        showLoading(false);
                        Log.d(TAG, "onCodeSent: OTP sent successfully");
                        verificationId = verId;
                        resendToken = token;
                        Toast.makeText(VerifyEmailActivity.this,
                                "OTP sent to " + phoneNumber,
                                Toast.LENGTH_SHORT).show();
                        startResendTimer();
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void resendOtp() {
        if (resendToken == null) {
            sendOtp();
            return;
        }

        showLoading(true);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setForceResendingToken(resendToken)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        showLoading(false);
                        linkPhoneCredential(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        showLoading(false);
                        Toast.makeText(VerifyEmailActivity.this,
                                "Failed to resend: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verId,
                            @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        showLoading(false);
                        verificationId = verId;
                        resendToken = token;
                        Toast.makeText(VerifyEmailActivity.this,
                                "OTP resent!",
                                Toast.LENGTH_SHORT).show();
                        startResendTimer();
                        clearOtp();
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
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

        if (verificationId == null) {
            Toast.makeText(this, "Please wait for OTP to be sent", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        String code = getOtpCode();
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        linkPhoneCredential(credential);
    }

    private void linkPhoneCredential(PhoneAuthCredential credential) {
        if (currentUser == null) {
            showLoading(false);
            Toast.makeText(this, "Session expired. Please register again.", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        // Link phone number to the current email/password account
        currentUser.linkWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "linkWithCredential: success");
                        saveUserToFirestore();
                        Toast.makeText(this, "Phone verified successfully!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        Log.e(TAG, "linkWithCredential: failure", task.getException());
                        String error = task.getException() != null ? task.getException().getMessage()
                                : "Verification failed";

                        // Check if already linked
                        if (error.contains("already linked") || error.contains("already in use")) {
                            // Phone already linked, just proceed
                            saveUserToFirestore();
                            Toast.makeText(this, "Verification successful!", Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        } else {
                            Toast.makeText(this, "Verification failed: " + error,
                                    Toast.LENGTH_LONG).show();
                            clearOtp();
                        }
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
        userData.put("phoneNumber", phoneNumber);
        userData.put("createdAt", System.currentTimeMillis());
        userData.put("isBlocked", false);
        userData.put("isAdmin", false);
        userData.put("phoneVerified", true);

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
