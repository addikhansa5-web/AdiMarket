package com.example.adimarket;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.ImageView;

/**
 * Helper untuk menerapkan wallpaper yang dipilih user ke semua halaman.
 * Cukup panggil WallpaperHelper.apply(context, imageView) di setiap Activity.
 */
public class WallpaperHelper {

    private static final String PREFS_NAME = "AdiMarketPrefs";
    private static final String KEY_WALLPAPER = "dashboard_wallpaper";

    /** Terapkan wallpaper tersimpan ke ImageView header */
    public static void apply(Context context, ImageView headerImage) {
        if (context == null || headerImage == null) return;
        String key = getSavedKey(context);
        headerImage.setImageResource(getDrawableRes(key));
    }

    /** Terapkan wallpaper tersimpan dengan animasi fade-in */
    public static void applyWithAnim(Context context, ImageView headerImage) {
        if (context == null || headerImage == null) return;
        String key = getSavedKey(context);
        headerImage.setImageResource(getDrawableRes(key));
        headerImage.setAlpha(0f);
        headerImage.animate().alpha(1f).setDuration(600).start();
    }

    /** Simpan pilihan wallpaper */
    public static void save(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_WALLPAPER, key).apply();
    }

    /** Ambil key wallpaper tersimpan */
    public static String getSavedKey(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_WALLPAPER, "default");
    }

    /** Konversi key ke drawable resource ID */
    public static int getDrawableRes(String key) {
        switch (key) {
            case "showroom": return R.drawable.wp_showroom;
            case "highway":  return R.drawable.wp_highway;
            case "mountain": return R.drawable.wp_mountain;
            case "garage":   return R.drawable.wp_garage;
            case "racing":   return R.drawable.wp_racing;
            default:         return R.drawable.bg_vehicle_header;
        }
    }
}
