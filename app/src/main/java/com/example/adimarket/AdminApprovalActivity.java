package com.example.adimarket;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AdminApprovalActivity extends AppCompatActivity {

    private RecyclerView rvPending;
    private AdminApprovalAdapter adapter;
    private DatabaseHelper dbHelper;
    private List<DatabaseHelper.VehicleData> pendingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_approval);
        WallpaperHelper.apply(this, findViewById(R.id.headerImage));

        dbHelper = new DatabaseHelper(this);
        rvPending = findViewById(R.id.rvPendingAds);
        rvPending.setLayoutManager(new LinearLayoutManager(this));

        loadPendingAds();
    }

    private void loadPendingAds() {
        pendingList = dbHelper.getPendingVehicles();
        adapter = new AdminApprovalAdapter(pendingList, new AdminApprovalAdapter.OnActionClickListener() {
            @Override
            public void onApprove(int id) {
                // Ambil nama kendaraan sebelum dihapus dari pending
                String namaKendaraan = getNamaById(id);
                dbHelper.approveVehicle(id);
                // 🔔 Notifikasi: iklan disetujui
                NotificationHelper.notifyIklanDisetujui(AdminApprovalActivity.this, namaKendaraan);
                Toast.makeText(AdminApprovalActivity.this, getString(R.string.ad_approved_toast), Toast.LENGTH_SHORT).show();
                loadPendingAds();
            }

            @Override
            public void onReject(int id) {
                String namaKendaraan = getNamaById(id);
                dbHelper.deleteVehicle(id);
                // 🔔 Notifikasi: iklan ditolak
                NotificationHelper.notifyIklanDitolak(AdminApprovalActivity.this, namaKendaraan);
                Toast.makeText(AdminApprovalActivity.this, getString(R.string.ad_rejected_toast), Toast.LENGTH_SHORT).show();
                loadPendingAds();
            }
        });
        rvPending.setAdapter(adapter);
    }

    private String getNamaById(int id) {
        for (DatabaseHelper.VehicleData v : pendingList) {
            if (v.id == id) return v.name;
        }
        return "Kendaraan";
    }
}