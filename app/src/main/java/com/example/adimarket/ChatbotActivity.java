package com.example.adimarket;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChatbotActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private EditText etMessage;
    private ImageButton btnSend;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;

    private GroqHelper groqHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);
        WallpaperHelper.apply(this, findViewById(R.id.headerImage));

        // Inisialisasi GroqHelper dengan Context
        groqHelper = new GroqHelper(this);

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
        addMessage("Bot",
            "Halo! Saya Asisten AI AdiMarket 🤖\n\n" +
            "Saya bisa membantu Anda:\n" +
            "🔍 Cari kendaraan sesuai budget & selera\n" +
            "📋 Panduan cara jual kendaraan\n" +
            "📊 Info stok kendaraan terkini\n" +
            "💰 Cek harga termurah & tertinggi\n\n" +
            "Coba tanyakan:\n" +
            "• \"Cari motor matik budget 20 juta\"\n" +
            "• \"Ada mobil Toyota di bawah 150 juta?\"\n" +
            "• \"Berapa stok kendaraan sekarang?\"",
            false);

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

        // Get bot response from Groq
        getGroqResponse(userMessage);
    }

    private void addMessage(String sender, String message, boolean isUser) {
        messageList.add(new ChatMessage(sender, message, isUser));
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        rvChat.scrollToPosition(messageList.size() - 1);
    }

    private void getGroqResponse(String userMessage) {
        // AI Hybrid: GroqHelper sekarang otomatis mengambil konteks database sendiri
        groqHelper.askGroq(messageList, new GroqHelper.GroqCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> addMessage("Bot", response, false));
            }

            @Override
            public void onError(String error) {
                Log.e("GroqAI", "Error: " + error);
                runOnUiThread(() -> addMessage("Bot", "Maaf, AI sedang beristirahat. Pastikan koneksi internet stabil.", false));
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
