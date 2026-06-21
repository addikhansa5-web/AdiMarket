package com.example.adimarket;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etUsername, etPhone, etNewPassword, etConfirmNewPassword;
    private Button btnResetPassword;
    private TextView tvBackToLogin;
    private ProgressBar progressBar;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        dbHelper             = new DatabaseHelper(this);
        etUsername           = findViewById(R.id.etForgotUsername);
        etPhone              = findViewById(R.id.etForgotPhone);
        etNewPassword        = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        btnResetPassword     = findViewById(R.id.btnResetPassword);
        tvBackToLogin        = findViewById(R.id.tvBackToLogin);
        progressBar          = findViewById(R.id.progressForgot);

        btnResetPassword.setOnClickListener(v -> doResetPassword());
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void doResetPassword() {
        String username    = etUsername.getText().toString().trim();
        String phone       = etPhone.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirm     = etConfirmNewPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) { etUsername.setError("Username wajib diisi"); return; }
        if (TextUtils.isEmpty(phone))    { etPhone.setError("Nomor HP wajib diisi"); return; }
        if (TextUtils.isEmpty(newPassword)) { etNewPassword.setError("Password baru wajib diisi"); return; }
        if (newPassword.length() < 6)    { etNewPassword.setError("Minimal 6 karakter"); return; }
        if (!newPassword.equals(confirm)) { etConfirmNewPassword.setError("Password tidak cocok"); return; }

        progressBar.setVisibility(View.VISIBLE);
        btnResetPassword.setEnabled(false);

        // Periksa apakah username dan nomor HP cocok di database
        boolean userExists = dbHelper.checkUserByUsernameAndPhone(username, phone);

        if (userExists) {
            // Lakukan reset password
            boolean success = dbHelper.resetPassword(username, phone, newPassword);
            progressBar.setVisibility(View.GONE);
            btnResetPassword.setEnabled(true);

            if (success) {
                Toast.makeText(this, "✅ Password berhasil diubah! Silakan masuk kembali.", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "❌ Gagal mereset password. Silakan coba lagi.", Toast.LENGTH_SHORT).show();
            }
        } else {
            progressBar.setVisibility(View.GONE);
            btnResetPassword.setEnabled(true);
            Toast.makeText(this, "❌ Username atau Nomor HP tidak terdaftar / cocok", Toast.LENGTH_LONG).show();
            etUsername.setError("Verifikasi data gagal");
            etPhone.setError("Verifikasi data gagal");
        }
    }
}
