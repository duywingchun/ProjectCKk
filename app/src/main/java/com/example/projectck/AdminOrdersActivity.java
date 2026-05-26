package com.example.projectck;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.core.view.GravityCompat;

public class AdminOrdersActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    NavigationView navView;
    ImageView imgMenu;
    RecyclerView recyclerOrders;
    ImageView imgProfile;

    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_orders);

        // init view
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        imgMenu = findViewById(R.id.imgMenu);
        recyclerOrders = findViewById(R.id.recyclerOrders);

        db = FirebaseFirestore.getInstance();

        recyclerOrders.setLayoutManager(new LinearLayoutManager(this));

        imgMenu.setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START)
        );

        // handle menu click
        navView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_food) {
                startActivity(new Intent(this, AdminHomeActivity.class));
                finish();
            }

            else if (id == R.id.nav_orders) {
            }

            else if (id == R.id.nav_revenue) {
                startActivity(new Intent(this, RevenueActivity.class));
            }

            drawerLayout.closeDrawers();
            return true;
        });
        imgProfile = findViewById(R.id.imgProfile);

        imgProfile.setOnClickListener(v -> {

            // Inflate layout profile
            View view = getLayoutInflater().inflate(R.layout.dialog_profile, null);

            // Create PopupWindow
            PopupWindow popupWindow = new PopupWindow(
                    view,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );

            popupWindow.setElevation(10);

            popupWindow.showAsDropDown(imgProfile);

            // Init views trong popup
            TextView txtEmail = view.findViewById(R.id.txtEmail);
            TextView txtRole = view.findViewById(R.id.txtRole);
            Button btnLogout = view.findViewById(R.id.btnLogout);

            // Get current user
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user != null) {
                txtEmail.setText("Email: " + user.getEmail());

                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.getUid())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            String role = documentSnapshot.getString("role");
                            txtRole.setText("Role: " + role);
                        });
            }

            // Logout action
            btnLogout.setOnClickListener(v1 -> {

                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent(AdminOrdersActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                popupWindow.dismiss();
            });
        });

        // thêm loadorder hiển thị ds đơn hàng
        //
         //
    }
}