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

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvDaftar, tvGuest, tvForgotPassword;
    private ProgressBar progressBar;
    private DatabaseHelper dbHelper;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Cek sesi — langsung ke Dashboard jika sudah login
        session = new SessionManager(this);
        if (session.isLoggedIn()) {
            goDashboard();
            return;
        }

        setContentView(R.layout.activity_login);
        dbHelper    = new DatabaseHelper(this);
        etUsername  = findViewById(R.id.etUsername);
        etPassword  = findViewById(R.id.etPassword);
        btnLogin    = findViewById(R.id.btnLogin);
        tvDaftar    = findViewById(R.id.tvDaftar);
        tvGuest     = findViewById(R.id.tvGuest);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        progressBar = findViewById(R.id.progressLogin);

        btnLogin.setOnClickListener(v -> doLogin());

        tvDaftar.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class)));

        tvForgotPassword.setOnClickListener(v ->
            startActivity(new Intent(this, ForgotPasswordActivity.class)));

        tvGuest.setOnClickListener(v -> {
            // Mode tamu: simpan sesi minimal
            session.saveSession(0, "tamu", "Pengguna Tamu", "");
            goDashboard();
        });

        // Demo: isi otomatis untuk kemudahan testing
        etUsername.setHint("Username / Email");
        etPassword.setHint("Password");
    }

    private void doLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) { etUsername.setError("Username wajib diisi"); return; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Password wajib diisi"); return; }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        DatabaseHelper.UserData user = dbHelper.loginUser(username, password);

        progressBar.setVisibility(View.GONE);
        btnLogin.setEnabled(true);

        if (user != null) {
            session.saveSession(user.id, user.username, user.fullName, user.phone);
            NotificationHelper.notifyWelcome(this);
            Toast.makeText(this, "Selamat datang, " + user.fullName + "! 👋", Toast.LENGTH_SHORT).show();
            goDashboard();
        } else {
            Toast.makeText(this, "❌ Username atau password salah", Toast.LENGTH_SHORT).show();
            etPassword.setText("");
        }
    }

    private void goDashboard() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }
}
