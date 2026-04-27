package com.example.adimarket;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;import java.util.List;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder> {

    private List<DatabaseHelper.VehicleData> vehicleList;

    public VehicleAdapter(List<DatabaseHelper.VehicleData> vehicleList) {
        this.vehicleList = vehicleList;
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Pastikan Anda sudah membuat item_vehicle.xml seperti di instruksi sebelumnya
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vehicle, parent, false);
        return new VehicleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        DatabaseHelper.VehicleData vehicle = vehicleList.get(position);
        holder.tvName.setText(vehicle.name);
        holder.tvType.setText(vehicle.type);
        holder.tvPrice.setText(vehicle.price);
        holder.tvYear.setText(String.valueOf(vehicle.year));
    }

    @Override
    public int getItemCount() {
        return vehicleList.size();
    }

    static class VehicleViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType, tvPrice, tvYear;

        public VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvVehicleName);
            tvType = itemView.findViewById(R.id.tvVehicleType);
            tvPrice = itemView.findViewById(R.id.tvVehiclePrice);
            tvYear = itemView.findViewById(R.id.tvVehicleYear);
        }
    }
}