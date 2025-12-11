package com.example.trackexpense.ui.admin;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackexpense.R;
import com.example.trackexpense.data.model.CategoryModel;
import com.example.trackexpense.data.remote.AdminService;
import com.example.trackexpense.utils.CategoryHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminCategoriesFragment extends Fragment {

    // Listener interfaces
    interface OnIconSelectedListener {
        void onSelected(String iconName);
    }

    interface OnColorSelectedListener {
        void onSelected(String colorHex);
    }

    private AdminService adminService;
    private RecyclerView rvCategories;
    private EditText etSearch;
    private com.google.android.material.card.MaterialCardView chipAll, chipExpense, chipIncome;
    private TextView tvCategoryCount;
    private com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fabAdd;
    private CategoryAdapter adapter;
    private List<CategoryModel> allCategories = new ArrayList<>();
    private String currentFilter = "ALL"; // ALL, EXPENSE, INCOME

    // Icon options - expanded list
    private static final String[] ICON_NAMES = {
            "ic_food", "ic_transport", "ic_shopping", "ic_entertainment",
            "ic_health", "ic_bills", "ic_education", "ic_travel",
            "ic_groceries", "ic_subscription", "ic_salary", "ic_freelance",
            "ic_investment", "ic_gift", "ic_school", "ic_location",
            "ic_document", "ic_book", "ic_grid", "ic_other"
    };

    private static final String[] COLOR_OPTIONS = {
            "#F97316", "#3B82F6", "#EC4899", "#8B5CF6",
            "#10B981", "#EAB308", "#06B6D4", "#F43F5E",
            "#22C55E", "#A855F7", "#14B8A6", "#F59E0B"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_categories, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adminService = AdminService.getInstance();

        etSearch = view.findViewById(R.id.etSearch);
        chipAll = view.findViewById(R.id.chipAll);
        chipExpense = view.findViewById(R.id.chipExpense);
        chipIncome = view.findViewById(R.id.chipIncome);
        tvCategoryCount = view.findViewById(R.id.tvCategoryCount);
        rvCategories = view.findViewById(R.id.rvCategories);
        fabAdd = view.findViewById(R.id.fabAdd);

        setupRecyclerView();
        setupSearch();
        setupFilters();
        setupFab();
        observeCategories();
    }

    private void setupRecyclerView() {
        adapter = new CategoryAdapter();
        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCategories.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCategories();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupFilters() {
        updateFilterChipStyles();

        if (chipAll != null) {
            chipAll.setOnClickListener(v -> {
                currentFilter = "ALL";
                updateFilterChipStyles();
                filterCategories();
            });
        }
        if (chipExpense != null) {
            chipExpense.setOnClickListener(v -> {
                currentFilter = "EXPENSE";
                updateFilterChipStyles();
                filterCategories();
            });
        }
        if (chipIncome != null) {
            chipIncome.setOnClickListener(v -> {
                currentFilter = "INCOME";
                updateFilterChipStyles();
                filterCategories();
            });
        }
    }

    private void updateFilterChipStyles() {
        // Update All chip
        if (chipAll != null) {
            if ("ALL".equals(currentFilter)) {
                chipAll.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary));
            } else {
                chipAll.setCardBackgroundColor(Color.parseColor("#F1F5F9"));
            }
        }
        // Update Expense chip
        if (chipExpense != null) {
            if ("EXPENSE".equals(currentFilter)) {
                chipExpense.setCardBackgroundColor(Color.parseColor("#FEE2E2"));
                chipExpense.setStrokeColor(Color.parseColor("#EF4444"));
            } else {
                chipExpense.setCardBackgroundColor(Color.parseColor("#FEF2F2"));
                chipExpense.setStrokeColor(Color.parseColor("#FECACA"));
            }
        }
        // Update Income chip
        if (chipIncome != null) {
            if ("INCOME".equals(currentFilter)) {
                chipIncome.setCardBackgroundColor(Color.parseColor("#DCFCE7"));
                chipIncome.setStrokeColor(Color.parseColor("#22C55E"));
            } else {
                chipIncome.setCardBackgroundColor(Color.parseColor("#F0FDF4"));
                chipIncome.setStrokeColor(Color.parseColor("#BBF7D0"));
            }
        }
    }

    private void setupFab() {
        fabAdd.setOnClickListener(v -> showAddCategoryDialog(null));
    }

    private void observeCategories() {
        adminService.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            allCategories = categories;
            filterCategories();
        });
    }

    private void filterCategories() {
        String query = etSearch.getText() != null ? etSearch.getText().toString().toLowerCase() : "";

        List<CategoryModel> filtered = allCategories.stream()
                .filter(c -> query.isEmpty() || c.getName().toLowerCase().contains(query))
                .filter(c -> {
                    if ("EXPENSE".equals(currentFilter))
                        return "EXPENSE".equals(c.getType());
                    if ("INCOME".equals(currentFilter))
                        return "INCOME".equals(c.getType());
                    return true;
                })
                .collect(Collectors.toList());

        tvCategoryCount.setText("All Categories (" + filtered.size() + ")");
        adapter.setCategories(filtered);
    }

    private void showAddCategoryDialog(CategoryModel existingCategory) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextInputEditText etName = dialogView.findViewById(R.id.etName);
        MaterialButtonToggleGroup toggleType = dialogView.findViewById(R.id.toggleType);
        RecyclerView rvIcons = dialogView.findViewById(R.id.rvIcons);
        RecyclerView rvColors = dialogView.findViewById(R.id.rvColors);
        View previewBg = dialogView.findViewById(R.id.previewBg);
        ImageView previewIcon = dialogView.findViewById(R.id.previewIcon);
        TextView previewName = dialogView.findViewById(R.id.previewName);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);

        // State
        final String[] selectedIcon = { ICON_NAMES[0] };
        final String[] selectedColor = { COLOR_OPTIONS[0] };

        if (existingCategory != null) {
            tvTitle.setText("Edit Category");
            etName.setText(existingCategory.getName());
            selectedIcon[0] = existingCategory.getIconName();
            selectedColor[0] = existingCategory.getColorHex();
            if ("INCOME".equals(existingCategory.getType())) {
                toggleType.check(R.id.btnIncome);
            }
        } else {
            tvTitle.setText("Add Category");
        }

        // Setup icon grid
        IconAdapter iconAdapter = new IconAdapter(selectedIcon[0], icon -> {
            selectedIcon[0] = icon;
            updatePreview(previewBg, previewIcon, previewName, icon, selectedColor[0],
                    etName.getText() != null ? etName.getText().toString() : "Category");
        });
        rvIcons.setLayoutManager(new GridLayoutManager(requireContext(), 5));
        rvIcons.setAdapter(iconAdapter);

        // Setup color grid
        ColorAdapter colorAdapter = new ColorAdapter(selectedColor[0], color -> {
            selectedColor[0] = color;
            updatePreview(previewBg, previewIcon, previewName, selectedIcon[0], color,
                    etName.getText() != null ? etName.getText().toString() : "Category");
        });
        rvColors.setLayoutManager(new GridLayoutManager(requireContext(), 6));
        rvColors.setAdapter(colorAdapter);

        // Custom color picker button
        MaterialButton btnCustomColor = dialogView.findViewById(R.id.btnCustomColor);
        View customColorPreview = dialogView.findViewById(R.id.customColorPreview);

        if (btnCustomColor != null) {
            btnCustomColor.setOnClickListener(v -> {
                showColorPickerDialog(selectedColor, colorAdapter, previewBg, previewIcon,
                        previewName, selectedIcon[0], etName, customColorPreview);
            });
        }

        // Update preview on name change
        etName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePreview(previewBg, previewIcon, previewName, selectedIcon[0],
                        selectedColor[0], s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Initial preview
        updatePreview(previewBg, previewIcon, previewName, selectedIcon[0], selectedColor[0],
                existingCategory != null ? existingCategory.getName() : "Category");

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                etName.setError("Required");
                return;
            }

            String type = toggleType.getCheckedButtonId() == R.id.btnIncome ? "INCOME" : "EXPENSE";

            if (existingCategory != null) {
                existingCategory.setName(name);
                existingCategory.setIconName(selectedIcon[0]);
                existingCategory.setColorHex(selectedColor[0]);
                existingCategory.setType(type);

                adminService.updateCategory(existingCategory, new AdminService.OnCompleteListener() {
                    @Override
                    public void onSuccess() {
                        dialog.dismiss();
                        Snackbar.make(requireView(), "Category updated", Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Snackbar.make(requireView(), "Error: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                    }
                });
            } else {
                CategoryModel newCategory = new CategoryModel(name, selectedIcon[0], selectedColor[0], type);

                adminService.addCategory(newCategory, new AdminService.OnCompleteListener() {
                    @Override
                    public void onSuccess() {
                        dialog.dismiss();
                        Snackbar.make(requireView(), "Category added", Snackbar.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Snackbar.make(requireView(), "Error: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
    }

    private void updatePreview(View bg, ImageView icon, TextView name, String iconName, String colorHex,
            String categoryName) {
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.OVAL);
        shape.setColor(Color.parseColor(colorHex));
        bg.setBackground(shape);

        int iconRes = getIconResource(iconName);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.WHITE);

        name.setText(categoryName.isEmpty() ? "Category" : categoryName);
    }

    private int getIconResource(String iconName) {
        switch (iconName) {
            case "ic_food":
                return R.drawable.ic_food;
            case "ic_transport":
                return R.drawable.ic_transport;
            case "ic_shopping":
                return R.drawable.ic_shopping;
            case "ic_entertainment":
                return R.drawable.ic_entertainment;
            case "ic_health":
                return R.drawable.ic_health;
            case "ic_bills":
                return R.drawable.ic_bills;
            case "ic_education":
                return R.drawable.ic_education;
            case "ic_travel":
                return R.drawable.ic_travel;
            case "ic_groceries":
                return R.drawable.ic_groceries;
            case "ic_subscription":
                return R.drawable.ic_subscription;
            case "ic_salary":
                return R.drawable.ic_salary;
            case "ic_freelance":
                return R.drawable.ic_freelance;
            case "ic_investment":
                return R.drawable.ic_investment;
            case "ic_gift":
                return R.drawable.ic_gift;
            case "ic_school":
                return R.drawable.ic_school;
            case "ic_location":
                return R.drawable.ic_location;
            case "ic_document":
                return R.drawable.ic_document;
            case "ic_book":
                return R.drawable.ic_book;
            case "ic_grid":
                return R.drawable.ic_grid;
            default:
                return R.drawable.ic_other;
        }
    }

    private void showColorPickerDialog(String[] selectedColor, ColorAdapter colorAdapter,
            View previewBg, ImageView previewIcon, TextView previewName,
            String selectedIcon, TextInputEditText etName, View customColorPreview) {

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_color_picker, null);

        View colorPreviewCircle = dialogView.findViewById(R.id.colorPreview);
        TextView tvColorHex = dialogView.findViewById(R.id.tvColorHex);
        TextView tvRgbValues = dialogView.findViewById(R.id.tvRgbValues);
        android.widget.SeekBar seekRed = dialogView.findViewById(R.id.seekRed);
        android.widget.SeekBar seekGreen = dialogView.findViewById(R.id.seekGreen);
        android.widget.SeekBar seekBlue = dialogView.findViewById(R.id.seekBlue);
        TextView tvRedValue = dialogView.findViewById(R.id.tvRedValue);
        TextView tvGreenValue = dialogView.findViewById(R.id.tvGreenValue);
        TextView tvBlueValue = dialogView.findViewById(R.id.tvBlueValue);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelColor);
        MaterialButton btnApply = dialogView.findViewById(R.id.btnApplyColor);

        // Quick pick colors
        View quickColor1 = dialogView.findViewById(R.id.quickColor1);
        View quickColor2 = dialogView.findViewById(R.id.quickColor2);
        View quickColor3 = dialogView.findViewById(R.id.quickColor3);
        View quickColor4 = dialogView.findViewById(R.id.quickColor4);
        View quickColor5 = dialogView.findViewById(R.id.quickColor5);

        // Initial color values
        final int[] rgb = { 124, 58, 237 }; // Purple default

        // Update preview function
        Runnable updateColorPreview = () -> {
            int color = Color.rgb(rgb[0], rgb[1], rgb[2]);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(color);
            colorPreviewCircle.setBackground(bg);

            String hex = String.format("#%02X%02X%02X", rgb[0], rgb[1], rgb[2]);
            tvColorHex.setText(hex);
            tvRgbValues.setText(String.format("RGB(%d, %d, %d)", rgb[0], rgb[1], rgb[2]));

            tvRedValue.setText(String.valueOf(rgb[0]));
            tvGreenValue.setText(String.valueOf(rgb[1]));
            tvBlueValue.setText(String.valueOf(rgb[2]));
        };

        // Initial preview
        seekRed.setProgress(rgb[0]);
        seekGreen.setProgress(rgb[1]);
        seekBlue.setProgress(rgb[2]);
        updateColorPreview.run();

        // SeekBar listeners
        android.widget.SeekBar.OnSeekBarChangeListener seekListener = new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (seekBar == seekRed)
                    rgb[0] = progress;
                else if (seekBar == seekGreen)
                    rgb[1] = progress;
                else if (seekBar == seekBlue)
                    rgb[2] = progress;
                updateColorPreview.run();
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
            }
        };

        seekRed.setOnSeekBarChangeListener(seekListener);
        seekGreen.setOnSeekBarChangeListener(seekListener);
        seekBlue.setOnSeekBarChangeListener(seekListener);

        // Quick pick handlers
        View.OnClickListener quickPickListener = v -> {
            if (v == quickColor1) {
                rgb[0] = 239;
                rgb[1] = 68;
                rgb[2] = 68;
            } // Red
            else if (v == quickColor2) {
                rgb[0] = 59;
                rgb[1] = 130;
                rgb[2] = 246;
            } // Blue
            else if (v == quickColor3) {
                rgb[0] = 16;
                rgb[1] = 185;
                rgb[2] = 129;
            } // Green
            else if (v == quickColor4) {
                rgb[0] = 249;
                rgb[1] = 115;
                rgb[2] = 22;
            } // Orange
            else if (v == quickColor5) {
                rgb[0] = 139;
                rgb[1] = 92;
                rgb[2] = 246;
            } // Purple

            seekRed.setProgress(rgb[0]);
            seekGreen.setProgress(rgb[1]);
            seekBlue.setProgress(rgb[2]);
            updateColorPreview.run();
        };

        quickColor1.setOnClickListener(quickPickListener);
        quickColor2.setOnClickListener(quickPickListener);
        quickColor3.setOnClickListener(quickPickListener);
        quickColor4.setOnClickListener(quickPickListener);
        quickColor5.setOnClickListener(quickPickListener);

        androidx.appcompat.app.AlertDialog colorDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> colorDialog.dismiss());

        btnApply.setOnClickListener(v -> {
            String hex = String.format("#%02X%02X%02X", rgb[0], rgb[1], rgb[2]);

            selectedColor[0] = hex;
            colorAdapter.clearSelection();
            colorAdapter.notifyDataSetChanged();

            // Update main preview
            updatePreview(previewBg, previewIcon, previewName, selectedIcon, hex,
                    etName.getText() != null ? etName.getText().toString() : "Category");

            // Show custom color preview
            if (customColorPreview != null) {
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(Color.rgb(rgb[0], rgb[1], rgb[2]));
                customColorPreview.setBackground(bg);
                customColorPreview.setVisibility(View.VISIBLE);
            }

            colorDialog.dismiss();
        });

        colorDialog.show();
    }

    private void confirmDeleteCategory(CategoryModel category) {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_delete_category, null);

        TextView tvCategoryName = dialogView.findViewById(R.id.tvCategoryName);
        TextView tvCategoryType = dialogView.findViewById(R.id.tvCategoryType);
        View categoryIconBg = dialogView.findViewById(R.id.categoryIconBg);
        ImageView ivCategoryIcon = dialogView.findViewById(R.id.ivCategoryIcon);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnDelete = dialogView.findViewById(R.id.btnDelete);

        // Set category info
        tvCategoryName.setText(category.getName());
        tvCategoryType.setText(category.getType());
        if ("INCOME".equals(category.getType())) {
            tvCategoryType.setTextColor(ContextCompat.getColor(requireContext(), R.color.income_green));
        } else {
            tvCategoryType.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_red));
        }

        // Set icon
        ivCategoryIcon.setImageResource(getIconResource(category.getIconName()));
        ivCategoryIcon.setColorFilter(Color.WHITE);

        // Set background color
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        try {
            bg.setColor(Color.parseColor(category.getColorHex()));
        } catch (Exception e) {
            bg.setColor(ContextCompat.getColor(requireContext(), R.color.primary));
        }
        categoryIconBg.setBackground(bg);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            adminService.deleteCategory(category.getId(), new AdminService.OnCompleteListener() {
                @Override
                public void onSuccess() {
                    Snackbar.make(requireView(), "Category deleted", Snackbar.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Exception e) {
                    Snackbar.make(requireView(), "Error: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
                }
            });
            dialog.dismiss();
        });

        dialog.show();
    }

    // Category Adapter
    class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        private List<CategoryModel> categories = new ArrayList<>();

        public void setCategories(List<CategoryModel> categories) {
            this.categories = categories;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_category, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(categories.get(position));
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View iconBg, iconRing, colorDot;
            ImageView ivIcon;
            View btnEdit, btnDelete;
            TextView tvName, chipType, chipDefault;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                iconBg = itemView.findViewById(R.id.iconBg);
                iconRing = itemView.findViewById(R.id.iconRing);
                ivIcon = itemView.findViewById(R.id.ivIcon);
                tvName = itemView.findViewById(R.id.tvName);
                chipType = itemView.findViewById(R.id.chipType);
                chipDefault = itemView.findViewById(R.id.chipDefault);
                colorDot = itemView.findViewById(R.id.colorDot);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }

            public void bind(CategoryModel category) {
                tvName.setText(category.getName());

                // Set icon and color
                int iconRes = getIconResource(category.getIconName());
                ivIcon.setImageResource(iconRes);
                ivIcon.setColorFilter(Color.WHITE);

                int categoryColor;
                try {
                    categoryColor = Color.parseColor(category.getColorHex());
                } catch (Exception e) {
                    categoryColor = ContextCompat.getColor(itemView.getContext(), R.color.primary);
                }

                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(categoryColor);
                iconBg.setBackground(bg);

                // Icon ring with lighter version of color
                if (iconRing != null) {
                    GradientDrawable ring = new GradientDrawable();
                    ring.setShape(GradientDrawable.OVAL);
                    ring.setStroke(4, Color.argb(40, Color.red(categoryColor),
                            Color.green(categoryColor), Color.blue(categoryColor)));
                    iconRing.setBackground(ring);
                }

                // Color dot preview
                if (colorDot != null) {
                    GradientDrawable dot = new GradientDrawable();
                    dot.setShape(GradientDrawable.OVAL);
                    dot.setColor(categoryColor);
                    colorDot.setBackground(dot);
                }

                // Set type badge
                chipType.setText(category.getType());
                GradientDrawable typeBg = new GradientDrawable();
                typeBg.setCornerRadius(24);
                if ("INCOME".equals(category.getType())) {
                    typeBg.setColor(ContextCompat.getColor(itemView.getContext(), R.color.income_green));
                } else {
                    typeBg.setColor(ContextCompat.getColor(itemView.getContext(), R.color.expense_red));
                }
                chipType.setBackground(typeBg);

                chipDefault.setVisibility(category.isDefault() ? View.VISIBLE : View.GONE);

                btnEdit.setOnClickListener(v -> showAddCategoryDialog(category));
                btnDelete.setOnClickListener(v -> confirmDeleteCategory(category));
            }
        }
    }

    // Icon Adapter
    class IconAdapter extends RecyclerView.Adapter<IconAdapter.ViewHolder> {
        private String selectedIcon;
        private OnIconSelectedListener listener;

        IconAdapter(String selectedIcon, OnIconSelectedListener listener) {
            this.selectedIcon = selectedIcon;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = new View(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(
                    (int) (48 * parent.getContext().getResources().getDisplayMetrics().density),
                    (int) (48 * parent.getContext().getResources().getDisplayMetrics().density)));

            // Use ImageView instead
            ImageView iv = new ImageView(parent.getContext());
            int size = (int) (44 * parent.getContext().getResources().getDisplayMetrics().density);
            iv.setLayoutParams(new ViewGroup.LayoutParams(size, size));
            iv.setPadding(8, 8, 8, 8);
            return new ViewHolder(iv);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(ICON_NAMES[position]);
        }

        @Override
        public int getItemCount() {
            return ICON_NAMES.length;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            ViewHolder(View itemView) {
                super(itemView);
                imageView = (ImageView) itemView;
            }

            void bind(String iconName) {
                imageView.setImageResource(getIconResource(iconName));

                boolean isSelected = iconName.equals(selectedIcon);
                if (isSelected) {
                    imageView.setBackgroundResource(R.drawable.circle_background);
                    imageView.setColorFilter(Color.WHITE);
                } else {
                    imageView.setBackground(null);
                    imageView
                            .setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.on_background_light));
                }

                imageView.setOnClickListener(v -> {
                    selectedIcon = iconName;
                    notifyDataSetChanged();
                    listener.onSelected(iconName);
                });
            }
        }
    }

    // Color Adapter
    class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ViewHolder> {
        private String selectedColor;
        private OnColorSelectedListener listener;

        ColorAdapter(String selectedColor, OnColorSelectedListener listener) {
            this.selectedColor = selectedColor;
            this.listener = listener;
        }

        public void clearSelection() {
            this.selectedColor = null;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Create container with margins for spacing
            android.widget.FrameLayout container = new android.widget.FrameLayout(parent.getContext());
            int size = (int) (40 * parent.getContext().getResources().getDisplayMetrics().density);
            int margin = (int) (6 * parent.getContext().getResources().getDisplayMetrics().density);

            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(size, size);
            params.setMargins(margin, margin, margin, margin);
            container.setLayoutParams(params);

            return new ViewHolder(container);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(COLOR_OPTIONS[position]);
        }

        @Override
        public int getItemCount() {
            return COLOR_OPTIONS.length;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ViewHolder(View itemView) {
                super(itemView);
            }

            void bind(String colorHex) {
                GradientDrawable bg = new GradientDrawable();
                bg.setShape(GradientDrawable.OVAL);
                bg.setColor(Color.parseColor(colorHex));

                if (colorHex.equals(selectedColor)) {
                    bg.setStroke(6, Color.WHITE);
                }

                itemView.setBackground(bg);

                itemView.setOnClickListener(v -> {
                    selectedColor = colorHex;
                    notifyDataSetChanged();
                    listener.onSelected(colorHex);
                });
            }
        }
    }
}
