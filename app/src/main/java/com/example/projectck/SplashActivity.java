package com.example.projectck;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                FirebaseUser user = auth.getCurrentUser();

                // nếu user chưa login -> chuyển sang loginactivity
                if (user == null) {
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    finish();
                }
                // user đã login -> kiểm tra role
                else {
                    String uid = user.getUid();
                    db.collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(new com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    if (documentSnapshot != null && documentSnapshot.exists()) {
                                        String role = documentSnapshot.getString("role");

                                        if ("admin".equals(role)) {
                                            startActivity(new Intent(SplashActivity.this, AdminHomeActivity.class));
                                        } else {
                                            startActivity(new Intent(SplashActivity.this, StaffHomeActivity.class));
                                        }
                                    } else {
                                        // Tài khoản không tồn tại trên Firestore -> Về Login
                                        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                                    }
                                    finish();
                                }
                            })
                            .addOnFailureListener(e -> {
                                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                                finish();
                            });
                }

            }
        }, 2000);

    }
}