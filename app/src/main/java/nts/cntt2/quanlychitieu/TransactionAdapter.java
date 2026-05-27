package nts.cntt2.quanlychitieu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    private List<TransactionModel> list = new ArrayList<>();

    public interface OnDeleteClickListener {
        void onDeleteClick(TransactionModel transaction);
    }

    private OnDeleteClickListener onDeleteClickListener;

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.onDeleteClickListener = listener;
    }

    public void setTransactions(List<TransactionModel> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionModel trans = list.get(position);

        holder.tvItemCategory.setText(trans.getCategory());
        holder.tvItemNote.setText(trans.getNote());

        if (trans.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.tvItemDate.setText(sdf.format(trans.getTimestamp().toDate()));
        } else {
            holder.tvItemDate.setText("");
        }

        String formattedAmount = String.format("%,.0f", trans.getAmount()) + " VND";

        if ("INCOME".equals(trans.getType())) {
            holder.tvItemAmount.setText("+ " + formattedAmount);
            holder.tvItemAmount.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
            holder.tvItemIcon.setText("💰");
        } else {
            holder.tvItemAmount.setText("- " + formattedAmount);
            holder.tvItemAmount.setTextColor(android.graphics.Color.parseColor("#C62828"));
            holder.tvItemIcon.setText("💸");
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onDeleteClick(trans);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemIcon, tvItemCategory, tvItemNote, tvItemDate, tvItemAmount;
        ImageButton btnDelete;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemIcon = itemView.findViewById(R.id.tvItemIcon);
            tvItemCategory = itemView.findViewById(R.id.tvItemCategory);
            tvItemNote = itemView.findViewById(R.id.tvItemNote);
            tvItemDate = itemView.findViewById(R.id.tvItemDate);
            tvItemAmount = itemView.findViewById(R.id.tvItemAmount);
            btnDelete = itemView.findViewById(R.id.btnDelete); // Đảm bảo trong XML có id btnDelete
        }
    }
}