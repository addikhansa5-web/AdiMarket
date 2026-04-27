package com.example.adimarket;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class DynamicVehicleAdapter extends RecyclerView.Adapter<DynamicVehicleAdapter.ViewHolder> {

    private List<DatabaseHelper.VehicleData> vehicleList;

    public DynamicVehicleAdapter(List<DatabaseHelper.VehicleData> vehicleList) {
        this.vehicleList = vehicleList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vehicle, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DatabaseHelper.VehicleData vehicle = vehicleList.get(position);
        
        holder.tvName.setText(vehicle.name);
        holder.tvPrice.setText("Rp " + vehicle.price);
        holder.tvYear.setText("Tahun: " + vehicle.year);
        holder.tvType.setText(vehicle.type);

        // --- PHASE 10: FRAUD DETECTION LOGIC ---
        FraudDetection fraudDetection = new FraudDetection(holder.itemView.getContext());
        // Konversi data vehicle ke format yang dipahami FraudDetection
        double priceValue = 0;
        try {
            priceValue = Double.parseDouble(vehicle.price.replaceAll("[^0-9]", ""));
        } catch (Exception e) {}

        FraudDetection.VehicleListing listing = new FraudDetection.VehicleListing(
                String.valueOf(vehicle.id),
                "seller_id", // Dummy
                vehicle.type,
                vehicle.name,
                vehicle.year,
                priceValue,
                vehicle.description != null ? vehicle.description : "",
                vehicle.sellerPhone != null ? vehicle.sellerPhone : "",
                "",
                1 // dummy count
        );

        FraudDetection.FraudResult result = fraudDetection.analyzeListing(listing);
        if (result.isFraudulent) {
            holder.tvFraudBadge.setVisibility(View.VISIBLE);
            holder.tvFraudBadge.setText(result.riskLevel + " RISK: " + result.reasons.get(0));
        } else {
            holder.tvFraudBadge.setVisibility(View.GONE);
        }
        // ----------------------------------------
        
        // Tampilkan Rating Otomatis dari Database
        DatabaseHelper db = new DatabaseHelper(holder.itemView.getContext());
        float avgRating = db.getAverageRating(vehicle.id);
        if (avgRating == 0) avgRating = 4.5f; // Default jika belum ada yang rating
        holder.ratingBar.setRating(avgRating);
        
        if (vehicle.description != null && !vehicle.description.isEmpty()) {
            holder.tvDescription.setText(vehicle.description);
            holder.tvDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        holder.containerFotoBeli.removeAllViews();
        if (vehicle.imageUri != null && !vehicle.imageUri.isEmpty()) {
            String[] images = vehicle.imageUri.split(",");
            for (String imgUri : images) {
                ImageView imageView = new ImageView(holder.itemView.getContext());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(300, 300);
                params.setMargins(0, 0, 15, 0);
                imageView.setLayoutParams(params);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                
                Glide.with(holder.itemView.getContext())
                        .load(imgUri)
                        .placeholder(R.drawable.car_placeholder)
                        .error(R.drawable.car_placeholder)
                        .into(imageView);

                imageView.setOnClickListener(v -> {
                    showFullImage(holder.itemView.getContext(), imgUri);
                });

                holder.containerFotoBeli.addView(imageView);
            }
        }

        holder.btnHubungi.setOnClickListener(v -> {
            String sellerPhone = vehicle.sellerPhone;
            if (sellerPhone == null || sellerPhone.isEmpty()) {
                Toast.makeText(holder.itemView.getContext(), "Nomor penjual tidak tersedia", Toast.LENGTH_SHORT).show();
                return;
            }
            if (sellerPhone.startsWith("0")) {
                sellerPhone = "62" + sellerPhone.substring(1);
            }
            String pesan = "Halo, saya tertarik dengan unit *" + vehicle.name + "* di AdiMarket. Masih ada?";
            try {
                String url = "https://wa.me/" + sellerPhone + "?text=" + Uri.encode(pesan);
                holder.itemView.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Exception e) {
                Toast.makeText(holder.itemView.getContext(), "Gagal buka WA", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFullImage(android.content.Context context, String imgUri) {
        Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_full_image);
        
        ImageView ivFull = dialog.findViewById(R.id.ivFullImage);
        ImageButton btnClose = dialog.findViewById(R.id.btnCloseFullImage);
        
        Glide.with(context)
                .load(imgUri)
                .into(ivFull);
        
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return vehicleList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout containerFotoBeli;
        TextView tvName, tvPrice, tvYear, tvType, tvDescription, tvFraudBadge;
        RatingBar ratingBar;
        Button btnHubungi;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            containerFotoBeli = itemView.findViewById(R.id.containerFotoBeli);
            tvName = itemView.findViewById(R.id.tvVehicleName);
            tvFraudBadge = itemView.findViewById(R.id.tvFraudBadge);
            ratingBar = itemView.findViewById(R.id.ratingBarSmall);
            tvPrice = itemView.findViewById(R.id.tvVehiclePrice);
            tvYear = itemView.findViewById(R.id.tvVehicleYear);
            tvType = itemView.findViewById(R.id.tvVehicleType);
            tvDescription = itemView.findViewById(R.id.tvVehicleDescription);
            btnHubungi = itemView.findViewById(R.id.btnHubungiPenjual);
        }
    }
}
