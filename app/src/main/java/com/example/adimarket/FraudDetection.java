package com.example.adimarket;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class FraudDetection {

    private Context context;
    private static final double FRAUD_THRESHOLD = 0.6; // 60% fraud score = suspicious

    public FraudDetection(Context context) {
        this.context = context;
    }

    // Main fraud detection method
    public FraudResult analyzeListing(VehicleListing listing) {
        double fraudScore = 0.0;
        List<String> suspiciousReasons = new ArrayList<>();

        // 1. Check price anomaly
        double priceScore = checkPriceAnomaly(listing);
        if (priceScore > 0) {
            fraudScore += priceScore;
            suspiciousReasons.add("Harga tidak wajar untuk tahun dan kondisi kendaraan");
        }

        // 2. Check description quality
        double descScore = checkDescription(listing);
        if (descScore > 0) {
            fraudScore += descScore;
            suspiciousReasons.add("Deskripsi mencurigakan atau tidak lengkap");
        }

        // 3. Check contact information
        double contactScore = checkContactInfo(listing);
        if (contactScore > 0) {
            fraudScore += contactScore;
            suspiciousReasons.add("Informasi kontak mencurigakan");
        }

        // 4. Check image quality
        double imageScore = checkImageQuality(listing);
        if (imageScore > 0) {
            fraudScore += imageScore;
            suspiciousReasons.add("Kualitas gambar rendah atau gambar tidak asli");
        }

        // 5. Check seller history
        double sellerScore = checkSellerHistory(listing.sellerId);
        if (sellerScore > 0) {
            fraudScore += sellerScore;
            suspiciousReasons.add("Riwayat penjual mencurigakan");
        }

        // Normalize score (0-1)
        fraudScore = Math.min(fraudScore / 5.0, 1.0);

        // Determine result
        boolean isFraudulent = fraudScore >= FRAUD_THRESHOLD;
        String riskLevel = getRiskLevel(fraudScore);

        return new FraudResult(isFraudulent, fraudScore, riskLevel, suspiciousReasons);
    }

    // Check if price is anomalous
    private double checkPriceAnomaly(VehicleListing listing) {
        double score = 0.0;

        // Get average price for similar vehicles
        double avgPrice = getAveragePrice(listing.type, listing.year, listing.brand);

        if (avgPrice > 0) {
            double deviation = Math.abs(listing.price - avgPrice) / avgPrice;

            // If price is 50% below or 30% above average
            if (listing.price < avgPrice * 0.5) {
                score = 0.4; // Too cheap - suspicious
            } else if (listing.price > avgPrice * 1.3) {
                score = 0.2; // Too expensive - less suspicious
            }
        }

        return score;
    }

    // Check description for fraud indicators
    private double checkDescription(VehicleListing listing) {
        double score = 0.0;
        String desc = listing.description.toLowerCase();

        // Check for fraud keywords
        String[] fraudKeywords = {
                "butuh cepat", "dijual cepat", "harga nego sampai jadi",
                "tanpa survey", "dp ringan", "kredit tanpa bi checking",
                "whatsapp only", "chat only", "no call"
        };

        for (String keyword : fraudKeywords) {
            if (desc.contains(keyword)) {
                score += 0.15;
            }
        }

        // Check description length (too short is suspicious)
        if (desc.length() < 50) {
            score += 0.2;
        }

        // Check for excessive urgency
        if (desc.contains("hari ini") || desc.contains("besok") || desc.contains("segera")) {
            score += 0.1;
        }

        return Math.min(score, 1.0);
    }

    // Check contact information
    private double checkContactInfo(VehicleListing listing) {
        double score = 0.0;

        // Check if phone number is valid
        if (!isValidPhoneNumber(listing.phone)) {
            score += 0.3;
        }

        // Check if using disposable/fake email
        if (listing.email != null && isSuspiciousEmail(listing.email)) {
            score += 0.2;
        }

        return score;
    }

    // Check image quality
    private double checkImageQuality(VehicleListing listing) {
        double score = 0.0;

        // Check number of images
        if (listing.imageCount < 3) {
            score += 0.2;
        }

        // Check if images are stock photos (would need ML model)
        // For now, basic check
        if (listing.imageCount == 0) {
            score += 0.4;
        }

        return score;
    }

    // Check seller history
    private double checkSellerHistory(String sellerId) {
        double score = 0.0;

        // Check if new seller (< 7 days)
        int sellerAge = getSellerAge(sellerId);
        if (sellerAge < 7) {
            score += 0.2;
        }

        // Check number of active listings
        int activeListings = getActiveListings(sellerId);
        if (activeListings > 10) {
            score += 0.15; // Too many listings at once
        }

        // Check negative reviews
        int negativeReviews = getNegativeReviews(sellerId);
        if (negativeReviews > 2) {
            score += 0.3;
        }

        return score;
    }

    // Helper methods
    private boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) return false;

        // Indonesian phone number pattern
        Pattern pattern = Pattern.compile("^(\\+62|62|0)[0-9]{9,12}$");
        return pattern.matcher(phone.replaceAll("[\\s-]", "")).matches();
    }

    private boolean isSuspiciousEmail(String email) {
        String[] suspiciousDomains = {
                "tempmail", "throwaway", "guerrillamail", "10minutemail", "fakeinbox"
        };

        String lowerEmail = email.toLowerCase();
        for (String domain : suspiciousDomains) {
            if (lowerEmail.contains(domain)) {
                return true;
            }
        }
        return false;
    }

    private double getAveragePrice(String type, int year, String brand) {
        // Query database asli untuk mendapatkan harga rata-rata kendaraan sejenis
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        List<DatabaseHelper.VehicleData> similarVehicles = dbHelper.getAllVehicles();
        
        double totalPrice = 0;
        int count = 0;
        
        for (DatabaseHelper.VehicleData v : similarVehicles) {
            if (v.type.equalsIgnoreCase(type) && v.year == year) {
                try {
                    totalPrice += Double.parseDouble(v.price.replaceAll("[^0-9]", ""));
                    count++;
                } catch (Exception e) {}
            }
        }
        
        if (count > 0) {
            return totalPrice / count;
        }
        return 100000000; // Fallback jika data belum cukup
    }

    private int getSellerAge(String sellerId) {
        // Get seller registration age in days
        // Placeholder
        return 30;
    }

    private int getActiveListings(String sellerId) {
        // Get number of active listings
        // Placeholder
        return 5;
    }

    private int getNegativeReviews(String sellerId) {
        // Get number of negative reviews
        // Placeholder
        return 0;
    }

    private String getRiskLevel(double score) {
        if (score < 0.3) {
            return "LOW";
        } else if (score < 0.6) {
            return "MEDIUM";
        } else {
            return "HIGH";
        }
    }

    // Result class
    public static class FraudResult {
        public boolean isFraudulent;
        public double fraudScore;
        public String riskLevel;
        public List<String> reasons;

        public FraudResult(boolean isFraudulent, double fraudScore,
                           String riskLevel, List<String> reasons) {
            this.isFraudulent = isFraudulent;
            this.fraudScore = fraudScore;
            this.riskLevel = riskLevel;
            this.reasons = reasons;
        }
    }

    // Vehicle Listing class
    public static class VehicleListing {
        String id;
        String sellerId;
        String type;
        String brand;
        int year;
        double price;
        String description;
        String phone;
        String email;
        int imageCount;

        public VehicleListing(String id, String sellerId, String type, String brand,
                              int year, double price, String description,
                              String phone, String email, int imageCount) {
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