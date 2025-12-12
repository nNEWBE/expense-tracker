package com.example.trackexpense.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.trackexpense.R;
import com.example.trackexpense.data.ExpenseRepository;
import com.example.trackexpense.data.remote.AdminService;
import com.example.trackexpense.ui.admin.AdminActivity;
import com.example.trackexpense.ui.auth.WelcomeActivity;
import com.example.trackexpense.utils.ExportUtils;
import com.example.trackexpense.utils.PreferenceManager;
import com.example.trackexpense.utils.BeautifulNotification;
import com.example.trackexpense.viewmodel.ExpenseViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;

public class ProfileFragment extends Fragment {

    private PreferenceManager preferenceManager;
    private ExpenseViewModel expenseViewModel;
    private AdminService adminService;

    private TextView tvHeaderName, tvHeaderEmail, tvName, tvEmail, tvCurrency, tvBudget, tvTheme;
    private TextView tvTotalTransactions, tvThisMonth, tvBudgetUsed;
    private View cardGuestMode;
    private LinearLayout layoutAdmin;
    private FrameLayout headerLayout;
    private com.google.android.material.card.MaterialCardView cardStats, cardAccount, cardPreferences;
    private ImageView btnMenu;

    // Skeleton loading
    private View skeletonView;
    private boolean isFirstLoad = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferenceManager = new PreferenceManager(requireContext());
        expenseViewModel = new ViewModelProvider(this).get(ExpenseViewModel.class);
        adminService = AdminService.getInstance();

        // Check if data is already available (cached)
        java.util.List<com.example.trackexpense.data.local.Expense> cachedData = expenseViewModel.getAllExpenses()
                .getValue();
        if (cachedData == null || cachedData.isEmpty()) {
            isFirstLoad = true;
            showSkeletonLoading(view);
        } else {
            isFirstLoad = false;
        }

        initViews(view);
        loadUserData();
        setupClickListeners(view);
        checkAdminAccess();
        observeData();
    }

    /**
     * Show skeleton loading placeholder while data loads.
     */
    /**
     * Show skeleton loading placeholder while data loads.
     */
    private void showSkeletonLoading(View rootView) {
        if (getActivity() == null)
            return;

        if (rootView instanceof ViewGroup) {
            skeletonView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.skeleton_profile, (ViewGroup) rootView, false);

            // Ensure skeleton overlaps everything
            skeletonView.setElevation(100f);

            ((ViewGroup) rootView).addView(skeletonView);
        }
    }

    /**
     * Hide skeleton loading with smooth fade animation.
     */
    /**
     * Hide skeleton loading with smooth fade animation.
     */
    private void hideSkeletonLoading(Runnable onAnimationEndAction) {
        if (skeletonView == null) {
            if (onAnimationEndAction != null) {
                onAnimationEndAction.run();
            }
            return;
        }

        skeletonView.animate()
                .alpha(0f)
                .setDuration(400)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .setListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        if (skeletonView != null && skeletonView.getParent() != null) {
                            ((ViewGroup) skeletonView.getParent()).removeView(skeletonView);
                            skeletonView = null;
                        }
                        if (onAnimationEndAction != null) {
                            onAnimationEndAction.run();
                        }
                    }
                })
                .start();
    }

    private void initViews(View view) {
        tvHeaderName = view.findViewById(R.id.tvHeaderName);
        tvHeaderEmail = view.findViewById(R.id.tvHeaderEmail);
        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvCurrency = view.findViewById(R.id.tvCurrency);
        tvBudget = view.findViewById(R.id.tvBudget);
        tvTheme = view.findViewById(R.id.tvTheme);
        cardGuestMode = view.findViewById(R.id.cardGuestMode);
        layoutAdmin = view.findViewById(R.id.layoutAdmin);

        // Stats views
        tvTotalTransactions = view.findViewById(R.id.tvTotalTransactions);
        tvThisMonth = view.findViewById(R.id.tvThisMonth);
        tvBudgetUsed = view.findViewById(R.id.tvBudgetUsed);

        // Animation views
        headerLayout = view.findViewById(R.id.headerLayout);
        cardStats = view.findViewById(R.id.cardStats);
        cardAccount = view.findViewById(R.id.cardAccount);
        cardPreferences = view.findViewById(R.id.cardPreferences);

        // Menu button
        btnMenu = view.findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (getActivity() instanceof com.example.trackexpense.MainActivity) {
                    ((com.example.trackexpense.MainActivity) getActivity()).openDrawer();
                }
            });
        }
    }

    private void observeData() {
        expenseViewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses != null) {
                if (isFirstLoad && skeletonView != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        hideSkeletonLoading(() -> {
                            updateStats(expenses);
                        });
                    }, 500);
                } else {
                    updateStats(expenses);
                }
            }
        });
    }

    private void updateStats(java.util.List<com.example.trackexpense.data.local.Expense> expenses) {
        // Total transactions
        int totalCount = expenses.size();
        animateCounter(tvTotalTransactions, 0, totalCount);

        // This month transactions
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        long monthStart = cal.getTimeInMillis();

        int thisMonthCount = 0;
        double monthlyExpense = 0;
        for (com.example.trackexpense.data.local.Expense e : expenses) {
            if (e.getDate() >= monthStart) {
                thisMonthCount++;
                if ("EXPENSE".equals(e.getType())) {
                    monthlyExpense += e.getAmount();
                }
            }
        }
        animateCounter(tvThisMonth, 0, thisMonthCount);

        // Budget used percentage
        double budget = preferenceManager.getMonthlyBudget();
        int budgetPercent = budget > 0 ? (int) ((monthlyExpense / budget) * 100) : 0;
        if (budgetPercent > 100)
            budgetPercent = 100;
        animatePercentage(tvBudgetUsed, 0, budgetPercent);
    }

    private void animateCounter(TextView textView, int start, int end) {
        if (textView == null)
            return;
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(start, end);
        animator.setDuration(1200);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            if (textView != null && isAdded()) {
                int value = (int) animation.getAnimatedValue();
                textView.setText(String.valueOf(value));
            }
        });
        animator.start();
    }

    private void animatePercentage(TextView textView, int start, int end) {
        if (textView == null)
            return;
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(start, end);
        animator.setDuration(1200);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            if (textView != null && isAdded()) {
                int value = (int) animation.getAnimatedValue();
                textView.setText(value + "%");
            }
        });
        animator.start();
    }

    private void loadUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        MaterialButton btnLogout = getView().findViewById(R.id.btnLogout);
        LinearLayout layoutDeleteAccount = getView().findViewById(R.id.layoutDeleteAccount);

        if (user != null) {
            // FIRST: Load cached data immediately for instant display
            if (preferenceManager.hasUserProfileCached()) {
                String cachedName = preferenceManager.getCachedUserName();
                String cachedEmail = preferenceManager.getCachedUserEmail();
                tvHeaderName.setText(cachedName);
                if (tvHeaderEmail != null)
                    tvHeaderEmail.setText(cachedEmail);
                tvName.setText(cachedName);
                tvEmail.setText(cachedEmail);
            }

            // THEN: Fetch fresh data from Firestore in background and update cache
            String userId = user.getUid();
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!isAdded())
                            return;

                        String displayName;
                        String email;

                        if (documentSnapshot.exists()) {
                            // Get data from Firestore
                            displayName = documentSnapshot.getString("displayName");
                            email = documentSnapshot.getString("email");
                        } else {
                            // Fallback to Firebase Auth data
                            displayName = user.getDisplayName();
                            email = user.getEmail();
                        }

                        // Apply fallbacks for null/empty values
                        if (displayName == null || displayName.trim().isEmpty()) {
                            displayName = "No Name Provided";
                        }
                        if (email == null || email.trim().isEmpty()) {
                            email = "No Email Provided";
                        }

                        // Update cache with fresh data
                        preferenceManager.cacheUserName(displayName);
                        preferenceManager.cacheUserEmail(email);

                        // Update UI only if different from cached (avoid flicker)
                        String cachedName = preferenceManager.getCachedUserName();
                        if (!displayName.equals(cachedName)) {
                            tvHeaderName.setText(displayName);
                            if (tvHeaderEmail != null)
                                tvHeaderEmail.setText(email);
                            tvName.setText(displayName);
                            tvEmail.setText(email);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isAdded())
                            return;

                        // Only update UI if no cached data
                        if (!preferenceManager.hasUserProfileCached()) {
                            String displayName = user.getDisplayName();
                            String email = user.getEmail();

                            if (displayName == null || displayName.trim().isEmpty()) {
                                displayName = "No Name Provided";
                            }
                            if (email == null || email.trim().isEmpty()) {
                                email = "No Email Provided";
                            }

                            tvHeaderName.setText(displayName);
                            if (tvHeaderEmail != null)
                                tvHeaderEmail.setText(email);
                            tvName.setText(displayName);
                            tvEmail.setText(email);
                        }
                    });

            cardGuestMode.setVisibility(View.GONE);

            // Show Logout for logged in users
            btnLogout.setText("Sign Out");
            btnLogout.setIconResource(R.drawable.ic_logout);

            // Show delete account in preferences for logged in users
            if (layoutDeleteAccount != null) {
                layoutDeleteAccount.setVisibility(View.VISIBLE);
                layoutDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
            }
        } else {
            tvHeaderName.setText("Guest User");
            if (tvHeaderEmail != null)
                tvHeaderEmail.setText("Sign in for more features");
            tvName.setText("Guest User");
            tvEmail.setText("Not signed in");
            cardGuestMode.setVisibility(View.VISIBLE);

            // Show Login for guest users
            btnLogout.setText("Login / Register");
            btnLogout.setIconResource(R.drawable.ic_person);

            // Hide delete account for guests
            if (layoutDeleteAccount != null) {
                layoutDeleteAccount.setVisibility(View.GONE);
            }

            // Show Delete All Data button for guests
            MaterialButton btnDeleteAllData = getView().findViewById(R.id.btnDeleteAllData);
            if (btnDeleteAllData != null) {
                btnDeleteAllData.setVisibility(View.VISIBLE);
                btnDeleteAllData.setOnClickListener(v -> showDeleteAllDataDialog());
            }
        }

        // Load preferences
        String currency = preferenceManager.getCurrency();
        String symbol = preferenceManager.getCurrencySymbol();
        tvCurrency.setText(currency + " (" + symbol + ")");

        double budget = preferenceManager.getMonthlyBudget();
        tvBudget.setText(symbol + String.format("%,.0f", budget));

        int themeMode = preferenceManager.getThemeMode();
        tvTheme.setText(getThemeName(themeMode));
    }

    private void checkAdminAccess() {
        if (layoutAdmin != null) {
            layoutAdmin.setVisibility(View.GONE);

            adminService.checkAdminStatus(isAdmin -> {
                if (isAdmin && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        layoutAdmin.setVisibility(View.VISIBLE);
                    });
                }
            });
        }
    }

    private void setupClickListeners(View view) {
        view.findViewById(R.id.layoutCurrency).setOnClickListener(v -> showCurrencyDialog());
        view.findViewById(R.id.layoutBudget).setOnClickListener(v -> showBudgetDialog());
        view.findViewById(R.id.layoutTheme).setOnClickListener(v -> showThemeDialog());
        view.findViewById(R.id.layoutExport).setOnClickListener(v -> exportData());

        if (layoutAdmin != null) {
            layoutAdmin.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), AdminActivity.class));
            });
        }

        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                logout();
            } else {
                // Guest user - go to welcome/login
                Intent intent = new Intent(requireContext(), WelcomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    private void showCurrencyDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_currency, null);

        android.widget.RadioGroup radioGroup = dialogView.findViewById(R.id.rgCurrency);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);

        // Set current selection
        String currentCurrency = preferenceManager.getCurrency();
        switch (currentCurrency) {
            case "USD":
                radioGroup.check(R.id.rbUSD);
                break;
            case "BDT":
                radioGroup.check(R.id.rbBDT);
                break;
            case "INR":
                radioGroup.check(R.id.rbINR);
                break;
            case "EUR":
                radioGroup.check(R.id.rbEUR);
                break;
            case "GBP":
                radioGroup.check(R.id.rbGBP);
                break;
        }

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            String selectedCurrency = "USD";
            if (selectedId == R.id.rbUSD)
                selectedCurrency = "USD";
            else if (selectedId == R.id.rbBDT)
                selectedCurrency = "BDT";
            else if (selectedId == R.id.rbINR)
                selectedCurrency = "INR";
            else if (selectedId == R.id.rbEUR)
                selectedCurrency = "EUR";
            else if (selectedId == R.id.rbGBP)
                selectedCurrency = "GBP";

            preferenceManager.setCurrency(selectedCurrency);
            loadUserData();
            BeautifulNotification.showSuccess(requireActivity(), "Currency updated!");
            dialog.dismiss();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private void showBudgetDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_monthly_budget, null);

        com.google.android.material.textfield.TextInputLayout tilBudget = dialogView.findViewById(R.id.tilBudget);
        com.google.android.material.textfield.TextInputEditText etBudget = dialogView.findViewById(R.id.etBudget);
        TextView tvCurrentBudget = dialogView.findViewById(R.id.tvCurrentBudget);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);

        // Quick amount chips
        com.google.android.material.chip.Chip chip5000 = dialogView.findViewById(R.id.chip5000);
        com.google.android.material.chip.Chip chip10000 = dialogView.findViewById(R.id.chip10000);
        com.google.android.material.chip.Chip chip20000 = dialogView.findViewById(R.id.chip20000);
        com.google.android.material.chip.Chip chip50000 = dialogView.findViewById(R.id.chip50000);

        // Set currency prefix
        String symbol = preferenceManager.getCurrencySymbol();
        tilBudget.setPrefixText(symbol + " ");

        // Display current budget
        double currentBudget = preferenceManager.getMonthlyBudget();
        tvCurrentBudget.setText(symbol + String.format("%,.0f", currentBudget));

        if (currentBudget > 0) {
            etBudget.setText(String.format("%.0f", currentBudget));
        }

        // Quick amount chip listeners
        View.OnClickListener chipListener = v -> {
            com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) v;
            String amount = chip.getText().toString().replace(",", "");
            etBudget.setText(amount);
            etBudget.setSelection(amount.length());
        };
        chip5000.setOnClickListener(chipListener);
        chip10000.setOnClickListener(chipListener);
        chip20000.setOnClickListener(chipListener);
        chip50000.setOnClickListener(chipListener);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
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

                preferenceManager.setMonthlyBudget(budget);
                saveBudgetToFirestore(budget);
                loadUserData();
                BeautifulNotification.showSuccess(requireActivity(),
                        "Budget updated to " + symbol + String.format("%,.0f", budget));
                dialog.dismiss();
            } catch (NumberFormatException e) {
                tilBudget.setError("Invalid amount");
            }
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private void saveBudgetToFirestore(double budget) {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (user == null)
            return;

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("monthlyBudget", budget);

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .update(updates);
    }

    private void showThemeDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_theme, null);

        com.google.android.material.card.MaterialCardView cardLight = dialogView.findViewById(R.id.cardLight);
        com.google.android.material.card.MaterialCardView cardDark = dialogView.findViewById(R.id.cardDark);
        com.google.android.material.card.MaterialCardView cardSystem = dialogView.findViewById(R.id.cardSystem);
        ImageView checkLight = dialogView.findViewById(R.id.checkLight);
        ImageView checkDark = dialogView.findViewById(R.id.checkDark);
        ImageView checkSystem = dialogView.findViewById(R.id.checkSystem);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        com.google.android.material.button.MaterialButton btnApply = dialogView.findViewById(R.id.btnApply);

        int[] selectedTheme = { preferenceManager.getThemeMode() };

        // Update UI based on current selection
        Runnable updateSelection = () -> {
            // Reset all
            cardLight.setStrokeColor(getResources().getColor(R.color.gray_200, null));
            cardDark.setStrokeColor(getResources().getColor(R.color.gray_200, null));
            cardSystem.setStrokeColor(getResources().getColor(R.color.gray_200, null));
            checkLight.setVisibility(View.GONE);
            checkDark.setVisibility(View.GONE);
            checkSystem.setVisibility(View.GONE);

            // Highlight selected
            if (selectedTheme[0] == AppCompatDelegate.MODE_NIGHT_NO) {
                cardLight.setStrokeColor(getResources().getColor(R.color.primary, null));
                checkLight.setVisibility(View.VISIBLE);
            } else if (selectedTheme[0] == AppCompatDelegate.MODE_NIGHT_YES) {
                cardDark.setStrokeColor(getResources().getColor(R.color.primary, null));
                checkDark.setVisibility(View.VISIBLE);
            } else {
                cardSystem.setStrokeColor(getResources().getColor(R.color.primary, null));
                checkSystem.setVisibility(View.VISIBLE);
            }
        };
        updateSelection.run();

        cardLight.setOnClickListener(v -> {
            selectedTheme[0] = AppCompatDelegate.MODE_NIGHT_NO;
            updateSelection.run();
        });

        cardDark.setOnClickListener(v -> {
            selectedTheme[0] = AppCompatDelegate.MODE_NIGHT_YES;
            updateSelection.run();
        });

        cardSystem.setOnClickListener(v -> {
            selectedTheme[0] = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            updateSelection.run();
        });

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnApply.setOnClickListener(v -> {
            preferenceManager.setThemeMode(selectedTheme[0]);
            AppCompatDelegate.setDefaultNightMode(selectedTheme[0]);
            loadUserData();
            BeautifulNotification.showSuccess(requireActivity(), "Theme updated!");
            dialog.dismiss();
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }

    private void exportData() {
        expenseViewModel.getAllExpenses().observe(getViewLifecycleOwner(), expenses -> {
            if (expenses != null && !expenses.isEmpty()) {
                try {
                    File file = ExportUtils.exportToCsv(requireContext(), expenses,
                            preferenceManager.getCurrencySymbol());
                    ExportUtils.shareFile(requireContext(), file);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Export failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "No data to export", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logout() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_logout, null);

        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelLogout);
        com.google.android.material.button.MaterialButton btnLogout = dialogView.findViewById(R.id.btnConfirmLogout);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnLogout.setOnClickListener(v -> {
            dialog.dismiss();
            // Clear cached user data
            preferenceManager.clearUserCache();
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(requireContext(), WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("SHOW_LOGOUT_MESSAGE", true);
            startActivity(intent);
        });

        dialog.show();
    }

    private void showDeleteAccountDialog() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "No account to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_delete_account, null);

        com.google.android.material.textfield.TextInputEditText etConfirm = dialogView
                .findViewById(R.id.etConfirmDelete);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelDelete);
        com.google.android.material.button.MaterialButton btnDelete = dialogView.findViewById(R.id.btnConfirmDelete);

        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                requireContext())
                .setView(dialogView)
                .create();

        // Enable delete button only when "DELETE" is typed
        etConfirm.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnDelete.setEnabled("DELETE".equals(s.toString().trim()));
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            btnDelete.setEnabled(false);
            btnDelete.setText("Deleting...");
            deleteAccount(user);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void deleteAccount(FirebaseUser user) {
        String userId = user.getUid();

        // Delete user data from Firestore first
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .delete()
                .addOnCompleteListener(task -> {
                    // Then delete the Firebase Auth account
                    user.delete()
                            .addOnCompleteListener(deleteTask -> {
                                if (deleteTask.isSuccessful()) {
                                    BeautifulNotification.showSuccess(requireActivity(),
                                            "Account deleted successfully!");
                                    Intent intent = new Intent(requireContext(), WelcomeActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                } else {
                                    // User may need to re-authenticate
                                    BeautifulNotification.showError(requireActivity(),
                                            "Failed to delete account. Please re-login and try again.");
                                }
                            });
                });
    }

    private String getThemeName(int mode) {
        switch (mode) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                return "Light";
            case AppCompatDelegate.MODE_NIGHT_YES:
                return "Dark";
            default:
                return "System Default";
        }
    }

    /**
     * Show dialog to confirm deleting all local data for guest user.
     */
    private void showDeleteAllDataDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_confirmation, null);

        // Find and configure views
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnConfirm = dialogView.findViewById(R.id.btnConfirm);
        ImageView ivIcon = dialogView.findViewById(R.id.ivDialogIcon);

        if (tvTitle != null)
            tvTitle.setText("Delete All Data?");
        if (tvMessage != null)
            tvMessage
                    .setText("This will permanently delete all your local transactions. This action cannot be undone.");
        if (btnConfirm != null)
            btnConfirm.setText("Delete All");

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext(),
                R.style.Theme_TrackExpense_Dialog)
                .setView(dialogView)
                .create();

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();

                // Delete all local data
                ExpenseRepository repository = new ExpenseRepository(requireActivity().getApplication());
                repository.deleteAllLocalExpenses(new ExpenseRepository.OnCompleteListener() {
                    @Override
                    public void onSuccess() {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                BeautifulNotification.showSuccess(requireActivity(),
                                        "All local data deleted successfully!");
                            });
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                BeautifulNotification.showError(requireActivity(), "Failed to delete data: " + error);
                            });
                        }
                    }
                });
            });
        }

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();
    }
}
