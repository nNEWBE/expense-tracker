package com.example.trackexpense.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.trackexpense.R;
import com.example.trackexpense.data.remote.AdminService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AdminActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private AdminService adminService;

    private final String[] tabTitles = { "Users", "Categories" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        adminService = AdminService.getInstance();

        // Check admin status
        adminService.checkAdminStatus(isAdmin -> {
            if (!isAdmin) {
                Toast.makeText(this, "Access denied: Admin privileges required", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            runOnUiThread(this::setupUI);
        });
    }

    private void setupUI() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        viewPager.setAdapter(new AdminPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])).attach();
    }

    private static class AdminPagerAdapter extends FragmentStateAdapter {
        public AdminPagerAdapter(AppCompatActivity activity) {
            super(activity);
        }

        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new AdminUsersFragment();
                case 1:
                    return new AdminCategoriesFragment();
                default:
                    return new AdminUsersFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
