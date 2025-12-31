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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class GettingStartedActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private FusedLocationProviderClient fusedLocationClient;
    private TextView tvLocation;
    private Button btnLanjutkan;
    private TextView tvSkip;
    private ProgressBar progressBar;

    private String kecamatan = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_getting_started);

        // Inisialisasi komponen
        tvLocation = findViewById(R.id.tvLocation);
        btnLanjutkan = findViewById(R.id.btnLanjutkan);
        tvSkip = findViewById(R.id.tvSkip);
        progressBar = findViewById(R.id.progressBar);

        // Inisialisasi FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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
                tvLocation.setText("Izin lokasi ditolak");
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Izin lokasi diperlukan untuk fitur ini",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            getAddressFromLocation(location.getLatitude(), location.getLongitude());
                        } else {
                            tvLocation.setText("Tidak dapat mendeteksi lokasi");
                            progressBar.setVisibility(View.GONE);
                            enableButton();
                        }
                    }
                });
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // Ambil kecamatan (subLocality)
                kecamatan = address.getSubLocality();
                String kota = address.getLocality();
                String provinsi = address.getAdminArea();

                // Tampilkan lokasi
                String lokasi = "";
                if (kecamatan != null && !kecamatan.isEmpty()) {
                    lokasi = "Kec. " + kecamatan;
                    if (kota != null) {
                        lokasi += ", " + kota;
                    }
                } else if (kota != null) {
                    lokasi = kota;
                    if (provinsi != null) {
                        lokasi += ", " + provinsi;
                    }
                } else {
                    lokasi = "Lokasi terdeteksi";
                }

                tvLocation.setText(lokasi);
                progressBar.setVisibility(View.GONE);
                enableButton();

            } else {
                tvLocation.setText("Tidak dapat mendapatkan alamat");
                progressBar.setVisibility(View.GONE);
                enableButton();
            }

        } catch (IOException e) {
            e.printStackTrace();
            tvLocation.setText("Error mendapatkan alamat");
            progressBar.setVisibility(View.GONE);
            enableButton();
        }
    }

    private void enableButton() {
        btnLanjutkan.setEnabled(true);
        btnLanjutkan.setAlpha(1.0f);
    }

    private void goToSplashScreen() {
        Intent intent = new Intent(GettingStartedActivity.this, SplashActivity.class);
        intent.putExtra("kecamatan", kecamatan);
        startActivity(intent);
        finish();
    }
}