package nts.cntt2.quanlychitieu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
    private List<TransactionModel> list = new ArrayList<>();

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

        // Gọi hàm xóa khi bấm nút thùng rác
        holder.btnDelete.setOnClickListener(v -> deleteTransaction(trans));
    }

    private void deleteTransaction(TransactionModel transaction) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("transactions").document(transaction.getTransactionId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // --- BƯỚC QUAN TRỌNG: XÓA TRANSACTION KHỎI LIST HIỆN TẠI VÀ UPDATE GIAO DIỆN ---
                    int position = list.indexOf(transaction);
                    if (position != -1) {
                        list.remove(position);
                        notifyItemRemoved(position); // Lệnh này giúp dòng giao dịch biến mất ngay lập tức với hiệu ứng mượt mà
                    }

                    // Cập nhật lại số dư ví (TotalBalance)
                    com.google.firebase.firestore.DocumentReference userRef = db.collection("users").document(uid);
                    userRef.get().addOnSuccessListener(doc -> {
                        Double currentBalance = doc.getDouble("totalBalance");
                        if (currentBalance == null) currentBalance = 0.0;

                        double newBalance;
                        if ("INCOME".equals(transaction.getType())) {
                            newBalance = currentBalance - transaction.getAmount();
                        } else {
                            newBalance = currentBalance + transaction.getAmount();
                        }
                        userRef.update("totalBalance", newBalance);
                    });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("DELETE_ERROR", "Lỗi xóa giao dịch: " + e.getMessage());
                });
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemIcon, tvItemCategory, tvItemNote, tvItemAmount;
        ImageButton btnDelete;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemIcon = itemView.findViewById(R.id.tvItemIcon);
            tvItemCategory = itemView.findViewById(R.id.tvItemCategory);
            tvItemNote = itemView.findViewById(R.id.tvItemNote);
            tvItemAmount = itemView.findViewById(R.id.tvItemAmount);
            btnDelete = itemView.findViewById(R.id.btnDelete); // Đảm bảo trong XML có id btnDelete
        }
    }
}