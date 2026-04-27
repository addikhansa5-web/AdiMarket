package com.example.adimarket;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class BeliKendaraanActivity extends AppCompatActivity {

    private EditText etCariKendaraan;
    private Button btnMobil, btnMotor, btnLainnya;
    private RecyclerView rvKendaraanDinamis;
    private DynamicVehicleAdapter adapter;
    private List<DatabaseHelper.VehicleData> allVehicles;
    private List<DatabaseHelper.VehicleData> displayedVehicles;

    private LinearLayout llCategorySelection, llContent;
    private ImageButton btnBackToMenu;
    private TextView tvSelectedCategory;
    private String currentCategory = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beli_kendaraan);

        // Inisialisasi komponen
        etCariKendaraan = findViewById(R.id.etCariKendaraan);
        btnMobil = findViewById(R.id.btnMobil);
        btnMotor = findViewById(R.id.btnMotor);
        btnLainnya = findViewById(R.id.btnLainnya);
        rvKendaraanDinamis = findViewById(R.id.rvKendaraanDinamis);

        llCategorySelection = findViewById(R.id.llCategorySelection);
        llContent = findViewById(R.id.llContent);
        btnBackToMenu = findViewById(R.id.btnBackToMenu);
        tvSelectedCategory = findViewById(R.id.tvSelectedCategory);

        // Setup Database dan List
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        allVehicles = dbHelper.getAllVehicles();
        displayedVehicles = new ArrayList<>();

        // Setup RecyclerView
        adapter = new DynamicVehicleAdapter(displayedVehicles);
        rvKendaraanDinamis.setLayoutManager(new LinearLayoutManager(this));
        rvKendaraanDinamis.setAdapter(adapter);

        // Kembalikan ke pilihan kategori semula
        llCategorySelection.setVisibility(View.VISIBLE);
        llContent.setVisibility(View.GONE);

        // Logika Pencarian
        etCariKendaraan.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Tombol Filter MOBIL
        btnMobil.setOnClickListener(v -> filterByType("Mobil"));

        // Tombol Filter MOTOR
        btnMotor.setOnClickListener(v -> filterByType("Motor"));

        // Tombol Filter LAIN-LAIN
        btnLainnya.setOnClickListener(v -> filterByType("Lain-lain"));

        // Tombol Kembali ke Menu Kategori
        btnBackToMenu.setOnClickListener(v -> {
            llContent.setVisibility(View.GONE);
            llCategorySelection.setVisibility(View.VISIBLE);
            currentCategory = "";
            etCariKendaraan.setText("");
        });
    }

    private void showAllAds() {
        currentCategory = ""; // Kosongkan kategori agar tidak memfilter
        tvSelectedCategory.setText("SEMUA IKLAN");
        llCategorySelection.setVisibility(View.GONE); // Sembunyikan pilihan kategori
        llContent.setVisibility(View.VISIBLE); // Tampilkan daftar langsung

        displayedVehicles.clear();
        displayedVehicles.addAll(allVehicles);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data dari database agar iklan baru otomatis muncul
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        allVehicles = dbHelper.getAllVehicles();
        if (!currentCategory.isEmpty()) {
            filterByType(currentCategory);
        }
    }

    private void filterList(String query) {
        displayedVehicles.clear();
        for (DatabaseHelper.VehicleData v : allVehicles) {
            // Jika kategori kosong (Semua Iklan), filter hanya berdasarkan nama
            if (currentCategory.isEmpty()) {
                if (v.name.toLowerCase().contains(query.toLowerCase())) {
                    displayedVehicles.add(v);
                }
            } else {
                // Jika kategori dipilih, filter berdasarkan kategori DAN nama
                if (v.type.equalsIgnoreCase(currentCategory) && 
                    v.name.toLowerCase().contains(query.toLowerCase())) {
                    displayedVehicles.add(v);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void filterByType(String type) {
        currentCategory = type;
        
        // Use string resource for category name in header
        String displayType = type;
        if (type.equalsIgnoreCase("Mobil")) {
            displayType = getString(R.string.car);
        } else if (type.equalsIgnoreCase("Motor")) {
            displayType = getString(R.string.motorcycle);
        } else if (type.equalsIgnoreCase("Lain-lain")) {
            displayType = getString(R.string.others);
        }
        
        tvSelectedCategory.setText(displayType.toUpperCase());
        llCategorySelection.setVisibility(View.GONE);
        llContent.setVisibility(View.VISIBLE);

        displayedVehicles.clear();
        for (DatabaseHelper.VehicleData v : allVehicles) {
            if (v.type.equalsIgnoreCase(type)) {
                displayedVehicles.add(v);
            }
        }
        adapter.notifyDataSetChanged();
    }
}