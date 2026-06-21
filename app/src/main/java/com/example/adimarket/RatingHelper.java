package com.example.adimarket;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Utility untuk menampilkan dialog rating bintang.
 * Panggil RatingHelper.show(context, vehicleId, vehicleName, sellerPhone, reviewerName, callback)
 */
public class RatingHelper {

    public interface OnRatingSubmit {
        void onSubmit(float rating, String comment);
    }

    private static final String[] LABELS = {
        "", "😞 Sangat Buruk", "😐 Kurang Baik", "🙂 Cukup Baik",
        "😊 Bagus", "🤩 Luar Biasa!"
    };

    public static void show(Context ctx, int vehicleId, String vehicleName,
                            String sellerPhone, String reviewerName,
                            OnRatingSubmit callback) {

        View view = LayoutInflater.from(ctx).inflate(R.layout.dialog_rating, null);

        TextView tvName    = view.findViewById(R.id.tvRatingVehicleName);
        TextView tvLabel   = view.findViewById(R.id.tvRatingLabel);
        EditText etComment = view.findViewById(R.id.etRatingComment);
        Button btnBatal    = view.findViewById(R.id.btnBatalRating);
        Button btnKirim    = view.findViewById(R.id.btnKirimRating);

        TextView[] stars = {
            view.findViewById(R.id.star1),
            view.findViewById(R.id.star2),
            view.findViewById(R.id.star3),
            view.findViewById(R.id.star4),
            view.findViewById(R.id.star5)
        };

        tvName.setText("Kendaraan: " + vehicleName);
        final int[] selectedRating = {0};

        // Setup klik bintang
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            stars[i].setOnClickListener(v -> {
                selectedRating[0] = idx + 1;
                updateStars(stars, selectedRating[0]);
                tvLabel.setText(LABELS[selectedRating[0]]);
                tvLabel.setTextColor(
                    selectedRating[0] >= 4 ? 0xFF00E676 :
                    selectedRating[0] >= 3 ? 0xFFFFD600 : 0xFFFF5252);
            });
        }

        AlertDialog dialog = new AlertDialog.Builder(ctx)
            .setView(view)
            .setCancelable(true)
            .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnBatal.setOnClickListener(v -> dialog.dismiss());

        btnKirim.setOnClickListener(v -> {
            if (selectedRating[0] == 0) {
                Toast.makeText(ctx, "Pilih dulu jumlah bintang!", Toast.LENGTH_SHORT).show();
                return;
            }
            String comment = etComment.getText().toString().trim();

            // Simpan ke DB
            DatabaseHelper db = new DatabaseHelper(ctx);
            long result = db.addRating(vehicleId, sellerPhone, selectedRating[0], comment, reviewerName);

            if (result != -1) {
                Toast.makeText(ctx, "✅ Rating berhasil dikirim! Terima kasih 🌟", Toast.LENGTH_SHORT).show();
                if (callback != null) callback.onSubmit(selectedRating[0], comment);
            } else {
                Toast.makeText(ctx, "Gagal menyimpan rating", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    /** Tampilkan rating dalam format bintang (misal: ★★★★☆ 4.2) */
    public static String formatStars(float rating) {
        StringBuilder sb = new StringBuilder();
        int full = (int) rating;
        for (int i = 0; i < 5; i++) {
            sb.append(i < full ? "★" : "☆");
        }
        sb.append(String.format(" %.1f", rating));
        return sb.toString();
    }

    private static void updateStars(TextView[] stars, int selected) {
        for (int i = 0; i < 5; i++) {
            stars[i].setText(i < selected ? "★" : "☆");
            stars[i].setAlpha(i < selected ? 1f : 0.5f);
        }
    }
}
