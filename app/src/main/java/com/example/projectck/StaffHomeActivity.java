package com.example.projectck;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class StaffHomeActivity extends AppCompatActivity {

    TextView tvWelcomeAdmin;
    Button btnLogout;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        // Firebase
        auth = FirebaseAuth.getInstance();

        // Ánh xạ view

        btnLogout = findViewById(R.id.btnLogout);

        // User hiện tại
        FirebaseUser user = auth.getCurrentUser();

        if(user != null){

            String email = user.getEmail();

            tvWelcomeAdmin.setText(
                    "Welcome Staff\n" + email
            );
        }

        // Logout
        btnLogout.setOnClickListener(v -> {

            auth.signOut();

            Intent intent = new Intent(
                    StaffHomeActivity.this,
                    LoginActivity.class
            );

            startActivity(intent);

            finish();

        });

    }
}