package com.example.trackexpense.ui.dashboard;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackexpense.R;
import com.example.trackexpense.data.local.Expense;
import com.example.trackexpense.utils.CategoryHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenses = new ArrayList<>();
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;
    private String currencySymbol = "$";

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.bind(expense);
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
        notifyDataSetChanged();
    }

    public void setCurrencySymbol(String symbol) {
        this.currencySymbol = symbol;
    }

    public Expense getExpenseAt(int position) {
        if (position >= 0 && position < expenses.size()) {
            return expenses.get(position);
        }
        return null;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public interface OnItemClickListener {
        void onItemClick(Expense expense);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Expense expense, int position);
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private View iconBg;
        private ImageView ivIcon;
        private TextView tvCategory, tvDate, tvAmount;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            iconBg = itemView.findViewById(R.id.iconBg);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(expenses.get(position));
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (longClickListener != null && position != RecyclerView.NO_POSITION) {
                    longClickListener.onItemLongClick(expenses.get(position), position);
                    return true;
                }
                return false;
            });
        }

        public void bind(Expense expense) {
            tvCategory.setText(expense.getCategory());

            // Format date
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(expense.getDate())));

            // Get category info for icon and colors
            CategoryHelper.CategoryInfo categoryInfo = CategoryHelper.getCategoryInfo(expense.getCategory());

            // Set icon
            ivIcon.setImageResource(categoryInfo.iconRes);
            ivIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), android.R.color.white));

            // Set icon background color
            GradientDrawable bgShape = new GradientDrawable();
            bgShape.setShape(GradientDrawable.OVAL);
            bgShape.setColor(ContextCompat.getColor(itemView.getContext(), categoryInfo.colorRes));
            iconBg.setBackground(bgShape);

            // Set amount with color based on type
            if ("INCOME".equals(expense.getType())) {
                tvAmount.setText(String.format("+%s%.2f", currencySymbol, expense.getAmount()));
                tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.income_green));
            } else {
                tvAmount.setText(String.format("-%s%.2f", currencySymbol, expense.getAmount()));
                tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.expense_red));
            }
        }
    }
}
