package com.example.adimarket;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Review & Rating System (Point 10)
 * Integrates with SentimentAnalyzer to evaluate user feedback.
 */
public class ReviewActivity extends AppCompatActivity {

    private RatingBar ratingBar;
    private EditText etReview;
    private Button btnSubmit;
    private TextView tvSentimentResult;
    private SentimentAnalyzer sentimentAnalyzer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        WallpaperHelper.apply(this, findViewById(R.id.headerImage));

        sentimentAnalyzer = new SentimentAnalyzer(this);
        
        ratingBar = findViewById(R.id.ratingBar);
        etReview = findViewById(R.id.etReview);
        btnSubmit = findViewById(R.id.btnSubmitReview);
        tvSentimentResult = findViewById(R.id.tvSentimentResult);

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReview();
            }
        });
    }

    private void submitReview() {
        String reviewText = etReview.getText().toString().trim();
        float rating = ratingBar.getRating();

        if (reviewText.isEmpty()) {
            Toast.makeText(this, "Mohon tuliskan ulasan Anda", Toast.LENGTH_SHORT).show();
            return;
        }

        // Analyze Sentiment (AI Integration)
        SentimentAnalyzer.SentimentResult result = sentimentAnalyzer.analyze(reviewText);
        
        String sentimentEmoji = sentimentAnalyzer.getSentimentEmoji(result.sentiment);
        tvSentimentResult.setVisibility(View.VISIBLE);
        tvSentimentResult.setText("Sentimen AI: " + result.sentiment + " " + sentimentEmoji);
        tvSentimentResult.setTextColor(sentimentAnalyzer.getSentimentColor(result.sentiment));

        // Save to DB (Logika simpan ke DatabaseHelper bisa ditambahkan di sini)
        Toast.makeText(this, "Terima kasih atas ulasan Anda!", Toast.LENGTH_LONG).show();
        
        // Simulasikan feedback otomatis berdasarkan sentimen
        if (result.sentiment.equals("NEGATIVE")) {
            Toast.makeText(this, "Kami mohon maaf atas ketidaknyamanan Anda.", Toast.LENGTH_SHORT).show();
        }
    }
}
