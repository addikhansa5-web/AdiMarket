package com.example.adimarket;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class BeliKendaraanActivity extends AppCompatActivity {

    private EditText etCariKendaraan;
    private Button btnMobil, btnMotor, btnTruk;
    private Button btnHubungiBRV, btnHubungiPCX;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beli_kendaraan);

        // Inisialisasi komponen
        etCariKendaraan = findViewById(R.id.etCariKendaraan);
        btnMobil = findViewById(R.id.btnMobil);
        btnMotor = findViewById(R.id.btnMotor);
        btnTruk = findViewById(R.id.btnTruk);
        btnHubungiBRV = findViewById(R.id.btnHubungiBRV);
        btnHubungiPCX = findViewById(R.id.btnHubungiPCX);

        // Filter button listeners
        btnMobil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(BeliKendaraanActivity.this, "Filter: Mobil", Toast.LENGTH_SHORT).show();
            }
        });

        btnMotor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(BeliKendaraanActivity.this, "Filter: Motor", Toast.LENGTH_SHORT).show();
            }
        });

        btnTruk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(BeliKendaraanActivity.this, "Filter: Truk", Toast.LENGTH_SHORT).show();
            }
        });

        // Hubungi Penjual BRV
        btnHubungiBRV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = "6281234567890"; // Ganti dengan nomor penjual
                String message = "Halo, saya tertarik dengan BRV Rp. 190.000.000";
                openWhatsApp(phoneNumber, message);
            }
        });

        // Hubungi Penjual PCX
        btnHubungiPCX.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = "6281234567890"; // Ganti dengan nomor penjual
                String message = "Halo, saya tertarik dengan PCX Rp. 20.000.000";
                openWhatsApp(phoneNumber, message);
            }
        });
    }

    // Method untuk membuka WhatsApp
    private void openWhatsApp(String phoneNumber, String message) {
        try {
            String url = "https://wa.me/" + phoneNumber + "?text=" + Uri.encode(message);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp tidak terinstall", Toast.LENGTH_SHORT).show();
        }
    }
}