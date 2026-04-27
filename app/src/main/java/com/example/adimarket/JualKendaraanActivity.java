package com.example.adimarket;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.telephony.SmsManager;
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
    private RadioGroup rgTipe;
    private Button btnSimpan;
    private LinearLayout containerFoto;
    private HorizontalScrollView scrollFoto;
    private CardView cardUploadImage;
    private ImageView ivLogoDaerah;
    private DatabaseHelper dbHelper;
    private List<Uri> listUriFoto = new ArrayList<>();

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

        // Ambil lokasi dari Intent
        String kecamatan = getIntent().getStringExtra("kecamatan");
        updateLogoDaerah(kecamatan);

        cardUploadImage.setOnClickListener(v -> {
            // Membuka file picker untuk tipe image
            pickMultipleMedia.launch("image/*");
        });

        btnSimpan.setOnClickListener(v -> {
            Log.d("JualKendaraan", "Tombol Pasang Iklan diklik");
            simpanData();
        });
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
        scrollFoto.setVisibility(View.VISIBLE);
        ImageView imageView = new ImageView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(250, 250);
        params.setMargins(0, 0, 15, 0);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageURI(uri);
        imageView.setOnClickListener(v -> {
            containerFoto.removeView(imageView);
            listUriFoto.remove(uri);
            if (listUriFoto.isEmpty()) scrollFoto.setVisibility(View.GONE);
        });
        containerFoto.addView(imageView);
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

        Log.d("JualKendaraan", "Menyimpan ke DB: " + nama);
        long res = dbHelper.insertVehicle(nama, tipe, merek, tahun, harga, allImages.toString(), noTelpon, deskripsi);
        
        if (res != -1) {
            Toast.makeText(this, "Iklan Berhasil Disimpan! Mengirim ke Admin...", Toast.LENGTH_LONG).show();
            
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
}