package com.example.adimarket;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class DynamicVehicleAdapter extends RecyclerView.Adapter<DynamicVehicleAdapter.ViewHolder> {

    private List<DatabaseHelper.VehicleData> vehicleList;
    private DatabaseHelper dbHelper;
    private String userId;

    // Constructor baru dengan dbHelper & userId untuk fitur favorit
    public DynamicVehicleAdapter(List<DatabaseHelper.VehicleData> vehicleList, DatabaseHelper dbHelper, String userId) {
        this.vehicleList = vehicleList;
        this.dbHelper = dbHelper;
        this.userId = userId;
    }

    // Constructor lama untuk kompatibilitas
    public DynamicVehicleAdapter(List<DatabaseHelper.VehicleData> vehicleList) {
        this.vehicleList = vehicleList;
        this.dbHelper = null;
        this.userId = "user_adimarket";
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vehicle, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DatabaseHelper.VehicleData vehicle = vehicleList.get(position);
        DatabaseHelper db = dbHelper != null ? dbHelper : new DatabaseHelper(holder.itemView.getContext());

        // Info dasar
        holder.tvName.setText(vehicle.name);
        holder.tvPrice.setText("Rp " + formatHarga(vehicle.price));
        holder.tvYear.setText(String.valueOf(vehicle.year));
        holder.tvType.setText(vehicle.type);

        // ── AI: Price Fairness Badge ──
        List<DatabaseHelper.VehicleData> allVehicles = db.getAllVehicles();
        VehicleAIEngine.PriceFairness pf = VehicleAIEngine.analyzePriceFairness(vehicle, allVehicles);
        if (holder.tvAIPriceBadge != null) {
            holder.tvAIPriceBadge.setText(pf.emoji + " " + pf.text);
            holder.tvAIPriceBadge.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(pf.color));
        }

        // ── AI: Health Score ──
        VehicleAIEngine.HealthScore hs = VehicleAIEngine.calcHealthScore(vehicle, allVehicles);
        if (holder.pbHealthScore != null) {
            holder.pbHealthScore.setProgress(hs.score);
            holder.pbHealthScore.setProgressTintList(
                android.content.res.ColorStateList.valueOf(hs.color));
        }
        if (holder.tvHealthScore != null) {
            holder.tvHealthScore.setText(hs.score + " " + hs.grade);
            holder.tvHealthScore.setTextColor(hs.color);
        }
        if (holder.tvHealthLabel != null) {
            holder.tvHealthLabel.setText("🏥 " + hs.summary);
        }

        // Deskripsi
        if (vehicle.description != null && !vehicle.description.isEmpty()) {
            holder.tvDescription.setText(vehicle.description);
            holder.tvDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        // Rating
        float avgRating = db.getAverageRating(vehicle.id);
        if (avgRating == 0) avgRating = 4.5f;
        holder.ratingBar.setRating(avgRating);

        // Fraud badge
        setupFraudBadge(holder, vehicle);

        // Favorit button
        boolean isFav = db.isFavorite(userId, String.valueOf(vehicle.id));
        updateFavButton(holder.btnFavorit, isFav);
        holder.btnFavorit.setOnClickListener(v -> {
            boolean currentFav = db.isFavorite(userId, String.valueOf(vehicle.id));
            if (currentFav) {
                db.removeFavorite(userId, String.valueOf(vehicle.id));
                updateFavButton(holder.btnFavorit, false);
                Toast.makeText(v.getContext(), "Dihapus dari favorit", Toast.LENGTH_SHORT).show();
            } else {
                db.insertFavorite(userId, String.valueOf(vehicle.id));
                updateFavButton(holder.btnFavorit, true);
                Toast.makeText(v.getContext(), "❤️ Ditambahkan ke favorit!", Toast.LENGTH_SHORT).show();
            }
        });

        // Foto
        holder.containerFotoBeli.removeAllViews();
        if (vehicle.imageUri != null && !vehicle.imageUri.isEmpty()) {
            String[] images = vehicle.imageUri.split("[|,]");
            for (String imgUri : images) {
                if (imgUri.trim().isEmpty()) continue;
                ImageView imageView = new ImageView(holder.itemView.getContext());
                int width = images.length == 1 ?
                        holder.itemView.getResources().getDisplayMetrics().widthPixels :
                        (int) (holder.itemView.getResources().getDisplayMetrics().widthPixels * 0.8);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT);
                params.setMargins(0, 0, 4, 0);
                imageView.setLayoutParams(params);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                Glide.with(holder.itemView.getContext())
                        .load(imgUri.trim())
                        .placeholder(R.drawable.car_placeholder)
                        .error(R.drawable.car_placeholder)
                        .into(imageView);

                final int clickedIndex = holder.containerFotoBeli.getChildCount();
                final String[] allImages = images;
                imageView.setOnClickListener(v -> showGallery(holder.itemView.getContext(), allImages, clickedIndex));
                holder.containerFotoBeli.addView(imageView);
            }
        } else {
            // Placeholder jika tidak ada foto
            ImageView placeholder = new ImageView(holder.itemView.getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            placeholder.setLayoutParams(params);
            placeholder.setImageResource(R.drawable.car_placeholder);
            placeholder.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.containerFotoBeli.addView(placeholder);
        }

        // Hubungi Penjual — Tampilkan Negosiasi AI dulu, lalu pilih WA/Telepon
        holder.btnHubungi.setOnClickListener(v -> {
            String rawPhone = vehicle.sellerPhone;
            if (rawPhone == null || rawPhone.trim().isEmpty()) {
                Toast.makeText(v.getContext(), "📵 Nomor penjual belum diisi", Toast.LENGTH_SHORT).show();
                return;
            }

            // Format nomor WA & telepon
            String phoneWa = rawPhone.trim();
            if (phoneWa.startsWith("0")) phoneWa = "62" + phoneWa.substring(1);
            else if (!phoneWa.startsWith("62")) phoneWa = "62" + phoneWa;
            final String finalPhoneWa = phoneWa;
            final String phoneDial = rawPhone.startsWith("0") ? rawPhone : "0" + rawPhone.replaceFirst("^62", "");

            // Pesan WA standar
            String pesanDefault = "Halo, saya tertarik dengan *" + vehicle.name
                    + "* tahun " + vehicle.year + " seharga *Rp " + vehicle.price
                    + "* yang Anda pasang di AdiMarket. Apakah masih tersedia? 🙏";

            // AI Negotiation Tips
            VehicleAIEngine.NegotiationTip tip = VehicleAIEngine.generateNegotiationTip(vehicle, allVehicles);

            android.content.Context ctx = v.getContext();

            // Tampilkan dialog AI Negosiasi
            StringBuilder tipMsg = new StringBuilder();
            tipMsg.append("💡 Rekomendasi harga tawar:\n");
            tipMsg.append("  ✅ ").append(tip.priceRange).append("\n\n");
            tipMsg.append("📋 Yang perlu dicek saat survey:\n");
            for (int i = 0; i < Math.min(3, tip.checkPoints.length); i++) {
                tipMsg.append("  ").append(i+1).append(". ").append(tip.checkPoints[i]).append("\n");
            }

            final String finalPesanWa = tip.whatsappScript;

            new android.app.AlertDialog.Builder(ctx)
                    .setTitle("🤖 AI Negotiation Assistant")
                    .setMessage(tipMsg.toString())
                    .setPositiveButton("💬 Kirim via WA", (d, w) -> {
                        try {
                            String url = "https://wa.me/" + finalPhoneWa + "?text=" + Uri.encode(finalPesanWa);
                            ctx.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        } catch (Exception e) {
                            Toast.makeText(ctx, "Gagal membuka WhatsApp", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("📞 Telepon", (d, w) -> {
                        try {
                            ctx.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneDial)));
                        } catch (Exception e) {
                            Toast.makeText(ctx, "Tidak dapat membuka telepon", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNeutralButton("Tutup", null)
                    .show();
        });

        // Tombol Telepon langsung
        if (holder.btnTelepon != null) {
            holder.btnTelepon.setOnClickListener(v -> {
                String rawPhone = vehicle.sellerPhone;
                if (rawPhone == null || rawPhone.trim().isEmpty()) {
                    Toast.makeText(v.getContext(), "📵 Nomor penjual belum diisi", Toast.LENGTH_SHORT).show();
                    return;
                }
                String phoneDial = rawPhone.trim().startsWith("0") ? rawPhone.trim()
                        : "0" + rawPhone.trim().replaceFirst("^62", "");
                try {
                    v.getContext().startActivity(new Intent(Intent.ACTION_DIAL,
                            Uri.parse("tel:" + phoneDial)));
                } catch (Exception e) {
                    Toast.makeText(v.getContext(), "Tidak dapat membuka telepon", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Badge Lokasi + Tombol Maps
        String lokasi = vehicle.location;
        if (lokasi != null && !lokasi.trim().isEmpty()) {
            holder.tvLokasiKota.setText("📍 " + lokasi);
            holder.tvLokasiKota.setTextColor(0xFF1976D2);
            if (holder.btnLihatMaps != null) {
                holder.btnLihatMaps.setVisibility(View.VISIBLE);
                holder.btnLihatMaps.setOnClickListener(v -> {
                    try {
                        // Buka Google Maps dengan query nama kota
                        String query = Uri.encode(lokasi + ", Indonesia");
                        Uri mapsUri = Uri.parse("geo:0,0?q=" + query);
                        Intent mapsIntent = new Intent(Intent.ACTION_VIEW, mapsUri);
                        mapsIntent.setPackage("com.google.android.apps.maps");
                        if (mapsIntent.resolveActivity(v.getContext().getPackageManager()) != null) {
                            v.getContext().startActivity(mapsIntent);
                        } else {
                            // Fallback: buka di browser jika Google Maps tidak terinstal
                            String webUrl = "https://maps.google.com/?q=" + query;
                            v.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)));
                        }
                    } catch (Exception e) {
                        Toast.makeText(v.getContext(), "Tidak dapat membuka Maps", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else {
            holder.tvLokasiKota.setText("📍 Lokasi tidak tersedia");
            holder.tvLokasiKota.setTextColor(0xFF9E9E9E);
            if (holder.btnLihatMaps != null)
                holder.btnLihatMaps.setVisibility(View.GONE);
        }

        // ── Tombol Beri Rating ──
        if (holder.btnRating != null) {
            int ratingCount = db.getRatingCount(vehicle.id);
            float avgRat = db.getAverageRating(vehicle.id);
            if (ratingCount > 0) {
                holder.btnRating.setText("⭐ " + String.format("%.1f", avgRat) + " (" + ratingCount + " ulasan)");
            } else {
                holder.btnRating.setText("⭐ Beri Rating Penjual");
            }
            holder.btnRating.setOnClickListener(v -> {
                SessionManager session = new SessionManager(v.getContext());
                String reviewerName = session.isLoggedIn() ? session.getFullName() : "Pembeli Anonim";
                String sellerPhone  = vehicle.sellerPhone != null ? vehicle.sellerPhone : "";
                RatingHelper.show(
                    v.getContext(),
                    vehicle.id,
                    vehicle.name,
                    sellerPhone,
                    reviewerName,
                    (rating, comment) -> {
                        // Refresh label setelah rating dikirim
                        int newCount = db.getRatingCount(vehicle.id);
                        float newAvg = db.getAverageRating(vehicle.id);
                        holder.btnRating.setText("⭐ " + String.format("%.1f", newAvg) + " (" + newCount + " ulasan)");
                        // Update RatingBar di card
                        holder.ratingBar.setRating(newAvg);
                    }
                );
            });
        }

        // ── Tombol Tawar Harga ──
        if (holder.btnTawar != null) {
            int pendingOffers = db.getOffersForVehicle(vehicle.id).size();
            holder.btnTawar.setText(pendingOffers > 0
                    ? "🏷️ Tawar Harga (" + pendingOffers + " penawaran)"
                    : "🏷️ Tawar Harga");
            holder.btnTawar.setOnClickListener(v -> {
                SessionManager session = new SessionManager(v.getContext());
                String buyerName  = session.isLoggedIn() ? session.getFullName() : "";
                String buyerPhone = session.isLoggedIn() ? session.getPhone() : "";
                TawarHargaHelper.show(v.getContext(), db, vehicle, buyerName, buyerPhone,
                        (offerPrice, msg) -> {
                            int count = db.getOffersForVehicle(vehicle.id).size();
                            holder.btnTawar.setText("🏷️ Tawar Harga (" + count + " penawaran)");
                        });
            });
        }
    }

    private void updateFavButton(ImageButton btn, boolean isFav) {
        btn.setImageResource(isFav ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        btn.setColorFilter(isFav ? 0xFFE91E63 : 0xFF888888);
    }

    private String formatHarga(String raw) {
        try {
            long harga = Long.parseLong(raw.replaceAll("[^0-9]", ""));
            if (harga >= 1_000_000_000) return String.format("%.1f M", harga / 1_000_000_000.0);
            if (harga >= 1_000_000) return String.format("%.0f Jt", harga / 1_000_000.0);
            return String.format("%,.0f", (double) harga);
        } catch (Exception e) {
            return raw;
        }
    }

    private void setupFraudBadge(ViewHolder holder, DatabaseHelper.VehicleData vehicle) {
        FraudDetection fraudDetection = new FraudDetection(holder.itemView.getContext());
        double priceValue = 0;
        try { priceValue = Double.parseDouble(vehicle.price.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}

        FraudDetection.VehicleListing listing = new FraudDetection.VehicleListing(
                String.valueOf(vehicle.id), "seller_id", vehicle.type, vehicle.name,
                vehicle.year, priceValue, vehicle.description != null ? vehicle.description : "",
                vehicle.sellerPhone != null ? vehicle.sellerPhone : "", "", 1);

        FraudDetection.FraudResult result = fraudDetection.analyzeListing(listing);
        holder.tvFraudBadge.setVisibility(View.VISIBLE);
        holder.tvFraudBadge.setText(result.trustLevel);

        if (result.confidenceScore >= 0.7) {
            holder.tvFraudBadge.setBackgroundColor(0xCCCC0000);
            holder.tvFraudBadge.setTextColor(0xFFFFFFFF);
        } else if (result.confidenceScore >= 0.5) {
            holder.tvFraudBadge.setBackgroundColor(0xCCFF8800);
            holder.tvFraudBadge.setTextColor(0xFFFFFFFF);
        } else if (result.confidenceScore >= 0.3) {
            holder.tvFraudBadge.setBackgroundColor(0xCC009900);
            holder.tvFraudBadge.setTextColor(0xFFFFFFFF);
        } else {
            holder.tvFraudBadge.setText("✅ TERVERIFIKASI");
            holder.tvFraudBadge.setBackgroundColor(0xCC1976D2);
            holder.tvFraudBadge.setTextColor(0xFFFFFFFF);
        }
    }

    private void showGallery(android.content.Context context, String[] imageUris, int startIndex) {
        android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_full_image);

        androidx.viewpager2.widget.ViewPager2 viewPager = dialog.findViewById(R.id.viewPagerFull);
        android.widget.ImageButton btnClose = dialog.findViewById(R.id.btnCloseFullImage);
        android.widget.TextView tvCounter = dialog.findViewById(R.id.tvPhotoCounter);
        android.widget.LinearLayout dotContainer = dialog.findViewById(R.id.dotIndicator);
        android.widget.TextView tvHint = dialog.findViewById(R.id.tvSwipeHint);

        // Filter URI kosong
        java.util.List<String> uriList = new java.util.ArrayList<>();
        for (String uri : imageUris) {
            if (uri != null && !uri.trim().isEmpty()) uriList.add(uri.trim());
        }
        if (uriList.isEmpty()) { dialog.dismiss(); return; }

        int total = uriList.size();
        int safeStart = Math.min(startIndex, total - 1);

        // Setup adapter
        GalleryAdapter adapter = new GalleryAdapter(context, uriList);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(safeStart, false);

        // Buat dot indicator
        android.view.View[] dots = new android.view.View[total];
        if (total > 1) {
            for (int i = 0; i < total; i++) {
                android.view.View dot = new android.view.View(context);
                android.widget.LinearLayout.LayoutParams lp =
                        new android.widget.LinearLayout.LayoutParams(10, 10);
                lp.setMargins(5, 0, 5, 0);
                dot.setLayoutParams(lp);
                dot.setBackgroundResource(R.drawable.bg_circle_white);
                dot.setAlpha(i == safeStart ? 1f : 0.35f);
                dotContainer.addView(dot);
                dots[i] = dot;
            }
        } else {
            dotContainer.setVisibility(android.view.View.GONE);
            if (tvHint != null) tvHint.setVisibility(android.view.View.GONE);
        }

        // Update counter & dot saat swipe
        tvCounter.setText((safeStart + 1) + " / " + total);
        viewPager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tvCounter.setText((position + 1) + " / " + total);
                for (int i = 0; i < dots.length; i++) {
                    if (dots[i] != null)
                        dots[i].animate().alpha(i == position ? 1f : 0.35f).setDuration(200).start();
                }
            }
        });

        // Sembunyikan hint swipe setelah 2.5 detik
        if (tvHint != null && total > 1) {
            tvHint.postDelayed(() ->
                tvHint.animate().alpha(0f).setDuration(600).start(), 2500);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public int getItemCount() { return vehicleList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout containerFotoBeli;
        TextView tvName, tvPrice, tvYear, tvType, tvDescription, tvFraudBadge, tvLokasiKota;
        TextView tvAIPriceBadge, tvHealthScore, tvHealthLabel;
        ProgressBar pbHealthScore;
        RatingBar ratingBar;
        Button btnHubungi, btnTelepon, btnLihatMaps, btnRating, btnTawar;
        ImageButton btnFavorit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            containerFotoBeli = itemView.findViewById(R.id.containerFotoBeli);
            tvName            = itemView.findViewById(R.id.tvVehicleName);
            tvFraudBadge      = itemView.findViewById(R.id.tvFraudBadge);
            ratingBar         = itemView.findViewById(R.id.ratingBarSmall);
            tvPrice           = itemView.findViewById(R.id.tvVehiclePrice);
            tvYear            = itemView.findViewById(R.id.tvVehicleYear);
            tvType            = itemView.findViewById(R.id.tvVehicleType);
            tvDescription     = itemView.findViewById(R.id.tvVehicleDescription);
            btnHubungi        = itemView.findViewById(R.id.btnHubungiPenjual);
            btnTelepon        = itemView.findViewById(R.id.btnTeleponPenjual);
            btnFavorit        = itemView.findViewById(R.id.btnFavorit);
            tvLokasiKota      = itemView.findViewById(R.id.tvLokasiKota);
            btnLihatMaps      = itemView.findViewById(R.id.btnLihatMaps);
            tvAIPriceBadge    = itemView.findViewById(R.id.tvAIPriceBadge);
            pbHealthScore     = itemView.findViewById(R.id.pbHealthScore);
            tvHealthScore     = itemView.findViewById(R.id.tvHealthScore);
            tvHealthLabel     = itemView.findViewById(R.id.tvHealthLabel);
            btnRating         = itemView.findViewById(R.id.btnRating);
            btnTawar          = itemView.findViewById(R.id.btnTawar);
        }
    }
}
