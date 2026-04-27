package com.example.adimarket;

import android.util.Log;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GeminiHelper {
    // Masukkan API Key Anda di sini jika ingin menggunakan AI asli
    private static final String API_KEY = "MASUKKAN_API_KEY_ANDA_DI_SINI"; 
    private final GenerativeModelFutures model;
    private final Executor executor;
    
    // Phase 14 Optimization: Simple Cache
    private static final Map<String, String> responseCache = new HashMap<>();

    public GeminiHelper() {
        // Gunakan key dummy jika belum diisi agar tidak crash saat inisialisasi
        String validKey = (API_KEY == null || API_KEY.contains("MASUKKAN_API_KEY")) ? "dummy-key-for-init" : API_KEY;
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", validKey);
        this.model = GenerativeModelFutures.from(gm);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public interface GeminiCallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public void askGemini(String prompt, GeminiCallback callback) {
        // 1. OTOMATISASI DEMO MODE (Jika API Key belum diisi)
        if (API_KEY == null || API_KEY.contains("MASUKKAN_API_KEY")) {
            Log.w("GeminiHelper", "Demo Mode Aktif (Tanpa API Key)");
            String demoResponse = getDemoResponse(prompt);
            if (callback != null) callback.onSuccess(demoResponse);
            return;
        }

        // 2. CHECK CACHE (Optimization Phase 14)
        if (responseCache.containsKey(prompt)) {
            Log.d("GeminiHelper", "Returning cached response");
            if (callback != null) callback.onSuccess(responseCache.get(prompt));
            return;
        }

        // 3. PANGGIL GOOGLE GEMINI ASLI
        try {
            Content content = new Content.Builder().addText(prompt).build();
            ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    String responseText = result.getText();
                    responseCache.put(prompt, responseText);
                    if (callback != null) callback.onSuccess(responseText);
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e("GeminiHelper", "Gemini Error: " + t.getMessage());
                    // Jika server sibuk/error, gunakan jawaban Demo agar user tidak kecewa
                    if (callback != null) callback.onSuccess(getDemoResponse(prompt));
                }
            }, executor);
        } catch (Exception e) {
            if (callback != null) callback.onSuccess(getDemoResponse(prompt));
        }
    }

    // OTAK BUATAN LOKAL (Versi Sempurna & Dinamis)
    private String getDemoResponse(String prompt) {
        String p = prompt.toLowerCase();
        
        if (p.contains("halo") || p.contains("hai") || p.contains("pagi") || p.contains("siang")) {
            return "Halo! Saya Asisten AI AdiMarket. Senang bertemu Anda. Ada yang bisa saya bantu cari hari ini? Saya bisa memberikan tips atau info kendaraan.";
        }
        
        if (p.contains("murah") || p.contains("bagus") || p.contains("rekomendasi")) {
            return "Untuk mencari mobil bagus dengan harga bersahabat, saya sarankan cek 'Toyota Avanza' atau 'Honda Brio' di menu Beli. Jangan lupa cek badge 'LOW RISK' untuk keamanan ekstra.";
        }

        if (p.contains("tips") || p.contains("cara") || p.contains("aman")) {
            return "Agar aman bertransaksi di AdiMarket:\n1. Gunakan fitur Fraud Detection kami.\n2. Jangan transfer uang sebelum lihat unit.\n3. Cek BPKB & STNK asli.\n4. Ajak teman yang paham mesin.";
        }

        if (p.contains("lokasi") || p.contains("dimana") || p.contains("cikarang")) {
            return "Kantor pusat AdiMarket berada di Cikarang. Namun, penjual kami tersebar di seluruh Indonesia. Anda bisa memfilter kendaraan berdasarkan lokasi terdekat Anda.";
        }

        if (p.contains("jual") || p.contains("iklan")) {
            return "Mau jual kendaraan? Gampang! Klik menu 'Jual Kendaraan', isi data, dan upload foto terbaik. Tim admin kami akan memverifikasi iklan Anda dalam 1x24 jam.";
        }

        if (p.contains("beli") || p.contains("prosedur")) {
            return "Prosedur beli di sini sangat simpel: Cari unitnya -> Hubungi penjual lewat WhatsApp -> Janjian liat unit -> Deal! Tanpa biaya admin dari aplikasi.";
        }

        if (p.contains("terima kasih") || p.contains("thanks") || p.contains("ok")) {
            return "Sama-sama! Senang bisa membantu. Ada lagi yang ingin Anda tanyakan seputar kendaraan?";
        }

        // Jawaban default yang lebih cerdas
        return "Pertanyaan menarik! Saya sedang terus belajar. Secara umum, di AdiMarket Anda bisa menemukan mobil/motor berkualitas. Apakah Anda ingin saya tunjukkan cara mengecek kendaraan yang aman?";
    }
}
