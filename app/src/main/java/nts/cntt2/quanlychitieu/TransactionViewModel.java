package nts.cntt2.quanlychitieu;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class TransactionViewModel extends ViewModel {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private ListenerRegistration firestoreListener;
    private boolean isListenerRegistered = false;

    private MutableLiveData<List<TransactionModel>> transactionList = new MutableLiveData<>();
    public LiveData<List<TransactionModel>> getTransactionList() { return transactionList; }

    public void listenToTransactions() {
        // Chỉ tạo listener 1 lần duy nhất, tránh trùng lặp
        if (isListenerRegistered) return;
        isListenerRegistered = true;

        firestoreListener = db.collection("transactions")
                .whereEqualTo("uid", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        List<TransactionModel> list = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            TransactionModel model = doc.toObject(TransactionModel.class);
                            if (model != null) {
                                // GÁN ID VÀO ĐÂY ĐỂ TRÁNH LỖI KHI XÓA
                                model.setTransactionId(doc.getId());
                                list.add(model);
                            }
                        }
                        transactionList.setValue(list);
                    }
                });
    }

    @Override
    public void onCleared() {
        super.onCleared();
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }

    public void addTransaction(double amount, String type, String category, String note) {
        DocumentReference transRef = db.collection("transactions").document();
        String docId = transRef.getId(); // Lấy ID trước khi ghi

        TransactionModel transaction = new TransactionModel(
                currentUserId, type, amount, category, Timestamp.now(), note
        );
        transaction.setTransactionId(docId);

        // === OPTIMISTIC UPDATE: Thêm vào LiveData NGAY LẬP TỨC ===
        List<TransactionModel> currentList = transactionList.getValue();
        List<TransactionModel> optimisticList = new ArrayList<>();
        if (currentList != null) {
            optimisticList.addAll(currentList);
        }
        optimisticList.add(0, transaction); // Thêm lên đầu (mới nhất)
        transactionList.setValue(optimisticList);

        // Đẩy lên Firebase (bất đồng bộ)
        transRef.set(transaction)
                .addOnSuccessListener(aVoid -> {
                    DocumentReference userRef = db.collection("users").document(currentUserId);
                    double changeAmount = type.equals("INCOME") ? amount : -amount;
                    userRef.update("totalBalance", FieldValue.increment(changeAmount));

                    // Refresh từ server để đồng bộ dữ liệu chính xác
                    refreshTransactions();
                })
                .addOnFailureListener(e -> {
                    // Nếu ghi Firebase thất bại, rollback: loại bỏ giao dịch vừa thêm
                    List<TransactionModel> rollbackList = transactionList.getValue();
                    if (rollbackList != null) {
                        rollbackList.remove(transaction);
                        transactionList.setValue(rollbackList);
                    }
                });
    }

    private void refreshTransactions() {
        db.collection("transactions")
                .whereEqualTo("uid", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<TransactionModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        TransactionModel model = doc.toObject(TransactionModel.class);
                        if (model != null) {
                            model.setTransactionId(doc.getId());
                            list.add(model);
                        }
                    }
                    transactionList.setValue(list);
                });
    }

    public void deleteTransaction(TransactionModel transaction) {
        DocumentReference transRef = db.collection("transactions").document(transaction.getTransactionId());
        DocumentReference userRef = db.collection("users").document(currentUserId);

        transRef.delete()
                .addOnSuccessListener(aVoid -> {
                    // Nếu xóa INCOME -> trừ tiền (vì trước đó đã cộng), Xóa EXPENSE -> cộng lại tiền
                    double refundAmount = transaction.getType().equals("INCOME") ? -transaction.getAmount() : transaction.getAmount();
                    userRef.update("totalBalance", FieldValue.increment(refundAmount));
                });
    }
}