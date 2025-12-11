package com.example.trackexpense.ui.admin;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackexpense.R;
import com.example.trackexpense.data.model.User;
import com.example.trackexpense.data.remote.AdminService;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.example.trackexpense.utils.BeautifulNotification;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminUsersFragment extends Fragment {

    private AdminService adminService;
    private RecyclerView rvUsers;
    private TextView tvUserCount;
    private MaterialCardView chipAll, chipVerified, chipAdmin, chipBlocked;
    private UserAdapter adapter;
    private List<User> allUsers = new ArrayList<>();
    private String currentFilter = "ALL"; // ALL, VERIFIED, ADMIN, BLOCKED

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adminService = AdminService.getInstance();

        tvUserCount = view.findViewById(R.id.tvUserCount);
        rvUsers = view.findViewById(R.id.rvUsers);

        // Filter chips
        chipAll = view.findViewById(R.id.chipAll);
        chipVerified = view.findViewById(R.id.chipVerified);
        chipAdmin = view.findViewById(R.id.chipAdmin);
        chipBlocked = view.findViewById(R.id.chipBlocked);

        setupRecyclerView();
        setupFilters();
        observeUsers();
    }

    private void setupRecyclerView() {
        adapter = new UserAdapter();
        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvUsers.setAdapter(adapter);
    }

    private void setupFilters() {
        updateFilterChipStyles();

        if (chipAll != null) {
            chipAll.setOnClickListener(v -> {
                currentFilter = "ALL";
                updateFilterChipStyles();
                filterUsers();
            });
        }
        if (chipVerified != null) {
            chipVerified.setOnClickListener(v -> {
                currentFilter = "VERIFIED";
                updateFilterChipStyles();
                filterUsers();
            });
        }
        if (chipAdmin != null) {
            chipAdmin.setOnClickListener(v -> {
                currentFilter = "ADMIN";
                updateFilterChipStyles();
                filterUsers();
            });
        }
        if (chipBlocked != null) {
            chipBlocked.setOnClickListener(v -> {
                currentFilter = "BLOCKED";
                updateFilterChipStyles();
                filterUsers();
            });
        }
    }

    private void updateFilterChipStyles() {
        // All chip
        if (chipAll != null) {
            if ("ALL".equals(currentFilter)) {
                chipAll.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary));
                chipAll.setStrokeWidth(0);
            } else {
                chipAll.setCardBackgroundColor(Color.WHITE);
                chipAll.setStrokeColor(Color.parseColor("#E2E8F0"));
                chipAll.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
            }
        }
        // Verified chip
        if (chipVerified != null) {
            if ("VERIFIED".equals(currentFilter)) {
                chipVerified.setCardBackgroundColor(Color.parseColor("#DCFCE7"));
                chipVerified.setStrokeColor(Color.parseColor("#22C55E"));
            } else {
                chipVerified.setCardBackgroundColor(Color.parseColor("#F0FDF4"));
                chipVerified.setStrokeColor(Color.parseColor("#BBF7D0"));
            }
        }
        // Admin chip
        if (chipAdmin != null) {
            if ("ADMIN".equals(currentFilter)) {
                chipAdmin.setCardBackgroundColor(Color.parseColor("#E0E7FF"));
                chipAdmin.setStrokeColor(Color.parseColor("#6366F1"));
            } else {
                chipAdmin.setCardBackgroundColor(Color.parseColor("#EEF2FF"));
                chipAdmin.setStrokeColor(Color.parseColor("#C7D2FE"));
            }
        }
        // Blocked chip
        if (chipBlocked != null) {
            if ("BLOCKED".equals(currentFilter)) {
                chipBlocked.setCardBackgroundColor(Color.parseColor("#FEE2E2"));
                chipBlocked.setStrokeColor(Color.parseColor("#EF4444"));
            } else {
                chipBlocked.setCardBackgroundColor(Color.parseColor("#FEF2F2"));
                chipBlocked.setStrokeColor(Color.parseColor("#FECACA"));
            }
        }
    }

    private void observeUsers() {
        adminService.getAllUsers().observe(getViewLifecycleOwner(), users -> {
            allUsers = users;
            filterUsers();
        });
    }

    private void filterUsers() {
        List<User> filtered = allUsers.stream()
                .filter(u -> {
                    // Category filter
                    switch (currentFilter) {
                        case "VERIFIED":
                            return u.isVerified();
                        case "ADMIN":
                            return u.isAdmin();
                        case "BLOCKED":
                            return u.isBlocked();
                        default:
                            return true;
                    }
                })
                .collect(Collectors.toList());

        // Update count text based on filter
        String filterLabel;
        switch (currentFilter) {
            case "VERIFIED":
                filterLabel = "Verified Users";
                break;
            case "ADMIN":
                filterLabel = "Admin Users";
                break;
            case "BLOCKED":
                filterLabel = "Blocked Users";
                break;
            default:
                filterLabel = "All Users";
                break;
        }
        tvUserCount.setText(filterLabel + " (" + filtered.size() + ")");
        adapter.setUsers(filtered);
    }

    private void showUserActions(User user, View anchor) {
        // Create custom popup window with icons
        View popupView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_user_actions, null);

        // Get current user ID to check if this is the logged-in admin
        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";
        boolean isCurrentUser = user.getId() != null && user.getId().equals(currentUserId);

        // Wrap content in ScrollView for better accessibility
        android.widget.ScrollView scrollView = new android.widget.ScrollView(requireContext());
        scrollView.addView(popupView);

        // Set max height for popup (for bottom users)
        int maxHeight = (int) (300 * getResources().getDisplayMetrics().density);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        PopupWindow popupWindow = new PopupWindow(scrollView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                maxHeight,
                true);

        popupWindow.setElevation(16f);
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_popup_menu));

        // Setup menu items
        View itemEdit = popupView.findViewById(R.id.itemEditUsername);
        View itemBlock = popupView.findViewById(R.id.itemBlockUser);
        View itemTransactions = popupView.findViewById(R.id.itemViewTransactions);
        View itemAdmin = popupView.findViewById(R.id.itemToggleAdmin);
        View itemDelete = popupView.findViewById(R.id.itemDeleteUser);

        TextView tvBlock = popupView.findViewById(R.id.tvBlockUser);
        ImageView ivBlock = popupView.findViewById(R.id.ivBlockUser);
        TextView tvAdmin = popupView.findViewById(R.id.tvToggleAdmin);
        ImageView ivAdmin = popupView.findViewById(R.id.ivToggleAdmin);

        // Hide dangerous options and divider for current logged-in admin
        if (isCurrentUser) {
            if (itemBlock != null)
                itemBlock.setVisibility(View.GONE);
            if (itemAdmin != null)
                itemAdmin.setVisibility(View.GONE);
            if (itemDelete != null)
                itemDelete.setVisibility(View.GONE);
            // Also hide divider
            View divider = popupView.findViewById(R.id.divider);
            if (divider != null)
                divider.setVisibility(View.GONE);
        }

        // Update block/unblock text
        if (tvBlock != null) {
            tvBlock.setText(user.isBlocked() ? "Unblock User" : "Block User");
        }
        if (ivBlock != null) {
            ivBlock.setImageResource(user.isBlocked() ? R.drawable.ic_check_circle : R.drawable.ic_block);
            ivBlock.setColorFilter(ContextCompat.getColor(requireContext(),
                    user.isBlocked() ? R.color.income_green : R.color.expense_red));
        }

        // Update admin text
        if (tvAdmin != null) {
            tvAdmin.setText(user.isAdmin() ? "Remove Admin" : "Make Admin");
        }
        if (ivAdmin != null) {
            ivAdmin.setColorFilter(ContextCompat.getColor(requireContext(),
                    user.isAdmin() ? R.color.expense_red : R.color.primary));
        }

        // Click listeners
        if (itemEdit != null) {
            itemEdit.setOnClickListener(v -> {
                popupWindow.dismiss();
                showEditUsernameDialog(user);
            });
        }
        if (itemBlock != null) {
            itemBlock.setOnClickListener(v -> {
                popupWindow.dismiss();
                toggleBlockUser(user);
            });
        }
        if (itemTransactions != null) {
            itemTransactions.setOnClickListener(v -> {
                popupWindow.dismiss();
                viewUserTransactions(user);
            });
        }
        if (itemAdmin != null) {
            itemAdmin.setOnClickListener(v -> {
                popupWindow.dismiss();
                toggleAdminStatus(user);
            });
        }
        if (itemDelete != null) {
            itemDelete.setOnClickListener(v -> {
                popupWindow.dismiss();
                confirmDeleteUser(user);
            });
        }

        // Show popup above anchor if near bottom of screen
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        if (location[1] > screenHeight / 2) {
            // Near bottom, show above
            popupWindow.showAsDropDown(anchor, -200, -maxHeight - anchor.getHeight(), Gravity.END);
        } else {
            popupWindow.showAsDropDown(anchor, 0, 0, Gravity.END);
        }
    }

    private void showEditUsernameDialog(User user) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_username, null);

        TextInputEditText etUsername = dialogView.findViewById(R.id.etUsername);
        etUsername.setText(user.getDisplayName());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Username")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
                    if (!newName.isEmpty()) {
                        user.setDisplayName(newName);
                        adminService.updateUser(user, new AdminService.OnCompleteListener() {
                            @Override
                            public void onSuccess() {
                                BeautifulNotification.showSuccess(requireActivity(), "Username updated!");
                            }

                            @Override
                            public void onFailure(Exception e) {
                                BeautifulNotification.showError(requireActivity(), "Error: " + e.getMessage());
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleBlockUser(User user) {
        boolean newBlockedStatus = !user.isBlocked();
        String actionText = newBlockedStatus ? "blocked" : "unblocked";

        adminService.blockUser(user.getId(), newBlockedStatus, new AdminService.OnCompleteListener() {
            @Override
            public void onSuccess() {
                if (newBlockedStatus) {
                    BeautifulNotification.showWarning(requireActivity(),
                            user.getDisplayName() + " has been blocked");
                } else {
                    BeautifulNotification.showSuccess(requireActivity(),
                            user.getDisplayName() + " has been unblocked");
                }
            }

            @Override
            public void onFailure(Exception e) {
                BeautifulNotification.showError(requireActivity(), "Error: " + e.getMessage());
            }
        });
    }

    private void toggleAdminStatus(User user) {
        boolean newAdminStatus = !user.isAdmin();
        user.setAdmin(newAdminStatus);

        adminService.updateUser(user, new AdminService.OnCompleteListener() {
            @Override
            public void onSuccess() {
                if (newAdminStatus) {
                    BeautifulNotification.showSuccess(requireActivity(),
                            user.getDisplayName() + " is now an admin");
                } else {
                    BeautifulNotification.showInfo(requireActivity(),
                            user.getDisplayName() + " admin access removed");
                }
            }

            @Override
            public void onFailure(Exception e) {
                BeautifulNotification.showError(requireActivity(), "Error: " + e.getMessage());
            }
        });
    }

    private void viewUserTransactions(User user) {
        AdminUserTransactionsDialog dialog = new AdminUserTransactionsDialog(user);
        dialog.show(getParentFragmentManager(), "user_transactions");
    }

    private void confirmDeleteUser(User user) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_delete_user, null);

        TextView tvUserInitial = dialogView.findViewById(R.id.tvUserInitial);
        TextView tvUserName = dialogView.findViewById(R.id.tvUserName);
        TextView tvUserEmail = dialogView.findViewById(R.id.tvUserEmail);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelDelete);
        MaterialButton btnDelete = dialogView.findViewById(R.id.btnConfirmDelete);

        String displayName = user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                ? user.getDisplayName()
                : "User";
        tvUserName.setText(displayName);
        tvUserEmail.setText(user.getEmail());
        tvUserInitial.setText(displayName.substring(0, 1).toUpperCase());

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            btnDelete.setEnabled(false);

            adminService.deleteUser(user.getId(), new AdminService.OnCompleteListener() {
                @Override
                public void onSuccess() {
                    dialog.dismiss();
                    BeautifulNotification.showSuccess(requireActivity(), "User deleted successfully!");
                }

                @Override
                public void onFailure(Exception e) {
                    dialog.dismiss();
                    BeautifulNotification.showError(requireActivity(), "Failed to delete user: " + e.getMessage());
                }
            });
        });

        dialog.show();
    }

    // Adapter
    class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private List<User> users = new ArrayList<>();

        public void setUsers(List<User> users) {
            this.users = users;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            holder.bind(users.get(position));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView tvInitial, tvName, tvEmail;
            View btnMore, avatarBg, adminIndicator, blockedIndicator;
            ImageView ivVerified;

            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                tvInitial = itemView.findViewById(R.id.tvInitial);
                tvName = itemView.findViewById(R.id.tvName);
                tvEmail = itemView.findViewById(R.id.tvEmail);
                btnMore = itemView.findViewById(R.id.btnMore);
                avatarBg = itemView.findViewById(R.id.avatarBg);
                ivVerified = itemView.findViewById(R.id.ivVerified);
                adminIndicator = itemView.findViewById(R.id.adminIndicator);
                blockedIndicator = itemView.findViewById(R.id.blockedIndicator);
            }

            public void bind(User user) {
                String name = user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                        ? user.getDisplayName()
                        : user.getEmail();
                tvName.setText(name);
                tvEmail.setText(user.getEmail());

                // Set initial
                if (name != null && !name.isEmpty()) {
                    tvInitial.setText(name.substring(0, 1).toUpperCase());
                }

                // Set avatar color - use hash-based color
                int[] colors = { R.color.category_food, R.color.category_transport,
                        R.color.category_shopping, R.color.category_entertainment,
                        R.color.category_health, R.color.primary };
                int colorIndex = Math.abs((user.getEmail() != null ? user.getEmail().hashCode() : 0))
                        % colors.length;
                int avatarColor = ContextCompat.getColor(itemView.getContext(), colors[colorIndex]);

                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(avatarColor);
                avatarBg.setBackground(bg);

                // Show avatar indicators (admin shield or blocked icon)
                if (adminIndicator != null) {
                    adminIndicator.setVisibility(user.isAdmin() && !user.isBlocked() ? View.VISIBLE : View.GONE);
                }
                if (blockedIndicator != null) {
                    blockedIndicator.setVisibility(user.isBlocked() ? View.VISIBLE : View.GONE);
                }

                // Show verified badge (starburst style)
                if (ivVerified != null) {
                    ivVerified.setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);
                }

                btnMore.setOnClickListener(v -> showUserActions(user, v));

                // Click on item to edit username
                itemView.setOnClickListener(v -> showEditUsernameDialog(user));
            }
        }
    }
}
