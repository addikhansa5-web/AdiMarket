package com.example.adimarket;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.util.Log;
import android.widget.Toast;

public class DashboardActivity extends AppCompatActivity {

    private CardView cardJualKendaraan;
    private CardView cardBeliKendaraan;
    private CardView cardRiwayatTransaksi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Inisialisasi CardView
        cardJualKendaraan = findViewById(R.id.cardJualKendaraan);
        cardBeliKendaraan = findViewById(R.id.cardBeliKendaraan);
        cardRiwayatTransaksi = findViewById(R.id.cardRiwayatTransaksi);

        // Set Click Listener untuk Jual Kendaraan
        cardJualKendaraan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Dashboard", "Tombol Jual Kendaraan diklik");
                Toast.makeText(DashboardActivity.this, "Membuka Jual Kendaraan", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(DashboardActivity.this, JualKendaraanActivity.class);
                startActivity(intent);
            }
        });

        // Set Click Listener untuk Beli Kendaraan
        cardBeliKendaraan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, BeliKendaraanActivity.class);
                startActivity(intent);
            }
        });

        // Set Click Listener untuk Riwayat Transaksi
        cardRiwayatTransaksi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, RiwayatTransaksiActivity.class);
                startActivity(intent);
            }
        });
    }
}