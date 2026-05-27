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
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Source;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

        // BƯỚC 1: Đọc dữ liệu từ SERVER trước (bỏ qua cache)
        // Điều này đảm bảo app luôn hiển thị dữ liệu mới nhất từ server
        // dù cho cache local đang có dữ liệu cũ
        refreshTransactions();

        // BƯỚC 2: Đăng ký snapshot listener cho các cập nhật realtime sau này
        // LƯU Ý: Không dùng .orderBy() để tránh lỗi thiếu composite index!
        firestoreListener = db.collection("transactions")
                .whereEqualTo("uid", currentUserId)
                .addSnapshotListener(MetadataChanges.INCLUDE, (value, error) -> {
                    if (error != null) {
                        Log.e("FIREBASE_SNAPSHOT", "Lỗi snapshot listener: " + error.getMessage(), error);
                        return;
                    }
                    if (value != null) {
                        boolean fromCache = value.getMetadata().isFromCache();
                        Log.d("FIREBASE_SNAPSHOT", "Listener fired (fromCache=" + fromCache + "), documents=" + value.size());

                        List<TransactionModel> list = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            TransactionModel model = doc.toObject(TransactionModel.class);
                            if (model != null) {
                                // GÁN ID VÀO ĐÂY ĐỂ TRÁNH LỖI KHI XÓA
                                model.setTransactionId(doc.getId());
                                list.add(model);
                            } else {
                                Log.w("FIREBASE_SNAPSHOT", "Không thể parse document: " + doc.getId());
                            }
                        }
                        // Sort theo timestamp giảm dần (mới nhất lên đầu)
                        sortTransactionsByTimestamp(list);
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

    public void addTransaction(double amount, String type, String category, String note, Timestamp timestamp) {
        if (timestamp == null) timestamp = Timestamp.now();

        DocumentReference transRef = db.collection("transactions").document();
        String docId = transRef.getId();

        TransactionModel transaction = new TransactionModel(
                currentUserId, type, amount, category, timestamp, note
        );
        transaction.setTransactionId(docId);

        // === OPTIMISTIC UPDATE: Thêm vào LiveData NGAY LẬP TỨC ===
        List<TransactionModel> currentList = transactionList.getValue();
        List<TransactionModel> optimisticList = new ArrayList<>();
        if (currentList != null) {
            optimisticList.addAll(currentList);
        }
        optimisticList.add(transaction);
        sortTransactionsByTimestamp(optimisticList);
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
                    Log.e("FIREBASE_ADD", "Lỗi thêm giao dịch: " + e.getMessage(), e);
                    // Nếu ghi Firebase thất bại, rollback: loại bỏ giao dịch vừa thêm
                    List<TransactionModel> rollbackList = transactionList.getValue();
                    if (rollbackList != null) {
                        rollbackList.remove(transaction);
                        transactionList.setValue(rollbackList);
                    }
                });
    }

    public void forceRefresh() {
        refreshTransactions();
    }

    private void refreshTransactions() {
        db.collection("transactions")
                .whereEqualTo("uid", currentUserId)
                .get(Source.SERVER)
                .addOnSuccessListener(querySnapshot -> {
                    Log.d("FIREBASE_REFRESH", "Refresh thành công từ SERVER, documents=" + querySnapshot.size());
                    List<TransactionModel> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        TransactionModel model = doc.toObject(TransactionModel.class);
                        if (model != null) {
                            model.setTransactionId(doc.getId());
                            list.add(model);
                        }
                    }
                    // Sort theo timestamp giảm dần (mới nhất lên đầu)
                    sortTransactionsByTimestamp(list);
                    transactionList.setValue(list);
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_REFRESH", "Lỗi đọc từ SERVER: " + e.getMessage(), e);
                });
    }

    // Hàm sort danh sách giao dịch theo timestamp giảm dần (mới nhất lên đầu)
    private void sortTransactionsByTimestamp(List<TransactionModel> list) {
        Collections.sort(list, new Comparator<TransactionModel>() {
            @Override
            public int compare(TransactionModel t1, TransactionModel t2) {
                if (t1.getTimestamp() == null && t2.getTimestamp() == null) return 0;
                if (t1.getTimestamp() == null) return 1;
                if (t2.getTimestamp() == null) return -1;
                // Timestamp giảm dần (mới nhất trước)
                return t2.getTimestamp().compareTo(t1.getTimestamp());
            }
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
                })
                .addOnFailureListener(e -> {
                    Log.e("FIREBASE_DELETE", "Lỗi xóa giao dịch: " + e.getMessage(), e);
                });
    }
}