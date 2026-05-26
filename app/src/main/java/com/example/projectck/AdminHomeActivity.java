package com.example.projectck;

import android.app.AlertDialog;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectck.adapters.FoodAdapter;
import com.example.projectck.models.Food;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;

public class AdminHomeActivity extends AppCompatActivity {

    RecyclerView recyclerFoods;

    ArrayList<Food> foodList;
    ArrayList<Food> allFoodList;
    FoodAdapter adapter;
    FirebaseFirestore db;
    FloatingActionButton fabAddFood;
    ImageView imgProfile;

    DrawerLayout drawerLayout;
    NavigationView navView;
    ImageView imgMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        recyclerFoods = findViewById(R.id.recyclerFoods);

        EditText edtSearch;

        fabAddFood = findViewById(R.id.fabAddFood);

        // Firestore
        db = FirebaseFirestore.getInstance();

        foodList = new ArrayList<>();
        allFoodList = new ArrayList<>();

        edtSearch = findViewById(R.id.edtSearch);

        adapter = new FoodAdapter(
                foodList,

                new FoodAdapter.OnFoodActionListener() {

                    @Override
                    public void onDeleteClick(Food food) {

                        deleteFood(food);

                    }

                    @Override
                    public void onEditClick(Food food) {

                        showEditFoodDialog(food);

                    }
                }
        );

        recyclerFoods.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerFoods.setAdapter(adapter);

        // Load data từ Firestore
        loadFoods();
        fabAddFood.setOnClickListener(v -> {

            showAddFoodDialog();

        });

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(
                    CharSequence s,
                    int start,
                    int count,
                    int after
            ) {

            }
            @Override
            public void onTextChanged(
                    CharSequence s,
                    int start,
                    int before,
                    int count
            ) {

                filterFood(s.toString());

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
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

                Intent intent = new Intent(AdminHomeActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                popupWindow.dismiss();
            });
        });
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        imgMenu = findViewById(R.id.imgMenu);
        imgMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(androidx.core.view.GravityCompat.START);

            navView.setNavigationItemSelectedListener(item -> {

                int id = item.getItemId();

                if (id == R.id.nav_food) {
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
        });
    }

    private void loadFoods() {

        db.collection("foods")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    foodList.clear();
                    allFoodList.clear();

                    // Duyệt từng document
                    for (QueryDocumentSnapshot document
                            : queryDocumentSnapshots) {

                        // Convert document -> Food object
                        Food food =
                                document.toObject(Food.class);
                        // Lưu document ID
                        food.setId(document.getId());
                        foodList.add(food);
                        allFoodList.add(food);
                    }

                    adapter.notifyDataSetChanged();

                });
    }

    private void showAddFoodDialog() {

        AlertDialog.Builder builder =
                new AlertDialog.Builder(this);

        View view = getLayoutInflater()
                .inflate(R.layout.dialog_add_food, null);

        builder.setView(view);

        EditText edtFoodName =
                view.findViewById(R.id.edtFoodName);

        EditText edtFoodPrice =
                view.findViewById(R.id.edtFoodPrice);

        EditText edtFoodDescription =
                view.findViewById(R.id.edtFoodDescription);

        builder.setTitle("Thêm món ăn");

        // tạo sự kiện nut save
        builder.setPositiveButton("Lưu",
                (dialog, which) -> {

                    // lấy dữ liệu user nhập
                    String name =
                            edtFoodName.getText().toString();

                    String description =
                            edtFoodDescription.getText().toString();

                    double price =
                            Double.parseDouble(
                                    edtFoodPrice.getText().toString()
                            );

                    HashMap<String, Object> foodMap =
                            new HashMap<>();

                    foodMap.put("name", name);

                    foodMap.put("description", description);

                    foodMap.put("price", price);

                    foodMap.put("imageUrl", "");

                    // lưu vào firestore
                    db.collection("foods")
                            .add(foodMap)
                            .addOnSuccessListener(documentReference -> {

                                // reload recyclerview
                                loadFoods();

                                Toast.makeText(
                                        this,
                                        "Đã thêm",
                                        Toast.LENGTH_SHORT
                                ).show();

                            });

                });


        // nút cancel
        builder.setNegativeButton("Thoát", null);

        // hiện dialog
        builder.show();
    }
    // xóa food
    private void deleteFood(Food food){

        AlertDialog.Builder builder =
                new AlertDialog.Builder(this);

        builder.setTitle("Xóa món ăn");

        builder.setMessage(
                "Bạn có chắc chắn muốn xóa "
                        + food.getName() + " ra khỏi danh sách ?"
        );

        // "yes"
        builder.setPositiveButton("Có",
                (dialog, which) -> {

                    db.collection("foods")
                            .document(food.getId())
                            .delete()
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(
                                        this,
                                        "Đã xóa",
                                        Toast.LENGTH_SHORT
                                ).show();

                                loadFoods();

                            });

                });

        //"no"
        builder.setNegativeButton("Không", null);

        builder.show();
    }
    private void showEditFoodDialog(Food food){

        AlertDialog.Builder builder =
                new AlertDialog.Builder(this);

        View view = getLayoutInflater()
                .inflate(R.layout.dialog_add_food, null);

        builder.setView(view);

        EditText edtFoodName =
                view.findViewById(R.id.edtFoodName);

        EditText edtFoodPrice =
                view.findViewById(R.id.edtFoodPrice);

        EditText edtFoodDescription =
                view.findViewById(R.id.edtFoodDescription);

        // set dữ liệu cũ
        edtFoodName.setText(food.getName());

        edtFoodPrice.setText(
                String.valueOf((int) food.getPrice())
        );

        edtFoodDescription.setText(
                food.getDescription()
        );

        builder.setTitle("Sửa món ăn");

        builder.setPositiveButton("Cập nhật",
                (dialog, which) -> {

                    String name =
                            edtFoodName.getText().toString();

                    String description =
                            edtFoodDescription.getText().toString();

                    double price =
                            Double.parseDouble(
                                    edtFoodPrice.getText().toString()
                            );

                    HashMap<String, Object> foodMap =
                            new HashMap<>();

                    foodMap.put("name", name);

                    foodMap.put("description", description);

                    foodMap.put("price", price);

                    // cập nhật firestore
                    db.collection("foods")
                            .document(food.getId())
                            .update(foodMap)
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(
                                        this,
                                        "Đã cập nhật",
                                        Toast.LENGTH_SHORT
                                ).show();

                                loadFoods();

                            });

                });

        builder.setNegativeButton("Thoát", null);

        builder.show();
    }
    //hàm tìm kiếm food
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
