package com.example.projectck.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        }
    }
}