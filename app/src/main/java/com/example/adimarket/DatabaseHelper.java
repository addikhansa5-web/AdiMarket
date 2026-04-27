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
    private static final int DATABASE_VERSION = 6; // Naik ke versi 6 untuk tabel rating

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE view_history (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id TEXT, vehicle_id TEXT)");
        db.execSQL("CREATE TABLE favorites (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id TEXT, vehicle_id TEXT)");
        db.execSQL("CREATE TABLE vehicles (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, type TEXT, brand TEXT, color TEXT, year INTEGER, price TEXT, image_uri TEXT, seller_phone TEXT, description TEXT, is_verified INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE ratings (id INTEGER PRIMARY KEY AUTOINCREMENT, vehicle_id INTEGER, rating REAL, comment TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Hapus tabel lama dan buat baru jika versi berubah untuk menghindari error kolom hilang
        db.execSQL("DROP TABLE IF EXISTS vehicles");
        db.execSQL("DROP TABLE IF EXISTS view_history");
        db.execSQL("DROP TABLE IF EXISTS favorites");
        db.execSQL("DROP TABLE IF EXISTS ratings");
        onCreate(db);
    }

    public long insertVehicle(String name, String type, String brand, int year, String price, String imageUri, String sellerPhone, String description) {
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
        values.put("is_verified", 0); // Status awal belum disetujui
        return db.insert("vehicles", null, values);
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
        List<VehicleData> vehicleList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM vehicles WHERE type = ? OR ? = 'semua'";
        Cursor cursor = db.rawQuery(query, new String[]{type, type.toLowerCase()});
        if (cursor.moveToFirst()) {
            do {
                VehicleData v = getVehicleFromCursor(cursor);
                try {
                    int p = Integer.parseInt(v.price.replaceAll("[^0-9]", ""));
                    if (p >= minPrice && p <= maxPrice) {
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

    public void insertRating(int vehicleId, float rating, String comment) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("vehicle_id", vehicleId);
        values.put("rating", rating);
        values.put("comment", comment);
        db.insert("ratings", null, values);
    }

    public float getAverageRating(int vehicleId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT AVG(rating) FROM ratings WHERE vehicle_id = ?", new String[]{String.valueOf(vehicleId)});
        float avg = 0;
        if (cursor.moveToFirst()) {
            avg = cursor.getFloat(0);
        }
        cursor.close();
        return avg;
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
        return vehicle;
    }
}
