package com.example.adimarket;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashMap;
import java.util.Map;

public class PersonalizationEngine {

    private Context context;
    private SharedPreferences preferences;
    private static final String PREF_NAME = "UserPersonalization";

    public PersonalizationEngine(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Track user behavior
    public void trackVehicleView(String vehicleId, String vehicleType,
                                 String brand, int price) {
        // Increment view count
        int viewCount = preferences.getInt("view_count_" + vehicleId, 0);
        preferences.edit().putInt("view_count_" + vehicleId, viewCount + 1).apply();

        // Track type preference
        incrementPreference("type_" + vehicleType);

        // Track brand preference
        incrementPreference("brand_" + brand);

        // Track price range preference
        String priceRange = getPriceRange(price);
        incrementPreference("price_range_" + priceRange);

        // Update last viewed
        preferences.edit().putLong("last_active", System.currentTimeMillis()).apply();
    }

    public void trackSearch(String searchQuery, String filterType) {
        // Track search keywords
        incrementPreference("search_" + searchQuery.toLowerCase());

        // Track filter usage
        if (filterType != null && !filterType.isEmpty()) {
            incrementPreference("filter_" + filterType);
        }
    }

    public void trackFavorite(String vehicleId, String vehicleType) {
        incrementPreference("favorite_type_" + vehicleType);
        preferences.edit().putBoolean("favorited_" + vehicleId, true).apply();
    }

    public void trackContact(String vehicleType, String contactMethod) {
        incrementPreference("contact_type_" + vehicleType);
        incrementPreference("contact_method_" + contactMethod);
    }

    // Get user preferences
    public UserProfile getUserProfile() {
        String preferredType = getMostPreferredType();
        String preferredBrand = getMostPreferredBrand();
        String preferredPriceRange = getMostPreferredPriceRange();
        int activityLevel = getActivityLevel();

        return new UserProfile(preferredType, preferredBrand,
                preferredPriceRange, activityLevel);
    }

    private String getMostPreferredType() {
        Map<String, Integer> typePrefs = new HashMap<>();
        typePrefs.put("mobil", preferences.getInt("type_mobil", 0));
        typePrefs.put("motor", preferences.getInt("type_motor", 0));
        typePrefs.put("truk", preferences.getInt("type_truk", 0));

        return getMaxKey(typePrefs);
    }

    private String getMostPreferredBrand() {
        Map<String, Integer> brandPrefs = new HashMap<>();

        // Common brands
        String[] brands = {"honda", "toyota", "yamaha", "suzuki", "mitsubishi", "daihatsu"};
        for (String brand : brands) {
            brandPrefs.put(brand, preferences.getInt("brand_" + brand, 0));
        }

        return getMaxKey(brandPrefs);
    }

    private String getMostPreferredPriceRange() {
        Map<String, Integer> pricePrefs = new HashMap<>();
        pricePrefs.put("low", preferences.getInt("price_range_low", 0));
        pricePrefs.put("medium", preferences.getInt("price_range_medium", 0));
        pricePrefs.put("high", preferences.getInt("price_range_high", 0));

        return getMaxKey(pricePrefs);
    }

    private int getActivityLevel() {
        // Calculate based on total interactions
        int totalViews = 0;
        int totalSearches = 0;
        int totalFavorites = 0;

        Map<String, ?> allPrefs = preferences.getAll();
        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            if (entry.getKey().startsWith("view_count_")) {
                totalViews += (Integer) entry.getValue();
            } else if (entry.getKey().startsWith("search_")) {
                totalSearches += (Integer) entry.getValue();
            } else if (entry.getKey().startsWith("favorite_")) {
                totalFavorites += (Integer) entry.getValue();
            }
        }

        int totalActivity = totalViews + (totalSearches * 2) + (totalFavorites * 3);

        // Return activity level (1-10)
        return Math.min(10, totalActivity / 10 + 1);
    }

    // Customize dashboard based on profile
    public DashboardCustomization getCustomDashboard() {
        UserProfile profile = getUserProfile();

        // Customize greeting
        String greeting = getPersonalizedGreeting(profile.activityLevel);

        // Customize featured section
        String featuredType = profile.preferredType != null ?
                profile.preferredType : "mobil";

        // Customize quick filters
        String[] quickFilters = getQuickFilters(profile);

        // Customize color theme
        String colorTheme = getColorTheme(profile.preferredType);

        return new DashboardCustomization(greeting, featuredType,
                quickFilters, colorTheme);
    }

    private String getPersonalizedGreeting(int activityLevel) {
        long lastActive = preferences.getLong("last_active", 0);
        long daysSinceActive = (System.currentTimeMillis() - lastActive) / (1000 * 60 * 60 * 24);

        if (activityLevel >= 7) {
            return "Selamat datang kembali, pembeli setia! 🌟";
        } else if (daysSinceActive > 7) {
            return "Lama tidak berjumpa! Ada kendaraan baru untukmu 🚗";
        } else {
            return "Halo! Siap menemukan kendaraan impianmu? 😊";
        }
    }

    private String[] getQuickFilters(UserProfile profile) {
        String[] filters = new String[3];

        if (profile.preferredType != null) {
            filters[0] = profile.preferredType;
        } else {
            filters[0] = "Semua";
        }

        if (profile.preferredBrand != null) {
            filters[1] = profile.preferredBrand;
        } else {
            filters[1] = "Semua Merek";
        }

        if (profile.preferredPriceRange != null) {
            filters[2] = profile.preferredPriceRange;
        } else {
            filters[2] = "Semua Harga";
        }

        return filters;
    }

    private String getColorTheme(String preferredType) {
        if (preferredType == null) return "default";

        switch (preferredType.toLowerCase()) {
            case "mobil":
                return "blue";
            case "motor":
                return "red";
            case "truk":
                return "green";
            default:
                return "default";
        }
    }

    // Helper methods
    private void incrementPreference(String key) {
        int count = preferences.getInt(key, 0);
        preferences.edit().putInt(key, count + 1).apply();
    }

    private String getPriceRange(int price) {
        if (price < 50000000) {
            return "low";
        } else if (price < 200000000) {
            return "medium";
        } else {
            return "high";
        }
    }

    private String getMaxKey(Map<String, Integer> map) {
        String maxKey = null;
        int maxValue = 0;

        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue() > maxValue) {
                maxValue = entry.getValue();
                maxKey = entry.getKey();
            }
        }

        return maxKey;
    }

    // Clear personalization data
    public void resetPersonalization() {
        preferences.edit().clear().apply();
    }

    // User Profile class
    public static class UserProfile {
        public String preferredType;
        public String preferredBrand;
        public String preferredPriceRange;
        public int activityLevel;

        public UserProfile(String preferredType, String preferredBrand,
                           String preferredPriceRange, int activityLevel) {
            this.preferredType = preferredType;
            this.preferredBrand = preferredBrand;
            this.preferredPriceRange = preferredPriceRange;
            this.activityLevel = activityLevel;
        }
    }

    // Dashboard Customization class
    public static class DashboardCustomization {
        public String greeting;
        public String featuredType;
        public String[] quickFilters;
        public String colorTheme;

        public DashboardCustomization(String greeting, String featuredType,
                                      String[] quickFilters, String colorTheme) {
            this.greeting = greeting;
            this.featuredType = featuredType;
            this.quickFilters = quickFilters;
            this.colorTheme = colorTheme;
        }
    }
}