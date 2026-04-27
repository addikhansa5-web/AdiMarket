package com.example.adimarket;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ImageRecognitionActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int CAMERA_PERMISSION_CODE = 101;

    private ImageView ivPreview;
    private TextView tvResult;
    private Button btnCapture, btnAnalyze;

    private Bitmap capturedImage;
    private Interpreter tflite;

    private String[] vehicleLabels = {"Mobil", "Motor", "Truk"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_recognition);

        // Initialize views
        ivPreview = findViewById(R.id.ivPreview);
        tvResult = findViewById(R.id.tvResult);
        btnCapture = findViewById(R.id.btnCapture);
        btnAnalyze = findViewById(R.id.btnAnalyze);

        // Load TensorFlow Lite model with optimization options
        try {
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4); // Use 4 CPU threads for faster inference
            options.setUseNNAPI(true); // Try to use hardware acceleration (NNAPI)
            
            tflite = new Interpreter(FileUtil.loadMappedFile(this, "vehicle_model.tflite"), options);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading model", Toast.LENGTH_SHORT).show();
        }

        // Capture button
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkCameraPermission()) {
                    openCamera();
                } else {
                    requestCameraPermission();
                }
            }
        });

        // Analyze button
        btnAnalyze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (capturedImage != null) {
                    analyzeImage();
                } else {
                    Toast.makeText(ImageRecognitionActivity.this,
                            "Ambil foto terlebih dahulu", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            capturedImage = (Bitmap) data.getExtras().get("data");
            ivPreview.setImageBitmap(capturedImage);
            btnAnalyze.setEnabled(true);
        }
    }

    private void analyzeImage() {
        if (capturedImage == null) return;

        // Resize image to model input size (224x224)
        Bitmap resizedImage = Bitmap.createScaledBitmap(capturedImage, 224, 224, true);

        // Convert to TensorImage
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(resizedImage);

        // Prepare output buffer
        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(
                new int[]{1, 3}, DataType.FLOAT32);

        // Run inference
        tflite.run(tensorImage.getBuffer(), outputBuffer.getBuffer());

        // Get results
        float[] results = outputBuffer.getFloatArray();

        // Find highest probability
        int maxIndex = 0;
        float maxProb = results[0];

        for (int i = 1; i < results.length; i++) {
            if (results[i] > maxProb) {
                maxProb = results[i];
                maxIndex = i;
            }
        }

        // Display result
        String detectedVehicle = vehicleLabels[maxIndex];
        float confidence = maxProb * 100;

        tvResult.setText(String.format("Terdeteksi: %s\nKonfiden: %.2f%%",
                detectedVehicle, confidence));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tflite != null) {
            tflite.close();
        }
    }
}