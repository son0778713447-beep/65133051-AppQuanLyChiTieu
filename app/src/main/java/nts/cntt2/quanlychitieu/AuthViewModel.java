package nts.cntt2.quanlychitieu;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthViewModel extends ViewModel {
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private MutableLiveData<Boolean> isAuthSuccess = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<Boolean> getIsAuthSuccess() { return isAuthSuccess; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void registerUser(String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().getUser() != null) {
                        String uid = task.getResult().getUser().getUid();
                        UserModel newUser = new UserModel(uid, email, 0.0, 5000000.0); // Hạn mức mặc định 5 triệu

                        db.collection("users").document(uid).set(newUser)
                                .addOnSuccessListener(aVoid -> isAuthSuccess.setValue(true))
                                .addOnFailureListener(e -> errorMessage.setValue(e.getMessage()));
                    } else {
                        errorMessage.setValue(task.getException() != null ? task.getException().getMessage() : "Đăng ký thất bại");
                    }
                });
    }

    public void loginUser(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        isAuthSuccess.setValue(true);
                    } else {
                        errorMessage.setValue(task.getException() != null ? task.getException().getMessage() : "Sai tài khoản hoặc mật khẩu");
                    }
                });
    }
}