package com.example.projectck.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectck.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    Context context;
    List<DocumentSnapshot> list;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    public OrderAdapter(Context context, List<DocumentSnapshot> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_order, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {

        DocumentSnapshot doc = list.get(position);

        String orderId = doc.getId();
        String status = doc.getString("status");

        Double total = doc.getDouble("total");
        if (total == null) total = 0.0;

        h.txtTotal.setText("Total: " + total + " đ");
        h.txtStatus.setText("Status: " + status);

        if ("done".equals(status)) {
            h.btnDone.setVisibility(View.GONE);
        } else {
            h.btnDone.setVisibility(View.VISIBLE);
        }

        h.btnDone.setOnClickListener(v -> {

            db.collection("orders")
                    .document(orderId)
                    .update("status", "done");
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtTotal, txtStatus;
        Button btnDone;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtTotal = itemView.findViewById(R.id.txtTotal);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            btnDone = itemView.findViewById(R.id.btnDone);
        }
    }
}