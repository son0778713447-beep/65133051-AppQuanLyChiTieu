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
import android.widget.ImageButton;
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
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    // Các thành phần View của Báo cáo thống kê
    private PieChart pieChartDetail;
    private TextView tvTotalIncome, tvTotalExpense, tvNetBalance;
    private ImageButton btnPrevPeriod, btnNextPeriod;
    private TextView tvPeriodDisplay;
    private Button btnMonthlyTab, btnYearlyTab;
    private Button btnChartExpense, btnChartIncome;
    private RecyclerView rvCategoryStats;
    private CategoryStatAdapter categoryStatAdapter;
    private int selectedMonth, selectedYear;
    private boolean isYearlyMode = false;
    private boolean isShowingExpense = true;

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
                    double budget = user.getMonthlyBudget();
                    tvBudgetDisplay.setText("Hạn mức tháng: " + String.format("%,.0f", budget) + " VND (Bấm để sửa)");

                    transactionViewModel.getTransactionList().observe(this, transactions -> {
                        if (transactions != null) {
                            // Tính tổng số dư từ danh sách giao dịch: INCOME - EXPENSE
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

                            if (budget > 0) {
                                int percent = (int) ((totalExpense / budget) * 100);
                                pbBudget.setProgress(Math.min(percent, 100));

                                if (percent >= 100) {
                                    double overSpent = totalExpense - budget;
                                    tvBudgetStatus.setText("⚠️ Bạn đã tiêu quá hạn mức " + String.format("%,.0f", overSpent) + " VND rồi!");
                                    pbBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.RED));
                                    cardWallet.setCardBackgroundColor(Color.parseColor("#D32F2F"));
                                } else {
                                    double remainingBudget = budget - totalExpense;
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
        // listenToTransactions() đã tự động gọi refresh từ server bên trong

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

        // Ánh xạ các View của Báo cáo thống kê
        pieChartDetail = findViewById(R.id.pieChartDetail);
        tvTotalIncome = findViewById(R.id.tvTotalIncome);
        tvTotalExpense = findViewById(R.id.tvTotalExpense);
        tvNetBalance = findViewById(R.id.tvNetBalance);
        btnPrevPeriod = findViewById(R.id.btnPrevPeriod);
        btnNextPeriod = findViewById(R.id.btnNextPeriod);
        tvPeriodDisplay = findViewById(R.id.tvPeriodDisplay);
        btnMonthlyTab = findViewById(R.id.btnMonthlyTab);
        btnYearlyTab = findViewById(R.id.btnYearlyTab);
        btnChartExpense = findViewById(R.id.btnChartExpense);
        btnChartIncome = findViewById(R.id.btnChartIncome);
        rvCategoryStats = findViewById(R.id.rvCategoryStats);

        // Khởi tạo danh sách thống kê chi tiết
        rvCategoryStats.setLayoutManager(new LinearLayoutManager(this));
        categoryStatAdapter = new CategoryStatAdapter(new ArrayList<>());
        rvCategoryStats.setAdapter(categoryStatAdapter);

        // Khởi tạo tháng/năm hiện tại
        Calendar now = Calendar.getInstance();
        selectedMonth = now.get(Calendar.MONTH);
        selectedYear = now.get(Calendar.YEAR);
        isYearlyMode = false;
        updatePeriodDisplay();
        updateToggleButtons();

        // Cấu hình PieChart
        configurePieChart(pieChartDetail, "Chi tiêu");

        // Cập nhật trạng thái nút Chi tiêu / Thu nhập
        updateChartToggleButtons();

        // Sự kiện nút Chi tiêu
        btnChartExpense.setOnClickListener(v -> {
            isShowingExpense = true;
            updateChartToggleButtons();
            updateReportChart();
        });

        // Sự kiện nút Thu nhập
        btnChartIncome.setOnClickListener(v -> {
            isShowingExpense = false;
            updateChartToggleButtons();
            updateReportChart();
        });

        // Sự kiện nút Tab Hàng tháng
        btnMonthlyTab.setOnClickListener(v -> {
            isYearlyMode = false;
            updateToggleButtons();
            updatePeriodDisplay();
            updateReportChart();
        });

        // Sự kiện nút Tab Hàng năm
        btnYearlyTab.setOnClickListener(v -> {
            isYearlyMode = true;
            updateToggleButtons();
            updatePeriodDisplay();
            updateReportChart();
        });

        // Sự kiện nút Kỳ trước
        btnPrevPeriod.setOnClickListener(v -> {
            if (isYearlyMode) {
                selectedYear--;
            } else {
                selectedMonth--;
                if (selectedMonth < 0) {
                    selectedMonth = 11;
                    selectedYear--;
                }
            }
            updatePeriodDisplay();
            updateReportChart();
        });

        // Sự kiện nút Kỳ sau
        btnNextPeriod.setOnClickListener(v -> {
            if (isYearlyMode) {
                selectedYear++;
            } else {
                selectedMonth++;
                if (selectedMonth > 11) {
                    selectedMonth = 0;
                    selectedYear++;
                }
            }
            updatePeriodDisplay();
            updateReportChart();
        });

        // Lắng nghe thay đổi dữ liệu để cập nhật biểu đồ
        transactionViewModel.getTransactionList().observe(this, transactions -> {
            if (transactions != null) {
                updateReportChart();
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
                updateReportChart();
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

    private void configurePieChart(PieChart chart, String type) {
        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);
        chart.setHoleRadius(50f);
        chart.setTransparentCircleRadius(55f);
        chart.setDrawEntryLabels(true);
        chart.setEntryLabelTextSize(11f);
        chart.setCenterTextSize(14f);
        chart.setCenterText(type);
        chart.setDrawCenterText(true);
        chart.animateY(500);
    }

    private void updateChartToggleButtons() {
        if (isShowingExpense) {
            btnChartExpense.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2E7D32")));
            btnChartIncome.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
        } else {
            btnChartExpense.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
            btnChartIncome.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2E7D32")));
        }
    }

    private void updateToggleButtons() {
        if (isYearlyMode) {
            btnMonthlyTab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
            btnYearlyTab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1A237E")));
        } else {
            btnMonthlyTab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#1A237E")));
            btnYearlyTab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
        }
    }

    private void updatePeriodDisplay() {
        if (isYearlyMode) {
            tvPeriodDisplay.setText("Năm " + selectedYear);
        } else {
            String[] monthNames = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"};
            tvPeriodDisplay.setText("Tháng " + monthNames[selectedMonth] + "/" + selectedYear);
        }
    }

    private void updateReportChart() {
        List<TransactionModel> allTransactions = transactionViewModel.getTransactionList().getValue();
        if (allTransactions == null) return;

        // Lọc giao dịch theo tháng hoặc năm được chọn
        double totalIncome = 0, totalExpense = 0;
        Map<String, Double> expenseByCategory = new HashMap<>();
        Map<String, Double> incomeByCategory = new HashMap<>();

        for (TransactionModel trans : allTransactions) {
            // Kiểm tra xem giao dịch có thuộc kỳ được chọn không
            if (trans.getTimestamp() != null) {
                Calendar transCal = Calendar.getInstance();
                transCal.setTime(trans.getTimestamp().toDate());
                int transMonth = transCal.get(Calendar.MONTH);
                int transYear = transCal.get(Calendar.YEAR);

                if (isYearlyMode) {
                    // Chế độ năm: chỉ lọc theo năm
                    if (transYear != selectedYear) {
                        continue;
                    }
                } else {
                    // Chế độ tháng: lọc theo cả tháng và năm
                    if (transMonth != selectedMonth || transYear != selectedYear) {
                        continue;
                    }
                }
            }

            double amount = trans.getAmount();
            String category = trans.getCategory() != null ? trans.getCategory() : "Khác";

            if ("INCOME".equals(trans.getType())) {
                totalIncome += amount;
                incomeByCategory.put(category, incomeByCategory.getOrDefault(category, 0.0) + amount);
            } else {
                totalExpense += amount;
                expenseByCategory.put(category, expenseByCategory.getOrDefault(category, 0.0) + amount);
            }
        }

        // Cập nhật thẻ tổng quan
        tvTotalIncome.setText(String.format("%,.0f", totalIncome) + "đ");
        tvTotalExpense.setText(String.format("%,.0f", totalExpense) + "đ");
        double netBalance = totalIncome - totalExpense;
        tvNetBalance.setText(String.format("%,.0f", netBalance) + "đ");

        // Màu sắc cho biểu đồ Chi tiêu (tông đỏ/cam)
        int[] expenseColors = {
                Color.parseColor("#E53935"), Color.parseColor("#FF7043"),
                Color.parseColor("#FFA726"), Color.parseColor("#EF5350"),
                Color.parseColor("#D84315"), Color.parseColor("#BF360C"),
                Color.parseColor("#F4511E"), Color.parseColor("#FFCC80")
        };

        // Màu sắc cho biểu đồ Thu nhập (tông xanh)
        int[] incomeColors = {
                Color.parseColor("#2E7D32"), Color.parseColor("#43A047"),
                Color.parseColor("#66BB6A"), Color.parseColor("#A5D6A7"),
                Color.parseColor("#1B5E20"), Color.parseColor("#388E3C"),
                Color.parseColor("#4CAF50"), Color.parseColor("#81C784")
        };

        // === Cập nhật center text và vẽ biểu đồ ===
        if (isShowingExpense) {
            pieChartDetail.setCenterText("Chi tiêu");
            drawPieChart(pieChartDetail, expenseByCategory, expenseColors, "Chưa có chi tiêu");
        } else {
            pieChartDetail.setCenterText("Thu nhập");
            drawPieChart(pieChartDetail, incomeByCategory, incomeColors, "Chưa có thu nhập");
        }

        // === Cập nhật danh sách thống kê chi tiết ===
        List<CategoryStatAdapter.CategoryItem> statItems = new ArrayList<>();

        if (isShowingExpense) {
            // Chỉ hiện danh sách CHI TIÊU (isExpense = true)
            addCategoryItems(statItems, expenseByCategory, totalExpense, expenseColors, true);
        } else {
            // Chỉ hiện danh sách THU NHẬP (isExpense = false)
            addCategoryItems(statItems, incomeByCategory, totalIncome, incomeColors, false);
        }

        // Nếu không có giao dịch nào, thêm thông báo
        if (statItems.isEmpty()) {
            String noDataMsg = isYearlyMode ? "Không có giao dịch trong năm " + selectedYear : "Không có giao dịch trong tháng này";
            statItems.add(new CategoryStatAdapter.CategoryItem(noDataMsg, 0, 0, Color.GRAY));
        }

        categoryStatAdapter.setItems(statItems);
    }

    private void drawPieChart(PieChart chart, Map<String, Double> categoryData, int[] colors, String noDataText) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryData.entrySet()) {
            if (entry.getValue() > 0) {
                entries.add(new PieEntry((float) entry.getValue().doubleValue(), entry.getKey()));
            }
        }

        if (entries.isEmpty()) {
            chart.setNoDataText(noDataText);
            chart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(5f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.0f", value) + "%";
            }
        });

        chart.setData(data);
        chart.invalidate();
    }

    private void addCategoryItems(List<CategoryStatAdapter.CategoryItem> items, Map<String, Double> categoryData, double total, int[] colors, boolean isExpense) {
        int colorIndex = 0;
        for (Map.Entry<String, Double> entry : categoryData.entrySet()) {
            if (entry.getValue() > 0) {
                float percent = total > 0 ? (float) (entry.getValue() / total * 100) : 0;
                int color = colors[colorIndex % colors.length];
                items.add(new CategoryStatAdapter.CategoryItem(
                        entry.getKey(), entry.getValue(), percent, color, isExpense
                ));
                colorIndex++;
            }
        }
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
        // KHÔNG gọi listenToTransactions() ở đây nữa — listener đã được đăng ký 1 lần duy nhất trong onCreate
        // Force refresh từ server để đồng bộ dữ liệu khi quay lại app (kể cả khi sửa trên Firestore Console)
        transactionViewModel.forceRefresh();

        if (bottomNavigation != null) {
            int currentTab = bottomNavigation.getSelectedItemId();
            if (currentTab == R.id.nav_calendar) {
                loadTransactionsByDate(calendarView.getDate());
            } else if (currentTab == R.id.nav_report) {
                updateReportChart();
            }
        }
    }
}