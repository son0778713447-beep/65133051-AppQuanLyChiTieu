package nts.cntt2.quanlychitieu;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class WalletFragment extends Fragment {
    private TextView tvBalance, tvBudgetStatus, tvBudgetDisplay;
    private RecyclerView rvTransactions;
    private CardView cardWallet;
    private ProgressBar pbBudget;

    private TransactionViewModel transactionViewModel;
    private TransactionAdapter adapter;
    private FirebaseFirestore db;
    private String uid;
    private double monthlyBudget = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        tvBalance = view.findViewById(R.id.tvBalance);
        tvBudgetStatus = view.findViewById(R.id.tvBudgetStatus);
        tvBudgetDisplay = view.findViewById(R.id.tvBudgetDisplay);
        rvTransactions = view.findViewById(R.id.rvTransactions);
        cardWallet = view.findViewById(R.id.cardWallet);
        pbBudget = view.findViewById(R.id.pbBudget);

        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionAdapter();
        rvTransactions.setAdapter(adapter);

        adapter.setOnDeleteClickListener(transaction -> {
            transactionViewModel.deleteTransaction(transaction);
        });

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        transactionViewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);

        db.collection("users").document(uid).addSnapshotListener((snapshot, e) -> {
            if (snapshot != null && snapshot.exists()) {
                UserModel user = snapshot.toObject(UserModel.class);
                if (user != null) {
                    monthlyBudget = user.getMonthlyBudget();
                    tvBudgetDisplay.setText("Hạn mức tháng: " + String.format("%,.0f", monthlyBudget) + " VND (Bấm để sửa)");
                }
            }
        });

        tvBudgetDisplay.setOnClickListener(v -> {
            EditText etLimit = new EditText(getContext());
            etLimit.setInputType(InputType.TYPE_CLASS_NUMBER);
            etLimit.setHint("Ví dụ: 3000000");

            new AlertDialog.Builder(requireContext())
                    .setTitle("Cài đặt hạn mức tháng")
                    .setMessage("Nhập số tiền tối đa bạn muốn giới hạn:")
                    .setView(etLimit)
                    .setPositiveButton("Lưu hạn mức", (dialog, which) -> {
                        String value = etLimit.getText().toString().trim();
                        if (!value.isEmpty()) {
                            double newBudget = Double.parseDouble(value);
                            db.collection("users").document(uid).update("monthlyBudget", newBudget)
                                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Cập nhật thành công!", Toast.LENGTH_SHORT).show());
                        }
                    })
                    .setNegativeButton("Hủy bỏ", null)
                    .show();
        });

        transactionViewModel.getTransactionList().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                adapter.setTransactions(transactions);

                double totalIncome = 0, totalExpense = 0;
                for (TransactionModel trans : transactions) {
                    if ("INCOME".equals(trans.getType())) {
                        totalIncome += trans.getAmount();
                    } else {
                        totalExpense += trans.getAmount();
                    }
                }
                double calculatedBalance = totalIncome - totalExpense;
                tvBalance.setText(String.format("%,.0f", calculatedBalance) + " VND");

                if (monthlyBudget > 0) {
                    int percent = (int) ((totalExpense / monthlyBudget) * 100);
                    pbBudget.setProgress(Math.min(percent, 100));

                    if (percent >= 100) {
                        double overSpent = totalExpense - monthlyBudget;
                        tvBudgetStatus.setText("⚠️ Bạn đã tiêu quá hạn mức " + String.format("%,.0f", overSpent) + " VND rồi!");
                        pbBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.RED));
                        cardWallet.setCardBackgroundColor(Color.parseColor("#D32F2F"));
                    } else {
                        double remainingBudget = monthlyBudget - totalExpense;
                        tvBudgetStatus.setText("☘️ Bạn còn " + String.format("%,.0f", remainingBudget) + " VND có thể chi tiêu.");
                        pbBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#DEFF9A")));
                        cardWallet.setCardBackgroundColor(Color.parseColor("#2E7D32"));
                    }
                } else {
                    pbBudget.setProgress(0);
                    tvBudgetStatus.setText("Chưa thiết lập hạn mức chi tiêu tháng.");
                    cardWallet.setCardBackgroundColor(Color.parseColor("#2E7D32"));
                }
            }
        });

        return view;
    }
}
