package com.example.adimarket;

import android.content.Context;
import android.util.Log;

/**
 * Dialogflow Integration Helper (Point 5)
 * Menyediakan antarmuka untuk menghubungkan chatbot dengan Google Dialogflow intents.
 */
public class DialogflowHelper {

    private Context context;

    public DialogflowHelper(Context context) {
        this.context = context;
    }

    /**
     * Mengirim pesan ke Dialogflow dan mendapatkan intent yang terdeteksi
     * (Placeholder untuk implementasi gRPC/V2 API)
     */
    public void detectIntent(String message, DialogflowCallback callback) {
        // Implementasi integrasi API Dialogflow di sini
        Log.d("Dialogflow", "Mengirim pesan: " + message);
        
        // Simulasi respon berdasarkan intent
        if (message.toLowerCase().contains("beli")) {
            callback.onResponse("Tentu, saya bisa membantu mencari kendaraan untuk dibeli.");
        } else {
            callback.onResponse("Halo! Ada yang bisa saya bantu di AdiMarket?");
        }
    }

    public interface DialogflowCallback {
        void onResponse(String response);
        void onError(String error);
    }
}
