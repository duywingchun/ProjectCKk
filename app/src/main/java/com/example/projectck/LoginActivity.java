package com.example.projectck;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 9001;

    private EditText edtEmail, edtPassword;
    private Button btnLogin, btnGoogleSign;
    private TextView tvRegister;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient; // Biến quản lý Google Client

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Ánh xạ View
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogleSign = findViewById(R.id.btnLoginGoogle); // Thêm ánh xạ nút Google
        tvRegister = findViewById(R.id.tvRegister);
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. Cấu hình Google Sign-In
        // Dùng default_web_client_id (Hệ thống tự sinh ra từ file google-services.json)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("762329506101-5oqp2gc2ah1dg3gp6caqf2c6kp3dqh5j.apps.googleusercontent.com")
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 3. Khởi chạy Auto Login nếu đã đăng nhập trước đó
        if (mAuth.getCurrentUser() != null) {
            checkUserRole(mAuth.getCurrentUser().getUid());
        }

        // Bấm nút chuyển sang Đăng ký
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        // Bấm nút Đăng nhập bằng Email/Password thông thường
        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Vui lòng nhập tài khoản/mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            checkUserRole(mAuth.getCurrentUser().getUid());
                        } else {
                            progressBar.setVisibility(View.GONE);
                            btnLogin.setEnabled(true);
                            Toast.makeText(LoginActivity.this, "Đăng nhập thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // 4. Xử lý sự kiện bấm nút Đăng nhập bằng Google
        btnGoogleSign.setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    // 5. Nhận kết quả trả về từ màn hình chọn tài khoản Google của hệ thống
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Đăng nhập Google thành công, lấy thông tin tài khoản Google để xác thực với Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Lỗi xác thực Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 6. Gửi token Google lên Firebase Auth để xác thực tài khoản
    private void firebaseAuthWithGoogle(String idToken) {
        progressBar.setVisibility(View.VISIBLE);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        String email = mAuth.getCurrentUser().getEmail();

                        // Đối với Google Sign-In, nếu tài khoản này mới đăng nhập lần đầu,
                        // ta cần tạo quyền mặc định "staff" cho họ trên Firestore
                        db.collection("users").document(uid).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (!documentSnapshot.exists()) {
                                        // Nếu chưa có thông tin trên Firestore -> Tạo mới
                                        Map<String, Object> userMap = new HashMap<>();
                                        userMap.put("email", email);
                                        userMap.put("role", "staff"); // Quyền mặc định cho thành viên mới

                                        db.collection("users").document(uid).set(userMap)
                                                .addOnSuccessListener(aVoid -> checkUserRole(uid))
                                                .addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "Không thể khởi tạo quyền!", Toast.LENGTH_SHORT).show());
                                    } else {
                                        // Nếu đã từng đăng nhập và có quyền rồi -> Tiến hành check role để chuyển màn hình
                                        checkUserRole(uid);
                                    }
                                });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Đăng nhập Google thất bại trên Firebase!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Hàm đọc dữ liệu Role từ Firestore để phân quyền (Màn hình Admin / Nhân viên)
    private void checkUserRole(String uid) {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (btnLogin != null) btnLogin.setEnabled(true);

                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if ("admin".equals(role)) {
                            startActivity(new Intent(LoginActivity.this, AdminHomeActivity.class));
                            finish();
                        } else {
                            startActivity(new Intent(LoginActivity.this, StaffHomeActivity.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Không tìm thấy thông tin phân quyền!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    if (btnLogin != null) btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Lỗi kết nối dữ liệu phân quyền!", Toast.LENGTH_SHORT).show();
                });
    }
}
