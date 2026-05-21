package nts.cntt2.quanlychitieu;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    private TextView tvBalance, tvBudgetStatus, tvBudgetDisplay;
    private RecyclerView rvTransactions;
    private FloatingActionButton fabAdd;
    private CardView cardWallet;
    private ProgressBar pbBudget; // Khai báo ProgressBar hạn mức

    private TransactionViewModel transactionViewModel;
    private TransactionAdapter adapter;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ánh xạ các View từ giao diện XML đầy đủ
        tvBalance = findViewById(R.id.tvBalance);
        tvBudgetStatus = findViewById(R.id.tvBudgetStatus);
        tvBudgetDisplay = findViewById(R.id.tvBudgetDisplay); // Ánh xạ nhãn hiển thị hạn mức
        rvTransactions = findViewById(R.id.rvTransactions);
        fabAdd = findViewById(R.id.fabAdd);
        cardWallet = findViewById(R.id.cardWallet);
        pbBudget = findViewById(R.id.pbBudget); // Ánh xạ thanh trạng thái hạn mức

        // Cấu hình RecyclerView hiển thị danh sách lịch sử
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo Adapter trống (không tham số) theo đúng cấu trúc mới sửa
        adapter = new TransactionAdapter();
        rvTransactions.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        // LẮNG NGHE REAL-TIME: Đồng bộ số dư, ngân sách và tính toán phần trăm đã tiêu dựa trên lịch sử giao dịch
        db.collection("users").document(uid).addSnapshotListener((snapshot, e) -> {
            if (snapshot != null && snapshot.exists()) {
                UserModel user = snapshot.toObject(UserModel.class);
                if (user != null) {
                    double currentBalance = user.getTotalBalance();
                    double budget = user.getMonthlyBudget();

                    // 1. Hiển thị số dư hiện tại lên màn hình chính
                    tvBalance.setText(String.format("%,.0f", currentBalance) + " VND");

                    // 2. Hiển thị hạn mức đã cài đặt
                    tvBudgetDisplay.setText("Hạn mức tháng: " + String.format("%,.0f", budget) + " VND (Bấm để sửa)");

                    // 3. ĐỒNG BỘ TÍNH TOÁN THEO LỊCH SỬ GIAO DỊCH THỰC TẾ
                    transactionViewModel.getTransactionList().observe(this, transactions -> {
                        if (transactions != null && budget > 0) {
                            double totalSpent = 0;

                            // Duyệt qua toàn bộ danh sách để cộng dồn các khoản CHI TIÊU (EXPENSE)
                            for (TransactionModel trans : transactions) {
                                if ("EXPENSE".equals(trans.getType())) {
                                    totalSpent += trans.getAmount();
                                }
                            }

                            // Tính tỷ lệ % dựa trên tổng tiền thực tế đã tiêu và hạn mức
                            int percent = (int) ((totalSpent / budget) * 100);

                            // Đẩy phần trăm vào thanh ProgressBar (Hạn chế tối đa 100% để thanh không bị tràn)
                            pbBudget.setProgress(Math.min(percent, 100));

                            // Kiểm tra điều kiện để BÁO ĐỎ cảnh báo (Thân thiện hóa thông báo)
                            if (percent >= 100) {
                                // Tính số tiền đã bị chi tiêu lố (vượt) ra so với hạn mức ban đầu
                                double overSpent = totalSpent - budget;

                                // THÔNG BÁO THÂN THIỆN KHI TIÊU QUÁ ĐỊNH MỨC
                                tvBudgetStatus.setText("⚠️ Bạn đã tiêu quá hạn mức " + String.format("%,.0f", overSpent) + " VND rồi, nhớ thắt chặt chi tiêu nhé!");

                                pbBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.RED)); // Thanh tiến trình màu Đỏ
                                cardWallet.setCardBackgroundColor(Color.parseColor("#D32F2F")); // Thẻ ví chuyển sang Đỏ rực
                            } else {
                                // Tính số tiền còn lại được phép tiêu trong hạn mức
                                double remainingBudget = budget - totalSpent;

                                // THÔNG BÁO THÂN THIỆN KHI CÒN TRONG HẠN MỨC AN TOÀN
                                tvBudgetStatus.setText("☘️ Bạn còn " + String.format("%,.0f", remainingBudget) + " VND có thể chi tiêu trong tháng này.");

                                pbBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#DEFF9A"))); // Thanh màu xanh mạ
                                cardWallet.setCardBackgroundColor(Color.parseColor("#2E7D32")); // Thẻ ví màu Xanh an toàn
                            }
                        } else if (budget <= 0) {
                            // Trường hợp hạn mức bằng 0 (chưa cài đặt)
                            pbBudget.setProgress(0);
                            tvBudgetStatus.setText("Chưa thiết lập hạn mức chi tiêu tháng.");
                            cardWallet.setCardBackgroundColor(Color.parseColor("#2E7D32"));
                        }
                    });
                }
            }
        });

        // SỰ KIỆN: HỘP THOẠI CÀI ĐẶT NHANH ĐỊNH MỨC KHI CLICK VÀO CHỮ tvBudgetDisplay
        tvBudgetDisplay.setOnClickListener(v -> {
            EditText etLimit = new EditText(this);
            etLimit.setInputType(InputType.TYPE_CLASS_NUMBER); // Chỉ cho phép nhập số kiểu tiền tệ
            etLimit.setHint("Ví dụ: 3000000");

            new AlertDialog.Builder(this)
                    .setTitle("Cài đặt hạn mức tháng")
                    .setMessage("Nhập số tiền tối đa bạn muốn giới hạn chi tiêu trong tháng này:")
                    .setView(etLimit)
                    .setPositiveButton("Lưu hạn mức", (dialog, which) -> {
                        String value = etLimit.getText().toString().trim();
                        if (!value.isEmpty()) {
                            double newBudget = Double.parseDouble(value);

                            // Đẩy trực tiếp giá trị định mức mới lên Firestore, SnapshotListener phía trên sẽ tự bắt dữ liệu vẽ lại UI
                            db.collection("users").document(uid).update("monthlyBudget", newBudget)
                                    .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Cập nhật hạn mức thành công!", Toast.LENGTH_SHORT).show());
                        }
                    })
                    .setNegativeButton("Hủy bỏ", null)
                    .show();
        });

        // ĐỒNG BỘ LỊCH SỬ: Nhận danh sách từ LiveData và nạp vào Adapter
        transactionViewModel.getTransactionList().observe(this, transactions -> {
            if (transactions != null) {
                adapter.setTransactions(transactions);
            }
        });

        // Kích hoạt tiến trình lắng nghe thay đổi Real-time từ Firestore
        transactionViewModel.listenToTransactions();

        // Sự kiện bấm nút dấu cộng (+) để mở màn hình Thêm giao dịch
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTransactionActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (transactionViewModel != null) {
            transactionViewModel.listenToTransactions();
        }
    }
}