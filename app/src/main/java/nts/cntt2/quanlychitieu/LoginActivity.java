package nts.cntt2.quanlychitieu;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private Button btnLogin, btnRegister;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Lắng nghe trạng thái đăng nhập/đăng ký thành công
        authViewModel.getIsAuthSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(LoginActivity.this, "Thành công!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Đóng LoginActivity để không quay lại được khi bấm nút Back
            }
        });

        // Hiển thị thông báo lỗi cụ thể nếu Firebase từ chối tạo tài khoản
        authViewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(LoginActivity.this, "Lỗi: " + msg, Toast.LENGTH_LONG).show();
            }
        });

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (!email.isEmpty() && !password.isEmpty()) {
                authViewModel.loginUser(email, password);
            } else {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            }
        });

        // SỬA ĐOẠN NÀY: Khi nhấn nút đăng ký cũ tại màn hình Đăng nhập, mở màn hình Đăng ký riêng biệt lên
        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}