package nts.cntt2.quanlychitieu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddFragment extends Fragment {
    private RadioGroup rgType;
    private RadioButton rbIncome;
    private EditText etAmount, etNote;
    private AutoCompleteTextView etCategory;
    private Button btnSave;
    private TransactionViewModel transactionViewModel;
    private Runnable onTransactionSavedListener;

    public void setOnTransactionSavedListener(Runnable listener) {
        this.onTransactionSavedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);

        rgType = view.findViewById(R.id.rgType);
        rbIncome = view.findViewById(R.id.rbIncome);
        etAmount = view.findViewById(R.id.etAmount);
        etCategory = view.findViewById(R.id.etCategory);
        etNote = view.findViewById(R.id.etNote);
        btnSave = view.findViewById(R.id.btnSave);

        String[] incomeCategories = new String[] {"Lương", "Thưởng", "Đầu tư", "Làm thêm", "Được tặng", "Khác"};
        String[] expenseCategories = new String[] {"Ăn uống", "Đi lại", "Mua sắm", "Tiền học", "Giải trí", "Sức khỏe", "Hóa đơn", "Khác"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, expenseCategories);
        etCategory.setAdapter(categoryAdapter);

        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            String[] newCategories = (checkedId == R.id.rbIncome) ? incomeCategories : expenseCategories;
            ArrayAdapter<String> newAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, newCategories);
            etCategory.setAdapter(newAdapter);
            etCategory.setText("");
        });

        transactionViewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);

        btnSave.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String note = etNote.getText().toString().trim();

            if (amountStr.isEmpty() || category.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập đầy đủ Số tiền và Danh mục!", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            String type = rbIncome.isChecked() ? "INCOME" : "EXPENSE";

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

                                transactionViewModel.addTransaction(amount, type, category, note);
                                etAmount.setText("");
                                etCategory.setText("");
                                etNote.setText("");
                                if (onTransactionSavedListener != null) {
                                    onTransactionSavedListener.run();
                                }
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());

            } else {
                transactionViewModel.addTransaction(amount, type, category, note);
                Toast.makeText(getContext(), "Nạp tiền thành công!", Toast.LENGTH_SHORT).show();
                etAmount.setText("");
                etCategory.setText("");
                etNote.setText("");
                if (onTransactionSavedListener != null) {
                    onTransactionSavedListener.run();
                }
            }
        });

        return view;
    }
}
