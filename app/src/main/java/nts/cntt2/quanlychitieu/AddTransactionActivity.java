package nts.cntt2.quanlychitieu;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddTransactionActivity extends AppCompatActivity {
    private RadioGroup rgType;
    private RadioButton rbIncome;
    private EditText etAmount, etNote;
    private AutoCompleteTextView etCategory; // Đổi sang AutoCompleteTextView
    private Button btnSave;
    private TransactionViewModel transactionViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        rgType = findViewById(R.id.rgType);
        rbIncome = findViewById(R.id.rbIncome);
        etAmount = findViewById(R.id.etAmount);
        etCategory = findViewById(R.id.etCategory); // Ánh xạ đúng ID mới
        etNote = findViewById(R.id.etNote);
        btnSave = findViewById(R.id.btnSave);

        // NẠP DANH MỤC VÀO MENU DROPDOWN
        String[] categories = new String[] {"Ăn uống", "Đi lại", "Mua sắm", "Tiền học", "Giải trí", "Lương", "Khác"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        etCategory.setAdapter(adapter);

        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);

        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String note = etNote.getText().toString().trim();

            if (amountStr.isEmpty() || category.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ Số tiền và Danh mục!", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            String type = rbIncome.isChecked() ? "INCOME" : "EXPENSE";

            if (type.equals("EXPENSE")) {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                FirebaseFirestore.getInstance().collection("users").document(uid).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                Double totalBalance = documentSnapshot.getDouble("totalBalance");
                                Double monthlyBudget = documentSnapshot.getDouble("monthlyBudget");

                                if (totalBalance == null) totalBalance = 0.0;
                                if (monthlyBudget == null) monthlyBudget = 0.0;

                                if (amount > totalBalance) {
                                    Toast.makeText(AddTransactionActivity.this, "Bạn không đủ số dư trong ví!", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                // Cảnh báo khi chi tiêu vượt hạn mức (chỉ mang tính chất nhắc nhở)
                                if (amount > monthlyBudget) {
                                    Toast.makeText(AddTransactionActivity.this, "CẢNH BÁO: Khoản chi này vượt quá định mức tháng!", Toast.LENGTH_LONG).show();
                                }

                                transactionViewModel.addTransaction(amount, type, category, note);
                                finish();
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());

            } else {
                transactionViewModel.addTransaction(amount, type, category, note);
                Toast.makeText(this, "Nạp tiền thành công!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}