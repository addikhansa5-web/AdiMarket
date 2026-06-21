package com.example.adimarket;

import android.content.Context;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import android.graphics.Bitmap;
import android.util.Base64;
import java.io.ByteArrayOutputStream;

public class GroqHelper {
    // Menggunakan API Key dari local.properties via BuildConfig
    private static final String API_KEY = BuildConfig.GROQ_API_KEY;
    private static final String MODEL = "llama-3.3-70b-versatile";
    private final OkHttpClient client;
    private final Context context;

    public GroqHelper(Context context) {
        this.context = context;
        this.client = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build();
    }

    public interface GroqCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public void askGroq(List<ChatbotActivity.ChatMessage> history, GroqCallback callback) {
        DatabaseHelper db = new DatabaseHelper(context);
        List<DatabaseHelper.VehicleData> vehicles = db.getAllVehicles();
        String lastUserMsg = history.get(history.size() - 1).message;

        // JIKA API KEY BELUM DIISI -> GUNAKAN INTENT ENGINE LOKAL YANG CANGGIH
        if (API_KEY.contains("MASUKKAN_API_KEY")) {
            callback.onSuccess(getAdvancedLocalResponse(lastUserMsg, vehicles));
            return;
        }

        // JIKA API KEY ADA -> CLOUD AI
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", MODEL);
            JSONArray messages = new JSONArray();
            
            StringBuilder contextStr = new StringBuilder("Stok AdiMarket: ");
            for(DatabaseHelper.VehicleData v : vehicles) contextStr.append(v.name).append(" (").append(v.price).append("), ");

            messages.put(new JSONObject().put("role", "system").put("content", 
                "Anda adalah AdiMarket Expert. Gunakan data ini: " + contextStr.toString() + 
                ". Jawab dengan ramah, berikan rekomendasi spesifik jika ditanya 'beli/cari', " +
                "dan berikan langkah-langkah jika ditanya 'jual'."));

            for (ChatbotActivity.ChatMessage msg : history) {
                messages.put(new JSONObject().put("role", msg.isUser ? "user" : "assistant").put("content", msg.message));
            }

            jsonBody.put("messages", messages);
            RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json"));
            Request request = new Request.Builder().url("https://api.groq.com/openai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + API_KEY).post(body).build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) { callback.onSuccess(getAdvancedLocalResponse(lastUserMsg, vehicles)); }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            String result = new JSONObject(response.body().string()).getJSONArray("choices")
                                    .getJSONObject(0).getJSONObject("message").getString("content");
                            callback.onSuccess(result);
                        } catch (Exception e) { callback.onSuccess(getAdvancedLocalResponse(lastUserMsg, vehicles)); }
                    } else { callback.onSuccess(getAdvancedLocalResponse(lastUserMsg, vehicles)); }
                }
            });
        } catch (Exception e) { callback.onSuccess(getAdvancedLocalResponse(lastUserMsg, vehicles)); }
    }

    public void suggestPrice(String merek, String nama, String tipe, int tahun, GroqCallback callback) {
        if (API_KEY.contains("MASUKKAN_API_KEY") || API_KEY.isEmpty()) {
            callback.onError("API_KEY_MISSING");
            return;
        }
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", MODEL);
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content",
                        "Anda adalah ahli valuasi dan copywriter kendaraan bekas Indonesia terbaik. " +
                        "Tugas Anda: berikan estimasi harga jual yang realistis di pasar Indonesia saat ini, " +
                        "dan buatlah iklan penjualan yang sangat profesional, formal, bersih, informatif, dan ringkas tanpa menggunakan emoji, ikon, atau simbol dekoratif apapun. " +
                        "Jawab HANYA dalam format JSON valid berikut tanpa teks tambahan:\n" +
                        "{\"harga\": 95000000, \"deskripsi\": \"teks iklan lengkap di sini\"}"));
            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content",
                        "Buatkan estimasi harga jual dan teks iklan profesional tanpa emoji untuk kendaraan berikut:\n" +
                        "Merek: " + merek + "\n" +
                        "Model: " + nama + "\n" +
                        "Tipe: " + tipe + "\n" +
                        "Tahun: " + tahun + "\n\n" +
                        "Jawab HANYA dengan JSON valid: {\"harga\": angka, \"deskripsi\": \"teks iklan\"}"));
            jsonBody.put("messages", messages);
            jsonBody.put("temperature", 0.3);
            jsonBody.put("max_tokens", 500);

            RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .post(body).build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String content = new JSONObject(response.body().string()).getJSONArray("choices")
                                    .getJSONObject(0).getJSONObject("message").getString("content").trim();
                            callback.onSuccess(content);
                        } catch (Exception e) { callback.onError("Gagal membaca respons AI"); }
                    } else {
                        String errBody = response.body() != null ? response.body().string() : "";
                        callback.onError("Error " + response.code() + ": " + errBody);
                    }
                }
            });
        } catch (Exception e) { callback.onError(e.getMessage()); }
    }

    public void analyzeVehicleImage(Bitmap bitmap, GroqCallback callback) {
        if (API_KEY.contains("MASUKKAN_API_KEY")) {
            callback.onError("API_KEY_MISSING");
            return;
        }

        try {
            // Resize the image to prevent payload from being too large (>4MB causes 400 Bad Request)
            int maxWidth = 1024;
            int maxHeight = 1024;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > maxWidth || height > maxHeight) {
                float ratioBitmap = (float) width / (float) height;
                float ratioMax = (float) maxWidth / (float) maxHeight;
                int finalWidth = maxWidth;
                int finalHeight = maxHeight;
                if (ratioMax > ratioBitmap) {
                    finalWidth = (int) ((float)maxHeight * ratioBitmap);
                } else {
                    finalHeight = (int) ((float)maxWidth / ratioBitmap);
                }
                bitmap = Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true);
            }

            // Compress and convert to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "meta-llama/llama-4-scout-17b-16e-instruct");
            jsonBody.put("temperature", 0.1);
            
            JSONArray messages = new JSONArray();
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            
            JSONArray contentArr = new JSONArray();
            
            JSONObject textContent = new JSONObject();
            textContent.put("type", "text");
            textContent.put("text", "Anda adalah AI ahli otomotif kelas dunia. Tugas Anda: analisis gambar kendaraan ini dan hasilkan output WAJIB DALAM FORMAT JSON VALID yang berisi kunci-kunci berikut (tanpa blok kode tambahan):\n\"tipe\" (pilih: Mobil / Motor / Truk)\n\"merek\" (contoh: Honda, Toyota, Yamaha)\n\"nama\" (model lengkap, misal: Civic Turbo, NMAX 155)\n\"tahun\" (tebak perkiraan tahun pembuatannya, misal: 2020)\n\"harga\" (tebak harga wajar pasaran Indonesia dalam angka murni, misal: 150000000)\n\"deskripsi\" (buatlah iklan jualan yang sangat profesional, formal, informatif, bersih, dan ringkas tanpa menggunakan emoji, ikon, atau simbol dekoratif apapun, melainkan hanya berfokus pada detail spesifikasi kendaraan berdasarkan visual).");
            contentArr.put(textContent);
            
            JSONObject imageContent = new JSONObject();
            imageContent.put("type", "image_url");
            JSONObject imgUrl = new JSONObject();
            imgUrl.put("url", "data:image/jpeg;base64," + base64Image);
            imageContent.put("image_url", imgUrl);
            contentArr.put(imageContent);
            
            userMsg.put("content", contentArr);
            messages.put(userMsg);
            jsonBody.put("messages", messages);
            
            RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json"));
            Request request = new Request.Builder().url("https://api.groq.com/openai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + API_KEY).post(body).build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String result = new JSONObject(response.body().string()).getJSONArray("choices")
                                    .getJSONObject(0).getJSONObject("message").getString("content");
                            callback.onSuccess(result);
                        } catch (Exception e) { callback.onError("Gagal membaca respons JSON AI"); }
                    } else { 
                        String errBody = response.body() != null ? response.body().string() : "";
                        callback.onError("Error API: " + response.code() + " - " + errBody); 
                    }
                }
            });
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    private String getAdvancedLocalResponse(String query, List<DatabaseHelper.VehicleData> vehicles) {
        String q = query.toLowerCase();

        // ── 1. REKOMENDASI / CARI / BELI ──
        if (q.contains("rekomen") || q.contains("cari") || q.contains("beli")
                || q.contains("mau") || q.contains("pengen") || q.contains("ingin")
                || q.contains("budget") || q.contains("juta") || q.contains("harga")) {

            if (vehicles.isEmpty()) {
                return "Saat ini stok kendaraan di AdiMarket masih kosong 😔\n"
                     + "Silakan cek lagi nanti atau hubungi admin ya!";
            }

            // Ekstrak budget dari pesan
            long budgetMax = extractBudget(q);

            // Filter tipe
            String filterTipe = "";
            if (q.contains("mobil") || q.contains("sedan") || q.contains("suv") || q.contains("mpv")) filterTipe = "Mobil";
            else if (q.contains("motor") || q.contains("sepeda motor") || q.contains("matik") || q.contains("matic")) filterTipe = "Motor";

            // Filter merek populer
            String[] mereks = {"toyota","honda","yamaha","suzuki","daihatsu","mitsubishi","nissan","kawasaki","ford","bmw","mercedes","hyundai","kia"};
            String filterMerek = "";
            for (String m : mereks) {
                if (q.contains(m)) { filterMerek = m; break; }
            }

            // Cari kendaraan yang cocok
            List<DatabaseHelper.VehicleData> matches = new java.util.ArrayList<>();
            for (DatabaseHelper.VehicleData v : vehicles) {
                long harga = parseHarga(v.price);
                boolean hargaOk = (budgetMax == 0) || (harga > 0 && harga <= budgetMax);
                boolean tipeOk  = filterTipe.isEmpty() || v.type.toLowerCase().contains(filterTipe.toLowerCase());
                boolean merekOk = filterMerek.isEmpty() || v.name.toLowerCase().contains(filterMerek);
                if (hargaOk && tipeOk && merekOk) matches.add(v);
            }

            if (matches.isEmpty()) {
                // Tidak ada yang cocok — tampilkan semua sebagai alternatif
                StringBuilder sb = new StringBuilder();
                sb.append("Hmm, belum ada kendaraan yang persis cocok dengan kriteria Anda saat ini 😊\n\n");
                sb.append("Tapi ini beberapa pilihan terbaik di AdiMarket yang mungkin menarik:\n\n");
                for (int i = 0; i < Math.min(vehicles.size(), 3); i++) {
                    DatabaseHelper.VehicleData v = vehicles.get(i);
                    sb.append(formatVehicleCard(v, i + 1));
                }
                sb.append("Ketuk menu **Beli Kendaraan** untuk melihat semua unit lengkapnya! 🚗");
                return sb.toString();
            }

            StringBuilder sb = new StringBuilder();
            if (budgetMax > 0) {
                sb.append("✅ Saya temukan **").append(matches.size()).append(" kendaraan** dengan budget ")
                  .append(formatRp(budgetMax)).append(":\n\n");
            } else {
                sb.append("🔍 Berikut rekomendasi kendaraan terbaik di AdiMarket:\n\n");
            }
            for (int i = 0; i < Math.min(matches.size(), 4); i++) {
                sb.append(formatVehicleCard(matches.get(i), i + 1));
            }
            if (matches.size() > 4) {
                sb.append("...dan ").append(matches.size() - 4).append(" unit lainnya tersedia.\n\n");
            }
            sb.append("💬 Ketuk **Beli Kendaraan** untuk lihat foto & hubungi penjual langsung!");
            return sb.toString();
        }

        // ── 2. JUAL KENDARAAN ──
        if (q.contains("jual") || q.contains("pasang iklan") || q.contains("titip") || q.contains("iklan")) {
            return "Mau jual kendaraan di AdiMarket? Mudah banget! 🎉\n\n"
                 + "📋 **Cara pasang iklan:**\n"
                 + "1️⃣ Buka menu **Jual Kendaraan** di dashboard\n"
                 + "2️⃣ Foto kendaraan Anda (bisa multi-foto)\n"
                 + "3️⃣ Isi detail: nama, tipe, merek, tahun, harga\n"
                 + "4️⃣ Gunakan **AI Price Advisor** untuk saran harga terbaik 💡\n"
                 + "5️⃣ Klik Simpan — iklan langsung masuk review admin\n\n"
                 + "🛡️ Setiap iklan dianalisis AI Fraud Detection kami sebelum tayang.\n"
                 + "Semoga cepat laku ya! 🙏";
        }

        // ── 3. BERAPA UNIT / STOK ──
        if (q.contains("berapa") || q.contains("stok") || q.contains("tersedia") || q.contains("ada apa")) {
            long mobilCount = vehicles.stream().filter(v -> "Mobil".equalsIgnoreCase(v.type)).count();
            long motorCount = vehicles.stream().filter(v -> "Motor".equalsIgnoreCase(v.type)).count();
            long lainCount  = vehicles.size() - mobilCount - motorCount;
            return "📊 **Stok AdiMarket saat ini:**\n\n"
                 + "🚗 Mobil   : " + mobilCount + " unit\n"
                 + "🏍️ Motor   : " + motorCount + " unit\n"
                 + "🚛 Lainnya : " + lainCount  + " unit\n"
                 + "──────────────\n"
                 + "📦 **Total : " + vehicles.size() + " unit**\n\n"
                 + "Ketik *cari mobil* atau *cari motor* untuk rekomendasi spesifik! 😊";
        }

        // ── 4. HARGA / MURAH / TERMAHAL ──
        if (q.contains("termurah") || q.contains("paling murah")) {
            DatabaseHelper.VehicleData cheapest = null;
            long minHarga = Long.MAX_VALUE;
            for (DatabaseHelper.VehicleData v : vehicles) {
                long h = parseHarga(v.price);
                if (h > 0 && h < minHarga) { minHarga = h; cheapest = v; }
            }
            if (cheapest != null) {
                return "💰 Unit termurah saat ini:\n\n" + formatVehicleCard(cheapest, 1)
                     + "Mau saya carikan pilihan lain yang serupa?";
            }
        }

        if (q.contains("termahal") || q.contains("premium") || q.contains("mewah")) {
            DatabaseHelper.VehicleData priciest = null;
            long maxHarga = 0;
            for (DatabaseHelper.VehicleData v : vehicles) {
                long h = parseHarga(v.price);
                if (h > maxHarga) { maxHarga = h; priciest = v; }
            }
            if (priciest != null) {
                return "👑 Unit premium termahal saat ini:\n\n" + formatVehicleCard(priciest, 1)
                     + "Kelas dunia nih! Mau lihat detail lengkapnya?";
            }
        }

        // ── 5. HARGA UMUM ──
        if (q.contains("harga") || q.contains("murah") || q.contains("biaya")) {
            return "💡 Harga di AdiMarket sangat kompetitif!\n\n"
                 + "Untuk menemukan sesuai budget Anda, cukup ketik:\n"
                 + "👉 *\"Cari motor budget 15 juta\"*\n"
                 + "👉 *\"Rekomendasikan mobil di bawah 100 juta\"*\n"
                 + "👉 *\"Cari Toyota di bawah 200 juta\"*\n\n"
                 + "Saya akan langsung carikan dari database! 🔍";
        }

        // ── 6. SALAM / HALO ──
        if (q.contains("halo") || q.contains("hai") || q.contains("hello") || q.contains("hi")
                || q.contains("selamat") || q.length() < 10) {
            return "Halo! Saya **Asisten AI AdiMarket** 🤖\n\n"
                 + "Saya bisa membantu Anda:\n"
                 + "🔍 **Cari kendaraan** sesuai budget & selera\n"
                 + "📋 **Panduan jual** kendaraan dengan mudah\n"
                 + "📊 **Info stok** kendaraan terkini\n"
                 + "💰 **Cek harga** termurah & tertinggi\n\n"
                 + "Contoh: *\"Cari motor matik budget 20 juta\"* 😊";
        }

        // ── DEFAULT ──
        return "Saya belum sepenuhnya paham pertanyaan Anda 😊\n\n"
             + "Coba tanyakan seperti ini:\n"
             + "👉 *\"Rekomendasikan mobil budget 150 juta\"*\n"
             + "👉 *\"Ada Honda matic di bawah 30 juta?\"*\n"
             + "👉 *\"Cara jual kendaraan di AdiMarket\"*\n"
             + "👉 *\"Berapa stok kendaraan sekarang?\"*";
    }

    /** Format kartu kendaraan untuk tampilan di chat */
    private String formatVehicleCard(DatabaseHelper.VehicleData v, int nomor) {
        String emoji = "Mobil".equalsIgnoreCase(v.type) ? "🚗" : "Motor".equalsIgnoreCase(v.type) ? "🏍️" : "🚛";
        String hargaFormatted = formatRp(parseHarga(v.price));
        if (hargaFormatted.equals("Rp 0")) hargaFormatted = v.price;
        StringBuilder sb = new StringBuilder();
        sb.append(nomor).append(". ").append(emoji).append(" **").append(v.name).append("**\n");
        sb.append("   💰 ").append(hargaFormatted).append("  📅 Tahun ").append(v.year).append("\n");
        if (v.description != null && !v.description.isEmpty()) {
            String desc = v.description.length() > 60 ? v.description.substring(0, 60) + "..." : v.description;
            sb.append("   📝 ").append(desc).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }

    /** Ekstrak budget maksimal dari teks (misal: "20 juta" → 20_000_000) */
    private long extractBudget(String q) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(\\d+[.,]?\\d*)\\s*(juta|jt|m|miliar)?")
                    .matcher(q);
            while (m.find()) {
                String angka = m.group(1).replace(",", ".");
                String satuan = m.group(2) != null ? m.group(2) : "";
                double val = Double.parseDouble(angka);
                if (satuan.startsWith("juta") || satuan.equals("jt")) return (long)(val * 1_000_000);
                if (satuan.equals("m") || satuan.startsWith("miliar")) return (long)(val * 1_000_000_000);
                if (val >= 1000) return (long)(val * 1000); // asumsi ribuan
            }
        } catch (Exception ignored) {}
        return 0;
    }

    /** Parse harga dari string ke long */
    private long parseHarga(String price) {
        try { return Long.parseLong(price.replaceAll("[^0-9]", "")); }
        catch (Exception e) { return 0; }
    }

    /** Format angka ke Rupiah singkat */
    private String formatRp(long val) {
        if (val >= 1_000_000_000) return String.format("Rp %.1f M", val / 1_000_000_000.0);
        if (val >= 1_000_000)     return String.format("Rp %.0f Jt", val / 1_000_000.0);
        if (val > 0)              return String.format("Rp %,.0f", (double) val);
        return "Rp 0";
    }
}
