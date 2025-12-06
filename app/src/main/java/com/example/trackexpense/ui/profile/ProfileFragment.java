package com.example.trackexpense.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.trackexpense.R;
import com.example.trackexpense.data.remote.AdminService;
import com.example.trackexpense.ui.admin.AdminActivity;
import com.example.trackexpense.ui.auth.LoginActivity;
import com.example.trackexpense.utils.ExportUtils;
import com.example.trackexpense.utils.PreferenceManager;
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
    
    private TextView tvHeaderName, tvName, tvEmail, tvCurrency, tvBudget, tvTheme;
    private Chip chipGuestMode;
    private LinearLayout layoutAdmin;
    private View dividerAdmin;

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

        initViews(view);
        loadUserData();
        setupClickListeners(view);
        checkAdminAccess();
    }

    private void initViews(View view) {
        tvHeaderName = view.findViewById(R.id.tvHeaderName);
        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvCurrency = view.findViewById(R.id.tvCurrency);
        tvBudget = view.findViewById(R.id.tvBudget);
        tvTheme = view.findViewById(R.id.tvTheme);
        chipGuestMode = view.findViewById(R.id.chipGuestMode);
        layoutAdmin = view.findViewById(R.id.layoutAdmin);
        dividerAdmin = view.findViewById(R.id.dividerAdmin);
    }

    private void loadUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String displayName = user.getDisplayName() != null ? user.getDisplayName() : "User";
            tvHeaderName.setText(displayName);
            tvName.setText(displayName);
            tvEmail.setText(user.getEmail());
            chipGuestMode.setVisibility(View.GONE);
        } else {
            tvHeaderName.setText("Guest User");
            tvName.setText("Guest User");
            tvEmail.setText("No account");
            chipGuestMode.setVisibility(View.VISIBLE);
        }

        // Load preferences
        String currency = preferenceManager.getCurrency();
        String symbol = preferenceManager.getCurrencySymbol();
        tvCurrency.setText(currency + " (" + symbol + ")");

        double budget = preferenceManager.getMonthlyBudget();
        tvBudget.setText(symbol + String.format("%.2f", budget));

        int themeMode = preferenceManager.getThemeMode();
        tvTheme.setText(getThemeName(themeMode));
    }

    private void checkAdminAccess() {
        if (layoutAdmin != null && dividerAdmin != null) {
            layoutAdmin.setVisibility(View.GONE);
            dividerAdmin.setVisibility(View.GONE);
            
            adminService.checkAdminStatus(isAdmin -> {
                if (isAdmin && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        layoutAdmin.setVisibility(View.VISIBLE);
                        dividerAdmin.setVisibility(View.VISIBLE);
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
        btnLogout.setOnClickListener(v -> logout());
    }

    private void showCurrencyDialog() {
        String[] currencies = { "USD", "BDT", "INR", "EUR", "GBP" };
        int currentIndex = java.util.Arrays.asList(currencies).indexOf(preferenceManager.getCurrency());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Currency")
                .setSingleChoiceItems(currencies, currentIndex, (dialog, which) -> {
                    preferenceManager.setCurrency(currencies[which]);
                    loadUserData();
                    dialog.dismiss();
                })
                .show();
    }

    private void showBudgetDialog() {
        TextInputEditText input = new TextInputEditText(requireContext());
        input.setHint("Enter monthly budget");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf(preferenceManager.getMonthlyBudget()));

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Set Monthly Budget")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        double budget = Double.parseDouble(input.getText().toString());
                        preferenceManager.setMonthlyBudget(budget);
                        loadUserData();
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showThemeDialog() {
        String[] themes = { "Light", "Dark", "System Default" };
        int[] themeModes = { AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES,
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM };
        int currentMode = preferenceManager.getThemeMode();
        int currentIndex = currentMode == AppCompatDelegate.MODE_NIGHT_NO ? 0
                : currentMode == AppCompatDelegate.MODE_NIGHT_YES ? 1 : 2;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Theme")
                .setSingleChoiceItems(themes, currentIndex, (dialog, which) -> {
                    preferenceManager.setThemeMode(themeModes[which]);
                    AppCompatDelegate.setDefaultNightMode(themeModes[which]);
                    loadUserData();
                    dialog.dismiss();
                })
                .show();
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
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
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
}
