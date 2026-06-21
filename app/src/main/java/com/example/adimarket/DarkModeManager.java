package com.example.adimarket;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Mengelola Dark Mode global menggunakan AppCompatDelegate.
 * Simpan preferensi agar tetap saat app restart.
 */
public class DarkModeManager {

    private static final String PREF_NAME = "dark_mode_pref";
    private static final String KEY_MODE  = "night_mode";

    // Mode: 0 = ikuti sistem, 1 = terang, 2 = gelap
    public static final int MODE_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    public static final int MODE_LIGHT  = AppCompatDelegate.MODE_NIGHT_NO;
    public static final int MODE_DARK   = AppCompatDelegate.MODE_NIGHT_YES;

    /** Terapkan mode yang tersimpan — panggil di Application.onCreate atau setiap Activity.onCreate sebelum setContentView */
    public static void applyStoredMode(Context ctx) {
        int mode = getSavedMode(ctx);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    /** Toggle antara Dark ↔ Light, simpan, dan terapkan */
    public static boolean toggle(Context ctx) {
        int current = getSavedMode(ctx);
        int next = (current == MODE_DARK) ? MODE_LIGHT : MODE_DARK;
        saveMode(ctx, next);
        AppCompatDelegate.setDefaultNightMode(next);
        return next == MODE_DARK;
    }

    /** Cek apakah Dark Mode aktif */
    public static boolean isDark(Context ctx) {
        return getSavedMode(ctx) == MODE_DARK;
    }

    /** Simpan mode ke SharedPreferences */
    public static void saveMode(Context ctx, int mode) {
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putInt(KEY_MODE, mode).apply();
    }

    /** Ambil mode tersimpan, default = MODE_SYSTEM */
    public static int getSavedMode(Context ctx) {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_MODE, MODE_SYSTEM);
    }
}
