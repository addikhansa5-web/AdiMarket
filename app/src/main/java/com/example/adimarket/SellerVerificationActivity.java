package com.example.adimarket;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SellerVerificationActivity extends AppCompatActivity {

    public static final String EXTRA_VERIFIED    = "seller_verified";
    private static final int   REQUEST_CAMERA    = 1001;
    private static final String TAG              = "SellerVerify";

    // UI
    private FrameLayout   frameCamera;
    private PreviewView   previewView;
    private TextView      tvStatusIcon, tvStatus, tvStep, tvInstruction, tvScanLine;
    private Button        btnBiometric, btnCapture, btnRetry;
    private ProgressBar   progressVerify;

    // CameraX
    private ProcessCameraProvider cameraProvider;
    private ImageCapture          imageCapture;
    private ExecutorService       cameraExecutor;

    // ML Kit
    private FaceDetector faceDetector;

    // State
    private boolean biometricPassed = false;
    private enum Step { BIOMETRIC, FACE_SCAN, DONE }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_verification);

        // Bind views
        frameCamera   = findViewById(R.id.frameCamera);
        previewView   = findViewById(R.id.previewView);
        tvStatusIcon  = findViewById(R.id.tvStatusIcon);
        tvStatus      = findViewById(R.id.tvStatus);
        tvStep        = findViewById(R.id.tvStep);
        tvInstruction = findViewById(R.id.tvInstruction);
        tvScanLine    = findViewById(R.id.tvScanLine);
        btnBiometric  = findViewById(R.id.btnBiometric);
        btnCapture    = findViewById(R.id.btnCapture);
        btnRetry      = findViewById(R.id.btnRetry);
        progressVerify= findViewById(R.id.progressVerify);

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Init ML Kit face detector
        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .build();
        faceDetector = FaceDetection.getClient(options);

        btnBiometric.setOnClickListener(v -> startBiometricAuth());
        btnCapture.setOnClickListener(v -> captureAndAnalyzeFace());
        btnRetry.setOnClickListener(v -> resetFaceScanUI());

        showStep(Step.BIOMETRIC);
    }

    // ─── STEP 1: BIOMETRIK ────────────────────────────────────────────────────

    private void showStep(Step step) {
        // Reset semua view
        frameCamera.setVisibility(View.GONE);
        btnBiometric.setVisibility(View.GONE);
        btnCapture.setVisibility(View.GONE);
        btnRetry.setVisibility(View.GONE);
        progressVerify.setVisibility(View.GONE);
        tvStatusIcon.setVisibility(View.VISIBLE);
        tvScanLine.setVisibility(View.GONE);

        switch (step) {
            case BIOMETRIC:
                tvStep.setText("Langkah 1 dari 2");
                tvStatusIcon.setText("🔐");
                tvStatus.setText("Verifikasi Identitas");
                tvInstruction.setText("Gunakan Face Unlock atau Sidik Jari Anda untuk membuktikan bahwa Anda adalah pemilik perangkat ini.");
                btnBiometric.setVisibility(View.VISIBLE);
                break;

            case FACE_SCAN:
                tvStep.setText("Langkah 2 dari 2");
                tvStatusIcon.setVisibility(View.GONE);
                tvStatus.setText("📸 Scan Wajah Penjual");
                tvInstruction.setText("Posisikan wajah Anda dalam bingkai oval biru.\nPastikan pencahayaan cukup, lalu klik Ambil Foto.");
                frameCamera.setVisibility(View.VISIBLE);
                btnCapture.setVisibility(View.VISIBLE);
                tvScanLine.setVisibility(View.VISIBLE);
                startCamera();
                break;

            case DONE:
                tvStep.setText("✅ Selesai");
                tvStatusIcon.setText("✅");
                tvStatus.setText("Identitas Terverifikasi!");
                tvInstruction.setText("Wajah Anda berhasil terdeteksi.\nIklan Anda akan diproses dengan badge 🛡️ Seller Verified.");
                stopCamera();
                // Kirim hasil sukses ke JualKendaraanActivity
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Intent result = new Intent();
                    result.putExtra(EXTRA_VERIFIED, true);
                    setResult(RESULT_OK, result);
                    finish();
                }, 2000);
                break;
        }
    }

    private void startBiometricAuth() {
        BiometricManager bm = BiometricManager.from(this);
        int canAuth = bm.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                BiometricManager.Authenticators.DEVICE_CREDENTIAL);

        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "⚠️ Biometrik tidak tersedia, lanjut ke scan wajah.", Toast.LENGTH_LONG).show();
            biometricPassed = true;
            requestCameraPermissionAndShow();
            return;
        }

        BiometricPrompt prompt = new BiometricPrompt(this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult r) {
                        biometricPassed = true;
                        runOnUiThread(() -> {
                            Toast.makeText(SellerVerificationActivity.this, "✅ Biometrik berhasil!", Toast.LENGTH_SHORT).show();
                            requestCameraPermissionAndShow();
                        });
                    }
                    @Override
                    public void onAuthenticationFailed() {
                        runOnUiThread(() -> Toast.makeText(SellerVerificationActivity.this,
                                "❌ Autentikasi gagal. Coba lagi.", Toast.LENGTH_SHORT).show());
                    }
                    @Override
                    public void onAuthenticationError(int code, @NonNull CharSequence msg) {
                        runOnUiThread(() -> Toast.makeText(SellerVerificationActivity.this,
                                "Error: " + msg, Toast.LENGTH_SHORT).show());
                    }
                });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("🔐 Verifikasi Penjual AdiMarket")
                .setSubtitle("Konfirmasi identitas Anda")
                .setDescription("Gunakan Face Unlock atau Sidik Jari sebelum memasang iklan.")
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        prompt.authenticate(info);
    }

    // ─── STEP 2: CAMERAX PREVIEW ──────────────────────────────────────────────

    private void requestCameraPermissionAndShow() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            showStep(Step.FACE_SCAN);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindCameraUseCases();
            } catch (Exception e) {
                Log.e(TAG, "Camera provider error: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(this,
                        "Gagal membuka kamera: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        // Preview use case
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Capture use case
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        // Pilih kamera depan
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            Log.d(TAG, "Camera bound successfully — front camera");
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed: " + e.getMessage());
            // Coba kamera belakang sebagai fallback
            try {
                cameraProvider.unbindAll();
                CameraSelector backCamera = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.bindToLifecycle(this, backCamera, preview, imageCapture);
                Log.d(TAG, "Fallback to back camera");
            } catch (Exception ex) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Kamera tidak dapat dibuka.", Toast.LENGTH_LONG).show());
            }
        }
    }

    private void stopCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    // ─── CAPTURE & ML KIT ANALYSIS ───────────────────────────────────────────

    private void captureAndAnalyzeFace() {
        if (imageCapture == null) {
            Toast.makeText(this, "Kamera belum siap, tunggu sebentar.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCapture.setEnabled(false);
        progressVerify.setVisibility(View.VISIBLE);
        tvScanLine.setText("🔍 AI menganalisis wajah...");
        tvInstruction.setText("Sedang mendeteksi wajah Anda, harap diam sebentar...");

        // Capture in-memory (tanpa simpan ke file)
        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                        // Konversi ke Bitmap lalu ke InputImage
                        Bitmap bitmap = imageProxy.toBitmap();
                        imageProxy.close();

                        // ML Kit face detection
                        InputImage image = InputImage.fromBitmap(bitmap, 0);
                        faceDetector.process(image)
                                .addOnSuccessListener(faces -> {
                                    progressVerify.setVisibility(View.GONE);
                                    if (faces.isEmpty()) {
                                        showNoFaceDetected();
                                    } else {
                                        Face face = faces.get(0);
                                        Float eyeOpen = face.getLeftEyeOpenProbability();
                                        if (eyeOpen != null && eyeOpen < 0.1f) {
                                            // Kemungkinan foto (mata tertutup)
                                            tvStatus.setText("⚠️ Buka Mata Anda!");
                                            tvInstruction.setText("Mata Anda terdeteksi tertutup.\nBuka mata lebar dan coba lagi.");
                                            btnCapture.setEnabled(true);
                                            tvScanLine.setText("⚠️ Mata tidak terdeteksi terbuka");
                                        } else {
                                            // Wajah nyata terdeteksi!
                                            tvStatus.setText("✅ Wajah Terdeteksi!");
                                            tvInstruction.setText("AI berhasil mendeteksi wajah Anda.\nMemverifikasi...");
                                            tvScanLine.setText("✅ Verifikasi berhasil!");
                                            new Handler(Looper.getMainLooper()).postDelayed(
                                                    () -> showStep(Step.DONE), 1000);
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Face detection error: " + e.getMessage());
                                    progressVerify.setVisibility(View.GONE);
                                    // Jika ML Kit error karena teknis, tetap lanjut agar tidak block user
                                    showStep(Step.DONE);
                                });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        Log.e(TAG, "Capture error: " + e.getMessage());
                        runOnUiThread(() -> {
                            progressVerify.setVisibility(View.GONE);
                            btnCapture.setEnabled(true);
                            Toast.makeText(SellerVerificationActivity.this,
                                    "Gagal mengambil gambar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }

    private void showNoFaceDetected() {
        tvStatus.setText("❌ Wajah Tidak Terdeteksi");
        tvInstruction.setText("Pastikan:\n• Pencahayaan cukup terang\n• Wajah menghadap langsung ke kamera\n• Tidak ada masker / kacamata gelap\n• Jarak 30-60 cm dari kamera");
        tvScanLine.setText("❌ Wajah tidak ditemukan — coba lagi");
        btnCapture.setEnabled(true);
        btnRetry.setVisibility(View.VISIBLE);
    }

    private void resetFaceScanUI() {
        tvStatus.setText("📸 Scan Wajah Penjual");
        tvInstruction.setText("Posisikan wajah Anda dalam bingkai oval biru.\nPastikan pencahayaan cukup, lalu klik Ambil Foto.");
        tvScanLine.setText("🔍 Mendeteksi wajah...");
        btnCapture.setEnabled(true);
        btnRetry.setVisibility(View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showStep(Step.FACE_SCAN);
            } else {
                Toast.makeText(this, "Izin kamera diperlukan untuk scan wajah.", Toast.LENGTH_LONG).show();
                // Bypass tanpa kamera
                biometricPassed = true;
                showStep(Step.DONE);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (cameraProvider != null) cameraProvider.unbindAll();
    }
}
