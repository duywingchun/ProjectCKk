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

public class StaffFoodAdapter extends RecyclerView.Adapter<StaffFoodAdapter.ViewHolder> {

    public interface OnAddClick {
        void onAdd(Food food);
    }

    ArrayList<Food> list;
    OnAddClick listener;

    public StaffFoodAdapter(ArrayList<Food> list, OnAddClick listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_food_staff, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Food food = list.get(position);

        holder.tvName.setText(food.getName());
        holder.tvPrice.setText(
                ((int)food.getPrice()) + " đ"
        );
        holder.tvDescription.setText(
                food.getDescription()
        );
        holder.btnAdd.setOnClickListener(v -> {
            listener.onAdd(food);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvPrice, tvDescription;
        Button btnAdd;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvName = itemView.findViewById(R.id.tvFoodName);
            tvPrice = itemView.findViewById(R.id.tvFoodPrice);
            btnAdd = itemView.findViewById(R.id.btnAdd);
            tvDescription = itemView.findViewById(R.id.tvFoodDescription);
        }
    }
}