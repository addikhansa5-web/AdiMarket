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
                dbHelper.approveVehicle(id);
                Toast.makeText(AdminApprovalActivity.this, getString(R.string.ad_approved_toast), Toast.LENGTH_SHORT).show();
                loadPendingAds(); // Refresh list
            }

            @Override
            public void onReject(int id) {
                dbHelper.deleteVehicle(id);
                Toast.makeText(AdminApprovalActivity.this, getString(R.string.ad_rejected_toast), Toast.LENGTH_SHORT).show();
                loadPendingAds(); // Refresh list
            }
        });
        rvPending.setAdapter(adapter);
    }
}