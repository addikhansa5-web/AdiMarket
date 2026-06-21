package com.example.adimarket;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Helper untuk push notification sistem Android + simpan ke DB.
 */
public class NotificationHelper {

    private static final String CHANNEL_ID_IKLAN   = "ch_iklan";
    private static final String CHANNEL_ID_ADMIN   = "ch_admin";
    private static final String CHANNEL_ID_PROMO   = "ch_promo";

    private static int notifIdCounter = 1000;

    /** Wajib dipanggil sekali saat aplikasi pertama dibuka (di DashboardActivity.onCreate) */
    public static void createChannels(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);

            // Channel iklan (untuk penjual)
            nm.createNotificationChannel(new NotificationChannel(
                CHANNEL_ID_IKLAN, "Status Iklan",
                NotificationManager.IMPORTANCE_HIGH));

            // Channel admin
            nm.createNotificationChannel(new NotificationChannel(
                CHANNEL_ID_ADMIN, "Admin Review",
                NotificationManager.IMPORTANCE_DEFAULT));

            // Channel promo
            nm.createNotificationChannel(new NotificationChannel(
                CHANNEL_ID_PROMO, "Promo & Info",
                NotificationManager.IMPORTANCE_LOW));
        }
    }

    // ─── NOTIF: IKLAN DISETUJUI (untuk penjual) ────────────────────────────────

    public static void notifyIklanDisetujui(Context ctx, String namaKendaraan) {
        String title   = "🎉 Iklan Anda Disetujui!";
        String message = "Iklan \"" + namaKendaraan + "\" sudah tayang dan bisa dilihat pembeli.";

        // Simpan ke DB
        new DatabaseHelper(ctx).addNotification(title, message, "iklan_disetujui");

        // Push notification sistem
        sendPushNotif(ctx, CHANNEL_ID_IKLAN, title, message, NotifikasiActivity.class);
    }

    // ─── NOTIF: IKLAN BARU MASUK (untuk admin) ─────────────────────────────────

    public static void notifyIklanBaru(Context ctx, String namaKendaraan, String tipe) {
        String title   = "📋 Iklan Baru Menunggu Review";
        String message = "Ada iklan " + tipe + " baru: \"" + namaKendaraan + "\". Silakan review di panel admin.";

        // Simpan ke DB
        new DatabaseHelper(ctx).addNotification(title, message, "iklan_baru");

        // Push notification sistem
        sendPushNotif(ctx, CHANNEL_ID_ADMIN, title, message, AdminApprovalActivity.class);
    }

    // ─── NOTIF: IKLAN DITOLAK (untuk penjual) ──────────────────────────────────

    public static void notifyIklanDitolak(Context ctx, String namaKendaraan) {
        String title   = "❌ Iklan Tidak Disetujui";
        String message = "Iklan \"" + namaKendaraan + "\" tidak lolos review admin. Periksa dan perbaiki sebelum pasang ulang.";

        new DatabaseHelper(ctx).addNotification(title, message, "iklan_ditolak");
        sendPushNotif(ctx, CHANNEL_ID_IKLAN, title, message, NotifikasiActivity.class);
    }

    // ─── NOTIF: SELAMAT DATANG ─────────────────────────────────────────────────

    public static void notifyWelcome(Context ctx) {
        String title   = "👋 Selamat Datang di AdiMarket!";
        String message = "Temukan kendaraan impian Anda atau pasang iklan gratis sekarang. Ditenagai AI 🤖";

        new DatabaseHelper(ctx).addNotification(title, message, "welcome");
        sendPushNotif(ctx, CHANNEL_ID_PROMO, title, message, DashboardActivity.class);
    }

    // ─── NOTIF: PROMO / INFO ───────────────────────────────────────────────────

    public static void notifyInfo(Context ctx, String title, String message) {
        new DatabaseHelper(ctx).addNotification(title, message, "info");
        sendPushNotif(ctx, CHANNEL_ID_PROMO, title, message, NotifikasiActivity.class);
    }

    // ─── INTERNAL: Kirim push notification sistem ─────────────────────────────

    private static void sendPushNotif(Context ctx, String channelId,
                                      String title, String message,
                                      Class<?> targetActivity) {
        // Intent saat notif diklik
        Intent intent = new Intent(ctx, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, notifIdCounter,
                intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, channelId)
                .setSmallIcon(R.drawable.ic_notif)  // ikon kecil
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pi);

        NotificationManagerCompat nm = NotificationManagerCompat.from(ctx);

        // Cek izin POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return; // Tidak kirim push jika belum ada izin (tetap tersimpan di DB)
            }
        }

        nm.notify(notifIdCounter++, builder.build());
    }
}
