package com.example.adimarket;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecommendationEngine {

    private Context context;
    private SharedPreferences preferences;
    private DatabaseHelper dbHelper;

    public RecommendationEngine(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        this.dbHelper = new DatabaseHelper(context);
    }

    // Get personalized vehicle recommendations
    public List<Vehicle> getRecommendations(String userId) {
        List<Vehicle> recommendations = new ArrayList<>();

        // Get user preferences
        String preferredType = preferences.getString("preferred_type", "");
        int minPrice = preferences.getInt("min_price", 0);
        int maxPrice = preferences.getInt("max_price", 500000000);

        // Get user view history
        List<String> viewHistory = getUserViewHistory(userId);

        // Collaborative filtering
        List<Vehicle> similarUserPreferences = getSimilarUserPreferences(userId);

        // Content-based filtering
        List<Vehicle> contentBased = getContentBasedRecommendations(preferredType, minPrice, maxPrice);

        // Combine recommendations
        recommendations.addAll(similarUserPreferences);
        recommendations.addAll(contentBased);

        // Remove duplicates and sort by score
        recommendations = removeDuplicatesAndSort(recommendations);

        return recommendations.subList(0, Math.min(10, recommendations.size()));
    }

    private List<String> getUserViewHistory(String userId) {
        // Get from database
        return dbHelper.getViewHistory(userId);
    }

    private List<Vehicle> getSimilarUserPreferences(String userId) {
        List<Vehicle> recommendations = new ArrayList<>();

        // Find users with similar preferences
        List<String> similarUsers = findSimilarUsers(userId);

        // Get their favorite vehicles
        for (String similarUserId : similarUsers) {
            List<DatabaseHelper.VehicleData> favorites = dbHelper.getUserFavorites(similarUserId);
            for (DatabaseHelper.VehicleData data : favorites) {
                recommendations.add(mapToVehicle(data));
            }
        }

        return recommendations;
    }

    private List<String> findSimilarUsers(String userId) {
        // Simple collaborative filtering
        List<String> similarUsers = new ArrayList<>();

        // Get user's viewed vehicles
        List<String> userViews = getUserViewHistory(userId);

        // Find users who viewed similar vehicles
        Map<String, Integer> userSimilarity = new HashMap<>();

        for (String vehicleId : userViews) {
            List<String> usersWhoViewed = dbHelper.getUsersWhoViewedVehicle(vehicleId);

            for (String otherUser : usersWhoViewed) {
                if (!otherUser.equals(userId)) {
                    userSimilarity.put(otherUser,
                            userSimilarity.getOrDefault(otherUser, 0) + 1);
                }
            }
        }

        // Get top similar users
        List<Map.Entry<String, Integer>> list = new ArrayList<>(userSimilarity.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });

        for (int i = 0; i < Math.min(5, list.size()); i++) {
            similarUsers.add(list.get(i).getKey());
        }

        return similarUsers;
    }

    private List<Vehicle> getContentBasedRecommendations(String type, int minPrice, int maxPrice) {
        List<DatabaseHelper.VehicleData> vehiclesData = dbHelper.searchVehicles(type, minPrice, maxPrice);
        List<Vehicle> vehicles = new ArrayList<>();

        for (DatabaseHelper.VehicleData data : vehiclesData) {
            vehicles.add(mapToVehicle(data));
        }

        // Filter based on user preferences
        List<Vehicle> filtered = new ArrayList<>();

        for (Vehicle vehicle : vehicles) {
            if (matchesUserPreferences(vehicle)) {
                filtered.add(vehicle);
            }
        }

        return filtered;
    }

    private boolean matchesUserPreferences(Vehicle vehicle) {
        String preferredBrand = preferences.getString("preferred_brand", "");
        String preferredColor = preferences.getString("preferred_color", "");
        int preferredYear = preferences.getInt("preferred_year", 2015);

        boolean matches = true;

        if (!preferredBrand.isEmpty()) {
            matches = matches && vehicle.brand.equalsIgnoreCase(preferredBrand);
        }

        if (!preferredColor.isEmpty()) {
            matches = matches && vehicle.color.equalsIgnoreCase(preferredColor);
        }

        matches = matches && vehicle.year >= preferredYear;

        return matches;
    }

    private List<Vehicle> removeDuplicatesAndSort(List<Vehicle> vehicles) {
        Map<String, Vehicle> uniqueVehicles = new HashMap<>();

        for (Vehicle vehicle : vehicles) {
            if (!uniqueVehicles.containsKey(vehicle.id)) {
                uniqueVehicles.put(vehicle.id, vehicle);
            }
        }

        List<Vehicle> result = new ArrayList<>(uniqueVehicles.values());

        // Sort by popularity/score
        result.sort((a, b) -> Integer.compare(b.viewCount, a.viewCount));

        return result;
    }

    // Track user interaction
    public void trackView(String userId, String vehicleId) {
        dbHelper.insertViewHistory(userId, vehicleId);
    }

    public void trackFavorite(String userId, String vehicleId) {
        dbHelper.insertFavorite(userId, vehicleId);
    }

    // Update user preferences
    public void updatePreferences(String type, String brand, String color, int year, int minPrice, int maxPrice) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("preferred_type", type);
        editor.putString("preferred_brand", brand);
        editor.putString("preferred_color", color);
        editor.putInt("preferred_year", year);
        editor.putInt("min_price", minPrice);
        editor.putInt("max_price", maxPrice);
        editor.apply();
    }

    private Vehicle mapToVehicle(DatabaseHelper.VehicleData data) {
        int price = 0;
        try {
            price = Integer.parseInt(data.price.replaceAll("[^0-9]", ""));
        } catch (Exception e) {}

        return new Vehicle(
                String.valueOf(data.id),
                data.name,
                data.type,
                data.brand,
                data.color,
                data.year,
                price,
                0 // Initial view count
        );
    }

    // Vehicle class
    public static class Vehicle {
        String id;
        String name;
        String type;
        String brand;
        String color;
        int year;
        int price;
        int viewCount;

        public Vehicle(String id, String name, String type, String brand,
                       String color, int year, int price, int viewCount) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.brand = brand;
            this.color = color;
            this.year = year;
            this.price = price;
            this.viewCount = viewCount;
        }
    }
}