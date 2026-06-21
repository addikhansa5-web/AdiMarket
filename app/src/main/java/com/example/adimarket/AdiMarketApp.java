package com.example.adimarket;

import android.app.Application;

/**
 * Application class — dijalankan pertama kali sebelum Activity manapun.
 * Menerapkan Dark Mode yang tersimpan agar tidak ada flash/flicker.
 */
public class AdiMarketApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Terapkan dark mode tersimpan sebelum UI manapun terbuka
        DarkModeManager.applyStoredMode(this);
    }
}
