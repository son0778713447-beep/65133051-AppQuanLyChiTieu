package nts.cntt2.quanlychitieu;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextView tvBalance, tvBudgetStatus, tvBudgetDisplay;
    private RecyclerView rvTransactions;
    private CardView cardWallet;
    private ProgressBar pbBudget;

    private LinearLayout lnMainContent, layoutCalendarView, layoutReportView, layoutAddView;
    private BottomNavigationView bottomNavigation;

    private TransactionViewModel transactionViewModel;
    private TransactionAdapter adapter;
    private FirebaseFirestore db;
    private String uid;

    // Các thành phần View của tính năng Lịch
    private CalendarView calendarView;
    private RecyclerView rvCalendarTransactions;
    private TransactionAdapter calendarAdapter;
    private TextView tvSelectedDateTitle;

    // Các thành phần View của form Nhập vào
    private RadioGroup rgType;
    private RadioButton rbIncome;
    private EditText etAmount, etNote;
    private AutoCompleteTextView etCategory;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvBalance = findViewById(R.id.tvBalance);
        tvBudgetStatus = findViewById(R.id.tvBudgetStatus);
        tvBudgetDisplay = findViewById(R.id.tvBudgetDisplay);
        rvTransactions = findViewById(R.id.rvTransactions);
        cardWallet = findViewById(R.id.cardWallet);
        pbBudget = findViewById(R.id.pbBudget);

        lnMainContent = findViewById(R.id.lnMainContent);
        layoutCalendarView = findViewById(R.id.layoutCalendarView);
        layoutReportView = findViewById(R.id.layoutReportView);
        layoutAddView = findViewById(R.id.layoutAddView);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Ánh xạ các View của màn hình Lịch từ XML
        calendarView = findViewById(R.id.calendarView);
        rvCalendarTransactions = findViewById(R.id.rvCalendarTransactions);
        tvSelectedDateTitle = findViewById(R.id.tvSelectedDateTitle);

        // Khởi tạo RecyclerView và Adapter riêng cho màn hình Lịch
        rvCalendarTransactions.setLayoutManager(new LinearLayoutManager(this));
        calendarAdapter = new TransactionAdapter();
        rvCalendarTransactions.setAdapter(calendarAdapter);

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter();
        rvTransactions.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        db.collection("users").document(uid).addSnapshotListener((snapshot, e) -> {
            if (snapshot != null && snapshot.exists()) {
                UserModel user = snapshot.toObject(UserModel.class);
                if (user != null) {
                    double currentBalance = user.getTotalBalance();
                    double budget = user.getMonthlyBudget();

                    tvBalance.setText(String.format("%,.0f", currentBalance) + " VND");
                    tvBudgetDisplay.setText("Hạn mức tháng: " + String.format("%,.0f", budget) + " VND (Bấm để sửa)");

                    transactionViewModel.getTransactionList().observe(this, transactions -> {
                        if (transactions != null && budget > 0) {
                            double totalSpent = 0;
                            for (TransactionModel trans : transactions) {
                                if ("EXPENSE".equals(trans.getType())) {
                                    totalSpent += trans.getAmount();
                                }
                            }

                            int percent = (int) ((totalSpent / budget) * 100);
                            pbBudget.setProgress(Math.min(percent, 100));

                            if (percent >= 100) {
                                double overSpent = totalSpent - budget;
                                tvBudgetStatus.setText("⚠️ Bạn đã tiêu quá hạn mức " + String.format("%,.0f", overSpent) + " VND rồi!");
                                pbBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.RED));
                                cardWallet.setCardBackgroundColor(Color.parseColor("#D32F2F"));
                            } else {
                                double remainingBudget = budget - totalSpent;
                                tvBudgetStatus.setText("☘️ Bạn còn " + String.format("%,.0f", remainingBudget) + " VND có thể chi tiêu.");
                                pbBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#DEFF9A")));
                                cardWallet.setCardBackgroundColor(Color.parseColor("#2E7D32"));
                            }
                        } else if (budget <= 0) {
                            pbBudget.setProgress(0);
                            tvBudgetStatus.setText("Chưa thiết lập hạn mức chi tiêu tháng.");
                            cardWallet.setCardBackgroundColor(Color.parseColor("#2E7D32"));
                        }
                    });
                }
            }
        });

        tvBudgetDisplay.setOnClickListener(v -> {
            EditText etLimit = new EditText(this);
            etLimit.setInputType(InputType.TYPE_CLASS_NUMBER);
            etLimit.setHint("Ví dụ: 3000000");

            new AlertDialog.Builder(this)
                    .setTitle("Cài đặt hạn mức tháng")
                    .setMessage("Nhập số tiền tối đa bạn muốn giới hạn:")
                    .setView(etLimit)
                    .setPositiveButton("Lưu hạn mức", (dialog, which) -> {
                        String value = etLimit.getText().toString().trim();
                        if (!value.isEmpty()) {
                            double newBudget = Double.parseDouble(value);
                            db.collection("users").document(uid).update("monthlyBudget", newBudget)
                                    .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show());
                        }
                    })
                    .setNegativeButton("Hủy bỏ", null)
                    .show();
        });

        transactionViewModel.getTransactionList().observe(this, transactions -> {
            if (transactions != null) {
                adapter.setTransactions(transactions);
            }
        });

        transactionViewModel.listenToTransactions();

        // Ánh xạ các View của form Nhập vào
        rgType = findViewById(R.id.rgType);
        rbIncome = findViewById(R.id.rbIncome);
        etAmount = findViewById(R.id.etAmount);
        etCategory = findViewById(R.id.etCategory);
        etNote = findViewById(R.id.etNote);
        btnSave = findViewById(R.id.btnSave);

        // NẠP DANH MỤC VÀO MENU DROPDOWN
        String[] categories = new String[] {"Ăn uống", "Đi lại", "Mua sắm", "Tiền học", "Giải trí", "Lương", "Khác"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        etCategory.setAdapter(categoryAdapter);

        // Xử lý nút Lưu giao dịch
        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String note = etNote.getText().toString().trim();

            if (amountStr.isEmpty() || category.isEmpty()) {
                Toast.makeText(MainActivity.this, "Vui lòng nhập đầy đủ Số tiền và Danh mục!", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            String type = rbIncome.isChecked() ? "INCOME" : "EXPENSE";

            if (type.equals("EXPENSE")) {
                db.collection("users").document(uid).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                Double totalBalance = documentSnapshot.getDouble("totalBalance");
                                Double monthlyBudget = documentSnapshot.getDouble("monthlyBudget");

                                if (totalBalance == null) totalBalance = 0.0;
                                if (monthlyBudget == null) monthlyBudget = 0.0;

                                if (amount > totalBalance) {
                                    Toast.makeText(MainActivity.this, "Bạn không đủ số dư trong ví!", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                if (amount > monthlyBudget) {
                                    Toast.makeText(MainActivity.this, "CẢNH BÁO: Khoản chi này vượt quá định mức tháng!", Toast.LENGTH_LONG).show();
                                }

                                transactionViewModel.addTransaction(amount, type, category, note);
                                etAmount.setText("");
                                etCategory.setText("");
                                etNote.setText("");
                                showMainWalletView();
                                bottomNavigation.setSelectedItemId(R.id.nav_home);
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());

            } else {
                transactionViewModel.addTransaction(amount, type, category, note);
                Toast.makeText(this, "Nạp tiền thành công!", Toast.LENGTH_SHORT).show();
                etAmount.setText("");
                etCategory.setText("");
                etNote.setText("");
                showMainWalletView();
                bottomNavigation.setSelectedItemId(R.id.nav_home);
            }
        });

        // Xử lý sự kiện click chọn ngày trên Lịch để lọc dữ liệu
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            loadTransactionsByDate(calendar.getTimeInMillis());
        });

        if (getIntent().hasExtra("open_calendar")) {
            lnMainContent.setVisibility(View.GONE);
            layoutCalendarView.setVisibility(View.VISIBLE);
            layoutReportView.setVisibility(View.GONE);
            bottomNavigation.setSelectedItemId(R.id.nav_calendar);
            loadTransactionsByDate(calendarView.getDate());
        }
        else if (getIntent().hasExtra("open_report")) {
            lnMainContent.setVisibility(View.GONE);
            layoutCalendarView.setVisibility(View.GONE);
            layoutReportView.setVisibility(View.VISIBLE);
            bottomNavigation.setSelectedItemId(R.id.nav_report);
        }
        else if (getIntent().hasExtra("open_add")) {
            layoutAddView.setVisibility(View.VISIBLE);
            layoutCalendarView.setVisibility(View.GONE);
            layoutReportView.setVisibility(View.GONE);
            lnMainContent.setVisibility(View.GONE);
            bottomNavigation.setSelectedItemId(R.id.nav_add);
        }
        else {
            showMainWalletView();
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                showMainWalletView();
                return true;
            } else if (id == R.id.nav_add) {
                lnMainContent.setVisibility(View.GONE);
                layoutCalendarView.setVisibility(View.GONE);
                layoutReportView.setVisibility(View.GONE);
                layoutAddView.setVisibility(View.VISIBLE);
                return true;
            } else if (id == R.id.nav_calendar) {
                lnMainContent.setVisibility(View.GONE);
                layoutAddView.setVisibility(View.GONE);
                layoutCalendarView.setVisibility(View.VISIBLE);
                layoutReportView.setVisibility(View.GONE);
                loadTransactionsByDate(calendarView.getDate());
                return true;
            } else if (id == R.id.nav_report) {
                lnMainContent.setVisibility(View.GONE);
                layoutAddView.setVisibility(View.GONE);
                layoutCalendarView.setVisibility(View.GONE);
                layoutReportView.setVisibility(View.VISIBLE);
                return true;
            }
            return false;
        });
    }

    // ĐÃ CHỈNH SỬA: Lọc dữ liệu chuẩn theo kiểu dữ liệu Firebase Timestamp của Sơn
    private void loadTransactionsByDate(long dateInMills) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String selectedDateStr = sdf.format(new Date(dateInMills));

        if (tvSelectedDateTitle != null) {
            tvSelectedDateTitle.setText("Giao dịch ngày " + selectedDateStr);
        }

        transactionViewModel.getTransactionList().observe(this, allTransactions -> {
            if (allTransactions != null) {
                List<TransactionModel> filteredList = new ArrayList<>();
                for (TransactionModel trans : allTransactions) {
                    // SỬA CHUẨN: Kiểm tra tránh crash nếu trường timestamp trên Firestore bị null,
                    // chuyển đổi mượt mà từ Firebase Timestamp sang đối tượng Date Java thông qua hàm .toDate()
                    if (trans.getTimestamp() != null) {
                        String transDateStr = sdf.format(trans.getTimestamp().toDate());
                        if (selectedDateStr.equals(transDateStr)) {
                            filteredList.add(trans);
                        }
                    }
                }
                calendarAdapter.setTransactions(filteredList);
            }
        });
    }

    private void showMainWalletView() {
        if (lnMainContent != null) lnMainContent.setVisibility(View.VISIBLE);
        if (layoutCalendarView != null) layoutCalendarView.setVisibility(View.GONE);
        if (layoutReportView != null) layoutReportView.setVisibility(View.GONE);
        if (layoutAddView != null) layoutAddView.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (transactionViewModel != null) {
            transactionViewModel.listenToTransactions();
        }

        if (bottomNavigation != null) {
            int currentTab = bottomNavigation.getSelectedItemId();
            if (currentTab == R.id.nav_calendar) {
                loadTransactionsByDate(calendarView.getDate());
            }
        }
    }
}