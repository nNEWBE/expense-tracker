package com.example.trackexpense.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trackexpense.MainActivity;
import com.example.trackexpense.R;
import com.example.trackexpense.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private CircularProgressIndicator progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> loginUser());

        findViewById(R.id.tvRegister).setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });

        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> showForgotPasswordDialog());

        findViewById(R.id.tvBack).setOnClickListener(v -> {
            startActivity(new Intent(this, WelcomeActivity.class));
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

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Email required");
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password required");
            etPassword.requestFocus();
            return;
        }

        showLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Check if email is verified (optional)
                            saveUserToFirestore();
                            new PreferenceManager(this).setGuestMode(false);
                            goToMain();
                        }
                    } else {
                        showLoading(false);
                        String error = task.getException() != null ? task.getException().getMessage()
                                : "Authentication failed";
                        Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null)
            return;

        String userId = user.getUid();
        String email = user.getEmail();
        String displayName = user.getDisplayName();

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("displayName", displayName != null ? displayName : "User");
        userData.put("lastLogin", System.currentTimeMillis());

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update(userData)
                .addOnFailureListener(e -> {
                    userData.put("createdAt", System.currentTimeMillis());
                    userData.put("isBlocked", false);
                    userData.put("isAdmin", false);
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .set(userData);
                });
    }

    private void showForgotPasswordDialog() {
        TextInputEditText input = new TextInputEditText(this);
        input.setHint("Enter your email");
        input.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your email to receive a password reset link.")
                .setView(input)
                .setPositiveButton("Send", (dialog, which) -> {
                    String email = input.getText() != null ? input.getText().toString().trim() : "";
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

    private void goToMain() {
        showLoading(false);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, WelcomeActivity.class));
        finish();
    }
}
