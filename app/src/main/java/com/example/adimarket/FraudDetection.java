package com.example.adimarket;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * AI-Driven Trust Engine for AdiMarket
 * Menganalisis risiko keamanan iklan kendaraan menggunakan multi-factor analysis.
 */
public class FraudDetection {

    private Context context;
    private static final double RISK_THRESHOLD = 0.65; 

    public FraudDetection(Context context) {
        this.context = context;
    }

    public FraudResult analyzeListing(VehicleListing listing) {
        double totalRiskScore = 0.0;
        List<String> insights = new ArrayList<>();

        // 1. AI Price Engine Analysis (35% Weight)
        double priceRisk = analyzePriceRisk(listing);
        totalRiskScore += priceRisk * 0.35;
        if (priceRisk > 0.6) insights.add("Harga jauh di bawah rata-rata pasar (Indikasi Scam)");

        // 2. Linguistic & Sentiment Analysis (25% Weight)
        double linguisticRisk = analyzeLinguisticPatterns(listing.description);
        totalRiskScore += linguisticRisk * 0.25;
        if (linguisticRisk > 0.5) insights.add("Pola kalimat mengandung unsur tekanan (Urgency Bias)");

        // 3. Metadata & Identity Validation (20% Weight)
        double identityRisk = validateIdentity(listing);
        totalRiskScore += identityRisk * 0.20;
        if (identityRisk > 0.4) insights.add("Verifikasi kontak atau dokumen tidak lengkap");

        // 4. Content Integrity (20% Weight)
        double integrityRisk = checkContentIntegrity(listing);
        totalRiskScore += integrityRisk * 0.20;
        if (integrityRisk > 0.5) insights.add("Inkonsistensi data deskripsi dan spesifikasi");

        // Determine Final Status
        boolean isSuspicious = totalRiskScore >= RISK_THRESHOLD;
        String trustLevel = calculateTrustLevel(totalRiskScore);

        return new FraudResult(isSuspicious, totalRiskScore, trustLevel, insights);
    }

    private double analyzePriceRisk(VehicleListing listing) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        List<DatabaseHelper.VehicleData> allVehicles = dbHelper.getAllVehicles();
        
        double avgPrice = 0;
        int matchCount = 0;
        
        for (DatabaseHelper.VehicleData v : allVehicles) {
            if (v.type.equalsIgnoreCase(listing.type) && Math.abs(v.year - listing.year) <= 1) {
                try {
                    avgPrice += Double.parseDouble(v.price.replaceAll("[^0-9]", ""));
                    matchCount++;
                } catch (Exception ignored) {}
            }
        }

        if (matchCount < 3) return 0.2; // Data tidak cukup, beri risiko rendah

        avgPrice /= matchCount;
        double ratio = listing.price / avgPrice;

        if (ratio < 0.4) return 1.0; // Terlalu murah (Scam alert)
        if (ratio < 0.7) return 0.6; // Mencurigakan
        if (ratio > 1.8) return 0.3; // Terlalu mahal (Overprice, bukan fraud)
        
        return 0.0;
    }

    private double analyzeLinguisticPatterns(String description) {
        if (description == null || description.isEmpty()) return 0.8;
        
        String desc = description.toLowerCase();
        double score = 0;

        // Pattern 1: Extreme Urgency (Typical of scams)
        String[] urgencyKeywords = {"butuh uang", "b.u", "hari ini saja", "siapa cepat", "siapa dapat", "cepat dapat"};
        for (String key : urgencyKeywords) {
            if (desc.contains(key)) score += 0.2;
        }

        // Pattern 2: Payment Redirection
        String[] paymentKeywords = {"dp dulu", "tanda jadi", "booking fee", "transfer dulu"};
        for (String key : paymentKeywords) {
            if (desc.contains(key)) score += 0.4;
        }

        // Pattern 3: Off-platform communication
        if (desc.contains("wa ke") || desc.contains("hubungi nomor ini") || desc.contains("jangan chat di aplikasi")) {
            score += 0.3;
        }

        return Math.min(score, 1.0);
    }

    private double validateIdentity(VehicleListing listing) {
        double risk = 0;
        
        // Validate Phone (Indonesian format)
        if (listing.phone == null || !Pattern.matches("^(\\+62|62|0)8[1-9][0-9]{7,11}$", listing.phone.replaceAll("[\\s-]", ""))) {
            risk += 0.5;
        }

        // Check image count (Visual Proof)
        if (listing.imageCount < 2) risk += 0.3;
        
        return risk;
    }

    private double checkContentIntegrity(VehicleListing listing) {
        String desc = listing.description.toLowerCase();
        double risk = 0;

        // Check if brand is mentioned in description
        if (!desc.contains(listing.brand.toLowerCase())) risk += 0.2;

        // Detect copy-paste patterns (usually very long or very short)
        if (listing.description.length() < 20) risk += 0.4;
        
        return risk;
    }

    private String calculateTrustLevel(double score) {
        if (score < 0.3) return "TERVERIFIKASI";
        if (score < 0.5) return "AMAN";
        if (score < 0.7) return "PERLU WASPADA";
        return "RISIKO TINGGI";
    }

    // --- Data Classes ---

    public static class FraudResult {
        public boolean isFraudulent;
        public double confidenceScore;
        public String trustLevel;
        public List<String> reasons;

        public FraudResult(boolean isFraudulent, double confidenceScore, String trustLevel, List<String> reasons) {
            this.isFraudulent = isFraudulent;
            this.confidenceScore = confidenceScore;
            this.trustLevel = trustLevel;
            this.reasons = reasons;
        }
    }

    public static class VehicleListing {
        String id, sellerId, type, brand, description, phone, email;
        int year, imageCount;
        double price;

        public VehicleListing(String id, String sellerId, String type, String brand, int year, 
                              double price, String description, String phone, String email, int imageCount) {
            this.id = id;
            this.sellerId = sellerId;
            this.type = type;
            this.brand = brand;
            this.year = year;
            this.price = price;
            this.description = description;
            this.phone = phone;
            this.email = email;
            this.imageCount = imageCount;
        }
    }
}
