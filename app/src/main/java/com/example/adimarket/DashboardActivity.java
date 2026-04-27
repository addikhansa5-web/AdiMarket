package com.example.adimarket;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Tombol Jual Kendaraan
        View btnJual = findViewById(R.id.btnJual);
        if (btnJual != null) {
            btnJual.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(DashboardActivity.this, JualKendaraanActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Gagal membuka menu Jual", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Tombol Beli Kendaraan
        View btnBeli = findViewById(R.id.btnBeli);
        if (btnBeli != null) {
            btnBeli.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(DashboardActivity.this, BeliKendaraanActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Gagal membuka menu Beli", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Tombol Admin Approval (Dengan Password)
        View btnAdmin = findViewById(R.id.btnAdmin);
        if (btnAdmin != null) {
            btnAdmin.setOnClickListener(v -> {
                showAdminPasswordDialog();
            });
        }

        // Tombol Analisis Penjualan (Grafik)
        View btnRating = findViewById(R.id.btnRating);
        if (btnRating != null) {
            btnRating.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(DashboardActivity.this, SalesAnalysisActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Gagal membuka menu Analisis", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Tombol AI Chatbot
        View btnChatbot = findViewById(R.id.btnChatbot);
        if (btnChatbot != null) {
            btnChatbot.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(DashboardActivity.this, ChatbotActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Gagal membuka Chatbot", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showAdminPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Akses Admin");
        builder.setMessage("Masukkan password untuk masuk:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Password");
        builder.setView(input);

        builder.setPositiveButton("MASUK", (dialog, which) -> {
            String password = input.getText().toString();
            if (password.equals("123")) {
                try {
                    Intent intent = new Intent(DashboardActivity.this, AdminApprovalActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Gagal membuka menu Admin", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Password Salah! Khusus Admin.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("BATAL", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
