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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCRActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int CAMERA_PERMISSION_CODE = 101;

    private ImageView ivDocument;
    private TextView tvNomorPolisi, tvNamaPemilik, tvJenisKendaraan, tvRawText;
    private Button btnCapture, btnExtract;

    private Bitmap documentImage;
    private TextRecognizer recognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        // Initialize views
        ivDocument = findViewById(R.id.ivDocument);
        tvNomorPolisi = findViewById(R.id.tvNomorPolisi);
        tvNamaPemilik = findViewById(R.id.tvNamaPemilik);
        tvJenisKendaraan = findViewById(R.id.tvJenisKendaraan);
        tvRawText = findViewById(R.id.tvRawText);
        btnCapture = findViewById(R.id.btnCapture);
        btnExtract = findViewById(R.id.btnExtract);

        // Initialize ML Kit Text Recognizer
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

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

        // Extract button
        btnExtract.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (documentImage != null) {
                    extractText();
                } else {
                    Toast.makeText(OCRActivity.this,
                            "Ambil foto dokumen terlebih dahulu",
                            Toast.LENGTH_SHORT).show();
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
            documentImage = (Bitmap) data.getExtras().get("data");
            ivDocument.setImageBitmap(documentImage);
            btnExtract.setEnabled(true);
        }
    }

    private void extractText() {
        InputImage image = InputImage.fromBitmap(documentImage, 0);

        recognizer.process(image)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text visionText) {
                        processTextResult(visionText);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(OCRActivity.this,
                                "Gagal ekstraksi teks: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void processTextResult(Text visionText) {
        String fullText = visionText.getText();
        tvRawText.setText("Teks Lengkap:\n" + fullText);

        // Extract specific information
        String nomorPolisi = extractNomorPolisi(fullText);
        String namaPemilik = extractNamaPemilik(fullText);
        String jenisKendaraan = extractJenisKendaraan(fullText);

        // Display extracted info
        tvNomorPolisi.setText("Nomor Polisi: " +
                (nomorPolisi != null ? nomorPolisi : "Tidak terdeteksi"));
        tvNamaPemilik.setText("Nama Pemilik: " +
                (namaPemilik != null ? namaPemilik : "Tidak terdeteksi"));
        tvJenisKendaraan.setText("Jenis Kendaraan: " +
                (jenisKendaraan != null ? jenisKendaraan : "Tidak terdeteksi"));
    }

    private String extractNomorPolisi(String text) {
        // Pattern untuk nomor polisi Indonesia (contoh: B 1234 XYZ)
        Pattern pattern = Pattern.compile("[A-Z]{1,2}\\s*\\d{1,4}\\s*[A-Z]{1,3}");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group().replaceAll("\\s+", " ").trim();
        }
        return null;
    }

    private String extractNamaPemilik(String text) {
        // Look for name after "Nama" keyword
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].toLowerCase().contains("nama")) {
                if (i + 1 < lines.length) {
                    return lines[i + 1].trim();
                }
            }
        }
        return null;
    }

    private String extractJenisKendaraan(String text) {
        String lowerText = text.toLowerCase();

        if (lowerText.contains("sepeda motor") || lowerText.contains("motor")) {
            return "Sepeda Motor";
        } else if (lowerText.contains("mobil") || lowerText.contains("penumpang")) {
            return "Mobil";
        } else if (lowerText.contains("truk") || lowerText.contains("truck")) {
            return "Truk";
        }

        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recognizer != null) {
            recognizer.close();
        }
    }
}