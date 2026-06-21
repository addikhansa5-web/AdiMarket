package com.example.adimarket;

import java.util.List;

/**
 * AI Engine untuk analisis kendaraan secara lokal.
 * Tidak butuh internet — menggunakan data dari database.
 */
public class VehicleAIEngine {

    // ─── 1. PRICE FAIRNESS ANALYSIS ────────────────────────────────────────────

    public enum PriceLabel { MURAH, WAJAR, TINGGI, TIDAK_DIKETAHUI }

    public static class PriceFairness {
        public PriceLabel label;
        public String text;
        public int color;       // ARGB
        public String emoji;
        public double deviation; // % dari rata-rata (negatif = lebih murah)

        PriceFairness(PriceLabel label, String text, int color, String emoji, double deviation) {
            this.label = label; this.text = text; this.color = color;
            this.emoji = emoji; this.deviation = deviation;
        }
    }

    /**
     * Bandingkan harga iklan dengan rata-rata kendaraan tipe/tahun serupa.
     */
    public static PriceFairness analyzePriceFairness(
            DatabaseHelper.VehicleData vehicle,
            List<DatabaseHelper.VehicleData> allVehicles) {

        long myPrice = parsePrice(vehicle.price);
        if (myPrice <= 0) return new PriceFairness(PriceLabel.TIDAK_DIKETAHUI, "?", 0xFF9E9E9E, "❓", 0);

        // Kumpulkan harga kendaraan serupa (tipe sama, tahun ±3 tahun)
        long sum = 0; int count = 0;
        for (DatabaseHelper.VehicleData v : allVehicles) {
            if (v.id == vehicle.id) continue;
            long p = parsePrice(v.price);
            if (p <= 0) continue;
            boolean sameTipe  = v.type != null && v.type.equalsIgnoreCase(vehicle.type);
            boolean nearYear  = Math.abs(v.year - vehicle.year) <= 3;
            if (sameTipe && nearYear) { sum += p; count++; }
        }

        if (count < 2) {
            // Data tidak cukup — gunakan semua tipe yang sama
            for (DatabaseHelper.VehicleData v : allVehicles) {
                if (v.id == vehicle.id) continue;
                long p = parsePrice(v.price);
                boolean sameTipe = v.type != null && v.type.equalsIgnoreCase(vehicle.type);
                if (sameTipe && p > 0) { sum += p; count++; }
            }
        }

        if (count == 0) return new PriceFairness(PriceLabel.TIDAK_DIKETAHUI, "Baru", 0xFF1976D2, "🆕", 0);

        double avg = (double) sum / count;
        double deviation = ((myPrice - avg) / avg) * 100.0;

        if (deviation <= -15) return new PriceFairness(PriceLabel.MURAH,  "MURAH",  0xFF2E7D32, "🟢", deviation);
        if (deviation <= 10)  return new PriceFairness(PriceLabel.WAJAR,  "WAJAR",  0xFFF57F17, "🟡", deviation);
        return                       new PriceFairness(PriceLabel.TINGGI, "TINGGI", 0xFFC62828, "🔴", deviation);
    }

    // ─── 2. VEHICLE HEALTH SCORE ───────────────────────────────────────────────

    public static class HealthScore {
        public int score;         // 0–100
        public String grade;      // A / B / C / D
        public int color;         // ARGB untuk progress bar
        public String summary;    // Teks ringkasan

        HealthScore(int score) {
            this.score = score;
            if (score >= 80) { grade = "A"; color = 0xFF2E7D32; summary = "Kondisi Sangat Baik ✨"; }
            else if (score >= 60) { grade = "B"; color = 0xFF1976D2; summary = "Kondisi Baik 👍"; }
            else if (score >= 40) { grade = "C"; color = 0xFFF57F17; summary = "Perlu Pengecekan ⚠️"; }
            else { grade = "D"; color = 0xFFC62828; summary = "Cek Kondisi Fisik ❗"; }
        }
    }

    /**
     * Hitung Health Score kendaraan dari 4 faktor:
     * 1. Usia (tahun pembuatan) — 30%
     * 2. Kewajaran harga — 30%
     * 3. Kelengkapan informasi — 20%
     * 4. Jumlah foto — 20%
     */
    public static HealthScore calcHealthScore(
            DatabaseHelper.VehicleData v,
            List<DatabaseHelper.VehicleData> allVehicles) {

        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        int age = currentYear - v.year;

        // 1. Skor usia (0-30): semakin baru semakin tinggi
        int scoreAge = Math.max(0, 30 - (age * 3));

        // 2. Skor harga (0-30): harga wajar = skor tinggi
        PriceFairness pf = analyzePriceFairness(v, allVehicles);
        int scorePrice = pf.label == PriceLabel.MURAH ? 30 :
                         pf.label == PriceLabel.WAJAR ? 25 :
                         pf.label == PriceLabel.TINGGI ? 10 : 20;

        // 3. Skor informasi (0-20): nama + deskripsi lengkap
        int scoreInfo = 0;
        if (v.name != null && v.name.length() > 3) scoreInfo += 8;
        if (v.description != null && v.description.length() > 30) scoreInfo += 7;
        if (v.sellerPhone != null && !v.sellerPhone.isEmpty()) scoreInfo += 5;

        // 4. Skor foto (0-20): lebih banyak foto = lebih transparan
        int photoCount = 0;
        if (v.imageUri != null && !v.imageUri.isEmpty()) {
            photoCount = v.imageUri.split("[|,]").length;
        }
        int scorePhoto = Math.min(20, photoCount * 5); // max di 4 foto

        int total = scoreAge + scorePrice + scoreInfo + scorePhoto;
        return new HealthScore(Math.min(100, total));
    }

    // ─── 3. NEGOTIATION TIPS ──────────────────────────────────────────────────

    public static class NegotiationTip {
        public String recommendedPrice;
        public String priceRange;
        public String[] checkPoints;
        public String whatsappScript;
    }

    public static NegotiationTip generateNegotiationTip(
            DatabaseHelper.VehicleData vehicle,
            List<DatabaseHelper.VehicleData> allVehicles) {

        NegotiationTip tip = new NegotiationTip();
        long price = parsePrice(vehicle.price);

        // Harga tawar: 85–95% dari harga listing
        long minOffer = (long)(price * 0.85);
        long maxOffer = (long)(price * 0.95);
        tip.recommendedPrice = formatRp(maxOffer);
        tip.priceRange = formatRp(minOffer) + " – " + formatRp(maxOffer);

        // Poin pengecekan berdasarkan tipe & usia
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        int age = currentYear - vehicle.year;
        boolean isMobil = "Mobil".equalsIgnoreCase(vehicle.type);
        boolean isTua = age > 5;

        tip.checkPoints = new String[]{
            isMobil ? "Cek kondisi mesin & oli (nyalakan, dengarkan suara)" : "Cek kondisi mesin & filter udara",
            isMobil ? "Test AC, power window, dan semua lampu" : "Cek kondisi ban & rantai/belt",
            isTua ? "Minta buku servis & STNK asli untuk verifikasi tahun" : "Pastikan garansi masih aktif",
            "Cek nomor rangka & nomor mesin sesuai STNK/BPKB",
            isMobil ? "Test drive minimal 5 menit, perhatikan rem & setir" : "Coba starter & test ride singkat"
        };

        // Script WA
        tip.whatsappScript = String.format(
            "Halo, saya tertarik dengan *%s* tahun %d yang Anda pasang di AdiMarket.\n" +
            "Apakah kondisinya masih oke? Boleh saya lihat langsung?\n" +
            "Untuk harga, apakah bisa di angka *%s*? Saya serius ingin beli 🙏",
            vehicle.name, vehicle.year, tip.recommendedPrice
        );

        return tip;
    }

    // ─── 4. SMART SEARCH INTENT PARSER ───────────────────────────────────────

    public static class SearchIntent {
        public String vehicleType = "";   // Mobil / Motor
        public String brand = "";
        public long budgetMax = 0;
        public int yearMin = 0;
        public String usage = "";         // harian, keluarga, perempuan, dll
        public boolean wantCheap = false;
        public boolean wantNew = false;
    }

    public static SearchIntent parseSearchIntent(String query) {
        SearchIntent intent = new SearchIntent();
        String q = query.toLowerCase();

        // Tipe
        if (q.contains("mobil") || q.contains("sedan") || q.contains("suv") || q.contains("mpv") || q.contains("minibus")) intent.vehicleType = "Mobil";
        else if (q.contains("motor") || q.contains("moto") || q.contains("matic") || q.contains("matik") || q.contains("bebek")) intent.vehicleType = "Motor";

        // Merek
        String[] brands = {"toyota","honda","yamaha","suzuki","daihatsu","mitsubishi","kawasaki","nissan","ford","bmw","hyundai"};
        for (String b : brands) { if (q.contains(b)) { intent.brand = b; break; } }

        // Budget
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d+[.,]?\\d*)\\s*(juta|jt|m)?").matcher(q);
        while (m.find()) {
            try {
                String angka = m.group(1).replace(",", ".");
                String satuan = m.group(2) != null ? m.group(2) : "";
                double val = Double.parseDouble(angka);
                long result = 0;
                if (satuan.startsWith("juta") || satuan.equals("jt")) result = (long)(val * 1_000_000);
                else if (satuan.equals("m")) result = (long)(val * 1_000_000_000);
                else if (val >= 10 && val <= 9999) result = (long)(val * 1_000_000);
                if (result > 0 && result > intent.budgetMax) intent.budgetMax = result;
            } catch (Exception ignored) {}
        }

        // Tahun
        java.util.regex.Matcher ym = java.util.regex.Pattern.compile("(20\\d{2}|19\\d{2})").matcher(q);
        if (ym.find()) { try { intent.yearMin = Integer.parseInt(ym.group(1)); } catch (Exception ignored) {} }

        // Preferensi
        intent.wantCheap = q.contains("murah") || q.contains("hemat") || q.contains("ekonomis");
        intent.wantNew   = q.contains("baru") || q.contains("terbaru") || q.contains("muda");
        if (q.contains("keluarga") || q.contains("family")) intent.usage = "keluarga";
        else if (q.contains("perempuan") || q.contains("wanita") || q.contains("ibu")) intent.usage = "perempuan";
        else if (q.contains("harian") || q.contains("commuter")) intent.usage = "harian";

        return intent;
    }

    /**
     * Filter kendaraan berdasarkan SearchIntent yang sudah diparsing.
     */
    public static List<DatabaseHelper.VehicleData> applySmartSearch(
            SearchIntent intent,
            List<DatabaseHelper.VehicleData> allVehicles) {

        List<DatabaseHelper.VehicleData> result = new java.util.ArrayList<>();
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);

        for (DatabaseHelper.VehicleData v : allVehicles) {
            // Tipe
            if (!intent.vehicleType.isEmpty() && !v.type.equalsIgnoreCase(intent.vehicleType)) continue;

            // Merek
            if (!intent.brand.isEmpty() && v.name != null && !v.name.toLowerCase().contains(intent.brand)) continue;

            // Budget
            if (intent.budgetMax > 0) {
                long p = parsePrice(v.price);
                if (p > intent.budgetMax) continue;
            }

            // Tahun minimum
            if (intent.yearMin > 0 && v.year < intent.yearMin) continue;

            // Mau yang terbaru
            if (intent.wantNew && (currentYear - v.year) > 5) continue;

            // Penggunaan keluarga → prioritaskan MPV/Mobil
            if ("keluarga".equals(intent.usage) && !"Mobil".equalsIgnoreCase(v.type)) continue;

            // Penggunaan perempuan → prioritaskan motor ringan
            if ("perempuan".equals(intent.usage) && "Motor".equalsIgnoreCase(v.type)) {
                long p = parsePrice(v.price);
                if (p > 30_000_000) continue; // motor mahal skip
            }

            result.add(v);
        }

        // Sort: kalau mau murah → urutkan harga ascending
        if (intent.wantCheap) {
            result.sort((a, b) -> Long.compare(parsePrice(a.price), parsePrice(b.price)));
        }

        return result;
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    public static long parsePrice(String price) {
        try { return Long.parseLong(price.replaceAll("[^0-9]", "")); }
        catch (Exception e) { return 0; }
    }

    public static String formatRp(long val) {
        if (val >= 1_000_000_000) return String.format("Rp %.1f M", val / 1_000_000_000.0);
        if (val >= 1_000_000)     return String.format("Rp %.0f Jt", val / 1_000_000.0);
        return val > 0 ? String.format("Rp %,.0f", (double)val) : "?";
    }
}
