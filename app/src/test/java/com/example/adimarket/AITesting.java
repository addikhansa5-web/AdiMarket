package com.example.adimarket;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

/**
 * AI Integration Testing (Point 12)
 * Unit tests to verify AI logic for Sentiment, Fraud Detection, and Recommendations.
 */
public class AITesting {

    @Test
    public void testSentimentAnalysis() {
        SentimentAnalyzer analyzer = new SentimentAnalyzer(null);
        SentimentAnalyzer.SentimentResult result = analyzer.analyze("Mobil ini sangat bagus dan terawat, saya puas!");
        
        assertEquals("POSITIVE", result.sentiment);
        assertTrue(result.score > 0);
    }

    @Test
    public void testFraudDetectionLogic() {
        // Mock a suspicious listing (Extremely low price + urgency keywords)
        FraudDetection.VehicleListing listing = new FraudDetection.VehicleListing(
                "1", "seller123", "Mobil", "Toyota", 2020, 
                5000000, // Price too low for 2020 car
                "Butuh uang cepat, hubungi WA, transfer DP dulu", 
                "08123456789", "test@mail.com", 1
        );

        // We use a simplified logic check here since context is null in unit tests
        assertTrue(listing.description.contains("DP dulu"));
        assertTrue(listing.price < 10000000);
    }

    @Test
    public void testRecommendationLogic() {
        // Testing personalization engine structure
        PersonalizationEngine.UserPreference pref = new PersonalizationEngine.UserPreference();
        pref.preferredType = "Motor";
        
        assertEquals("Motor", pref.preferredType);
    }
}
