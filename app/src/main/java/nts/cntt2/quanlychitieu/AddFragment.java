package nts.cntt2.quanlychitieu;

import android.app.DatePickerDialog;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddFragment extends Fragment {
    private LinearLayout layoutTypeToggle;
    private TextView btnIncomeToggle, btnExpenseToggle;
    private EditText etAmount, etNote, etDate;
    private Button btnSave;
    private GridLayout gridCategories;
    private TransactionViewModel transactionViewModel;
    private Runnable onTransactionSavedListener;

    private boolean isIncome = true;
    private String selectedCategory = "";
    private View selectedCategoryView = null;

    private static class CategoryItem {
        String name, icon, color;
        CategoryItem(String name, String icon, String color) {
            this.name = name; this.icon = icon; this.color = color;
        }
    }

    private final CategoryItem[] incomeCategories = {
        new CategoryItem("Lương", "💼", "#1565C0"),
        new CategoryItem("Thưởng", "🏆", "#FFA000"),
        new CategoryItem("Đầu tư", "📈", "#2E7D32"),
        new CategoryItem("Làm thêm", "🔧", "#6A1B9A"),
        new CategoryItem("Được tặng", "🎁", "#D81B60"),
        new CategoryItem("Khác", "📦", "#795548"),
    };

    private final CategoryItem[] expenseCategories = {
        new CategoryItem("Ăn uống", "🍔", "#FF5722"),
        new CategoryItem("Đi lại", "🚗", "#2196F3"),
        new CategoryItem("Mua sắm", "🛍️", "#E91E63"),
        new CategoryItem("Tiền học", "📚", "#9C27B0"),
        new CategoryItem("Giải trí", "🎮", "#FF9800"),
        new CategoryItem("Sức khỏe", "💊", "#4CAF50"),
        new CategoryItem("Hóa đơn", "📄", "#607D8B"),
        new CategoryItem("Khác", "📦", "#795548"),
    };

    public void setOnTransactionSavedListener(Runnable listener) {
        this.onTransactionSavedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);

        etAmount = view.findViewById(R.id.etAmount);
        etNote = view.findViewById(R.id.etNote);
        etDate = view.findViewById(R.id.etDate);
        btnSave = view.findViewById(R.id.btnSave);
        layoutTypeToggle = view.findViewById(R.id.layoutTypeToggle);
        btnIncomeToggle = view.findViewById(R.id.btnIncomeToggle);
        btnExpenseToggle = view.findViewById(R.id.btnExpenseToggle);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etDate.setText(dateFormat.format(new Date()));
        etDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            String currentDate = etDate.getText().toString().trim();
            if (!currentDate.isEmpty()) {
                try {
                    Date parsed = dateFormat.parse(currentDate);
                    cal.setTime(parsed);
                } catch (Exception ignored) {}
            }
            DatePickerDialog picker = new DatePickerDialog(getContext(),
                    (view1, year, month, dayOfMonth) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, dayOfMonth);
                        etDate.setText(dateFormat.format(selected.getTime()));
                    },
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            picker.show();
        });
        gridCategories = view.findViewById(R.id.gridCategories);

        updateTypeToggleUI();

        btnIncomeToggle.setOnClickListener(v -> {
            if (!isIncome) {
                isIncome = true;
                updateTypeToggleUI();
                buildCategoryGrid(true);
            }
        });

        btnExpenseToggle.setOnClickListener(v -> {
            if (isIncome) {
                isIncome = false;
                updateTypeToggleUI();
                buildCategoryGrid(false);
            }
        });

        buildCategoryGrid(true);

        transactionViewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);

        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String note = etNote.getText().toString().trim();

            if (amountStr.isEmpty() || selectedCategory.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập Số tiền và chọn Danh mục!", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            String type = isIncome ? "INCOME" : "EXPENSE";

            if (type.equals("EXPENSE")) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                db.collection("users").document(uid).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                Double totalBalance = documentSnapshot.getDouble("totalBalance");
                                Double monthlyBudget = documentSnapshot.getDouble("monthlyBudget");

                                if (totalBalance == null) totalBalance = 0.0;
                                if (monthlyBudget == null) monthlyBudget = 0.0;

                                if (amount > totalBalance) {
                                    Toast.makeText(getContext(), "Bạn không đủ số dư trong ví!", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                if (amount > monthlyBudget) {
                                    Toast.makeText(getContext(), "CẢNH BÁO: Khoản chi này vượt quá định mức tháng!", Toast.LENGTH_LONG).show();
                                }

                                transactionViewModel.addTransaction(amount, type, selectedCategory, note, parseDate(etDate.getText().toString().trim()));
                                clearForm();
                                if (onTransactionSavedListener != null) {
                                    onTransactionSavedListener.run();
                                }
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());

            } else {
                transactionViewModel.addTransaction(amount, type, selectedCategory, note, parseDate(etDate.getText().toString().trim()));
                Toast.makeText(getContext(), "Nạp tiền thành công!", Toast.LENGTH_SHORT).show();
                clearForm();
                if (onTransactionSavedListener != null) {
                    onTransactionSavedListener.run();
                }
            }
        });

        return view;
    }

    private void updateTypeToggleUI() {
        GradientDrawable incomeBg = new GradientDrawable();
        incomeBg.setShape(GradientDrawable.RECTANGLE);
        incomeBg.setCornerRadii(new float[]{24f, 24f, 4f, 4f, 4f, 4f, 24f, 24f});
        GradientDrawable expenseBg = new GradientDrawable();
        expenseBg.setShape(GradientDrawable.RECTANGLE);
        expenseBg.setCornerRadii(new float[]{4f, 4f, 24f, 24f, 24f, 24f, 4f, 4f});

        if (isIncome) {
            incomeBg.setColor(android.graphics.Color.parseColor("#1A237E"));
            btnIncomeToggle.setBackground(incomeBg);
            btnIncomeToggle.setTextColor(android.graphics.Color.WHITE);
            expenseBg.setColor(android.graphics.Color.parseColor("#9E9E9E"));
            btnExpenseToggle.setBackground(expenseBg);
            btnExpenseToggle.setTextColor(android.graphics.Color.WHITE);
        } else {
            expenseBg.setColor(android.graphics.Color.parseColor("#1A237E"));
            btnExpenseToggle.setBackground(expenseBg);
            btnExpenseToggle.setTextColor(android.graphics.Color.WHITE);
            incomeBg.setColor(android.graphics.Color.parseColor("#9E9E9E"));
            btnIncomeToggle.setBackground(incomeBg);
            btnIncomeToggle.setTextColor(android.graphics.Color.WHITE);
        }
    }

    private void buildCategoryGrid(boolean isIncome) {
        gridCategories.removeAllViews();
        selectedCategory = "";
        selectedCategoryView = null;

        CategoryItem[] items = isIncome ? incomeCategories : expenseCategories;

        for (CategoryItem item : items) {
            View itemView = getLayoutInflater().inflate(R.layout.item_category_grid, gridCategories, false);

            TextView tvIcon = itemView.findViewById(R.id.tvCategoryIcon);
            TextView tvName = itemView.findViewById(R.id.tvCategoryName);

            tvIcon.setText(item.icon);
            tvName.setText(item.name);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            drawable.setColor(android.graphics.Color.parseColor(item.color));

            tvIcon.setBackground(drawable);

            itemView.setOnClickListener(v -> {
                if (selectedCategoryView != null) {
                    selectedCategoryView.setAlpha(1f);
                }
                v.setAlpha(0.6f);
                selectedCategoryView = v;
                selectedCategory = item.name;
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            itemView.setLayoutParams(params);

            gridCategories.addView(itemView);
        }
    }

    private Timestamp parseDate(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date parsedDate = sdf.parse(dateStr);
            Calendar cal = Calendar.getInstance();
            Calendar dateCal = Calendar.getInstance();
            dateCal.setTime(parsedDate);
            cal.set(Calendar.YEAR, dateCal.get(Calendar.YEAR));
            cal.set(Calendar.MONTH, dateCal.get(Calendar.MONTH));
            cal.set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH));
            return new Timestamp(cal.getTime());
        } catch (Exception e) {
            return Timestamp.now();
        }
    }

    private void clearForm() {
        etAmount.setText("");
        etNote.setText("");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etDate.setText(dateFormat.format(new Date()));
        selectedCategory = "";
        if (selectedCategoryView != null) {
            selectedCategoryView.setAlpha(1f);
            selectedCategoryView = null;
        }
    }
}
