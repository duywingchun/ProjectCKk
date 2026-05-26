package com.example.projectck;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectck.adapters.CartAdapter;
import com.example.projectck.data.CartManager;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import com.example.projectck.models.CartItem;

import java.util.ArrayList;
import java.util.HashMap;

public class CartActivity extends AppCompatActivity {

    RecyclerView recyclerCart;
    TextView txtTotal;
    Button btnCheckout;
    ImageView imgMenu,imgProfile;
    CartAdapter adapter;
    DrawerLayout drawerLayout;
    NavigationView navView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        recyclerCart = findViewById(R.id.recyclerCart);
        txtTotal = findViewById(R.id.txtTotal);
        btnCheckout = findViewById(R.id.btnCheckout);
        imgProfile = findViewById(R.id.imgProfile);

        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        imgMenu = findViewById(R.id.imgMenu);

        recyclerCart.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CartAdapter(CartManager.cartList, this::updateTotal);

        recyclerCart.setAdapter(adapter);

        updateTotal();

        btnCheckout.setOnClickListener(v -> {

            if (CartManager.cartList.isEmpty()) {
                Toast.makeText(this, "Giỏ hàng trống", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            ArrayList<Object> items = new ArrayList<>();

            for (CartItem item : CartManager.cartList) {

                HashMap<String, Object> foodMap = new HashMap<>();
                foodMap.put("name", item.getFood().getName());
                foodMap.put("price", item.getFood().getPrice());
                foodMap.put("quantity", item.getQuantity());

                items.add(foodMap);
            }

            // taọ hóa đơn
            HashMap<String, Object> order = new HashMap<>();
            order.put("items", items);
            order.put("total", CartManager.getTotalPrice());
            order.put("status", "pending");
            order.put("staffId", FirebaseAuth.getInstance().getUid());
            order.put("time", FieldValue.serverTimestamp());

            // push lên firebase
            db.collection("orders")
                    .add(order)
                    .addOnSuccessListener(doc -> {

                        Toast.makeText(this, "Tạo hóa đơn thành công", Toast.LENGTH_SHORT).show();

                        // clear cart
                        CartManager.clearCart();

                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi tạo hóa đơn", Toast.LENGTH_SHORT).show();
                    });

        });
        // open drawer
        imgMenu.setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START)
        );

        // navigation menu
        navView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_staff_home) {
                startActivity(new Intent(this, StaffHomeActivity.class));
            }

            else if (id == R.id.nav_cart) {
                //
            }


            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

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

                Intent intent = new Intent(CartActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                popupWindow.dismiss();
            });
        });
    }

    private void updateTotal() {
        txtTotal.setText("Total: " + CartManager.getTotalPrice() + " đ");
    }
}
