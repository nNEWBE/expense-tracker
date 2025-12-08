package com.example.trackexpense.ui.admin;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.trackexpense.R;
import com.example.trackexpense.data.remote.AdminService;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AdminActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private AdminService adminService;
    private TextView tvTotalUsers, tvTotalCategories;
    private FrameLayout headerLayout;
    private MaterialCardView cardUsers, cardCategories, tabCard;
    private View btnBack;

    private final String[] tabTitles = { "Users", "Categories" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge fullscreen
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

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
        // Back button
        btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Stats
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvTotalCategories = findViewById(R.id.tvTotalCategories);

        // Animation views
        headerLayout = findViewById(R.id.headerLayout);
        cardUsers = findViewById(R.id.cardUsers);
        cardCategories = findViewById(R.id.cardCategories);
        tabCard = findViewById(R.id.tabCard);

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        viewPager.setAdapter(new AdminPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])).attach();

        // Load stats
        loadStats();

        // Run entrance animations
        runEntranceAnimations();
    }

    private void runEntranceAnimations() {
        try {
            Animation slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down);
            Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);
            Animation scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up);

            // Header slides down
            if (headerLayout != null) {
                headerLayout.startAnimation(slideDown);
            }

            // Stats cards scale up with stagger
            if (cardUsers != null) {
                cardUsers.setVisibility(View.INVISIBLE);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    cardUsers.setVisibility(View.VISIBLE);
                    Animation anim = AnimationUtils.loadAnimation(this, R.anim.scale_up);
                    cardUsers.startAnimation(anim);
                }, 200);
            }

            if (cardCategories != null) {
                cardCategories.setVisibility(View.INVISIBLE);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    cardCategories.setVisibility(View.VISIBLE);
                    Animation anim = AnimationUtils.loadAnimation(this, R.anim.scale_up);
                    cardCategories.startAnimation(anim);
                }, 350);
            }

            // Tab card slides up
            if (tabCard != null) {
                tabCard.setVisibility(View.INVISIBLE);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    tabCard.setVisibility(View.VISIBLE);
                    Animation anim = AnimationUtils.loadAnimation(this, R.anim.slide_up);
                    tabCard.startAnimation(anim);
                }, 400);
            }

        } catch (Exception e) {
            // Ensure visibility if animations fail
            if (cardUsers != null)
                cardUsers.setVisibility(View.VISIBLE);
            if (cardCategories != null)
                cardCategories.setVisibility(View.VISIBLE);
            if (tabCard != null)
                tabCard.setVisibility(View.VISIBLE);
        }
    }

    private void loadStats() {
        // Observe users LiveData
        adminService.getAllUsers().observe(this, users -> {
            if (users != null && tvTotalUsers != null) {
                animateCounter(tvTotalUsers, users.size());
            }
        });

        // Observe categories LiveData
        adminService.getAllCategories().observe(this, categories -> {
            if (categories != null && tvTotalCategories != null) {
                animateCounter(tvTotalCategories, categories.size());
            }
        });
    }

    private void animateCounter(TextView textView, int endValue) {
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(0, endValue);
        animator.setDuration(1500);
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            textView.setText(String.valueOf(value));
        });
        animator.start();
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
