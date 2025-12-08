package com.example.trackexpense.ui.admin;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminUsersFragment extends Fragment {

    private AdminService adminService;
    private RecyclerView rvUsers;
    private EditText etSearch;
    private TextView tvUserCount;
    private UserAdapter adapter;
    private List<User> allUsers = new ArrayList<>();

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

        etSearch = view.findViewById(R.id.etSearch);
        tvUserCount = view.findViewById(R.id.tvUserCount);
        rvUsers = view.findViewById(R.id.rvUsers);

        setupRecyclerView();
        setupSearch();
        observeUsers();
    }

    private void setupRecyclerView() {
        adapter = new UserAdapter();
        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvUsers.setAdapter(adapter);

        // Add item animation
        android.view.animation.LayoutAnimationController controller = android.view.animation.AnimationUtils
                .loadLayoutAnimation(requireContext(), R.anim.layout_animation_fall_down);
        rvUsers.setLayoutAnimation(controller);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void observeUsers() {
        adminService.getAllUsers().observe(getViewLifecycleOwner(), users -> {
            allUsers = users;
            tvUserCount.setText(users.size() + " Users");
            adapter.setUsers(users);
        });
    }

    private void filterUsers(String query) {
        if (query.isEmpty()) {
            adapter.setUsers(allUsers);
        } else {
            List<User> filtered = allUsers.stream()
                    .filter(u -> (u.getEmail() != null && u.getEmail().toLowerCase().contains(query.toLowerCase())) ||
                            (u.getDisplayName() != null
                                    && u.getDisplayName().toLowerCase().contains(query.toLowerCase())))
                    .collect(Collectors.toList());
            adapter.setUsers(filtered);
        }
    }

    private void showUserActions(User user, View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenu().add("Edit Username");
        popup.getMenu().add(user.isBlocked() ? "Unblock User" : "Block User");
        popup.getMenu().add("View Transactions");
        popup.getMenu().add(user.isAdmin() ? "Remove Admin" : "Make Admin");
        popup.getMenu().add("Delete User");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Edit Username")) {
                showEditUsernameDialog(user);
            } else if (title.contains("Block")) {
                toggleBlockUser(user);
            } else if (title.equals("View Transactions")) {
                viewUserTransactions(user);
            } else if (title.contains("Admin")) {
                toggleAdminStatus(user);
            } else if (title.equals("Delete User")) {
                confirmDeleteUser(user);
            }
            return true;
        });

        popup.show();
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
                                Snackbar.make(requireView(), "Username updated", Snackbar.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Snackbar.make(requireView(), "Error: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleBlockUser(User user) {
        boolean newStatus = !user.isBlocked();
        adminService.blockUser(user.getId(), newStatus, new AdminService.OnCompleteListener() {
            @Override
            public void onSuccess() {
                Snackbar.make(requireView(),
                        user.getEmail() + " " + (newStatus ? "blocked" : "unblocked"),
                        Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Snackbar.make(requireView(), "Error: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void toggleAdminStatus(User user) {
        user.setAdmin(!user.isAdmin());
        adminService.updateUser(user, new AdminService.OnCompleteListener() {
            @Override
            public void onSuccess() {
                Snackbar.make(requireView(),
                        user.getEmail() + " admin status updated",
                        Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Snackbar.make(requireView(), "Error: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void viewUserTransactions(User user) {
        AdminUserTransactionsDialog dialog = new AdminUserTransactionsDialog(user);
        dialog.show(getParentFragmentManager(), "user_transactions");
    }

    private void confirmDeleteUser(User user) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete " + user.getEmail()
                        + "?\n\nThis will permanently delete:\n• User account\n• All transactions\n• All data")
                .setPositiveButton("Delete", (dialog, which) -> {
                    adminService.deleteUser(user.getId(), new AdminService.OnCompleteListener() {
                        @Override
                        public void onSuccess() {
                            Snackbar.make(requireView(), "User deleted successfully", Snackbar.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Snackbar.make(requireView(), "Error: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
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
            Chip chipBlocked, chipAdmin;
            View btnMore;
            View avatarBg;
            ImageView ivVerified;

            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                tvInitial = itemView.findViewById(R.id.tvInitial);
                tvName = itemView.findViewById(R.id.tvName);
                tvEmail = itemView.findViewById(R.id.tvEmail);
                chipBlocked = itemView.findViewById(R.id.chipBlocked);
                chipAdmin = itemView.findViewById(R.id.chipAdmin);
                btnMore = itemView.findViewById(R.id.btnMore);
                avatarBg = itemView.findViewById(R.id.avatarBg);
                ivVerified = itemView.findViewById(R.id.ivVerified);
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

                // Set avatar color based on email hash
                int[] colors = { R.color.category_food, R.color.category_transport,
                        R.color.category_shopping, R.color.category_entertainment,
                        R.color.category_health, R.color.primary };
                int colorIndex = Math.abs((user.getEmail() != null ? user.getEmail().hashCode() : 0)) % colors.length;

                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(ContextCompat.getColor(itemView.getContext(), colors[colorIndex]));
                avatarBg.setBackground(bg);

                // Show badges
                chipBlocked.setVisibility(user.isBlocked() ? View.VISIBLE : View.GONE);
                chipAdmin.setVisibility(user.isAdmin() ? View.VISIBLE : View.GONE);

                // Show verified badge
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
