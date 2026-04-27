package com.example.adimarket;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChatbotActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private EditText etMessage;
    private ImageButton btnSend;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;

    // Masukkan API Key Gemini Anda di sini
    private static final String GEMINI_API_KEY = "MASUKKAN_API_KEY_ANDA_DI_SINI"; 

    private GenerativeModelFutures model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        // Inisialisasi Model Gemini
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", GEMINI_API_KEY);
        model = GenerativeModelFutures.from(gm);

        // Initialize views
        rvChat = findViewById(R.id.rvChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        // Setup RecyclerView
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);

        // Add welcome message
        addMessage("Bot", "Halo! Saya Asisten AI AdiMarket (Gemini). Ada yang bisa saya bantu tentang kendaraan?", false);

        // Send button
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    private void sendMessage() {
        String userMessage = etMessage.getText().toString().trim();

        if (userMessage.isEmpty()) {
            return;
        }

        // Add user message
        addMessage("Anda", userMessage, true);
        etMessage.setText("");

        // Get bot response from Gemini
        getGeminiResponse(userMessage);
    }

    private void addMessage(String sender, String message, boolean isUser) {
        messageList.add(new ChatMessage(sender, message, isUser));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        rvChat.scrollToPosition(messageList.size() - 1);
    }

    private void getGeminiResponse(String userMessage) {
        // Gunakan GeminiHelper yang sudah kita optimalkan di Phase 14
        GeminiHelper geminiHelper = new GeminiHelper();
        
        String prompt = "Anda adalah asisten AI untuk AdiMarket, sebuah marketplace jual beli kendaraan. " +
                "Jawab pertanyaan berikut dengan ramah dan informatif: " + userMessage;

        geminiHelper.askGemini(prompt, new GeminiHelper.GeminiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> addMessage("Bot", response, false));
            }

            @Override
            public void onError(String error) {
                Log.e("GeminiAI", "Error: " + error);
                runOnUiThread(() -> addMessage("Bot", "Koneksi AI terputus. Pastikan API Key sudah benar di GeminiHelper.java", false));
            }
        });
    }

    // Inner class for chat messages (Sesuaikan dengan ChatAdapter Anda)
    public static class ChatMessage {
        String sender;
        String message;
        boolean isUser;

        public ChatMessage(String sender, String message, boolean isUser) {
            this.sender = sender;
            this.message = message;
            this.isUser = isUser;
        }
    }
}
