package com.example.adimarket;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName, etUsername, etPhone, etPassword, etConfirm;
    private Button btnDaftar;
    private TextView tvLogin;
    private ProgressBar progressBar;
    private DatabaseHelper dbHelper;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbHelper    = new DatabaseHelper(this);
        session     = new SessionManager(this);
        etFullName  = findViewById(R.id.etFullName);
        etUsername  = findViewById(R.id.etRegUsername);
        etPhone     = findViewById(R.id.etPhone);
        etPassword  = findViewById(R.id.etRegPassword);
        etConfirm   = findViewById(R.id.etConfirmPassword);
        btnDaftar   = findViewById(R.id.btnDaftar);
        tvLogin     = findViewById(R.id.tvSudahPunya);
        progressBar = findViewById(R.id.progressRegister);

        btnDaftar.setOnClickListener(v -> doRegister());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void doRegister() {
        String nama     = etFullName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String phone    = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm  = etConfirm.getText().toString().trim();

        if (TextUtils.isEmpty(nama))     { etFullName.setError("Nama wajib diisi"); return; }
        if (TextUtils.isEmpty(username)) { etUsername.setError("Username wajib diisi"); return; }
        if (username.length() < 4)       { etUsername.setError("Minimal 4 karakter"); return; }
        if (TextUtils.isEmpty(phone))    { etPhone.setError("Nomor HP wajib diisi"); return; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Password wajib diisi"); return; }
        if (password.length() < 6)       { etPassword.setError("Minimal 6 karakter"); return; }
        if (!password.equals(confirm))   { etConfirm.setError("Password tidak cocok"); return; }

        progressBar.setVisibility(View.VISIBLE);
        btnDaftar.setEnabled(false);

        long newId = dbHelper.registerUser(username, password, nama, phone);

        progressBar.setVisibility(View.GONE);
        btnDaftar.setEnabled(true);

        if (newId != -1) {
            session.saveSession((int) newId, username, nama, phone);
            Toast.makeText(this, "✅ Akun berhasil dibuat! Selamat datang, " + nama + " 🎉", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, DashboardActivity.class));
            finishAffinity();
        } else {
            Toast.makeText(this, "❌ Username sudah dipakai, coba yang lain.", Toast.LENGTH_SHORT).show();
            etUsername.setError("Username sudah terdaftar");
        }
    }
}
