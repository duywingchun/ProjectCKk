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

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.core.view.GravityCompat;

public class RevenueActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    NavigationView navView;
    ImageView imgMenu;
    ImageView imgProfile;
    TextView txtRevenueToday, txtRevenueTotal;

    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue);

        // init view
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        imgMenu = findViewById(R.id.imgMenu);
        imgProfile = findViewById(R.id.imgProfile);
        txtRevenueToday = findViewById(R.id.txtRevenueToday);
        txtRevenueTotal = findViewById(R.id.txtRevenueTotal);

        db = FirebaseFirestore.getInstance();

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
                startActivity(new Intent(this, AdminOrdersActivity.class));
            }

            else if (id == R.id.nav_revenue) {
                startActivity(new Intent(this, RevenueActivity.class));
            }

            drawerLayout.closeDrawers();
            return true;
        });
        loadTodayRevenue();
        loadTotalRevenue();

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
            TextView txtName = view.findViewById(R.id.txtName);
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
                            String name = documentSnapshot.getString("name");
                            String role = documentSnapshot.getString("role");
                            if (name != null) {
                                txtName.setText("Name: " + name);
                            }
                            txtRole.setText("Role: " + role);
                        });
            }

            // Logout action
            btnLogout.setOnClickListener(v1 -> {

                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent(RevenueActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                popupWindow.dismiss();
            });
        });
    }
    private void loadTodayRevenue() {

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Date startOfDay = calendar.getTime();

        db.collection("orders")
                .whereGreaterThanOrEqualTo("time", startOfDay)
                .get()
                .addOnSuccessListener(snapshot -> {

                    double total = 0;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        Double price = doc.getDouble("total");
                        String status = doc.getString("status");

                        if (price != null && "done".equals(status)) {
                            total += price;
                        }
                    }

                    DecimalFormat df = new DecimalFormat("#,###");
                    txtRevenueToday.setText("Today: " + df.format(total) + " đ");
                });
    }
    private void loadTotalRevenue() {

        db.collection("orders")
                .get()
                .addOnSuccessListener(snapshot -> {

                    double total = 0;

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {

                        Double price = doc.getDouble("total");
                        String status = doc.getString("status");

                        if (price != null && "done".equals(status)) {
                            total += price;
                        }
                    }

                    DecimalFormat df = new DecimalFormat("#,###");
                    txtRevenueTotal.setText("Total: " + df.format(total) + " đ");
                });
    }
}
