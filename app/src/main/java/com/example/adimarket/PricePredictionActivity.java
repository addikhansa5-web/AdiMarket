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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PricePredictionActivity extends AppCompatActivity {

    private EditText etTahun, etKilometer;
    private Spinner spKondisi, spJenisKendaraan;
    private Button btnPredict;
    private TextView tvPredictedPrice;

    private Interpreter tflite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_price_prediction);

        // Initialize views
        etTahun = findViewById(R.id.etTahun);
        etKilometer = findViewById(R.id.etKilometer);
        spKondisi = findViewById(R.id.spKondisi);
        spJenisKendaraan = findViewById(R.id.spJenisKendaraan);
        btnPredict = findViewById(R.id.btnPredict);
        tvPredictedPrice = findViewById(R.id.tvPredictedPrice);

        // Setup spinners
        setupSpinners();

        // Load TensorFlow Lite model
        try {
            tflite = new Interpreter(FileUtil.loadMappedFile(this, "price_prediction_model.tflite"));
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading model", Toast.LENGTH_SHORT).show();
        }

        // Predict button
        btnPredict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                predictPrice();
            }
        });
    }

    private void setupSpinners() {
        // Kondisi kendaraan
        String[] kondisiArray = {"Baru", "Sangat Baik", "Baik", "Cukup", "Perlu Perbaikan"};
        ArrayAdapter<String> kondisiAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, kondisiArray);
        kondisiAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spKondisi.setAdapter(kondisiAdapter);

        // Jenis kendaraan
        String[] jenisArray = {"Mobil", "Motor", "Truk"};
        ArrayAdapter<String> jenisAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, jenisArray);
        jenisAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spJenisKendaraan.setAdapter(jenisAdapter);
    }

    private void predictPrice() {
        // Validate inputs
        if (etTahun.getText().toString().isEmpty() ||
                etKilometer.getText().toString().isEmpty()) {
            Toast.makeText(this, "Lengkapi semua field", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Get input values
            int tahun = Integer.parseInt(etTahun.getText().toString());
            int kilometer = Integer.parseInt(etKilometer.getText().toString());
            int kondisi = spKondisi.getSelectedItemPosition();
            int jenisKendaraan = spJenisKendaraan.getSelectedItemPosition();

            // Normalize inputs
            float normalizedTahun = (tahun - 2000) / 25.0f; // Normalize year
            float normalizedKm = kilometer / 300000.0f; // Normalize km
            float normalizedKondisi = kondisi / 4.0f; // Normalize condition (0-4)
            float normalizedJenis = jenisKendaraan / 2.0f; // Normalize type (0-2)

            // Prepare input buffer
            ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * 4); // 4 inputs * 4 bytes
            inputBuffer.order(ByteOrder.nativeOrder());
            inputBuffer.putFloat(normalizedTahun);
            inputBuffer.putFloat(normalizedKm);
            inputBuffer.putFloat(normalizedKondisi);
            inputBuffer.putFloat(normalizedJenis);

            // Prepare output buffer
            ByteBuffer outputBuffer = ByteBuffer.allocateDirect(4); // 1 output * 4 bytes
            outputBuffer.order(ByteOrder.nativeOrder());

            // Run inference
            tflite.run(inputBuffer, outputBuffer);

            // Get prediction
            outputBuffer.rewind();
            float predictedPrice = outputBuffer.getFloat();

            // Denormalize price (assuming model output is normalized)
            long finalPrice = (long) (predictedPrice * 500000000); // Scale back to rupiah

            // Display result
            tvPredictedPrice.setText(String.format("Estimasi Harga:\nRp %,d", finalPrice));
            tvPredictedPrice.setVisibility(View.VISIBLE);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Input tidak valid", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tflite != null) {
            tflite.close();
        }
    }
}