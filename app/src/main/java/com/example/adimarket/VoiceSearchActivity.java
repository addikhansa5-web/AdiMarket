package com.example.adimarket;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VoiceSearchActivity extends AppCompatActivity {

    private static final int SPEECH_REQUEST_CODE = 100;

    private ImageButton btnVoiceInput;
    private TextView tvVoiceCommand;
    private RecyclerView rvResults;

    private TextToSpeech textToSpeech;
    private VehicleAdapter vehicleAdapter;
    private List<DatabaseHelper.VehicleData> vehicleList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_search);

        // Initialize views
        btnVoiceInput = findViewById(R.id.btnVoiceInput);
        tvVoiceCommand = findViewById(R.id.tvVoiceCommand);
        rvResults = findViewById(R.id.rvResults);

        // Setup RecyclerView
        vehicleList = new ArrayList<>();
        vehicleAdapter = new VehicleAdapter(vehicleList);
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setAdapter(vehicleAdapter);

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech.setLanguage(new Locale("id", "ID"));
                }
            }
        });

        // Voice input button
        btnVoiceInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVoiceRecognition();
            }
        });
    }

    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Katakan perintah Anda...");

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition tidak tersedia",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                ArrayList<String> results = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);

                if (results != null && !results.isEmpty()) {
                    String voiceCommand = results.get(0);
                    tvVoiceCommand.setText("Perintah: " + voiceCommand);
                    processVoiceCommand(voiceCommand);
                }
            }
        }
    }

    private void processVoiceCommand(String command) {
        String lowerCommand = command.toLowerCase();

        // Clear previous results
        vehicleList.clear();

        // Process different voice commands
        if (lowerCommand.contains("cari") || lowerCommand.contains("tampilkan")) {

            if (lowerCommand.contains("mobil")) {
                searchVehicles("mobil");
                speak("Menampilkan hasil pencarian mobil");
            } else if (lowerCommand.contains("motor")) {
                searchVehicles("motor");
                speak("Menampilkan hasil pencarian motor");
            } else if (lowerCommand.contains("truk")) {
                searchVehicles("truk");
                speak("Menampilkan hasil pencarian truk");
            } else {
                searchVehicles("semua");
                speak("Menampilkan semua kendaraan");
            }

        } else if (lowerCommand.contains("harga") && lowerCommand.contains("di bawah")) {
            // Extract price from command
            filterByPrice(lowerCommand);

        } else if (lowerCommand.contains("tahun")) {
            // Extract year from command
            filterByYear(lowerCommand);

        } else {
            speak("Maaf, perintah tidak dikenali. Coba lagi.");
            Toast.makeText(this, "Perintah tidak dikenali", Toast.LENGTH_SHORT).show();
        }
    }

    private void searchVehicles(String type) {
        // Simulate vehicle search (replace with actual database query)
        vehicleList.clear();

        if (type.equals("mobil") || type.equals("semua")) {
            vehicleList.add(createVehicleData("Honda BRV", "Mobil", "Rp 190.000.000", 2020));
            vehicleList.add(createVehicleData("Toyota Avanza", "Mobil", "Rp 150.000.000", 2019));
        }

        if (type.equals("motor") || type.equals("semua")) {
            vehicleList.add(createVehicleData("Honda PCX", "Motor", "Rp 20.000.000", 2021));
            vehicleList.add(createVehicleData("Yamaha NMAX", "Motor", "Rp 22.000.000", 2020));
        }

        if (type.equals("truk") || type.equals("semua")) {
            vehicleList.add(createVehicleData("Mitsubishi Colt Diesel", "Truk", "Rp 180.000.000", 2018));
        }

        vehicleAdapter.notifyDataSetChanged();
    }

    private DatabaseHelper.VehicleData createVehicleData(String name, String type, String price, int year) {
        DatabaseHelper.VehicleData data = new DatabaseHelper.VehicleData();
        data.name = name;
        data.type = type;
        data.price = price;
        data.year = year;
        return data;
    }

    private void filterByPrice(String command) {
        // Extract price from command (simplified)
        speak("Memfilter berdasarkan harga");
        searchVehicles("semua"); // Replace with actual price filter
    }

    private void filterByYear(String command) {
        // Extract year from command (simplified)
        speak("Memfilter berdasarkan tahun");
        searchVehicles("semua"); // Replace with actual year filter
    }

    private void speak(String text) {
        if (textToSpeech != null) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

}