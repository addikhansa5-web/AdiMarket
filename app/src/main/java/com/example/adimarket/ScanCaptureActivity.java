package com.example.adimarket;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanCaptureActivity extends AppCompatActivity {
    private static final String TAG = "ScanCapture";
    private static final int REQUEST_CAMERA_PERMISSION = 2002;
    private static final int REQUEST_GALLERY = 2003;

    private PreviewView previewView;
    private ScannerOverlayView scannerOverlay;
    private FrameLayout btnShutter;
    private ImageButton btnBack, btnGallery, btnFlash;

    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private Camera camera;
    private ExecutorService cameraExecutor;
    private boolean isFlashOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_capture);

        // Bind views
        previewView = findViewById(R.id.previewView);
        scannerOverlay = findViewById(R.id.scannerOverlay);
        btnShutter = findViewById(R.id.btnShutter);
        btnBack = findViewById(R.id.btnBack);
        btnGallery = findViewById(R.id.btnGallery);
        btnFlash = findViewById(R.id.btnFlash);

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Click listeners
        btnBack.setOnClickListener(v -> finish());
        btnGallery.setOnClickListener(v -> openGallery());
        btnFlash.setOnClickListener(v -> toggleFlash());
        btnShutter.setOnClickListener(v -> captureImage());

        // Check camera permission
        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Izin kamera diperlukan untuk fitur pindai.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (Exception e) {
                Log.e(TAG, "Gagal memulai kamera: " + e.getMessage());
                Toast.makeText(this, "Gagal memulai kamera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        } catch (Exception e) {
            Log.e(TAG, "Gagal mengaitkan use case kamera: " + e.getMessage());
        }
    }

    private void toggleFlash() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            isFlashOn = !isFlashOn;
            camera.getCameraControl().enableTorch(isFlashOn);
            // Change icon indicator
            btnFlash.setImageResource(isFlashOn ? android.R.drawable.btn_star_big_on : android.R.drawable.ic_menu_compass);
        } else {
            Toast.makeText(this, "Flash tidak tersedia pada perangkat ini.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GALLERY && resultCode == RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                try {
                    // Copy to temp file and return
                    File tempFile = copyUriToTempFile(selectedImageUri);
                    if (tempFile != null) {
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("image_path", tempFile.getAbsolutePath());
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Gagal memuat gambar dari galeri.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private File copyUriToTempFile(Uri uri) {
        try {
            File tempFile = new File(getCacheDir(), "temp_captured_doc.jpg");
            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();
            return tempFile;
        } catch (Exception e) {
            Log.e(TAG, "Gagal menyalin gambar galeri: " + e.getMessage());
            return null;
        }
    }

    private void captureImage() {
        if (imageCapture == null) return;

        btnShutter.setEnabled(false);
        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                Bitmap bitmap = imageProxy.toBitmap();
                imageProxy.close();

                // Save captured bitmap to temporary file
                try {
                    File tempFile = new File(getCacheDir(), "temp_captured_doc.jpg");
                    FileOutputStream fos = new FileOutputStream(tempFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                    fos.flush();
                    fos.close();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("image_path", tempFile.getAbsolutePath());
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Gagal menyimpan foto hasil jepretan: " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(ScanCaptureActivity.this, "Gagal mengambil foto.", Toast.LENGTH_SHORT).show();
                        btnShutter.setEnabled(true);
                    });
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Gagal memotret: " + exception.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(ScanCaptureActivity.this, "Error memotret: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    btnShutter.setEnabled(true);
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}
