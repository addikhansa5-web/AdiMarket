package com.example.adimarket;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

/**
 * Helper untuk dialog Tawar Harga — negosiasi harga in-app antara pembeli dan penjual.
 */
public class TawarHargaHelper {

    public interface OnOfferSentListener {
        void onOfferSent(String offerPrice, String message);
    }

    /**
     * Tampilkan dialog tawar harga.
     *
     * @param context      Activity context
     * @param db           DatabaseHelper instance
     * @param vehicle      Kendaraan yang ditawar
     * @param buyerName    Nama pembeli (dari sesi login jika ada)
     * @param buyerPhone   No. HP pembeli
     * @param listener     Callback setelah penawaran dikirim
     */
    public static void show(Context context, DatabaseHelper db,
                            DatabaseHelper.VehicleData vehicle,
                            String buyerName, String buyerPhone,
                            OnOfferSentListener listener) {

        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_tawar_harga, null);

        TextView tvVehicleName  = dialogView.findViewById(R.id.tvOfferVehicleName);
        TextView tvListPrice    = dialogView.findViewById(R.id.tvOfferListPrice);
        EditText etOfferPrice   = dialogView.findViewById(R.id.etOfferPrice);
        EditText etMessage      = dialogView.findViewById(R.id.etOfferMessage);
        EditText etBuyerName    = dialogView.findViewById(R.id.etOfferBuyerName);
        EditText etBuyerPhone   = dialogView.findViewById(R.id.etOfferBuyerPhone);

        tvVehicleName.setText(vehicle.name + " (" + vehicle.year + ")");

        // Format harga listing
        String listPriceText;
        try {
            long p = Long.parseLong(vehicle.price.replaceAll("[^0-9]", ""));
            if (p >= 1_000_000_000) listPriceText = String.format("Rp %.1f M", p / 1_000_000_000.0);
            else if (p >= 1_000_000) listPriceText = String.format("Rp %.0f Jt", p / 1_000_000.0);
            else listPriceText = "Rp " + String.format("%,d", p);
        } catch (Exception e) { listPriceText = "Rp " + vehicle.price; }
        tvListPrice.setText(listPriceText);

        // Pre-fill buyer info dari sesi
        if (!buyerName.isEmpty()) etBuyerName.setText(buyerName);
        if (!buyerPhone.isEmpty()) etBuyerPhone.setText(buyerPhone);

        new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton("🏷️ Kirim Penawaran", (d, w) -> {
                    String priceInput  = etOfferPrice.getText().toString().trim();
                    String msgInput    = etMessage.getText().toString().trim();
                    String nameInput   = etBuyerName.getText().toString().trim();
                    String phoneInput  = etBuyerPhone.getText().toString().trim();

                    if (priceInput.isEmpty()) {
                        Toast.makeText(context, "Masukkan harga tawaran dulu!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Simpan ke DB
                    String offerPriceFormatted = priceInput + " Jt";
                    String sellerPhone = vehicle.sellerPhone != null ? vehicle.sellerPhone : "";

                    db.addOffer(vehicle.id, nameInput, phoneInput,
                            sellerPhone, offerPriceFormatted, msgInput);

                    Toast.makeText(context,
                            "✅ Penawaran Rp " + priceInput + " Jt terkirim!",
                            Toast.LENGTH_LONG).show();

                    // Kirim via WhatsApp ke penjual juga
                    if (!sellerPhone.isEmpty()) {
                        String waPhone = sellerPhone.trim().startsWith("0")
                                ? "62" + sellerPhone.trim().substring(1)
                                : sellerPhone.trim().replaceFirst("^0", "62");

                        String waMsg = "Halo, saya " + (nameInput.isEmpty() ? "pembeli" : nameInput)
                                + " tertarik dengan *" + vehicle.name + "* ("+ vehicle.year + ").\n\n"
                                + "💰 Harga tawaran saya: *Rp " + priceInput + " Juta*\n"
                                + (msgInput.isEmpty() ? "" : "\n💬 " + msgInput + "\n")
                                + "\nBoleh kita negosiasi? 🙏";

                        try {
                            String waUrl = "https://api.whatsapp.com/send?phone=" + waPhone
                                    + "&text=" + Uri.encode(waMsg);
                            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)));
                        } catch (Exception ignored) {}
                    }

                    if (listener != null) listener.onOfferSent(offerPriceFormatted, msgInput);
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}
