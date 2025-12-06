package com.example.trackexpense.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trackexpense.MainActivity;
import com.example.trackexpense.R;
import com.example.trackexpense.data.ExpenseRepository;
import com.example.trackexpense.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGuest;
    private TextView tvForgotPassword, tvRegister;
    private CircularProgressIndicator progressBar;
    private FirebaseAuth mAuth;
    private PreferenceManager preferenceManager;
    private boolean wasGuestMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        preferenceManager = new PreferenceManager(this);

        // Check if user was in guest mode before
        wasGuestMode = preferenceManager.isGuestMode();

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGuest = findViewById(R.id.btnGuest);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister = findViewById(R.id.tvRegister);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> loginUser());
        btnGuest.setOnClickListener(v -> continueAsGuest());
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Email required");
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password required");
            return;
        }

        showLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        preferenceManager.setGuestMode(false);

                        // Create/update user document in Firestore
                        saveUserToFirestore();

                        // Check if user had data in guest mode
                        if (wasGuestMode) {
                            showSyncDataDialog();
                        } else {
                            navigateToMain();
                        }
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage()
                                : "Authentication failed";
                        Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore() {
        if (mAuth.getCurrentUser() == null)
            return;

        String userId = mAuth.getCurrentUser().getUid();
        String email = mAuth.getCurrentUser().getEmail();
        String displayName = mAuth.getCurrentUser().getDisplayName();

        java.util.Map<String, Object> userData = new java.util.HashMap<>();
        userData.put("email", email);
        userData.put("displayName", displayName != null ? displayName : "User");
        userData.put("lastLoginAt", System.currentTimeMillis());

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .set(userData, com.google.firebase.firestore.SetOptions.merge());
    }

    private void showSyncDataDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sync Local Data")
                .setMessage(
                        "You have local expense data from Guest Mode. Would you like to sync it to your cloud account?")
                .setPositiveButton("Yes, Sync", (dialog, which) -> {
                    syncLocalDataToCloud();
                    navigateToMain();
                })
                .setNegativeButton("No, Skip", (dialog, which) -> {
                    navigateToMain();
                })
                .setCancelable(false)
                .show();
    }

    private void syncLocalDataToCloud() {
        ExpenseRepository repository = new ExpenseRepository(getApplication());
        repository.syncLocalToCloud();
        Toast.makeText(this, "Syncing data to cloud...", Toast.LENGTH_SHORT).show();
    }

    private void navigateToMain() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private void continueAsGuest() {
        preferenceManager.setGuestMode(true);
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private void showForgotPasswordDialog() {
        TextInputEditText input = new TextInputEditText(this);
        input.setHint("Enter your email");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Reset Password")
                .setMessage("We'll send you a password reset link.")
                .setView(input)
                .setPositiveButton("Send", (dialog, which) -> {
                    String email = input.getText().toString().trim();
                    if (!email.isEmpty()) {
                        mAuth.sendPasswordResetEmail(email)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(this, "Reset email sent!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(this, "Failed to send reset email", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLoading(boolean show) {
        if (show) {
            btnLogin.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            btnLogin.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }
}
