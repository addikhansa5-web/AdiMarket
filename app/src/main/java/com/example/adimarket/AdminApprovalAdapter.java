package com.example.adimarket;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class AdminApprovalAdapter extends RecyclerView.Adapter<AdminApprovalAdapter.ViewHolder> {

    private List<DatabaseHelper.VehicleData> pendingList;
    private OnActionClickListener listener;

    public interface OnActionClickListener {
        void onApprove(int id);
        void onReject(int id);
    }

    public AdminApprovalAdapter(List<DatabaseHelper.VehicleData> pendingList, OnActionClickListener listener) {
        this.pendingList = pendingList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_approval, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DatabaseHelper.VehicleData data = pendingList.get(position);
        
        // Info Utama (Nama & Harga)
        holder.tvInfo.setText(data.name + " - Rp " + data.price);
        
        // Detail Kendaraan (Merek, Tipe, Tahun)
        String details = "Merek: " + data.brand + " | Tipe: " + data.type + " | Tahun: " + data.year;
        holder.tvDetails.setText(details);
        
        // Deskripsi
        holder.tvDesc.setText("Deskripsi: " + data.description);
        
        // Nomor HP Penjual
        holder.tvPhone.setText("WA: " + data.sellerPhone);

        // Tampilkan Foto di Layar Admin
        holder.containerFotoAdmin.removeAllViews();
        if (data.imageUri != null && !data.imageUri.isEmpty()) {
            String[] images = data.imageUri.split(",");
            for (String imgUri : images) {
                ImageView imageView = new ImageView(holder.itemView.getContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(250, 250);
                params.setMargins(0, 0, 10, 0);
                imageView.setLayoutParams(params);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                
                Glide.with(holder.itemView.getContext())
                        .load(Uri.parse(imgUri))
                        .placeholder(R.drawable.car_placeholder)
                        .into(imageView);

                holder.containerFotoAdmin.addView(imageView);
            }
        }
        
        holder.btnApprove.setOnClickListener(v -> listener.onApprove(data.id));
        holder.btnReject.setOnClickListener(v -> listener.onReject(data.id));
    }

    @Override
    public int getItemCount() {
        return pendingList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInfo, tvDetails, tvDesc, tvPhone;
        LinearLayout containerFotoAdmin;
        Button btnApprove, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInfo = itemView.findViewById(R.id.tvAdminVehicleInfo);
            tvDetails = itemView.findViewById(R.id.tvAdminVehicleDetails);
            tvDesc = itemView.findViewById(R.id.tvAdminVehicleDesc);
            tvPhone = itemView.findViewById(R.id.tvAdminSellerPhone);
            containerFotoAdmin = itemView.findViewById(R.id.containerFotoAdmin);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}