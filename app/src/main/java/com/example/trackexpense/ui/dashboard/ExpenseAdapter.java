package com.example.trackexpense.ui.dashboard;

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
        private TextView tvCategory, tvDate, tvAmount, tvNotes;
        private ImageView ivIcon;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            ivIcon = itemView.findViewById(R.id.ivIcon);

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

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            tvDate.setText(sdf.format(new Date(expense.getDate())));

            // Show notes if available
            if (tvNotes != null) {
                if (expense.getNotes() != null && !expense.getNotes().isEmpty()) {
                    tvNotes.setText(expense.getNotes());
                    tvNotes.setVisibility(View.VISIBLE);
                } else {
                    tvNotes.setVisibility(View.GONE);
                }
            }

            if ("INCOME".equals(expense.getType())) {
                tvAmount.setText(String.format("+ %s%.2f", currencySymbol, expense.getAmount()));
                tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.income_green));
            } else {
                tvAmount.setText(String.format("- %s%.2f", currencySymbol, expense.getAmount()));
                tvAmount.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.expense_red));
            }
        }
    }
}
