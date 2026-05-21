package nts.cntt2.quanlychitieu;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class RegisterActivity extends AppCompatActivity {
    private EditText etRegisterEmail, etRegisterPassword, etConfirmPassword;
    private Button btnConfirmRegister, tvBackToLogin; // Đổi kiểu dữ liệu tvBackToLogin thành Button để khớp với MaterialButton trong XML mới
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Ánh xạ các ô nhập liệu
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword); // Thêm ánh xạ ô xác nhận mật khẩu mới

        // ĐÃ SỬA: Đổi ID tìm kiếm để khớp hoàn toàn với file XML mới của bạn
        btnConfirmRegister = findViewById(R.id.btnRegisterSubmit);
        tvBackToLogin = findViewById(R.id.btnBackToLogin);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Lắng nghe trạng thái đăng ký từ Firebase gửi về
        authViewModel.getIsAuthSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                // Đăng ký xong tự động đưa người dùng vào Màn hình chính
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Lắng nghe nếu có lỗi từ Firebase xảy ra
        authViewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(RegisterActivity.this, "Lỗi: " + msg, Toast.LENGTH_LONG).show();
            }
        });

        // Xử lý khi nhấn nút Xác nhận đăng ký
        btnConfirmRegister.setOnClickListener(v -> {
            String email = etRegisterEmail.getText().toString().trim();
            String password = etRegisterPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim(); // Lấy dữ liệu ô xác nhận mật khẩu

            // Kiểm tra rỗng toàn bộ các trường
            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Kiểm tra độ dài mật khẩu chính
            if (password.length() < 6) {
                Toast.makeText(this, "Mật khẩu phải từ 6 ký tự trở lên!", Toast.LENGTH_SHORT).show();
                return;
            }

            // THÊM LOGIC: Kiểm tra xem mật khẩu nhập lại có khớp với mật khẩu ban đầu không
            if (!password.equals(confirmPassword)) {
                Toast.makeText(this, "Mật khẩu xác nhận không trùng khớp!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gọi AuthViewModel tiến hành đăng ký tài khoản mới lên Firebase
            authViewModel.registerUser(email, password);
        });

        // Nhấn dòng chữ dưới cùng để quay lại màn hình đăng nhập nếu muốn
        tvBackToLogin.setOnClickListener(v -> {
            finish();
        });
    }
}