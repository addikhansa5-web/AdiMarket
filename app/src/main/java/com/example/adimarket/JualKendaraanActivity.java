package com.example.adimarket;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.telephony.SmsManager;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class JualKendaraanActivity extends AppCompatActivity {

    private EditText etNama, etHarga, etTahun, etMerek, etNoTelpon, etDeskripsi;
    private Spinner spinnerKota;
    private RadioGroup rgTipe;
    private Button btnSimpan;
    private LinearLayout containerFoto;
    private HorizontalScrollView scrollFoto;
    private CardView cardUploadImage;
    private ImageView ivLogoDaerah;
    private DatabaseHelper dbHelper;
    private List<Uri> listUriFoto = new ArrayList<>();
    private List<String> listHashFoto = new ArrayList<>();
    private VehicleClassifier vehicleClassifier;
    private FraudDetection fraudDetection;
    private boolean sellerVerified = false; // Flag verifikasi wajah
    private static final int REQUEST_SELLER_VERIFY = 2001;

    private final ActivityResultLauncher<String> pickMultipleMedia =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    for (Uri uri : uris) {
                        Uri localUri = copyImageToInternal(uri);
                        if (localUri != null) {
                            tambahFotoKeView(localUri);
                        }
                    }
                }
            });

    private Uri copyImageToInternal(Uri uri) {
        try {
            String fileName = "img_" + System.currentTimeMillis() + ".jpg";
            File file = new File(getFilesDir(), fileName);
            InputStream is = getContentResolver().openInputStream(uri);
            OutputStream os = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();
            os.close();
            is.close();
            return Uri.fromFile(file);
        } catch (Exception e) {
            Log.e("JualKendaraan", "Gagal salin foto: " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jual_kendaraan);
        WallpaperHelper.apply(this, findViewById(R.id.headerImage));

        dbHelper = new DatabaseHelper(this);
        etNama = findViewById(R.id.etNamaKendaraan);
        etHarga = findViewById(R.id.etHarga);
        etTahun = findViewById(R.id.etTahun);
        etMerek = findViewById(R.id.etMerek);
        etNoTelpon = findViewById(R.id.etNoTelpon);
        etDeskripsi = findViewById(R.id.etDeskripsi);
        rgTipe = findViewById(R.id.rgTipeKendaraan);
        btnSimpan = findViewById(R.id.btnUpload);
        cardUploadImage = findViewById(R.id.cardUploadImage);
        containerFoto = findViewById(R.id.containerFoto);
        scrollFoto = findViewById(R.id.scrollFoto);
        ivLogoDaerah = findViewById(R.id.ivLogoDaerahDinamis);

        // Setup spinner kota
        spinnerKota = findViewById(R.id.spinnerKota);
        String[] kotaList = {
            "-- Pilih Kota --",
            "Aceh", "Ambon", "Balikpapan", "Banda Aceh", "Bandar Lampung",
            "Bandung", "Banjarmasin", "Batam", "Bekasi", "Bengkulu",
            "Bogor", "Cirebon", "Denpasar", "Depok", "Jakarta",
            "Jambi", "Jayapura", "Kendari", "Kupang", "Makassar",
            "Malang", "Manado", "Mataram", "Medan", "Padang",
            "Palangka Raya", "Palembang", "Palu", "Pangkal Pinang",
            "Pekanbaru", "Pontianak", "Samarinda", "Semarang",
            "Serang", "Solo", "Sorong", "Surabaya", "Tangerang",
            "Tanjung Pinang", "Ternate", "Yogyakarta"
        };
        ArrayAdapter<String> kotaAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, kotaList);
        kotaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerKota.setAdapter(kotaAdapter);

        // ---- AI PRICE ADVISOR ----
        Button btnAIPriceAdvisor = findViewById(R.id.btnAIPriceAdvisor);
        if (btnAIPriceAdvisor != null) {
            btnAIPriceAdvisor.setOnClickListener(v -> {
                String nama  = etNama.getText().toString().trim();
                String merek = etMerek.getText().toString().trim();
                String tahunStr = etTahun.getText().toString().trim();
                if (nama.isEmpty() || merek.isEmpty() || tahunStr.isEmpty()) {
                    Toast.makeText(this, "Isi Nama, Merek, dan Tahun dulu ya!", Toast.LENGTH_SHORT).show();
                    return;
                }
                int tahun = 2020;
                try { tahun = Integer.parseInt(tahunStr); } catch (Exception ignored) {}

                // Deteksi tipe dari radio button
                String tipeKendaraan = "Kendaraan";
                int selectedId = rgTipe.getCheckedRadioButtonId();
                if (selectedId == R.id.rbMobil) tipeKendaraan = "Mobil";
                else if (selectedId == R.id.rbMotor) tipeKendaraan = "Motor";

                final String tipe = tipeKendaraan;
                final int yr = tahun;

                android.app.AlertDialog loadingDialog = new android.app.AlertDialog.Builder(this)
                        .setTitle("💡 AI Price Advisor")
                        .setMessage("Menganalisis harga pasar untuk " + merek + " " + nama + " tahun " + tahunStr + "...")
                        .setCancelable(false).create();
                loadingDialog.show();

                GroqHelper groqHelper = new GroqHelper(this);
                groqHelper.suggestPrice(merek, nama, tipe, yr, new GroqHelper.GroqCallback() {
                    @Override public void onSuccess(String response) {
                        runOnUiThread(() -> {
                            loadingDialog.dismiss();
                            try {
                                // Coba parse JSON: {"harga": 95000000, "deskripsi": "..."}
                                // Cari JSON object dalam response (bisa ada teks sebelum/sesudah)
                                int start = response.indexOf('{');
                                int end   = response.lastIndexOf('}');
                                String jsonStr = (start >= 0 && end > start)
                                        ? response.substring(start, end + 1)
                                        : response;

                                org.json.JSONObject json = new org.json.JSONObject(jsonStr);

                                // Isi Harga
                                if (json.has("harga")) {
                                    String hargaRaw = String.valueOf(json.get("harga"))
                                            .replaceAll("[^0-9]", "");
                                    if (!hargaRaw.isEmpty()) {
                                        etHarga.setText(hargaRaw);
                                    }
                                }

                                // Isi Deskripsi
                                if (json.has("deskripsi")) {
                                    String desc = json.getString("deskripsi");
                                    etDeskripsi.setText(desc);
                                }

                                String hargaDisplay = formatHargaDisplay(
                                        etHarga.getText().toString().trim());
                                Toast.makeText(JualKendaraanActivity.this,
                                        "✅ AI mengisi harga & deskripsi!\nHarga: Rp " + hargaDisplay,
                                        Toast.LENGTH_LONG).show();

                            } catch (Exception e) {
                                // Fallback: kalau JSON gagal, coba ambil angka saja
                                String cleaned = response.replaceAll("[^0-9]", "");
                                if (!cleaned.isEmpty()) {
                                    etHarga.setText(cleaned);
                                    Toast.makeText(JualKendaraanActivity.this,
                                            "✅ Harga: Rp " + formatHargaDisplay(cleaned),
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(JualKendaraanActivity.this,
                                            "AI: " + response, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    }
                    @Override public void onError(String error) {
                        runOnUiThread(() -> {
                            loadingDialog.dismiss();
                            Toast.makeText(JualKendaraanActivity.this,
                                    "Gagal mendapat saran AI: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
            });
        }

        // Initialize AI Components
        try {
            vehicleClassifier = new VehicleClassifier(this);
            fraudDetection = new FraudDetection(this);
        } catch (Exception e) {
            Log.e("JualKendaraan", "AI Init Error: " + e.getMessage());
        }

        // Ambil lokasi dari Intent
        String kecamatan = getIntent().getStringExtra("kecamatan");
        updateLogoDaerah(kecamatan);

        cardUploadImage.setOnClickListener(v -> {
            // Membuka file picker untuk tipe image
            pickMultipleMedia.launch("image/*");
        });

        btnSimpan.setOnClickListener(v -> {
            Log.d("JualKendaraan", "Tombol Pasang Iklan diklik");
            if (!sellerVerified) {
                // Wajibkan verifikasi wajah sebelum simpan
                new android.app.AlertDialog.Builder(this)
                    .setTitle("🛡️ Verifikasi Penjual Diperlukan")
                    .setMessage(
                        "Untuk keamanan marketplace, AdiMarket mewajibkan verifikasi identitas penjual sebelum iklan diproses.\n\n" +
                        "Proses ini meliputi:\n" +
                        "🔐 Step 1: Biometrik (Face Unlock / Sidik Jari)\n" +
                        "📸 Step 2: Scan wajah real-time\n\n" +
                        "Data Anda aman dan tidak dikirim ke server mana pun."
                    )
                    .setPositiveButton("Mulai Verifikasi", (d, w) -> {
                        Intent verifyIntent = new Intent(this, SellerVerificationActivity.class);
                        startActivityForResult(verifyIntent, REQUEST_SELLER_VERIFY);
                    })
                    .setNegativeButton("Batal", null)
                    .show();
            } else {
                simpanData();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELLER_VERIFY && resultCode == RESULT_OK) {
            sellerVerified = true;
            Toast.makeText(this, "✅ Verifikasi berhasil! Menyimpan iklan...", Toast.LENGTH_SHORT).show();
            // Langsung simpan setelah verifikasi berhasil
            new Handler(Looper.getMainLooper()).postDelayed(this::simpanData, 500);
        } else if (requestCode == REQUEST_SELLER_VERIFY) {
            Toast.makeText(this, "❌ Verifikasi dibatalkan. Iklan tidak disimpan.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLogoDaerah(String kecamatan) {
        if (ivLogoDaerah == null) return;

        if (kecamatan == null || kecamatan.isEmpty()) {
            ivLogoDaerah.setVisibility(View.GONE);
            return;
        }

        ivLogoDaerah.setVisibility(View.VISIBLE);
        String loc = kecamatan.toLowerCase();

        if (loc.contains("bekasi")) {
            ivLogoDaerah.setImageResource(R.drawable.ic_logo_bekasi);
        } else if (loc.contains("serang")) {
            ivLogoDaerah.setImageResource(R.drawable.ic_logo_serang);
        } else {
            ivLogoDaerah.setImageResource(R.drawable.ic_logo_daerah);
        }
    }

    private void tambahFotoKeView(Uri uri) {
        listUriFoto.add(uri);
        
        // AI: Calculate hash and check for duplicates (Protection against fraud)
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
            if (bitmap != null) {
                String hash = AIUtils.getBitmapHash(bitmap);
                listHashFoto.add(hash);
                
                if (dbHelper.isDuplicatePhoto(hash)) {
                    Toast.makeText(this, R.string.duplicate_photo_warning, Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            Log.e("JualKendaraan", "Hash calculation failed: " + e.getMessage());
        }

        // AI: Auto-classify vehicle from first photo (Point 4)
        if (listUriFoto.size() == 1) {
            runVehicleClassification(uri);
        }

        scrollFoto.setVisibility(View.VISIBLE);
        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(250, 250);
        params.setMargins(0, 0, 15, 0);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageURI(uri);
        imageView.setOnClickListener(v -> {
            int index = containerFoto.indexOfChild(imageView);
            if (index != -1 && index < listHashFoto.size()) {
                listHashFoto.remove(index);
            }
            containerFoto.removeView(imageView);
            listUriFoto.remove(uri);
            if (listUriFoto.isEmpty()) scrollFoto.setVisibility(View.GONE);
        });
        containerFoto.addView(imageView);
    }

    private void runVehicleClassification(Uri uri) {
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("AI Auto-Sell Aktif \u2728")
                .setMessage("Mohon tunggu, AI LLaMA-Vision sedang menganalisis foto kendaraan Anda untuk memprediksi spesifikasi, harga, dan menulis promosi...")
                .setCancelable(false)
                .create();
        dialog.show();

        try {
            InputStream is = getContentResolver().openInputStream(uri);
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
            if (bitmap != null) {
                GroqHelper groqHelper = new GroqHelper(this);
                groqHelper.analyzeVehicleImage(bitmap, new GroqHelper.GroqCallback() {
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(() -> {
                            if (dialog.isShowing()) dialog.dismiss();
                            try {
                                String cleanJson = response.replace("```json", "").replace("```", "").trim();
                                org.json.JSONObject data = new org.json.JSONObject(cleanJson);
                                
                                String type = data.optString("tipe", "Mobil");
                                updateTypeSelection(type);
                                
                                etNama.setText(data.optString("nama", ""));
                                etMerek.setText(data.optString("merek", ""));
                                etTahun.setText(data.optString("tahun", ""));
                                etHarga.setText(data.optString("harga", ""));
                                etDeskripsi.setText(data.optString("deskripsi", ""));
                                
                                Toast.makeText(JualKendaraanActivity.this, "Sukses! Data telah diisi otomatis oleh LLaMA Vision \u2728", Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Log.e("AI_DEBUG", "JSON Parsing error: " + e.getMessage() + " | Raw: " + response);
                                fallbackMockData("JSON Parse: " + e.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("AI_DEBUG", "Groq Error: " + error);
                        runOnUiThread(() -> {
                            if (dialog.isShowing()) dialog.dismiss();
                            if (error.equals("API_KEY_MISSING")) {
                                Toast.makeText(JualKendaraanActivity.this, "GROQ_API_KEY belum disetel! Menampilkan data simulasi...", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(JualKendaraanActivity.this, "Gagal koneksi AI: " + error, Toast.LENGTH_LONG).show();
                            }
                            fallbackMockData(error);
                        });
                    }
                });
            } else {
                Log.e("AI_DEBUG", "Bitmap is null");
                if (dialog.isShowing()) dialog.dismiss();
            }
        } catch (Exception e) {
            Log.e("AI_DEBUG", "Image error: " + e.getMessage());
            if (dialog.isShowing()) dialog.dismiss();
            fallbackMockData("Image error: " + e.getMessage());
        }
    }

    private void fallbackMockData(String reason) {
        updateTypeSelection("Mobil");
        etNama.setText("Civic Turbo CVT (Mock)");
        etMerek.setText("Honda");
        etTahun.setText("2020");
        etHarga.setText("350000000");
        etDeskripsi.setText("DIJUAL CEPAT \uD83D\uDE97\n\nHonda Civic Turbo 2020 CVT.\nKondisi sangat istimewa, mulus seperti baru! (Mock Data. Reason: " + reason + ")");
    }

    private void updateTypeSelection(String type) {
        if (type.equalsIgnoreCase("Mobil")) {
            rgTipe.check(R.id.rbMobil);
        } else if (type.equalsIgnoreCase("Motor")) {
            rgTipe.check(R.id.rbMotor);
        }
    }

    private void simpanData() {
        String nama = etNama.getText().toString().trim();
        String harga = etHarga.getText().toString().trim();
        String tahunStr = etTahun.getText().toString().trim();
        String merek = etMerek.getText().toString().trim();
        String noTelpon = etNoTelpon.getText().toString().trim();
        String deskripsi = etDeskripsi.getText().toString().trim();

        // Validasi minimal: Nama, Harga, dan No Telpon harus ada
        if (nama.isEmpty() || harga.isEmpty() || noTelpon.isEmpty()) {
            Toast.makeText(this, "Mohon lengkapi Nama, Harga, dan No Telpon", Toast.LENGTH_SHORT).show();
            return;
        }

        // ══════════════════════════════════════════════════
        // 🔒 SECURITY LAYER 4: Cegah Iklan Bercabang (Duplikat)
        // ══════════════════════════════════════════════════
        int selectedIdTemp = rgTipe.getCheckedRadioButtonId();
        String tipeTemp = "Mobil";
        if (selectedIdTemp != -1) {
            RadioButton rb = findViewById(selectedIdTemp);
            tipeTemp = rb.getText().toString();
        }

        if (dbHelper.isDuplicateListing(noTelpon, nama, tipeTemp)) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("🚫 Iklan Bercabang Terdeteksi")
                .setMessage(
                    "Anda sudah memiliki iklan dengan nama dan tipe kendaraan yang sama!\n\n" +
                    "📋 Kendaraan: " + nama + " (" + tipeTemp + ")\n" +
                    "📱 Nomor HP: " + noTelpon + "\n\n" +
                    "❌ Sistem AdiMarket melarang iklan duplikat untuk menjaga kualitas marketplace.\n\n" +
                    "💡 Silakan hapus iklan lama atau gunakan nama yang berbeda jika ini kendaraan berbeda."
                )
                .setPositiveButton("Mengerti", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
            return;
        }

        // ══════════════════════════════════════════════════
        // 🔒 SECURITY LAYER 5: Rate Limiter (Max 3 Iklan per HP)
        // ══════════════════════════════════════════════════
        int activeCount = dbHelper.countActiveListingsByPhone(noTelpon);
        if (activeCount >= 3) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("⏱️ Batas Iklan Tercapai")
                .setMessage(
                    "Nomor HP Anda sudah memiliki " + activeCount + " iklan aktif.\n\n" +
                    "📌 Batas maksimal: 3 iklan per nomor HP\n\n" +
                    "Untuk memasang iklan baru, silakan:\n" +
                    "1️⃣ Hapus iklan lama yang sudah terjual\n" +
                    "2️⃣ Tunggu admin menyelesaikan review iklan sebelumnya\n\n" +
                    "Kebijakan ini melindungi pembeli dari spam iklan."
                )
                .setPositiveButton("Oke", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
            return;
        }

        // ══════════════════════════════════════════════════
        // 🔒 SECURITY LAYER 6: Content Similarity (Anti-Spam Deskripsi)
        // ══════════════════════════════════════════════════
        if (!deskripsi.isEmpty() && dbHelper.hasSimilarDescription(noTelpon, deskripsi)) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("📝 Deskripsi Terlalu Mirip")
                .setMessage(
                    "Sistem AI kami mendeteksi deskripsi iklan Anda sangat mirip dengan iklan Anda sebelumnya (>75% kesamaan).\n\n" +
                    "❌ Ini terindikasi sebagai upaya posting iklan duplikat.\n\n" +
                    "💡 Tips:\n" +
                    "• Tulis deskripsi yang berbeda dan lebih spesifik\n" +
                    "• Sebutkan kondisi detail kendaraan ini\n" +
                    "• Tambahkan fitur unggulan yang unik"
                )
                .setPositiveButton("Ubah Deskripsi", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
            return;
        }

        // ══════════════════════════════════════════════════
        // 🔒 AI: Fraud Detection Analysis
        // ══════════════════════════════════════════════════
        if (fraudDetection != null) {
            double priceVal = 0;
            try { priceVal = Double.parseDouble(harga.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}

            int selectedId = rgTipe.getCheckedRadioButtonId();
            String tipe = "Mobil";
            if (selectedId != -1) {
                RadioButton rbSelected = findViewById(selectedId);
                tipe = rbSelected.getText().toString();
            }

            FraudDetection.VehicleListing listing = new FraudDetection.VehicleListing(
                    "new", "user_1", tipe, merek, tahunStr.isEmpty() ? 2024 : Integer.parseInt(tahunStr),
                    priceVal, deskripsi, noTelpon, "", listUriFoto.size()
            );

            FraudDetection.FraudResult fraudResult = fraudDetection.analyzeListing(listing);
            if (fraudResult.isFraudulent) {
                // Blokir iklan dengan risiko tinggi
                new android.app.AlertDialog.Builder(this)
                    .setTitle("🛡️ Iklan Diblokir oleh AI Trust Engine")
                    .setMessage(
                        "Iklan Anda tidak dapat diproses karena terindikasi berisiko tinggi:\n\n" +
                        "⚠️ Level: " + fraudResult.trustLevel + "\n" +
                        "📋 Alasan: " + (fraudResult.reasons.isEmpty() ? "Informasi tidak lengkap / mencurigakan" : fraudResult.reasons) + "\n\n" +
                        "💡 Solusi:\n" +
                        "• Pastikan harga sesuai pasaran\n" +
                        "• Lengkapi semua informasi kendaraan\n" +
                        "• Unggah minimal 1 foto nyata kendaraan\n" +
                        "• Gunakan nomor HP aktif yang valid"
                    )
                    .setPositiveButton("Perbaiki Iklan", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
                Log.w("FraudDetection", "BLOCKED: " + fraudResult.trustLevel + " - " + fraudResult.reasons);
                return; // Hentikan proses simpan
            }
        }

        int selectedId = rgTipe.getCheckedRadioButtonId();
        String tipe = "Mobil"; // Default
        if (selectedId != -1) {
            RadioButton rbSelected = findViewById(selectedId);
            tipe = rbSelected.getText().toString();
        }

        int tahun = 0;
        try {
            if (!tahunStr.isEmpty()) tahun = Integer.parseInt(tahunStr);
        } catch (Exception e) {
            tahun = 2024;
        }
        
        StringBuilder allImages = new StringBuilder();
        for (int i = 0; i < listUriFoto.size(); i++) {
            allImages.append(listUriFoto.get(i).toString());
            if (i < listUriFoto.size() - 1) {
                allImages.append(",");
            }
        }

        StringBuilder allHashes = new StringBuilder();
        for (int i = 0; i < listHashFoto.size(); i++) {
            allHashes.append(listHashFoto.get(i));
            if (i < listHashFoto.size() - 1) {
                allHashes.append(",");
            }
        }

        Log.d("JualKendaraan", "Menyimpan ke DB: " + nama);
        String selectedKota = spinnerKota.getSelectedItemPosition() > 0
                ? spinnerKota.getSelectedItem().toString() : "";
        long res = dbHelper.insertVehicle(nama, tipe, merek, tahun, harga, allImages.toString(), noTelpon, deskripsi, allHashes.toString(), selectedKota);

        
        if (res != -1) {
            Toast.makeText(this, "Iklan Berhasil Disimpan! Mengirim ke Admin...", Toast.LENGTH_LONG).show();

            // 🔔 Notifikasi untuk Admin: ada iklan baru menunggu review
            NotificationHelper.notifyIklanBaru(this, nama, tipe);

            // Kirim ke WhatsApp
            kirimWA(nama, harga, tipe, deskripsi, noTelpon, tahunStr, listUriFoto.size());

            // Tutup halaman setelah sukses
            new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2500);
        } else {
            Toast.makeText(this, "Gagal menyimpan data ke Database", Toast.LENGTH_SHORT).show();
        }
    }

    private void kirimWA(String n, String h, String t, String d, String p, String thn, int jmlFoto) {
        String pesan = "🤖 *IKLAN BARU (" + jmlFoto + " FOTO)*\n" +
                "------------------------------------------\n" +
                "Halo Pemilik Adi Market, unit baru masuk dari penjual:\n\n" +
                "🚗 *Unit:* " + n + "\n" +
                "💰 *Harga:* Rp " + h + "\n" +
                "📅 *Tahun:* " + thn + "\n" +
                "🏷️ *Tipe:* " + t + "\n\n" +
                "📝 *Deskripsi:*\n" + d + "\n\n" +
                "📞 *Kontak Penjual:* " + p;
        
        // Nomor tujuan Pemilik Adi Market: 087837566610
        String url = "https://api.whatsapp.com/send?phone=6287837566610&text=" + Uri.encode(pesan);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setPackage("com.whatsapp");
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }

    private String formatHargaDisplay(String angka) {
        try {
            long val = Long.parseLong(angka.replaceAll("[^0-9]", ""));
            if (val >= 1_000_000_000) return String.format("%.1f Miliar", val / 1_000_000_000.0);
            if (val >= 1_000_000) return String.format("%.0f Juta", val / 1_000_000.0);
            return String.format("%,d", val);
        } catch (Exception e) { return angka; }
    }
}