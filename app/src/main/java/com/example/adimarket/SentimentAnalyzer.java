package com.example.adimarket;

import android.content.Context;
import java.util.HashMap;
import java.util.Map;

public class SentimentAnalyzer {

    private Context context;

    // Positive and negative word dictionaries
    private static final String[] POSITIVE_WORDS = {
            "bagus", "baik", "excellent", "recommended", "puas", "memuaskan",
            "responsif", "cepat", "profesional", "ramah", "jujur", "terpercaya",
            "mantap", "oke", "ok", "sempurna", "amazing", "fantastic", "great",
            "sukses", "lancar", "mulus", "terawat", "bersih"
    };

    private static final String[] NEGATIVE_WORDS = {
            "jelek", "buruk", "kecewa", "mengecewakan", "lambat", "lama",
            "tidak responsif", "penipu", "bohong", "palsu", "rusak", "cacat",
            "kotor", "tidak sesuai", "tidak recommended", "bad", "terrible",
            "waste", "sampah", "zonk", "tipu", "scam"
    };

    public SentimentAnalyzer(Context context) {
        this.context = context;
    }

    // Analyze sentiment of a review
    public SentimentResult analyze(String review) {
        String lowerReview = review.toLowerCase();

        int positiveCount = 0;
        int negativeCount = 0;

        // Count positive words
        for (String word : POSITIVE_WORDS) {
            if (lowerReview.contains(word)) {
                positiveCount++;
            }
        }

        // Count negative words
        for (String word : NEGATIVE_WORDS) {
            if (lowerReview.contains(word)) {
                negativeCount++;
            }
        }

        // Calculate sentiment score (-1 to 1)
        double sentimentScore = 0.0;
        int totalWords = positiveCount + negativeCount;

        if (totalWords > 0) {
            sentimentScore = (double)(positiveCount - negativeCount) / totalWords;
        } else {
            // Neutral if no sentiment words found
            sentimentScore = 0.0;
        }

        // Determine sentiment
        String sentiment;
        if (sentimentScore > 0.3) {
            sentiment = "POSITIVE";
        } else if (sentimentScore < -0.3) {
            sentiment = "NEGATIVE";
        } else {
            sentiment = "NEUTRAL";
        }

        // Calculate confidence (0-1)
        double confidence = Math.min(totalWords / 5.0, 1.0);

        return new SentimentResult(sentiment, sentimentScore, confidence,
                positiveCount, negativeCount);
    }

    // Analyze multiple reviews and get overall sentiment
    public OverallSentiment analyzeMultiple(String[] reviews) {
        int positive = 0;
        int negative = 0;
        int neutral = 0;
        double totalScore = 0.0;

        for (String review : reviews) {
            SentimentResult result = analyze(review);
            totalScore += result.score;

            switch (result.sentiment) {
                case "POSITIVE":
                    positive++;
                    break;
                case "NEGATIVE":
                    negative++;
                    break;
                case "NEUTRAL":
                    neutral++;
                    break;
            }
        }

        double averageScore = reviews.length > 0 ? totalScore / reviews.length : 0.0;

        // Calculate percentages
        int total = reviews.length;
        double positivePercent = total > 0 ? (positive * 100.0 / total) : 0;
        double negativePercent = total > 0 ? (negative * 100.0 / total) : 0;
        double neutralPercent = total > 0 ? (neutral * 100.0 / total) : 0;

        return new OverallSentiment(positive, negative, neutral,
                averageScore, positivePercent,
                negativePercent, neutralPercent);
    }

    // Get emoji representation of sentiment
    public String getSentimentEmoji(String sentiment) {
        switch (sentiment) {
            case "POSITIVE":
                return "😊";
            case "NEGATIVE":
                return "😞";
            case "NEUTRAL":
                return "😐";
            default:
                return "";
        }
    }

    // Get color for sentiment
    public int getSentimentColor(String sentiment) {
        switch (sentiment) {
            case "POSITIVE":
                return 0xFF4CAF50; // Green
            case "NEGATIVE":
                return 0xFFF44336; // Red
            case "NEUTRAL":
                return 0xFFFF9800; // Orange
            default:
                return 0xFF9E9E9E; // Grey
        }
    }

    public boolean isAdminApproved(String message) {
        String lower = message.toLowerCase();
        return lower.equals("ok") || lower.equals("oke") || lower.equals("sip") || lower.equals("siap");
    }

    // Result class for single review
    public static class SentimentResult {
        public String sentiment; // POSITIVE, NEGATIVE, NEUTRAL
        public double score; // -1 to 1
        public double confidence; // 0 to 1
        public int positiveWords;
        public int negativeWords;

        public SentimentResult(String sentiment, double score, double confidence,
                               int positiveWords, int negativeWords) {
            this.sentiment = sentiment;
            this.score = score;
            this.confidence = confidence;
            this.positiveWords = positiveWords;
            this.negativeWords = negativeWords;
        }
    }

    // Result class for multiple reviews
    public static class OverallSentiment {
        public int positiveCount;
        public int negativeCount;
        public int neutralCount;
        public double averageScore;
        public double positivePercent;
        public double negativePercent;
        public double neutralPercent;

        public OverallSentiment(int positiveCount, int negativeCount, int neutralCount,
                                double averageScore, double positivePercent,
                                double negativePercent, double neutralPercent) {
            this.positiveCount = positiveCount;
            this.negativeCount = negativeCount;
            this.neutralCount = neutralCount;
            this.averageScore = averageScore;
            this.positivePercent = positivePercent;
            this.negativePercent = negativePercent;
            this.neutralPercent = neutralPercent;
        }
    }
}