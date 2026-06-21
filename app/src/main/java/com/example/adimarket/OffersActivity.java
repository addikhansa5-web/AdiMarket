package com.example.adimarket;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Kotak Penawaran Harga — dua tab:
 *   📥 MASUK  : penawaran dari pembeli ke saya (sebagai penjual)
 *   📤 TERKIRIM: penawaran yang saya kirim (sebagai pembeli) + balasan penjual
 */
public class OffersActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private SessionManager session;

    private TextView tabMasuk, tabTerkirim;
    private LinearLayout llMasuk, llTerkirim;
    private LinearLayout listMasuk, listTerkirim;
    private TextView tvEmptyMasuk, tvEmptyTerkirim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DarkModeManager.applyStoredMode(this);
        setContentView(R.layout.activity_offers);

        db      = new DatabaseHelper(this);
        session = new SessionManager(this);

        // Bind views
        tabMasuk       = findViewById(R.id.tabOfferMasuk);
        tabTerkirim    = findViewById(R.id.tabOfferTerkirim);
        llMasuk        = findViewById(R.id.llOffersMasuk);
        llTerkirim     = findViewById(R.id.llOffersTerkirim);
        listMasuk      = findViewById(R.id.listMasuk);
        listTerkirim   = findViewById(R.id.listTerkirim);
        tvEmptyMasuk   = findViewById(R.id.tvEmptyMasuk);
        tvEmptyTerkirim= findViewById(R.id.tvEmptyTerkirim);

        // Back button
        ImageButton btnBack = findViewById(R.id.btnBackOffers);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Tabs
        tabMasuk.setOnClickListener(v -> showTab(true));
        tabTerkirim.setOnClickListener(v -> showTab(false));

        showTab(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAll();
    }

    private void showTab(boolean masuk) {
        llMasuk.setVisibility(masuk ? View.VISIBLE : View.GONE);
        llTerkirim.setVisibility(masuk ? View.GONE : View.VISIBLE);

        tabMasuk.setBackgroundResource(masuk
                ? R.drawable.btn_green_rounded : R.drawable.btn_outline_rounded);
        tabMasuk.setTextColor(masuk ? 0xFFFFFFFF : getColor(R.color.primary_green));
        tabTerkirim.setBackgroundResource(masuk
                ? R.drawable.btn_outline_rounded : R.drawable.btn_green_rounded);
        tabTerkirim.setTextColor(masuk ? getColor(R.color.primary_green) : 0xFFFFFFFF);

        refreshAll();
    }

    private void refreshAll() {
        String myPhone = session.isLoggedIn() ? session.getPhone() : "";

        // ── TAB MASUK (saya sebagai penjual) ──
        List<DatabaseHelper.OfferData> masukList = myPhone.isEmpty()
                ? new ArrayList<>() : db.getOffersForSeller(myPhone);
        listMasuk.removeAllViews();
        if (masukList.isEmpty()) {
            tvEmptyMasuk.setVisibility(View.VISIBLE);
        } else {
            tvEmptyMasuk.setVisibility(View.GONE);
            for (DatabaseHelper.OfferData offer : masukList) {
                listMasuk.addView(buildMasukCard(offer));
            }
        }

        // ── TAB TERKIRIM (saya sebagai pembeli) ──
        List<DatabaseHelper.OfferData> terkirimList = myPhone.isEmpty()
                ? new ArrayList<>() : db.getOffersByBuyerPhone(myPhone);
        listTerkirim.removeAllViews();
        if (terkirimList.isEmpty()) {
            tvEmptyTerkirim.setVisibility(View.VISIBLE);
        } else {
            tvEmptyTerkirim.setVisibility(View.GONE);
            for (DatabaseHelper.OfferData offer : terkirimList) {
                listTerkirim.addView(buildTerkirimCard(offer));
            }
        }
    }

    // ── Kartu PENAWARAN MASUK (tampilan penjual) ──────────────────────────────

    private View buildMasukCard(DatabaseHelper.OfferData o) {
        View card = getLayoutInflater().inflate(R.layout.item_offer_masuk, null);

        TextView tvBuyer   = card.findViewById(R.id.tvOfferBuyerName);
        TextView tvPrice   = card.findViewById(R.id.tvOfferPrice);
        TextView tvMsg     = card.findViewById(R.id.tvOfferMsg);
        TextView tvStatus  = card.findViewById(R.id.tvOfferStatus);
        TextView tvDate    = card.findViewById(R.id.tvOfferDate);
        Button btnTerima   = card.findViewById(R.id.btnTerima);
        Button btnTolak    = card.findViewById(R.id.btnTolak);
        Button btnBalas    = card.findViewById(R.id.btnBalasHarga);

        tvBuyer.setText("👤 " + notEmpty(o.buyerName, "Pembeli") + "  📱 " + notEmpty(o.buyerPhone, "-"));
        tvPrice.setText("💰 Penawaran: Rp " + o.offerPrice);
        tvMsg.setText(o.message.isEmpty() ? "" : "💬 " + o.message);
        tvDate.setText(formatDate(o.createdAt));

        boolean done = !o.status.equals("pending");
        applyStatusBadge(tvStatus, o);

        // Sembunyikan tombol jika sudah dibalas
        btnTerima.setVisibility(done ? View.GONE : View.VISIBLE);
        btnTolak.setVisibility(done ? View.GONE : View.VISIBLE);
        btnBalas.setVisibility(done ? View.GONE : View.VISIBLE);

        if (!done) {
            btnTerima.setOnClickListener(v -> {
                db.sellerReply(o.id, "accepted", "", "Penjual menerima penawaran Anda! 🎉");
                notifyBuyerWhatsApp(o, "accepted", "", "Penawaran Anda diterima! 🎉 Silakan hubungi saya untuk proses selanjutnya.");
                refreshAll();
            });
            btnTolak.setOnClickListener(v -> {
                db.sellerReply(o.id, "rejected", "", "Maaf, penawaran tidak dapat diterima.");
                notifyBuyerWhatsApp(o, "rejected", "", "Mohon maaf, penawaran Anda tidak dapat kami terima saat ini.");
                refreshAll();
            });
            btnBalas.setOnClickListener(v -> showCounterDialog(o));
        }

        return card;
    }

    // ── Kartu PENAWARAN TERKIRIM (tampilan pembeli) ───────────────────────────

    private View buildTerkirimCard(DatabaseHelper.OfferData o) {
        View card = getLayoutInflater().inflate(R.layout.item_offer_terkirim, null);

        TextView tvVehicle    = card.findViewById(R.id.tvTerkirimVehicle);
        TextView tvMyPrice    = card.findViewById(R.id.tvTerkirimPrice);
        TextView tvMyMsg      = card.findViewById(R.id.tvTerkirimMsg);
        TextView tvStatus     = card.findViewById(R.id.tvTerkirimStatus);
        TextView tvDate       = card.findViewById(R.id.tvTerkirimDate);
        View     layoutReply  = card.findViewById(R.id.layoutSellerReply);
        TextView tvReplyText  = card.findViewById(R.id.tvSellerReplyBox);

        // Cari nama kendaraan dari DB
        String vehicleName = "Kendaraan #" + o.vehicleId;
        List<DatabaseHelper.VehicleData> all = db.getAllVehicles();
        for (DatabaseHelper.VehicleData v : all) {
            if (v.id == o.vehicleId) { vehicleName = v.name + " (" + v.year + ")"; break; }
        }

        tvVehicle.setText("🚗 " + vehicleName);
        tvMyPrice.setText("💰 Tawaran saya: Rp " + o.offerPrice);
        tvMyMsg.setText(o.message.isEmpty() ? "" : "💬 " + o.message);
        tvDate.setText(formatDate(o.createdAt));
        applyStatusBadge(tvStatus, o);

        // Balasan penjual
        if (!o.status.equals("pending") && (!o.sellerReply.isEmpty() || !o.counterPrice.isEmpty())) {
            layoutReply.setVisibility(View.VISIBLE);
            StringBuilder sb = new StringBuilder("📨 Balasan Penjual:\n");
            if (!o.sellerReply.isEmpty())  sb.append(o.sellerReply);
            if (!o.counterPrice.isEmpty()) sb.append("\n💬 Tawaran balik: Rp ").append(o.counterPrice);
            tvReplyText.setText(sb.toString());
        } else if (!o.status.equals("pending")) {
            layoutReply.setVisibility(View.VISIBLE);
            tvReplyText.setText("📨 Penjual belum memberikan keterangan.");
        } else {
            layoutReply.setVisibility(View.GONE);
        }

        return card;
    }

    // ── Dialog: penjual balas dengan harga counter ─────────────────────────

    private void showCounterDialog(DatabaseHelper.OfferData o) {
        View dv = getLayoutInflater().inflate(R.layout.dialog_seller_reply, null);
        EditText etCounter = dv.findViewById(R.id.etCounterPrice);
        EditText etReply   = dv.findViewById(R.id.etSellerReplyMsg);
        ((TextView) dv.findViewById(R.id.tvReplyBuyerName))
                .setText("Balas ke: " + notEmpty(o.buyerName, "Pembeli") + " (Rp " + o.offerPrice + ")");

        new AlertDialog.Builder(this)
                .setView(dv)
                .setPositiveButton("📨 Kirim Balasan", (d, w) -> {
                    String counter = etCounter.getText().toString().trim();
                    String reply   = etReply.getText().toString().trim();
                    String status  = counter.isEmpty() ? "replied" : "countered";
                    db.sellerReply(o.id, status, counter.isEmpty() ? "" : counter + " Jt", reply);
                    notifyBuyerWhatsApp(o, status, counter, reply);
                    refreshAll();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    // ── Kirim notif WA ke pembeli ─────────────────────────────────────────

    private void notifyBuyerWhatsApp(DatabaseHelper.OfferData o, String status,
                                     String counter, String replyMsg) {
        if (o.buyerPhone.isEmpty()) return;
        String waPhone = o.buyerPhone.trim().startsWith("0")
                ? "62" + o.buyerPhone.trim().substring(1)
                : o.buyerPhone.trim().replaceFirst("^0", "62");

        String emoji = status.equals("accepted") ? "✅" : status.equals("rejected") ? "❌" : "💬";
        String text  = emoji + " *Balasan Penawaran AdiMarket*\n\n"
                + "Kendaraan: *" + "ID #" + o.vehicleId + "*\n"
                + "Penawaran Anda: Rp " + o.offerPrice + "\n";
        if (!replyMsg.isEmpty()) text += "\nPesan: " + replyMsg;
        if (!counter.isEmpty())  text += "\nTawaran balik penjual: Rp *" + counter + " Jt*";

        try {
            String url = "https://api.whatsapp.com/send?phone=" + waPhone
                    + "&text=" + Uri.encode(text);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {}
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void applyStatusBadge(TextView tv, DatabaseHelper.OfferData o) {
        switch (o.status) {
            case "accepted":
                tv.setText("✅ Diterima"); tv.setTextColor(0xFF2E7D32); break;
            case "rejected":
                tv.setText("❌ Ditolak"); tv.setTextColor(0xFFC62828); break;
            case "countered":
                tv.setText("💬 Dibalas (harga baru)"); tv.setTextColor(0xFFE65100); break;
            case "replied":
                tv.setText("📨 Dibalas"); tv.setTextColor(0xFF1565C0); break;
            default:
                tv.setText("⏳ Menunggu Balasan"); tv.setTextColor(0xFF757575); break;
        }
    }

    private String formatDate(long ms) {
        return new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id")).format(new Date(ms));
    }

    private String notEmpty(String s, String fallback) {
        return (s == null || s.trim().isEmpty()) ? fallback : s.trim();
    }
}
