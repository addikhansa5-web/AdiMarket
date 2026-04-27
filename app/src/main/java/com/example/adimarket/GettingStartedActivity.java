package com.example.adimarket;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnSuccessListener;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class GettingStartedActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvLocation, tvWelcome;
    private Button btnLanjutkan;
    private TextView tvSkip;
    private ProgressBar progressBar;
    private ImageView ivFlagId, ivFlagEn;
    private LinearLayout layoutFlagId, layoutFlagEn;

    private String kecamatan = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_getting_started);

        // Inisialisasi komponen
        tvWelcome = findViewById(R.id.tvWelcome);
        tvLocation = findViewById(R.id.tvLocation);
        btnLanjutkan = findViewById(R.id.btnLanjutkan);
        tvSkip = findViewById(R.id.tvSkip);
        progressBar = findViewById(R.id.progressBar);
        ivFlagId = findViewById(R.id.ivFlagId);
        ivFlagEn = findViewById(R.id.ivFlagEn);
        layoutFlagId = findViewById(R.id.layoutFlagId);
        layoutFlagEn = findViewById(R.id.layoutFlagEn);

        // Inisialisasi FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Language Selection
        layoutFlagId.setOnClickListener(v -> setLocale("id"));
        layoutFlagEn.setOnClickListener(v -> setLocale("en"));

        // Request location permission
        if (checkLocationPermission()) {
            getLocation();
        } else {
            requestLocationPermission();
        }

        // Button Lanjutkan
        btnLanjutkan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToSplashScreen();
            }
        });

        // Button Skip
        tvSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToSplashScreen();
            }
        });
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();
            } else {
                tvLocation.setText(getString(R.string.location_denied));
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, getString(R.string.location_required),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvLocation.setText(getString(R.string.detecting_location));

        // Gunakan getCurrentLocation untuk akurasi maksimal (mengabaikan cache lama)
        CancellationTokenSource cts = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        getAddressFromLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        // Jika getCurrentLocation gagal, fallback ke requestLocationUpdates
                        requestNewLocation();
                    }
                })
                .addOnFailureListener(e -> requestNewLocation());
    }

    private void requestNewLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMaxUpdates(1) // Kita hanya butuh 1 kali update yang paling akurat
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    getAddressFromLocation(location.getLatitude(), location.getLongitude());
                } else {
                    tvLocation.setText(getString(R.string.location_not_found));
                    progressBar.setVisibility(View.GONE);
                    enableButton();
                }
            }
        }, getMainLooper());
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // Ambil data lokasi
                kecamatan = address.getSubLocality();
                String kota = address.getLocality();
                String kabupaten = address.getSubAdminArea(); // Ini untuk Kabupaten/Regency
                String provinsi = address.getAdminArea();

                // Tampilkan lokasi
                String lokasi = "";
                if (kecamatan != null && !kecamatan.isEmpty()) {
                    lokasi = getString(R.string.district_prefix) + kecamatan;
                    if (kabupaten != null) {
                        lokasi += ", " + kabupaten;
                    } else if (kota != null) {
                        lokasi += ", " + kota;
                    }
                } else if (kabupaten != null) {
                    lokasi = kabupaten;
                } else if (kota != null) {
                    lokasi = kota;
                } else {
                    lokasi = getString(R.string.location_detected);
                }

                tvLocation.setText(lokasi);
                
                progressBar.setVisibility(View.GONE);
                enableButton();

            } else {
                tvLocation.setText(getString(R.string.address_not_found));
                progressBar.setVisibility(View.GONE);
                enableButton();
            }

        } catch (IOException e) {
            e.printStackTrace();
            tvLocation.setText(getString(R.string.address_error));
            progressBar.setVisibility(View.GONE);
            enableButton();
        }
    }

    private void enableButton() {
        btnLanjutkan.setEnabled(true);
        btnLanjutkan.setAlpha(1.0f);
    }

    private void setLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        
        // Refresh UI Text
        recreate();
    }

    private void goToSplashScreen() {
        Intent intent = new Intent(GettingStartedActivity.this, SplashActivity.class);
        intent.putExtra("kecamatan", kecamatan);
        startActivity(intent);
        finish();
    }
}