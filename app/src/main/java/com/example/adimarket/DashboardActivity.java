package com.example.adimarket;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.CheckBox;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private PersonalizationEngine personalizationEngine;
    private View rootDashboard;
    private TextView tvGreeting;
    private ImageView headerImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // 1. Hubungkan view ke ID terlebih dahulu
        rootDashboard = findViewById(R.id.rootDashboard);
        tvGreeting = findViewById(R.id.tvGreeting);
        headerImage = findViewById(R.id.headerImage);

        // Tampilkan nama user dari sesi
        SessionManager session = new SessionManager(this);
        String namaUser = session.getFullName();
        if (tvGreeting != null && !namaUser.isEmpty()) {
            tvGreeting.setText("Halo, " + namaUser + " 👋");
        }

        // Tombol profil/avatar
        View btnProfil = findViewById(R.id.btnProfil);
        if (btnProfil != null) {
            // Tampilkan inisial nama di avatar
            android.widget.TextView tvAvatarDash = findViewById(R.id.tvAvatarDash);
            if (tvAvatarDash != null && !namaUser.isEmpty()) {
                tvAvatarDash.setText(String.valueOf(namaUser.charAt(0)).toUpperCase());
            }
            btnProfil.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, ProfilActivity.class)));
        }

        // Tombol Dark Mode Toggle 🌙/☀️
        android.widget.TextView btnDarkMode = findViewById(R.id.btnDarkMode);
        if (btnDarkMode != null) {
            // Set ikon sesuai mode saat ini
            btnDarkMode.setText(DarkModeManager.isDark(this) ? "☀️" : "🌙");
            btnDarkMode.setOnClickListener(v -> {
                boolean nowDark = DarkModeManager.toggle(this);
                btnDarkMode.setText(nowDark ? "☀️" : "🌙");
                // Tidak perlu recreate() karena AppCompatDelegate handle sendiri
                android.widget.Toast.makeText(this,
                    nowDark ? "🌙 Dark Mode aktif" : "☀️ Light Mode aktif",
                    android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        // Restore saved wallpaper
        restoreSavedWallpaper();

        // Init notification channels (wajib dipanggil sekali)
        NotificationHelper.createChannels(this);

        // Request izin notifikasi (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 9999);
            }
        }

        // Wallpaper picker button
        ImageButton btnWallpaper = findViewById(R.id.btnWallpaper);
        if (btnWallpaper != null) {
            btnWallpaper.setOnClickListener(v -> showWallpaperPicker());
        }

        // 2. Baru jalankan Personalization Engine
        try {
            personalizationEngine = new PersonalizationEngine(this);
            applyPersonalizedUI();
        } catch (Exception e) {
            e.printStackTrace();
        }


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

        // Tombol Notifikasi 🔔 dengan badge
        View btnNotif = findViewById(R.id.btnNotifikasi);
        android.widget.TextView tvNotifBadge = findViewById(R.id.tvNotifBadge);
        if (btnNotif != null) {
            btnNotif.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, NotifikasiActivity.class));
            });
        }

        // Update live stats
        updateStats();

        // Animate cards
        animateCards();

        // Refresh badge notifikasi
        refreshNotifBadge();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNotifBadge(); // Update badge notifikasi
        updateStats();        // Update statistik dashboard
    }

    private void refreshNotifBadge() {
        android.widget.TextView badge = findViewById(R.id.tvNotifBadge);
        if (badge == null) return;
        int unread = new DatabaseHelper(this).getUnreadCount();
        if (unread > 0) {
            badge.setText(String.valueOf(unread > 99 ? "99+" : unread));
            badge.setVisibility(android.view.View.VISIBLE);
        } else {
            badge.setVisibility(android.view.View.GONE);
        }
    }

    private void showWallpaperPicker() {
        View sheet = LayoutInflater.from(this).inflate(R.layout.dialog_wallpaper_picker, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(sheet)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        int[] wpIds = { R.id.wpDefault, R.id.wpShowroom, R.id.wpHighway,
                        R.id.wpMountain, R.id.wpGarage, R.id.wpRacing };
        String[] wpKeys = { "default", "showroom", "highway", "mountain", "garage", "racing" };

        for (int i = 0; i < wpIds.length; i++) {
            final String key = wpKeys[i];
            View wp = sheet.findViewById(wpIds[i]);
            if (wp != null) {
                wp.setOnClickListener(v -> {
                    WallpaperHelper.save(this, key);
                    WallpaperHelper.applyWithAnim(this, headerImage);
                    Toast.makeText(this, "✅ Wallpaper semua halaman diubah!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }
        }

        dialog.show();
    }

    private void restoreSavedWallpaper() {
        WallpaperHelper.apply(this, headerImage);
    }

    private void updateStats() {
        try {
            DatabaseHelper db = new DatabaseHelper(this);
            int totalIklan = db.getAllVehicles().size();
            int totalPending = db.getPendingVehicles().size();

            TextView tvStatsIklan = findViewById(R.id.tvStatsIklan);
            TextView tvStatsPending = findViewById(R.id.tvStatsPending);

            if (tvStatsIklan != null) tvStatsIklan.setText("📦 " + totalIklan + " Iklan");
            if (tvStatsPending != null) tvStatsPending.setText("⏳ " + totalPending + " Pending");
        } catch (Exception ignored) {}
    }

    private void animateCards() {
        try {
            View[] cards = {
                    findViewById(R.id.btnJual),
                    findViewById(R.id.btnBeli),
                    findViewById(R.id.btnChatbot),
                    findViewById(R.id.btnRating),
                    findViewById(R.id.btnAdmin)
            };
            for (int i = 0; i < cards.length; i++) {
                if (cards[i] != null) {
                    cards[i].setAlpha(0f);
                    cards[i].setTranslationY(60f);
                    cards[i].animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(500)
                            .setStartDelay(i * 100L)
                            .start();
                }
            }
        } catch (Exception ignored) {}
    }

    private void applyPersonalizedUI() {
        if (personalizationEngine == null) return;
        
        try {
            PersonalizationEngine.DashboardCustomization customization = personalizationEngine.getCustomDashboard();
            
            // 1. Set Personalized Greeting
            if (tvGreeting != null) {
                SessionManager session = new SessionManager(this);
                String namaUser = session.getFullName();
                if (!namaUser.isEmpty()) {
                    tvGreeting.setText("Halo, " + namaUser + " 👋");
                }
            }

            // 2. Set Dynamic Background based on AI Theme
            if (rootDashboard != null) {
                int backgroundResId;
                switch (customization.colorTheme) {
                    case "blue":
                        backgroundResId = R.drawable.bg_gradient_mobil;
                        break;
                    case "red":
                        backgroundResId = R.drawable.bg_gradient_motor;
                        break;
                    case "green":
                        backgroundResId = R.drawable.bg_gradient_truk;
                        break;
                    default:
                        backgroundResId = R.drawable.bg_gradient_default;
                        break;
                }
                rootDashboard.setBackgroundResource(backgroundResId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAdminPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Akses Admin");

        // Container linear layout
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(60, 30, 60, 30);

        // Edit text input password
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint("Password");
        container.addView(input);

        // Baris kontrol (Lihat Password + Lupa Password)
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams controlParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        controlParams.setMargins(0, 20, 0, 0);
        controls.setLayoutParams(controlParams);

        // Checkbox lihat password
        CheckBox cbShow = new CheckBox(this);
        cbShow.setText("Lihat");
        cbShow.setTextSize(12);
        cbShow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            // Letakkan kursor di akhir teks
            input.setSelection(input.getText().length());
        });
        
        // Tombol Reset Password (Lupa)
        TextView tvReset = new TextView(this);
        tvReset.setText("Lupa Password?");
        tvReset.setTextColor(0xFFD32F2F);
        tvReset.setTextSize(12);
        tvReset.setPaintFlags(tvReset.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        tvReset.setGravity(android.view.Gravity.END);
        
        LinearLayout.LayoutParams lpShow = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        LinearLayout.LayoutParams lpReset = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        controls.addView(cbShow, lpShow);
        controls.addView(tvReset, lpReset);

        container.addView(controls);
        builder.setView(container);

        // Aksi Lupa Password
        tvReset.setOnClickListener(v -> {
            // Tutup dialog password utama dan buka dialog reset password
            Toast.makeText(this, "Silakan hubungi Administrator atau gunakan opsi reset.", Toast.LENGTH_SHORT).show();
            showResetPasswordDialog();
        });

        builder.setPositiveButton("MASUK", (dialog, which) -> {
            String password = input.getText().toString();
            // Cek password dari SharedPreferences (jika ada yang di-reset) atau default "123"
            String currentAdminPassword = getSharedPreferences("admin_prefs", MODE_PRIVATE)
                    .getString("admin_password", "123");

            if (password.equals(currentAdminPassword)) {
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

    private void showResetPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password Admin");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(60, 30, 60, 30);

        final EditText etSecret = new EditText(this);
        etSecret.setHint("Masukkan Kode Rahasia Admin (Default: adimarket)");
        etSecret.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        container.addView(etSecret);

        final EditText etNewPassword = new EditText(this);
        etNewPassword.setHint("Masukkan Password Baru");
        etNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 20, 0, 0);
        etNewPassword.setLayoutParams(params);
        container.addView(etNewPassword);

        builder.setView(container);

        builder.setPositiveButton("RESET", (dialog, which) -> {
            String secret = etSecret.getText().toString();
            String newPass = etNewPassword.getText().toString();

            if (secret.equals("adimarket")) {
                if (newPass.length() >= 3) {
                    getSharedPreferences("admin_prefs", MODE_PRIVATE)
                            .edit()
                            .putString("admin_password", newPass)
                            .apply();
                    Toast.makeText(this, "✅ Password Admin Berhasil Di-reset!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Password minimal 3 karakter!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Kode Rahasia Salah!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("BATAL", null);
        builder.show();
    }
}
