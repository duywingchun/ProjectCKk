package com.example.projectck;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectck.adapters.OrderAdapter;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class StaffOrdersActivity extends AppCompatActivity {

    RecyclerView recyclerOrders;
    FirebaseFirestore db;

    List<DocumentSnapshot> orderList;
    OrderAdapter adapter;
    ImageView imgProfile;
    ImageView imgMenu;
    DrawerLayout drawerLayout;
    NavigationView navView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_orders);

        recyclerOrders = findViewById(R.id.recyclerOrders);
        recyclerOrders.setLayoutManager(new LinearLayoutManager(this));
        imgProfile = findViewById(R.id.imgProfile);
        imgMenu = findViewById(R.id.imgMenu);
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);

        db = FirebaseFirestore.getInstance();
        orderList = new ArrayList<>();

        loadOrders();

        imgProfile.setOnClickListener(v -> {
            View view = getLayoutInflater().inflate(R.layout.dialog_profile, null);
            PopupWindow popupWindow = new PopupWindow(
                    view,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );
            popupWindow.setElevation(10);
            popupWindow.showAsDropDown(imgProfile);

            TextView txtName = view.findViewById(R.id.txtName);
            TextView txtEmail = view.findViewById(R.id.txtEmail);
            TextView txtRole = view.findViewById(R.id.txtRole);
            Button btnLogout = view.findViewById(R.id.btnLogout);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                txtEmail.setText("Email: " + user.getEmail());
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.getUid())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            String name = documentSnapshot.getString("name");
                            String role = documentSnapshot.getString("role");
                            if (name != null) {
                                txtName.setText("Name: " + name);
                            }
                            txtRole.setText("Role: " + role);
                        });
            }

            btnLogout.setOnClickListener(v1 -> {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(StaffOrdersActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                popupWindow.dismiss();
            });
        });

        imgMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_staff_home) {
                startActivity(new Intent(this, StaffHomeActivity.class));
            } else if (id == R.id.nav_cart) {
                startActivity(new Intent(this, CartActivity.class));
            } else if (id == R.id.nav_orders) {
                // Already here
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void loadOrders() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("orders")
                .whereEqualTo("staffId", uid)
                .orderBy("time", Query.Direction.DESCENDING)
                .limit(10) //hiển thị 10 đơn gần nhất
                .addSnapshotListener((value, error) -> {

                    if (error != null || value == null) return;

                    orderList.clear();

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        orderList.add(doc);
                    }

                    if (adapter == null) {
                        adapter = new OrderAdapter(this, orderList, true);
                        recyclerOrders.setAdapter(adapter);
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}
