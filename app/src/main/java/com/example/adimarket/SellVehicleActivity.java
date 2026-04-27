package com.example.adimarket;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

public class SellVehicleActivity extends AppCompatActivity {

    private EditText etNamaKendaraan, etMerek, etTahun, etKilometer;
    private Spinner spKondisi, spJenisKendaraan;
    private Button btnCheckPrice, btnPostAd;
    private TextView tvPriceSuggestion;
    private Interpreter tflite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sell_vehicle);

        // Inisialisasi
        etNamaKendaraan = findViewById(R.id.etNamaKendaraan);
        etMerek = findViewById(R.id.etMerek);
        etTahun = findViewById(R.id.etTahun);
        etKilometer = findViewById(R.id.etKilometer);
        spKondisi = findViewById(R.id.spKondisi);
        spJenisKendaraan = findViewById(R.id.spJenisKendaraan);
        btnCheckPrice = findViewById(R.id.btnCheckPrice);
        btnPostAd = findViewById(R.id.btnPostAd);
        tvPriceSuggestion = findViewById(R.id.tvPriceSuggestion);

        setupSpinners();
        loadModel();

        btnCheckPrice.setOnClickListener(v -> predictPrice());
        btnPostAd.setOnClickListener(v -> postAd());
    }

    private void setupSpinners() {
        String[] kondisi = {"Baru", "Sangat Baik", "Baik", "Cukup", "Perlu Perbaikan"};
        spKondisi.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, kondisi));

        String[] jenis = {"Mobil", "Motor", "Truk"};
        spJenisKendaraan.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, jenis));
    }

    private void loadModel() {
        try {
            tflite = new Interpreter(FileUtil.loadMappedFile(this, "price_prediction_model.tflite"));
        } catch (IOException e) {
            Toast.makeText(this, "Gagal memuat AI", Toast.LENGTH_SHORT).show();
        }
    }

    private void predictPrice() {
        try {
            int tahun = Integer.parseInt(etTahun.getText().toString());
            int km = Integer.parseInt(etKilometer.getText().toString());

            float nTahun = (tahun - 2000) / 25.0f;
            float nKm = km / 300000.0f;
            float nKon = spKondisi.getSelectedItemPosition() / 4.0f;
            float nJen = spJenisKendaraan.getSelectedItemPosition() / 2.0f;

            float[][] input = {{nTahun, nKm, nKon, nJen}};
            float[][] output = new float[1][1];

            if (tflite != null) {
                tflite.run(input, output);
                long price = (long) (output[0][0] * 500000000L);
                String formatted = NumberFormat.getCurrencyInstance(new Locale("id", "ID")).format(price);
                tvPriceSuggestion.setText("Saran Harga Jual AI: " + formatted);
                tvPriceSuggestion.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Mohon lengkapi data", Toast.LENGTH_SHORT).show();
        }
    }

    private void postAd() {
        // Simulasi berhasil posting
        Toast.makeText(this, "Iklan " + etNamaKendaraan.getText().toString() + " Berhasil Dipasang!", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        if (tflite != null) tflite.close();
        super.onDestroy();
    }
}
