package com.example.adimarket;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * AI Utility classes (Point 2)
 * Common helper functions for AI operations across the app.
 */
public class AIUtils {

    /**
     * Converts a bitmap to grayscale (useful for some OCR/classification tasks)
     */
    public static Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    /**
     * Reads a JSON or text file from assets (useful for labels/configs)
     */
    public static String loadJSONFromAsset(Context context, String fileName) {
        String json;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    /**
     * Normalize confidence score to percentage string
     */
    public static String formatConfidence(float confidence) {
        return String.format("%.1f%%", confidence * 100);
    }

    /**
     * Generates a unique hash (MD5) for a bitmap to detect duplicates.
     */
    public static String getBitmapHash(Bitmap bitmap) {
        if (bitmap == null) return "";
        try {
            // Resize to small size for faster hashing and tolerance to minor changes
            Bitmap smallBitmap = Bitmap.createScaledBitmap(bitmap, 64, 64, true);
            int size = smallBitmap.getRowBytes() * smallBitmap.getHeight();
            ByteBuffer byteBuffer = ByteBuffer.allocate(size);
            smallBitmap.copyPixelsToBuffer(byteBuffer);
            byte[] bytes = byteBuffer.array();

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytes);
            
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return String.valueOf(System.currentTimeMillis());
        }
    }
}
