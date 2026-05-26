package com.example.projectck.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.projectck.R;
import com.example.projectck.models.Food;

import java.util.ArrayList;

public class FoodAdapter
        extends RecyclerView.Adapter<FoodAdapter.FoodViewHolder> {

    ArrayList<Food> foodList;

    OnFoodActionListener listener;

    public interface OnFoodActionListener{
        void onDeleteClick(Food food);
        void onEditClick(Food food);
    }
    public FoodAdapter(ArrayList<Food> foodList,
                       OnFoodActionListener listener) {

        this.foodList = foodList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.item_food,
                        parent,
                        false);

        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull FoodViewHolder holder,
            int position
    ) {

        Food food = foodList.get(position);

        holder.tvFoodName.setText(
                food.getName()
        );
        holder.tvFoodPrice.setText(
                ((int)food.getPrice()) + " đ"
        );
        holder.tvFoodDescription.setText(
                food.getDescription()
        );

        if (food.getImageUrl() != null && !food.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(food.getImageUrl())
                    .placeholder(R.drawable.ic_add) // Ảnh chờ khi đang tải
                    .error(R.drawable.ic_menu)       // Ảnh báo lỗi nếu link sai
                    .into(holder.imgFood);
        } else {
            holder.imgFood.setImageResource(R.drawable.ic_add);
        }

        holder.btnDelete.setOnClickListener(v -> {

            listener.onDeleteClick(food);
        });
        holder.btnEdit.setOnClickListener(v -> {

            listener.onEditClick(food);

        });
    }

    @Override
    public int getItemCount() {
        return foodList.size();
    }

    public static class FoodViewHolder
            extends RecyclerView.ViewHolder {

        TextView tvFoodName, tvFoodPrice, tvFoodDescription;
        Button btnEdit, btnDelete;
        ImageView imgFood;

        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);

            tvFoodName =
                    itemView.findViewById(R.id.tvFoodName);

            tvFoodPrice =
                    itemView.findViewById(R.id.tvFoodPrice);

            tvFoodDescription =
                    itemView.findViewById(R.id.tvFoodDescription);

            btnEdit =
                    itemView.findViewById(R.id.btnEdit);

            btnDelete =
                    itemView.findViewById(R.id.btnDelete);
            
            imgFood = itemView.findViewById(R.id.imgFood);
        }
    }
}