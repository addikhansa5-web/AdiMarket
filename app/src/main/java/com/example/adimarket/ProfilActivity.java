package com.example.adimarket;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ProfilActivity extends AppCompatActivity {

    private TextView tvAvatar, tvUsername, tvJoinDate;
    private EditText etEditNama, etEditPhone;
    private Button btnSimpan, btnLogout;
    private DatabaseHelper dbHelper;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profil);

        dbHelper    = new DatabaseHelper(this);
        session     = new SessionManager(this);

        tvAvatar    = findViewById(R.id.tvAvatar);
        tvUsername  = findViewById(R.id.tvProfileUsername);
        tvJoinDate  = findViewById(R.id.tvJoinDate);
        etEditNama  = findViewById(R.id.etEditNama);
        etEditPhone = findViewById(R.id.etEditPhone);
        btnSimpan   = findViewById(R.id.btnSimpanProfil);
        btnLogout   = findViewById(R.id.btnLogout);

        findViewById(R.id.btnBackProfil).setOnClickListener(v -> finish());

        loadProfile();

        btnSimpan.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> doLogout());

        View btnKotak = findViewById(R.id.btnKotakPenawaran);
        if (btnKotak != null) {
            btnKotak.setOnClickListener(v -> {
                startActivity(new Intent(ProfilActivity.this, OffersActivity.class));
            });
        }
    }

    private void loadProfile() {
        int userId = session.getUserId();
        DatabaseHelper.UserData user = userId > 0 ? dbHelper.getUserById(userId) : null;

        if (user != null) {
            // Avatar: inisial nama
            String inisial = user.fullName.isEmpty() ? "?" :
                    String.valueOf(user.fullName.charAt(0)).toUpperCase();
            tvAvatar.setText(inisial);
            tvUsername.setText("@" + user.username);

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault());
            tvJoinDate.setText("Bergabung sejak " + sdf.format(new java.util.Date(user.createdAt)));

            etEditNama.setText(user.fullName);
            etEditPhone.setText(user.phone);
        } else {
            // Mode tamu
            tvAvatar.setText("👤");
            tvUsername.setText("@tamu");
            tvJoinDate.setText("Mode Tamu — Daftar untuk fitur penuh");
            etEditNama.setText(session.getFullName());
            etEditPhone.setText(session.getPhone());
            btnSimpan.setEnabled(false);
        }
    }

    private void saveProfile() {
        String nama  = etEditNama.getText().toString().trim();
        String phone = etEditPhone.getText().toString().trim();

        if (TextUtils.isEmpty(nama))  { etEditNama.setError("Nama wajib diisi");  return; }
        if (TextUtils.isEmpty(phone)) { etEditPhone.setError("HP wajib diisi"); return; }

        boolean ok = dbHelper.updateUserProfile(session.getUserId(), nama, phone);
        if (ok) {
            session.updateProfile(nama, phone);
            Toast.makeText(this, "✅ Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show();
            // Update avatar inisial
            tvAvatar.setText(String.valueOf(nama.charAt(0)).toUpperCase());
        } else {
            Toast.makeText(this, "Gagal menyimpan profil", Toast.LENGTH_SHORT).show();
        }
    }

    private void doLogout() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🚪 Logout")
            .setMessage("Yakin mau keluar dari akun " + session.getFullName() + "?")
            .setPositiveButton("Logout", (d, w) -> {
                session.logout();
                Toast.makeText(this, "Sampai jumpa! 👋", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            })
            .setNegativeButton("Batal", null)
            .show();
    }
}
