package com.example.adimarket;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "adimarket.db";
    private static final int DATABASE_VERSION = 13; // Versi 13: tambah seller_reply di offers

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE view_history (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id TEXT, vehicle_id TEXT)");
        db.execSQL("CREATE TABLE favorites (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id TEXT, vehicle_id TEXT)");
        db.execSQL("CREATE TABLE vehicles (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, type TEXT, brand TEXT, color TEXT, year INTEGER, price TEXT, image_uri TEXT, seller_phone TEXT, description TEXT, is_verified INTEGER DEFAULT 0, image_hash TEXT, location TEXT DEFAULT '')");
        db.execSQL("CREATE TABLE ratings (id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_id INTEGER, seller_phone TEXT DEFAULT '', rating REAL, comment TEXT, reviewer_name TEXT DEFAULT 'Anonim', created_at INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE notifications (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, message TEXT, type TEXT, is_read INTEGER DEFAULT 0, created_at INTEGER)");
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT, full_name TEXT, phone TEXT, avatar_uri TEXT, created_at INTEGER)");
        db.execSQL("CREATE TABLE offers (id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_id INTEGER, buyer_name TEXT, buyer_phone TEXT, seller_phone TEXT, offer_price TEXT, message TEXT, status TEXT DEFAULT 'pending', counter_price TEXT DEFAULT '', seller_reply TEXT DEFAULT '', created_at INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 13) {
            try {
                db.execSQL("ALTER TABLE offers ADD COLUMN counter_price TEXT DEFAULT ''");
                db.execSQL("ALTER TABLE offers ADD COLUMN seller_reply TEXT DEFAULT ''");
            } catch (Exception ignored) {}
        }
        if (oldVersion < 12) {
            try { db.execSQL("CREATE TABLE IF NOT EXISTS offers (id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_id INTEGER, buyer_name TEXT, buyer_phone TEXT, seller_phone TEXT, offer_price TEXT, message TEXT, status TEXT DEFAULT 'pending', created_at INTEGER)"); } catch (Exception ignored) {}
        }
        if (oldVersion < 11) {
            // Upgrade tabel ratings: tambah kolom seller_phone, reviewer_name, created_at
            try {
                db.execSQL("ALTER TABLE ratings ADD COLUMN seller_phone TEXT DEFAULT ''");
                db.execSQL("ALTER TABLE ratings ADD COLUMN reviewer_name TEXT DEFAULT 'Anonim'");
                db.execSQL("ALTER TABLE ratings ADD COLUMN created_at INTEGER DEFAULT 0");
            } catch (Exception ignored) {}
        }
        if (oldVersion < 10) {
            try { db.execSQL("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT, full_name TEXT, phone TEXT, avatar_uri TEXT, created_at INTEGER)"); } catch (Exception ignored) {}
        }
        if (oldVersion < 9) {
            try { db.execSQL("CREATE TABLE IF NOT EXISTS notifications (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, message TEXT, type TEXT, is_read INTEGER DEFAULT 0, created_at INTEGER)"); } catch (Exception ignored) {}
        }
        if (oldVersion < 8) {
            db.execSQL("DROP TABLE IF EXISTS vehicles");
            db.execSQL("DROP TABLE IF EXISTS view_history");
            db.execSQL("DROP TABLE IF EXISTS favorites");
            db.execSQL("DROP TABLE IF EXISTS ratings");
            onCreate(db);
        }
    }

    // ─── USERS (Login & Profil) ─────────────────────────────────────────────────

    public static class UserData {
        public int id;
        public String username, fullName, phone, avatarUri;
        public long createdAt;

        public UserData(int id, String username, String fullName, String phone, String avatarUri, long createdAt) {
            this.id = id; this.username = username; this.fullName = fullName;
            this.phone = phone; this.avatarUri = avatarUri; this.createdAt = createdAt;
        }
    }

    /** Daftar user baru. Return id baru, atau -1 jika username sudah ada. */
    public long registerUser(String username, String password, String fullName, String phone) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("username", username.trim().toLowerCase());
        v.put("password", password); // Produksi: hash dulu
        v.put("full_name", fullName.trim());
        v.put("phone", phone.trim());
        v.put("avatar_uri", "");
        v.put("created_at", System.currentTimeMillis());
        try { return db.insertOrThrow("users", null, v); }
        catch (Exception e) { return -1; } // username duplikat
    }

    /** Login. Return UserData jika cocok, null jika gagal. */
    public UserData loginUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM users WHERE username=? AND password=?",
                new String[]{username.trim().toLowerCase(), password});
        UserData user = null;
        if (c.moveToFirst()) {
            user = new UserData(
                c.getInt(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("username")),
                c.getString(c.getColumnIndexOrThrow("full_name")),
                c.getString(c.getColumnIndexOrThrow("phone")),
                c.getString(c.getColumnIndexOrThrow("avatar_uri")),
                c.getLong(c.getColumnIndexOrThrow("created_at"))
            );
        }
        c.close();
        return user;
    }

    /** Ambil data user by ID. */
    public UserData getUserById(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM users WHERE id=?", new String[]{String.valueOf(userId)});
        UserData user = null;
        if (c.moveToFirst()) {
            user = new UserData(
                c.getInt(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("username")),
                c.getString(c.getColumnIndexOrThrow("full_name")),
                c.getString(c.getColumnIndexOrThrow("phone")),
                c.getString(c.getColumnIndexOrThrow("avatar_uri")),
                c.getLong(c.getColumnIndexOrThrow("created_at"))
            );
        }
        c.close();
        return user;
    }

    /** Update profil user (nama & telepon). */
    public boolean updateUserProfile(int userId, String fullName, String phone) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("full_name", fullName);
        v.put("phone", phone);
        return db.update("users", v, "id=?", new String[]{String.valueOf(userId)}) > 0;
    }

    /** Reset/update password user. */
    public boolean updatePassword(int userId, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("password", newPassword);
        return db.update("users", v, "id=?", new String[]{String.valueOf(userId)}) > 0;
    }

    /** Cek kecocokan username dan nomor hp */
    public boolean checkUserByUsernameAndPhone(String username, String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM users WHERE username=? AND phone=?",
                new String[]{username.trim().toLowerCase(), phone.trim()});
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    /** Reset password berdasarkan username dan nomor hp */
    public boolean resetPassword(String username, String phone, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("password", newPassword);
        return db.update("users", v, "username=? AND phone=?",
                new String[]{username.trim().toLowerCase(), phone.trim()}) > 0;
    }

    // ─── TAWAR HARGA (OFFERS) ──────────────────────────────────────────────────

    public static class OfferData {
        public int id, vehicleId;
        public String buyerName, buyerPhone, sellerPhone;
        public String offerPrice, message, status;
        public String counterPrice, sellerReply;   // balasan penjual
        public long createdAt;

        public OfferData(int id, int vehicleId, String buyerName, String buyerPhone,
                         String sellerPhone, String offerPrice, String message,
                         String status, String counterPrice, String sellerReply, long createdAt) {
            this.id = id; this.vehicleId = vehicleId;
            this.buyerName = buyerName; this.buyerPhone = buyerPhone;
            this.sellerPhone = sellerPhone; this.offerPrice = offerPrice;
            this.message = message; this.status = status;
            this.counterPrice = counterPrice != null ? counterPrice : "";
            this.sellerReply  = sellerReply  != null ? sellerReply  : "";
            this.createdAt = createdAt;
        }
    }

    /** Kirim penawaran harga baru */
    public long addOffer(int vehicleId, String buyerName, String buyerPhone,
                         String sellerPhone, String offerPrice, String message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("vehicle_id", vehicleId);
        v.put("buyer_name", buyerName);
        v.put("buyer_phone", buyerPhone);
        v.put("seller_phone", sellerPhone);
        v.put("offer_price", offerPrice);
        v.put("message", message);
        v.put("status", "pending");
        v.put("counter_price", "");
        v.put("seller_reply", "");
        v.put("created_at", System.currentTimeMillis());
        return db.insert("offers", null, v);
    }

    /** Helper: baca OfferData dari Cursor */
    private OfferData offerFromCursor(Cursor c) {
        return new OfferData(
            c.getInt(c.getColumnIndexOrThrow("id")),
            c.getInt(c.getColumnIndexOrThrow("vehicle_id")),
            c.getString(c.getColumnIndexOrThrow("buyer_name")),
            c.getString(c.getColumnIndexOrThrow("buyer_phone")),
            c.getString(c.getColumnIndexOrThrow("seller_phone")),
            c.getString(c.getColumnIndexOrThrow("offer_price")),
            c.getString(c.getColumnIndexOrThrow("message")),
            c.getString(c.getColumnIndexOrThrow("status")),
            safeCol(c, "counter_price"),
            safeCol(c, "seller_reply"),
            c.getLong(c.getColumnIndexOrThrow("created_at"))
        );
    }
    private String safeCol(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return idx >= 0 && !c.isNull(idx) ? c.getString(idx) : "";
    }

    /** Semua penawaran untuk satu kendaraan */
    public List<OfferData> getOffersForVehicle(int vehicleId) {
        List<OfferData> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT * FROM offers WHERE vehicle_id=? ORDER BY created_at DESC",
            new String[]{String.valueOf(vehicleId)});
        while (c.moveToNext()) list.add(offerFromCursor(c));
        c.close();
        return list;
    }

    /** Semua penawaran masuk ke penjual berdasarkan nomor HP */
    public List<OfferData> getOffersForSeller(String sellerPhone) {
        List<OfferData> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT * FROM offers WHERE seller_phone=? ORDER BY created_at DESC",
            new String[]{sellerPhone});
        while (c.moveToNext()) list.add(offerFromCursor(c));
        c.close();
        return list;
    }

    /** Semua penawaran yang sudah dikirim pembeli berdasarkan no HP */
    public List<OfferData> getOffersByBuyerPhone(String buyerPhone) {
        List<OfferData> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT * FROM offers WHERE buyer_phone=? ORDER BY created_at DESC",
            new String[]{buyerPhone});
        while (c.moveToNext()) list.add(offerFromCursor(c));
        c.close();
        return list;
    }

    /** Penjual balas penawaran: status + counter_price + seller_reply */
    public boolean sellerReply(int offerId, String status, String counterPrice, String replyMsg) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("status", status);
        v.put("counter_price", counterPrice != null ? counterPrice : "");
        v.put("seller_reply", replyMsg != null ? replyMsg : "");
        return db.update("offers", v, "id=?", new String[]{String.valueOf(offerId)}) > 0;
    }

    /** Update status penawaran: accepted / rejected / countered */
    public boolean updateOfferStatus(int offerId, String newStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("status", newStatus);
        return db.update("offers", v, "id=?", new String[]{String.valueOf(offerId)}) > 0;
    }

    /** Jumlah penawaran pending untuk penjual */
    public int getPendingOfferCount(String sellerPhone) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT COUNT(*) FROM offers WHERE seller_phone=? AND status='pending'",
            new String[]{sellerPhone});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    // ─── RATING PENJUAL ────────────────────────────────────────────────────────


    public static class RatingData {
        public int id, vehicleId;
        public String sellerPhone, reviewerName, comment;
        public float rating;
        public long createdAt;

        public RatingData(int id, int vehicleId, String sellerPhone, float rating,
                          String comment, String reviewerName, long createdAt) {
            this.id = id; this.vehicleId = vehicleId; this.sellerPhone = sellerPhone;
            this.rating = rating; this.comment = comment;
            this.reviewerName = reviewerName; this.createdAt = createdAt;
        }
    }

    /** Tambah rating baru */
    public long addRating(int vehicleId, String sellerPhone, float rating,
                          String comment, String reviewerName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("vehicle_id", vehicleId);
        v.put("seller_phone", sellerPhone);
        v.put("rating", rating);
        v.put("comment", comment);
        v.put("reviewer_name", reviewerName);
        v.put("created_at", System.currentTimeMillis());
        return db.insert("ratings", null, v);
    }

    /** Rata-rata rating untuk satu kendaraan */
    public float getAverageRating(int vehicleId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT AVG(rating) FROM ratings WHERE vehicle_id=?",
            new String[]{String.valueOf(vehicleId)});
        float avg = 0f;
        if (c.moveToFirst() && !c.isNull(0)) avg = c.getFloat(0);
        c.close();
        return avg;
    }

    /** Jumlah ulasan untuk satu kendaraan */
    public int getRatingCount(int vehicleId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT COUNT(*) FROM ratings WHERE vehicle_id=?",
            new String[]{String.valueOf(vehicleId)});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    /** Semua rating untuk satu kendaraan, terbaru duluan */
    public List<RatingData> getVehicleRatings(int vehicleId) {
        List<RatingData> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT * FROM ratings WHERE vehicle_id=? ORDER BY created_at DESC",
            new String[]{String.valueOf(vehicleId)});
        while (c.moveToNext()) {
            list.add(new RatingData(
                c.getInt(c.getColumnIndexOrThrow("id")),
                c.getInt(c.getColumnIndexOrThrow("vehicle_id")),
                c.getString(c.getColumnIndexOrThrow("seller_phone")),
                c.getFloat(c.getColumnIndexOrThrow("rating")),
                c.getString(c.getColumnIndexOrThrow("comment")),
                c.getString(c.getColumnIndexOrThrow("reviewer_name")),
                c.getLong(c.getColumnIndexOrThrow("created_at"))
            ));
        }
        c.close();
        return list;
    }

    /** Rata-rata rating per nomor penjual (untuk profil penjual) */
    public float getSellerAverageRating(String sellerPhone) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(
            "SELECT AVG(rating) FROM ratings WHERE seller_phone=?",
            new String[]{sellerPhone});
        float avg = 0f;
        if (c.moveToFirst() && !c.isNull(0)) avg = c.getFloat(0);
        c.close();
        return avg;
    }

    // ─── NOTIFIKASI ────────────────────────────────────────────────────────────


    public static class NotifData {
        public int id;
        public String title, message, type;
        public boolean isRead;
        public long createdAt;

        public NotifData(int id, String title, String message, String type, boolean isRead, long createdAt) {
            this.id = id; this.title = title; this.message = message;
            this.type = type; this.isRead = isRead; this.createdAt = createdAt;
        }
    }

    /** Simpan notifikasi baru ke database */
    public long addNotification(String title, String message, String type) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("title", title);
        v.put("message", message);
        v.put("type", type);
        v.put("is_read", 0);
        v.put("created_at", System.currentTimeMillis());
        return db.insert("notifications", null, v);
    }

    /** Jumlah notifikasi yang belum dibaca */
    public int getUnreadCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM notifications WHERE is_read = 0", null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    /** Semua notifikasi, terbaru duluan */
    public List<NotifData> getAllNotifications() {
        List<NotifData> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM notifications ORDER BY created_at DESC", null);
        while (c.moveToNext()) {
            list.add(new NotifData(
                c.getInt(c.getColumnIndexOrThrow("id")),
                c.getString(c.getColumnIndexOrThrow("title")),
                c.getString(c.getColumnIndexOrThrow("message")),
                c.getString(c.getColumnIndexOrThrow("type")),
                c.getInt(c.getColumnIndexOrThrow("is_read")) == 1,
                c.getLong(c.getColumnIndexOrThrow("created_at"))
            ));
        }
        c.close();
        return list;
    }

    /** Tandai semua notifikasi sebagai sudah dibaca */
    public void markAllRead() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("is_read", 1);
        db.update("notifications", v, null, null);
    }

    /** Tandai satu notifikasi sebagai dibaca */
    public void markRead(int notifId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("is_read", 1);
        db.update("notifications", v, "id = ?", new String[]{String.valueOf(notifId)});
    }

    public long insertVehicle(String name, String type, String brand, int year, String price, String imageUri, String sellerPhone, String description, String imageHash) {
        return insertVehicle(name, type, brand, year, price, imageUri, sellerPhone, description, imageHash, "");
    }

    public long insertVehicle(String name, String type, String brand, int year, String price, String imageUri, String sellerPhone, String description, String imageHash, String location) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("type", type);
        values.put("brand", brand);
        values.put("color", "Default");
        values.put("year", year);
        values.put("price", price);
        values.put("image_uri", imageUri);
        values.put("seller_phone", sellerPhone);
        values.put("description", description);
        values.put("is_verified", 0);
        values.put("image_hash", imageHash);
        values.put("location", location != null ? location : "");
        return db.insert("vehicles", null, values);
    }

    /** Layer 1 — Cegah foto duplikat berdasarkan hash */
    public boolean isDuplicatePhoto(String hash) {
        if (hash == null || hash.isEmpty()) return false;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM vehicles WHERE image_hash LIKE ?", new String[]{"%" + hash + "%"});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /** Layer 4 — Cegah iklan bercabang: nomor HP + nama kendaraan + tipe yang sama sudah ada */
    public boolean isDuplicateListing(String sellerPhone, String name, String type) {
        if (sellerPhone == null || sellerPhone.isEmpty()) return false;
        SQLiteDatabase db = this.getReadableDatabase();
        // Cek apakah sudah ada iklan dengan HP + nama + tipe yang sama (pending atau aktif)
        Cursor cursor = db.rawQuery(
            "SELECT id FROM vehicles WHERE seller_phone = ? AND LOWER(name) = LOWER(?) AND LOWER(type) = LOWER(?)",
            new String[]{sellerPhone.trim(), name.trim(), type.trim()});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /** Layer 5 — Rate Limiter: max 3 iklan aktif/pending per nomor HP */
    public int countActiveListingsByPhone(String sellerPhone) {
        if (sellerPhone == null || sellerPhone.isEmpty()) return 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT COUNT(*) FROM vehicles WHERE seller_phone = ?",
            new String[]{sellerPhone.trim()});
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    /**
     * Layer 6 — Content Similarity: deteksi deskripsi yang sangat mirip (>75% Jaccard similarity).
     * Mencegah penjual spam dengan mengubah sedikit kata di deskripsi.
     */
    public boolean hasSimilarDescription(String sellerPhone, String newDescription) {
        if (newDescription == null || newDescription.length() < 20) return false;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT description FROM vehicles WHERE seller_phone = ? AND description IS NOT NULL AND description != ''",
            new String[]{sellerPhone.trim()});
        while (cursor.moveToNext()) {
            String existing = cursor.getString(0);
            if (existing != null && jaccardSimilarity(newDescription.toLowerCase(), existing.toLowerCase()) > 0.75) {
                cursor.close();
                return true;
            }
        }
        cursor.close();
        return false;
    }

    /** Hitung kemiripan teks menggunakan metode Jaccard (berbasis kata) */
    private double jaccardSimilarity(String a, String b) {
        java.util.Set<String> setA = new java.util.HashSet<>(java.util.Arrays.asList(a.split("\\s+")));
        java.util.Set<String> setB = new java.util.HashSet<>(java.util.Arrays.asList(b.split("\\s+")));
        java.util.Set<String> intersection = new java.util.HashSet<>(setA);
        intersection.retainAll(setB);
        java.util.Set<String> union = new java.util.HashSet<>(setA);
        union.addAll(setB);
        if (union.isEmpty()) return 0.0;
        return (double) intersection.size() / union.size();
    }

    public void approveVehicle(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_verified", 1);
        db.update("vehicles", values, "id = ?", new String[]{String.valueOf(id)});
    }

    public List<VehicleData> getAllVehicles() {
        List<VehicleData> vehicleList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Hanya ambil yang sudah diverifikasi (is_verified = 1)
        Cursor cursor = db.rawQuery("SELECT * FROM vehicles WHERE is_verified = 1 ORDER BY id DESC", null);

        if (cursor.moveToFirst()) {
            do {
                vehicleList.add(getVehicleFromCursor(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return vehicleList;
    }

    public static class VehicleData {
        public int id;
        public String name;
        public String type;
        public String brand;
        public String color;
        public int year;
        public String price;
        public String imageUri;
        public String sellerPhone;
        public String description;
        public String location;
    }

    public void insertViewHistory(String userId, String vehicleId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("vehicle_id", vehicleId);
        db.insert("view_history", null, values);
    }

    public List<String> getViewHistory(String userId) {
        List<String> history = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT vehicle_id FROM view_history WHERE user_id = ?", new String[]{userId});
        if (cursor.moveToFirst()) {
            do {
                history.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return history;
    }

    public List<String> getUsersWhoViewedVehicle(String vehicleId) {
        List<String> users = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT user_id FROM view_history WHERE vehicle_id = ?", new String[]{vehicleId});
        if (cursor.moveToFirst()) {
            do {
                users.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return users;
    }

    public void insertFavorite(String userId, String vehicleId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("vehicle_id", vehicleId);
        db.insert("favorites", null, values);
    }

    public List<VehicleData> getUserFavorites(String userId) {
        List<VehicleData> vehicleList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT v.* FROM vehicles v JOIN favorites f ON v.id = f.vehicle_id WHERE f.user_id = ?";
        Cursor cursor = db.rawQuery(query, new String[]{userId});
        if (cursor.moveToFirst()) {
            do {
                vehicleList.add(getVehicleFromCursor(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return vehicleList;
    }

    public List<VehicleData> searchVehicles(String type, int minPrice, int maxPrice) {
        return searchAdvanced(type, "", minPrice, maxPrice, 0, 9999);
    }

    public List<VehicleData> searchAdvanced(String type, String brand, long minPrice, long maxPrice, int minYear, int maxYear) {
        List<VehicleData> vehicleList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM vehicles WHERE is_verified = 1");
        List<String> args = new ArrayList<>();

        if (type != null && !type.isEmpty() && !type.equalsIgnoreCase("semua")) {
            queryBuilder.append(" AND type = ?");
            args.add(type);
        }
        if (brand != null && !brand.isEmpty()) {
            queryBuilder.append(" AND LOWER(brand) LIKE ?");
            args.add("%" + brand.toLowerCase() + "%");
        }
        queryBuilder.append(" ORDER BY id DESC");

        Cursor cursor = db.rawQuery(queryBuilder.toString(), args.toArray(new String[0]));
        if (cursor.moveToFirst()) {
            do {
                VehicleData v = getVehicleFromCursor(cursor);
                // Filter price and year in Java (stored as string)
                try {
                    long price = Long.parseLong(v.price.replaceAll("[^0-9]", ""));
                    if (price >= minPrice && price <= maxPrice && v.year >= minYear && v.year <= maxYear) {
                        vehicleList.add(v);
                    }
                } catch (NumberFormatException e) {
                    vehicleList.add(v);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return vehicleList;
    }

    public boolean isFavorite(String userId, String vehicleId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM favorites WHERE user_id = ? AND vehicle_id = ?",
                new String[]{userId, vehicleId});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public void removeFavorite(String userId, String vehicleId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("favorites", "user_id = ? AND vehicle_id = ?", new String[]{userId, vehicleId});
    }

    public List<VehicleData> getFavoriteVehicles(String userId) {
        List<VehicleData> vehicleList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT v.* FROM vehicles v JOIN favorites f ON CAST(v.id AS TEXT) = f.vehicle_id WHERE f.user_id = ? AND v.is_verified = 1";
        Cursor cursor = db.rawQuery(query, new String[]{userId});
        if (cursor.moveToFirst()) {
            do {
                vehicleList.add(getVehicleFromCursor(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return vehicleList;
    }

    public List<VehicleData> getPendingVehicles() {
        List<VehicleData> vehicleList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM vehicles WHERE is_verified = 0 ORDER BY id DESC", null);
        if (cursor.moveToFirst()) {
            do {
                vehicleList.add(getVehicleFromCursor(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return vehicleList;
    }

    public void deleteVehicle(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("vehicles", "id = ?", new String[]{String.valueOf(id)});
    }

    private VehicleData getVehicleFromCursor(Cursor cursor) {
        VehicleData vehicle = new VehicleData();
        vehicle.id = cursor.getInt(0);
        vehicle.name = cursor.getString(1);
        vehicle.type = cursor.getString(2);
        vehicle.brand = cursor.getString(3);
        vehicle.color = cursor.getString(4);
        vehicle.year = cursor.getInt(5);
        vehicle.price = cursor.getString(6);
        vehicle.imageUri = cursor.getString(7);
        vehicle.sellerPhone = cursor.getString(8);
        vehicle.description = cursor.getString(9);
        // Baca kolom location secara aman (mungkin belum ada di DB lama)
        try {
            int locIdx = cursor.getColumnIndex("location");
            vehicle.location = (locIdx >= 0) ? cursor.getString(locIdx) : "";
        } catch (Exception e) { vehicle.location = ""; }
        return vehicle;
    }

    /** Ambil daftar kota unik yang ada di iklan */
    public List<String> getDistinctCities() {
        List<String> cities = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT DISTINCT location FROM vehicles WHERE is_verified = 1 AND location IS NOT NULL AND location != '' ORDER BY location ASC", null);
        if (cursor.moveToFirst()) {
            do { cities.add(cursor.getString(0)); } while (cursor.moveToNext());
        }
        cursor.close();
        return cities;
    }

    /** Filter kendaraan berdasarkan kota */
    public List<VehicleData> getVehiclesByCity(String city) {
        List<VehicleData> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT * FROM vehicles WHERE is_verified = 1 AND location = ? ORDER BY id DESC",
            new String[]{city});
        if (cursor.moveToFirst()) {
            do { list.add(getVehicleFromCursor(cursor)); } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
}
