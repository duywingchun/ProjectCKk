package com.example.projectck;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.projectck.adapters.FoodAdapter;
import com.example.projectck.models.Food;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

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

    private Uri imageUri;
    private ImageView imgPreview;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        if (imgPreview != null) {
                            imgPreview.setImageURI(imageUri);
                        }
                    }
                }
        );

        recyclerFoods = findViewById(R.id.recyclerFoods);
        fabAddFood = findViewById(R.id.fabAddFood);
        db = FirebaseFirestore.getInstance();

        foodList = new ArrayList<>();
        allFoodList = new ArrayList<>();

        adapter = new FoodAdapter(foodList, new FoodAdapter.OnFoodActionListener() {
            @Override
            public void onDeleteClick(Food food) { deleteFood(food); }
            @Override
            public void onEditClick(Food food) { showEditFoodDialog(food); }
        });

        recyclerFoods.setLayoutManager(new LinearLayoutManager(this));
        recyclerFoods.setAdapter(adapter);

        loadFoods();
        fabAddFood.setOnClickListener(v -> showAddFoodDialog());

        EditText edtSearch = findViewById(R.id.edtSearch);
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { filterFood(s.toString()); }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        imgProfile = findViewById(R.id.imgProfile);
        imgProfile.setOnClickListener(v -> showProfilePopup());

        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        imgMenu = findViewById(R.id.imgMenu);
        imgMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(androidx.core.view.GravityCompat.START);
            navView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_orders) startActivity(new Intent(this, AdminOrdersActivity.class));
                else if (id == R.id.nav_revenue) startActivity(new Intent(this, RevenueActivity.class));
                drawerLayout.closeDrawers();
                return true;
            });
        });
    }

    private void showProfilePopup() {
        View view = getLayoutInflater().inflate(R.layout.dialog_profile, null);
        PopupWindow popupWindow = new PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setElevation(10);
        popupWindow.showAsDropDown(imgProfile);

        TextView txtName = view.findViewById(R.id.txtName);
        TextView txtEmail = view.findViewById(R.id.txtEmail);
        TextView txtRole = view.findViewById(R.id.txtRole);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            txtEmail.setText("Email: " + user.getEmail());
            FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String name = documentSnapshot.getString("name");
                        String role = documentSnapshot.getString("role");
                        if (name != null) txtName.setText("Name: " + name);
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
    }

    private void loadFoods() {
        db.collection("foods").get().addOnSuccessListener(queryDocumentSnapshots -> {
            foodList.clear();
            allFoodList.clear();
            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Food food = document.toObject(Food.class);
                food.setId(document.getId());
                foodList.add(food);
                allFoodList.add(food);
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void showAddFoodDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_food, null);
        builder.setView(view);

        EditText edtFoodName = view.findViewById(R.id.edtFoodName);
        EditText edtFoodPrice = view.findViewById(R.id.edtFoodPrice);
        EditText edtFoodDescription = view.findViewById(R.id.edtFoodDescription);
        EditText edtImageUrl = view.findViewById(R.id.edtImageUrl); // Ô nhập link ảnh
        imgPreview = view.findViewById(R.id.imgFoodPreview);
        Button btnSelectImage = view.findViewById(R.id.btnSelectImage);

        imageUri = null;
        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        builder.setTitle("Thêm món ăn");
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String name = edtFoodName.getText().toString();
            String desc = edtFoodDescription.getText().toString();
            String priceStr = edtFoodPrice.getText().toString();
            String manualUrl = edtImageUrl.getText().toString().trim();

            if (name.isEmpty() || desc.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }
            double price = Double.parseDouble(priceStr);

            if (imageUri != null) {
                // Nếu có chọn ảnh từ máy thì tải lên (nhưng sẽ lỗi nếu chưa bật Blaze)
                uploadImageAndSaveFood(name, desc, price);
            } else {
                // Nếu không chọn ảnh máy -> Dùng link ảnh vừa dán (Hoặc rỗng)
                saveFoodToFirestore(name, desc, price, manualUrl);
            }
        });
        builder.setNegativeButton("Thoát", null);
        builder.show();
    }

    private void uploadImageAndSaveFood(String name, String description, double price) {
        String fileName = UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("food_images/" + fileName);
        ref.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) saveFoodToFirestore(name, description, price, task.getResult().toString());
                    else Toast.makeText(this, "Lỗi tải ảnh: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveFoodToFirestore(String name, String description, double price, String imageUrl) {
        HashMap<String, Object> foodMap = new HashMap<>();
        foodMap.put("name", name);
        foodMap.put("description", description);
        foodMap.put("price", price);
        foodMap.put("imageUrl", imageUrl);
        db.collection("foods").add(foodMap).addOnSuccessListener(documentReference -> {
            loadFoods();
            Toast.makeText(this, "Đã thêm món ăn", Toast.LENGTH_SHORT).show();
        });
    }

    private void deleteFood(Food food) {
        new AlertDialog.Builder(this).setTitle("Xóa món ăn").setMessage("Xóa " + food.getName() + "?")
                .setPositiveButton("Có", (dialog, which) -> {
                    db.collection("foods").document(food.getId()).delete().addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show();
                        loadFoods();
                    });
                }).setNegativeButton("Không", null).show();
    }

    private void showEditFoodDialog(Food food) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_food, null);
        builder.setView(view);

        EditText edtFoodName = view.findViewById(R.id.edtFoodName);
        EditText edtFoodPrice = view.findViewById(R.id.edtFoodPrice);
        EditText edtFoodDescription = view.findViewById(R.id.edtFoodDescription);
        EditText edtImageUrl = view.findViewById(R.id.edtImageUrl);
        imgPreview = view.findViewById(R.id.imgFoodPreview);
        Button btnSelectImage = view.findViewById(R.id.btnSelectImage);

        edtFoodName.setText(food.getName());
        edtFoodPrice.setText(String.valueOf((int) food.getPrice()));
        edtFoodDescription.setText(food.getDescription());
        edtImageUrl.setText(food.getImageUrl()); // Hiện link cũ nếu có

        if (food.getImageUrl() != null && !food.getImageUrl().isEmpty()) Glide.with(this).load(food.getImageUrl()).into(imgPreview);

        imageUri = null;
        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        builder.setTitle("Sửa món ăn");
        builder.setPositiveButton("Cập nhật", (dialog, which) -> {
            String name = edtFoodName.getText().toString();
            String desc = edtFoodDescription.getText().toString();
            String priceStr = edtFoodPrice.getText().toString();
            String manualUrl = edtImageUrl.getText().toString().trim();

            if (name.isEmpty() || desc.isEmpty() || priceStr.isEmpty()) return;
            double price = Double.parseDouble(priceStr);

            if (imageUri != null) {
                updateImageAndSaveFood(food.getId(), name, desc, price);
            } else {
                // Dùng link ảnh vừa sửa (Hoặc giữ link cũ)
                updateFoodToFirestore(food.getId(), name, desc, price, manualUrl);
            }
        });
        builder.setNegativeButton("Thoát", null);
        builder.show();
    }

    private void updateImageAndSaveFood(String foodId, String name, String description, double price) {
        String fileName = UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("food_images/" + fileName);
        ref.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) updateFoodToFirestore(foodId, name, description, price, task.getResult().toString());
                    else Toast.makeText(this, "Lỗi cập nhật ảnh", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateFoodToFirestore(String foodId, String name, String description, double price, String imageUrl) {
        HashMap<String, Object> foodMap = new HashMap<>();
        foodMap.put("name", name);
        foodMap.put("description", description);
        foodMap.put("price", price);
        foodMap.put("imageUrl", imageUrl);
        db.collection("foods").document(foodId).update(foodMap).addOnSuccessListener(unused -> {
            loadFoods();
            Toast.makeText(this, "Đã cập nhật", Toast.LENGTH_SHORT).show();
        });
    }

    private void filterFood(String text) {
        foodList.clear();
        for (Food food : allFoodList) {
            if (food.getName().toLowerCase().contains(text.toLowerCase())) foodList.add(food);
        }
        adapter.notifyDataSetChanged();
    }
}
