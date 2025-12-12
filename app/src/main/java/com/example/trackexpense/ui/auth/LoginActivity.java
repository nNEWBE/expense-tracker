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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.trackexpense.MainActivity;
import com.example.trackexpense.R;
import com.example.trackexpense.utils.BeautifulNotification;
import com.example.trackexpense.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private CircularProgressIndicator progressBar;
    private FirebaseAuth mAuth;

    // Animation views
    private MaterialCardView formCard;
    private View sphere1, sphere2, sphere3;
    private View tvTitle, tvEmailLabel, emailContainer, passwordLabelContainer, passwordContainer;
    private View registerContainer, dividerContainer, btnGuest, tvBack;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        startEntranceAnimations();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        // Animation views
        formCard = findViewById(R.id.formCard);
        sphere1 = findViewById(R.id.sphere1);
        sphere2 = findViewById(R.id.sphere2);
        sphere3 = findViewById(R.id.sphere3);
        tvTitle = findViewById(R.id.tvTitle);
        tvEmailLabel = findViewById(R.id.tvEmailLabel);
        emailContainer = findViewById(R.id.emailContainer);
        passwordLabelContainer = findViewById(R.id.passwordLabelContainer);
        passwordContainer = findViewById(R.id.passwordContainer);
        registerContainer = findViewById(R.id.registerContainer);
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

        // Form fields cascade animation
        animateViewSlideUp(tvEmailLabel, 500, 30f);
        animateViewSlideUp(emailContainer, 550, 30f);
        animateViewSlideUp(passwordLabelContainer, 600, 30f);
        animateViewSlideUp(passwordContainer, 650, 30f);

        // Button with scale effect
        btnLogin.setScaleX(0.8f);
        btnLogin.setScaleY(0.8f);
        ObjectAnimator btnScaleX = ObjectAnimator.ofFloat(btnLogin, "scaleX", 0.8f, 1f);
        ObjectAnimator btnScaleY = ObjectAnimator.ofFloat(btnLogin, "scaleY", 0.8f, 1f);
        ObjectAnimator btnAlpha = ObjectAnimator.ofFloat(btnLogin, "alpha", 0f, 1f);

        AnimatorSet btnAnim = new AnimatorSet();
        btnAnim.playTogether(btnScaleX, btnScaleY, btnAlpha);
        btnAnim.setDuration(400);
        btnAnim.setInterpolator(new OvershootInterpolator(1.2f));
        btnAnim.setStartDelay(750);
        btnAnim.start();

        // Bottom elements fade in
        animateViewFadeIn(registerContainer, 850);
        animateViewFadeIn(dividerContainer, 900);
        animateViewFadeIn(btnGuest, 950);
        animateViewFadeIn(tvBack, 1000);

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
        animSet.setDuration(400);
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
            PreferenceManager pm = new PreferenceManager(this);
            pm.setGuestMode(true);
            // Check if guest has budget set, if not show prompt
            if (!pm.isBudgetSetupDone() && !pm.hasGuestBudgetSet()) {
                showGuestBudgetPromptDialog();
            } else {
                goToMain();
            }
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
                            // First check if user is blocked in Firestore
                            checkIfUserBlocked(user);
                        }
                    } else {
                        showLoading(false);
                        String error = task.getException() != null ? task.getException().getMessage()
                                : "Authentication failed";
                        BeautifulNotification.showError(this, error);
                    }
                });
    }

    private void checkIfUserBlocked(FirebaseUser user) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean isBlocked = documentSnapshot.getBoolean("isBlocked");
                        if (isBlocked != null && isBlocked) {
                            // User is blocked - sign out and show error
                            mAuth.signOut();
                            showLoading(false);
                            BeautifulNotification.showError(this,
                                    "Your account has been blocked. Please contact support for assistance.");
                            return;
                        }
                    }
                    // User is not blocked - continue with login flow
                    proceedWithLogin(user);
                })
                .addOnFailureListener(e -> {
                    // If we can't check blocked status, proceed with login
                    // (user might be new and not in Firestore yet)
                    proceedWithLogin(user);
                });
    }

    private void proceedWithLogin(FirebaseUser user) {
        // Check if email is verified
        if (!user.isEmailVerified()) {
            showLoading(false);
            // Email not verified - send to verification screen
            user.sendEmailVerification().addOnCompleteListener(emailTask -> {
                Intent intent = new Intent(this, EmailVerificationActivity.class);
                intent.putExtra("USER_NAME", user.getDisplayName());
                startActivity(intent);
                finish();
            });
            BeautifulNotification.showWarning(this, "Please verify your email to continue.");
            return;
        }

        // Email is verified - proceed to login
        completeLogin(user);
    }

    /**
     * Complete the login process after sync decision.
     */
    private void completeLogin(FirebaseUser user) {
        showLoading(true);

        PreferenceManager preferenceManager = new PreferenceManager(this);
        preferenceManager.setGuestMode(false);

        // IMPORTANT: Clear previous user's budget data to ensure per-user budget
        // handling
        // Each user should have their own budget stored in Firestore
        preferenceManager.clearBudgetData();

        // Fetch user data from Firestore
        String userId = user.getUid();
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String displayName;
                    String email;

                    if (documentSnapshot.exists()) {
                        displayName = documentSnapshot.getString("displayName");
                        email = documentSnapshot.getString("email");
                    } else {
                        displayName = user.getDisplayName();
                        email = user.getEmail();
                    }

                    // Apply fallbacks
                    if (displayName == null || displayName.trim().isEmpty()) {
                        displayName = "User";
                    }
                    if (email == null || email.trim().isEmpty()) {
                        email = user.getEmail() != null ? user.getEmail() : "No Email";
                    }

                    // Cache user data for instant loading
                    preferenceManager.cacheUserName(displayName);
                    preferenceManager.cacheUserEmail(email);

                    // Save to Firestore and navigate
                    saveUserToFirestore();
                    BeautifulNotification.showSuccess(this, "Welcome back! You've successfully signed in.");

                    // Check if THIS USER has budget set in Firestore (per-user budget)
                    Double monthlyBudget = documentSnapshot.getDouble("monthlyBudget");
                    boolean hasBudget = monthlyBudget != null && monthlyBudget > 0;

                    if (hasBudget) {
                        // User has budget in Firestore - load it locally
                        preferenceManager.setMonthlyBudget(monthlyBudget);
                        preferenceManager.setBudgetSetupDone(true);
                        new Handler(Looper.getMainLooper()).postDelayed(this::goToMain, 1500);
                    } else {
                        // User doesn't have budget - always prompt to set one
                        new Handler(Looper.getMainLooper()).postDelayed(() -> showBudgetPromptDialog(false), 1200);
                    }
                })
                .addOnFailureListener(e -> {
                    // Fallback to Firebase Auth data
                    String displayName = user.getDisplayName();
                    String email = user.getEmail();

                    if (displayName == null || displayName.trim().isEmpty()) {
                        displayName = "User";
                    }
                    if (email == null || email.trim().isEmpty()) {
                        email = "No Email";
                    }

                    preferenceManager.cacheUserName(displayName);
                    preferenceManager.cacheUserEmail(email);

                    saveUserToFirestore();
                    BeautifulNotification.showSuccess(this, "Welcome back! You've successfully signed in.");

                    // On Firestore failure, always prompt for budget (to be safe)
                    new Handler(Looper.getMainLooper()).postDelayed(() -> showBudgetPromptDialog(false), 1200);
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

    /**
     * Show budget prompt dialog for logged-in users.
     */
    private void showBudgetPromptDialog(boolean isGuest) {
        showLoading(false);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_set_budget_prompt, null);
        TextInputLayout tilBudget = dialogView.findViewById(R.id.tilBudget);
        TextInputEditText etBudget = dialogView.findViewById(R.id.etBudget);

        // Quick amount chips
        Chip chip5000 = dialogView.findViewById(R.id.chip5000);
        Chip chip10000 = dialogView.findViewById(R.id.chip10000);
        Chip chip20000 = dialogView.findViewById(R.id.chip20000);
        Chip chip50000 = dialogView.findViewById(R.id.chip50000);

        PreferenceManager pm = new PreferenceManager(this);

        // Set currency prefix
        String currencySymbol = pm.getCurrencySymbol();
        tilBudget.setPrefixText(currencySymbol + " ");

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_TrackExpense_Dialog)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // Quick chip listeners
        View.OnClickListener chipListener = v -> {
            Chip chip = (Chip) v;
            etBudget.setText(chip.getText().toString().replace(",", ""));
        };
        chip5000.setOnClickListener(chipListener);
        chip10000.setOnClickListener(chipListener);
        chip20000.setOnClickListener(chipListener);
        chip50000.setOnClickListener(chipListener);

        // Save button
        dialogView.findViewById(R.id.btnSaveBudget).setOnClickListener(v -> {
            String budgetStr = etBudget.getText() != null ? etBudget.getText().toString() : "";
            if (budgetStr.isEmpty()) {
                tilBudget.setError("Please enter a budget");
                return;
            }

            try {
                double budget = Double.parseDouble(budgetStr);
                if (budget <= 0) {
                    tilBudget.setError("Budget must be greater than 0");
                    return;
                }

                if (isGuest) {
                    pm.setGuestMonthlyBudget(budget);
                    pm.setMonthlyBudget(budget); // Also set regular budget for consistency
                } else {
                    pm.setMonthlyBudget(budget);
                    saveBudgetToFirestore(budget);
                }
                pm.setBudgetSetupDone(true);
                dialog.dismiss();
                BeautifulNotification.showSuccess(this, "Monthly budget set successfully!");
                goToMain();

            } catch (NumberFormatException e) {
                tilBudget.setError("Invalid number");
            }
        });

        // Skip button
        dialogView.findViewById(R.id.btnSkipBudget).setOnClickListener(v -> {
            pm.setBudgetSetupDone(true);
            dialog.dismiss();
            goToMain();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    /**
     * Show budget prompt for guest users.
     */
    private void showGuestBudgetPromptDialog() {
        showBudgetPromptDialog(true);
    }

    /**
     * Save budget to Firestore for logged-in users.
     */
    private void saveBudgetToFirestore(double budget) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null)
            return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("monthlyBudget", budget);

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .update(updates)
                .addOnFailureListener(e -> {
                    // If update fails (document doesn't exist), the budget was still saved locally
                });
    }

    private void showForgotPasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);

        TextInputEditText etResetEmail = dialogView.findViewById(R.id.etResetEmail);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelReset);
        com.google.android.material.button.MaterialButton btnSend = dialogView.findViewById(R.id.btnSendReset);

        // Pre-fill email if user already entered one
        String currentEmail = etEmail.getText().toString().trim();
        if (!currentEmail.isEmpty()) {
            etResetEmail.setText(currentEmail);
        }

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSend.setOnClickListener(v -> {
            String email = etResetEmail.getText() != null ? etResetEmail.getText().toString().trim() : "";
            if (email.isEmpty()) {
                etResetEmail.setError("Email required");
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etResetEmail.setError("Enter a valid email");
                return;
            }

            // Show loading state
            btnSend.setEnabled(false);
            btnSend.setText("Sending...");

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        dialog.dismiss();
                        if (task.isSuccessful()) {
                            BeautifulNotification.showSuccess(this, "Password reset link sent to your email!");
                        } else {
                            BeautifulNotification.showError(this, "Failed to send reset email. Please try again.");
                        }
                    });
        });

        dialog.show();
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
