package com.example.projectck;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectck.adapters.StaffFoodAdapter;
import com.example.projectck.models.Food;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import com.example.projectck.data.CartManager;

public class StaffHomeActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    NavigationView navView;
    ImageView imgMenu, imgProfile;
    EditText edtSearch;

    RecyclerView recyclerFoods;

    FirebaseFirestore db;

    ArrayList<Food> foodList, allFoodList;
    StaffFoodAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_staff_home);

        // init views
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        imgMenu = findViewById(R.id.imgMenu);
        imgProfile = findViewById(R.id.imgProfile);
        recyclerFoods = findViewById(R.id.recyclerFoods);
        edtSearch = findViewById(R.id.edtSearch);

        db = FirebaseFirestore.getInstance();

        // setup recycler
        recyclerFoods.setLayoutManager(new LinearLayoutManager(this));
        foodList = new ArrayList<>();
        allFoodList = new ArrayList<>();
        
        //thêm món vào giỏ hàng
        adapter = new StaffFoodAdapter(foodList, food -> {

            CartManager.addToCart(food);

            Toast.makeText(this,
                    "Đã thêm: " + food.getName(),
                    Toast.LENGTH_SHORT).show();
        });

        recyclerFoods.setAdapter(adapter);

        // search functionality
        if (edtSearch != null) {
            edtSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterFood(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        // open drawer
        imgMenu.setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.START)
        );

        // navigation menu
        navView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_staff_home) {
                loadFoods();
            }

            else if (id == R.id.nav_cart) {
                startActivity(new Intent(this, CartActivity.class));
            }
            else if (id == R.id.nav_orders) {
                startActivity(new Intent(this, StaffOrdersActivity.class));
            }


            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // profile popup
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

                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                popupWindow.dismiss();
            });
        });

        // AI Chat FAB
        findViewById(R.id.fabAIChat).setOnClickListener(v -> 
            startActivity(new Intent(this, ChatActivity.class))
        );

        // load data
        loadFoods();
    }

    private void loadFoods() {

        db.collection("foods")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    foodList.clear();
                    allFoodList.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Food food = doc.toObject(Food.class);
                        if (food != null) {
                            food.setId(doc.getId());
                            foodList.add(food);
                            allFoodList.add(food);
                        }
                    }

                    adapter.notifyDataSetChanged();

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Load fail", Toast.LENGTH_SHORT).show()
                );
    }
    private void filterFood(String text){

        foodList.clear();

        for(Food food : allFoodList){

            if(food.getName()
                    .toLowerCase()
                    .contains(text.toLowerCase())){

                foodList.add(food);

            }
        }

        adapter.notifyDataSetChanged();
    }
}
