package nts.cntt2.quanlychitieu;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportFragment extends Fragment {
    private PieChart pieChartDetail;
    private TextView tvTotalIncome, tvTotalExpense, tvNetBalance;
    private ImageButton btnPrevPeriod, btnNextPeriod;
    private TextView tvPeriodDisplay;
    private Button btnMonthlyTab, btnYearlyTab;
    private TextView btnChartExpense, btnChartIncome;
    private RecyclerView rvCategoryStats;
    private CategoryStatAdapter categoryStatAdapter;

    private TransactionViewModel transactionViewModel;
    private int selectedMonth, selectedYear;
    private boolean isYearlyMode = false;
    private boolean isShowingExpense = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        pieChartDetail = view.findViewById(R.id.pieChartDetail);
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome);
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense);
        tvNetBalance = view.findViewById(R.id.tvNetBalance);
        btnPrevPeriod = view.findViewById(R.id.btnPrevPeriod);
        btnNextPeriod = view.findViewById(R.id.btnNextPeriod);
        tvPeriodDisplay = view.findViewById(R.id.tvPeriodDisplay);
        btnMonthlyTab = view.findViewById(R.id.btnMonthlyTab);
        btnYearlyTab = view.findViewById(R.id.btnYearlyTab);
        btnChartExpense = view.findViewById(R.id.btnChartExpense);
        btnChartIncome = view.findViewById(R.id.btnChartIncome);
        rvCategoryStats = view.findViewById(R.id.rvCategoryStats);

        rvCategoryStats.setLayoutManager(new LinearLayoutManager(getContext()));
        categoryStatAdapter = new CategoryStatAdapter(new ArrayList<>());
        rvCategoryStats.setAdapter(categoryStatAdapter);

        transactionViewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);

        Calendar now = Calendar.getInstance();
        selectedMonth = now.get(Calendar.MONTH);
        selectedYear = now.get(Calendar.YEAR);
        isYearlyMode = false;
        updatePeriodDisplay();
        updateToggleButtons();

        configurePieChart(pieChartDetail, "Chi tiêu");
        updateChartToggleButtons();

        btnChartExpense.setOnClickListener(v -> {
            isShowingExpense = true;
            updateChartToggleButtons();
            updateReportChart();
        });

        btnChartIncome.setOnClickListener(v -> {
            isShowingExpense = false;
            updateChartToggleButtons();
            updateReportChart();
        });

        btnMonthlyTab.setOnClickListener(v -> {
            isYearlyMode = false;
            updateToggleButtons();
            updatePeriodDisplay();
            updateReportChart();
        });

        btnYearlyTab.setOnClickListener(v -> {
            isYearlyMode = true;
            updateToggleButtons();
            updatePeriodDisplay();
            updateReportChart();
        });

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

        transactionViewModel.getTransactionList().observe(getViewLifecycleOwner(), transactions -> {
            if (transactions != null) {
                updateReportChart();
            }
        });

        return view;
    }

    private void configurePieChart(PieChart chart, String type) {
        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);
        chart.setHoleRadius(35f);
        chart.setTransparentCircleRadius(40f);
        chart.setDrawEntryLabels(true);
        chart.setCenterTextSize(14f);
        chart.setCenterText(type);
        chart.setDrawCenterText(true);
        chart.setEntryLabelColor(Color.BLACK);
        chart.setEntryLabelTextSize(12f);
        chart.animateY(500);
    }

    private void updateChartToggleButtons() {
        if (isShowingExpense) {
            btnChartExpense.setTextColor(Color.parseColor("#C62828"));
            btnChartIncome.setTextColor(Color.parseColor("#9E9E9E"));
        } else {
            btnChartExpense.setTextColor(Color.parseColor("#9E9E9E"));
            btnChartIncome.setTextColor(Color.parseColor("#2E7D32"));
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

        double totalIncome = 0, totalExpense = 0;
        Map<String, Double> expenseByCategory = new HashMap<>();
        Map<String, Double> incomeByCategory = new HashMap<>();

        for (TransactionModel trans : allTransactions) {
            if (trans.getTimestamp() != null) {
                Calendar transCal = Calendar.getInstance();
                transCal.setTime(trans.getTimestamp().toDate());
                int transMonth = transCal.get(Calendar.MONTH);
                int transYear = transCal.get(Calendar.YEAR);

                if (isYearlyMode) {
                    if (transYear != selectedYear) continue;
                } else {
                    if (transMonth != selectedMonth || transYear != selectedYear) continue;
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

        tvTotalIncome.setText("+ " + String.format("%,.0f", totalIncome) + "đ");
        tvTotalExpense.setText("- " + String.format("%,.0f", totalExpense) + "đ");
        double netBalance = totalIncome - totalExpense;
        String balanceSign = netBalance >= 0 ? "+ " : "";
        tvNetBalance.setText(balanceSign + String.format("%,.0f", netBalance) + "đ");

        int[] expenseColors = {
            Color.parseColor("#E53935"), Color.parseColor("#FF7043"),
            Color.parseColor("#FFA726"), Color.parseColor("#EF5350"),
            Color.parseColor("#D84315"), Color.parseColor("#BF360C"),
            Color.parseColor("#F4511E"), Color.parseColor("#FFCC80")
        };

        int[] incomeColors = {
            Color.parseColor("#2E7D32"), Color.parseColor("#1565C0"),
            Color.parseColor("#6A1B9A"), Color.parseColor("#00838F"),
            Color.parseColor("#F57F17"), Color.parseColor("#4E342E"),
            Color.parseColor("#D81B60"), Color.parseColor("#558B2F")
        };

        if (isShowingExpense) {
            pieChartDetail.setCenterText("Chi tiêu");
            drawPieChart(pieChartDetail, expenseByCategory, expenseColors, "Chưa có chi tiêu");
        } else {
            pieChartDetail.setCenterText("Thu nhập");
            drawPieChart(pieChartDetail, incomeByCategory, incomeColors, "Chưa có thu nhập");
        }

        List<CategoryStatAdapter.CategoryItem> statItems = new ArrayList<>();

        if (isShowingExpense) {
            addCategoryItems(statItems, expenseByCategory, totalExpense, expenseColors, true);
        } else {
            addCategoryItems(statItems, incomeByCategory, totalIncome, incomeColors, false);
        }

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
            chart.clear();
            chart.setNoDataText(noDataText);
            chart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(13f);
        dataSet.setValueTextColor(Color.parseColor("#1565C0"));
        dataSet.setValueTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(5f);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueLinePart1OffsetPercentage(60f);
        dataSet.setValueLinePart1Length(0.55f);
        dataSet.setValueLinePart2Length(0.15f);
        dataSet.setValueLineColor(Color.DKGRAY);
        dataSet.setValueLineWidth(2.5f);
        dataSet.setValueLineVariableLength(false);

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
}
