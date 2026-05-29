package nts.cntt2.quanlychitieu;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryStatAdapter extends RecyclerView.Adapter<CategoryStatAdapter.ViewHolder> {

    public static class CategoryItem {
        private String categoryName;
        private double amount;
        private float percentage;
        private int color;
        private boolean isExpense; // true = expense, false = income

        public CategoryItem(String categoryName, double amount, float percentage, int color, boolean isExpense) {
            this.categoryName = categoryName;
            this.amount = amount;
            this.percentage = percentage;
            this.color = color;
            this.isExpense = isExpense;
        }

        // Overloaded constructor for separator lines / no-data messages
        public CategoryItem(String categoryName, double amount, float percentage, int color) {
            this(categoryName, amount, percentage, color, true);
        }

        public String getCategoryName() { return categoryName; }
        public double getAmount() { return amount; }
        public float getPercentage() { return percentage; }
        public int getColor() { return color; }
        public boolean isExpense() { return isExpense; }
    }

    private List<CategoryItem> items;

    public CategoryStatAdapter(List<CategoryItem> items) {
        this.items = items;
    }

    public void setItems(List<CategoryItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_stat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryItem item = items.get(position);

        holder.tvCategoryName.setText(item.getCategoryName());
        holder.tvCategoryAmount.setText(String.format("%,.0f đ", item.getAmount()));
        holder.tvCategoryPercent.setText(String.format("%.0f%%", item.getPercentage()));
        holder.viewColorDot.setBackgroundColor(item.getColor());

        // Hiển thị dấu + / - và màu sắc tương ứng
        if (item.isExpense()) {
            holder.tvSignPrefix.setText("-");
            holder.tvSignPrefix.setTextColor(Color.parseColor("#C62828"));
        } else {
            holder.tvSignPrefix.setText("+");
            holder.tvSignPrefix.setTextColor(Color.parseColor("#2E7D32"));
        }

        // Nếu là separator (amount = 0 và percentage = 0) hoặc no-data message thì ẩn sign + percent
        if (item.getAmount() == 0 && item.getPercentage() == 0) {
            holder.tvSignPrefix.setVisibility(View.GONE);
            holder.tvCategoryPercent.setVisibility(View.GONE);
        } else {
            holder.tvSignPrefix.setVisibility(View.VISIBLE);
            holder.tvCategoryPercent.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View viewColorDot;
        TextView tvCategoryName, tvSignPrefix, tvCategoryAmount, tvCategoryPercent;

        ViewHolder(View itemView) {
            super(itemView);
            viewColorDot = itemView.findViewById(R.id.viewColorDot);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvSignPrefix = itemView.findViewById(R.id.tvSignPrefix);
            tvCategoryAmount = itemView.findViewById(R.id.tvCategoryAmount);
            tvCategoryPercent = itemView.findViewById(R.id.tvCategoryPercent);
        }
    }
}
