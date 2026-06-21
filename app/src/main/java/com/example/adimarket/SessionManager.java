package com.example.adimarket;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Mengelola sesi login user menggunakan SharedPreferences.
 * Simpan & baca userId, nama, dan username yang sedang login.
 */
public class SessionManager {

    private static final String PREF_NAME    = "adimarket_session";
    private static final String KEY_USER_ID  = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULLNAME = "full_name";
    private static final String KEY_PHONE    = "phone";
    private static final int    NO_USER      = -1;

    private final SharedPreferences prefs;

    public SessionManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** Simpan sesi setelah login berhasil */
    public void saveSession(int userId, String username, String fullName, String phone) {
        prefs.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_FULLNAME, fullName)
            .putString(KEY_PHONE, phone)
            .apply();
    }

    /** Cek apakah user sudah login */
    public boolean isLoggedIn() {
        return prefs.getInt(KEY_USER_ID, NO_USER) != NO_USER;
    }

    /** Ambil user ID yang sedang login, -1 jika belum login */
    public int getUserId() {
        return prefs.getInt(KEY_USER_ID, NO_USER);
    }

    /** Ambil username */
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    /** Ambil nama lengkap */
    public String getFullName() {
        return prefs.getString(KEY_FULLNAME, "Pengguna");
    }

    /** Ambil nomor telepon */
    public String getPhone() {
        return prefs.getString(KEY_PHONE, "");
    }

    /** Perbarui nama & telepon di sesi (setelah edit profil) */
    public void updateProfile(String fullName, String phone) {
        prefs.edit()
            .putString(KEY_FULLNAME, fullName)
            .putString(KEY_PHONE, phone)
            .apply();
    }

    /** Logout — hapus semua data sesi */
    public void logout() {
        prefs.edit().clear().apply();
    }
}
