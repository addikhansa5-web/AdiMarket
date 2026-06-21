package com.example.adimarket;

import android.content.Context;
import android.os.Debug;
import android.util.Log;

/**
 * AI Optimization Utility (Point 13)
 * Handles performance monitoring, memory management, and model caching strategies.
 */
public class AIOptimizer {

    private static final String TAG = "AIOptimizer";

    public static void logMemoryUsage(String stage) {
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        Log.d(TAG, "Memory Usage at [" + stage + "]: " + (usedMemory / 1024 / 1024) + " MB");
    }

    /**
     * Measure inference time for AI models
     */
    public static void trackInferenceTime(String modelName, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        Log.d(TAG, "Model [" + modelName + "] Inference Time: " + duration + " ms");
        
        if (duration > 500) {
            Log.w(TAG, "Warning: Inference time too high! Consider reducing model size or using GPU delegate.");
        }
    }

    /**
     * Clear cache to free up memory for heavy AI tasks
     */
    public static void optimizeForInference(Context context) {
        System.gc();
        Log.d(TAG, "Garbage Collection triggered before AI task.");
    }
}
