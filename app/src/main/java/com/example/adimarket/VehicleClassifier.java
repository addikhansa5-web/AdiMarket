package com.example.adimarket;

import android.content.Context;
import android.graphics.Bitmap;
import java.io.IOException;

/**
 * High-level classifier for Vehicle types.
 * Extends ImageClassifier functionality with vehicle-specific taxonomy.
 */
public class VehicleClassifier {

    private ImageClassifier imageClassifier;

    public VehicleClassifier(Context context) throws IOException {
        imageClassifier = new ImageClassifier(context);
    }

    public VehicleType classifyVehicle(Bitmap bitmap) {
        ImageClassifier.ClassificationResult result = imageClassifier.classify(bitmap);
        
        if (result == null) return new VehicleType("Unknown", 0);

        // Map general labels to specific vehicle taxonomy if needed
        String type = result.label;
        float confidence = result.confidence;

        return new VehicleType(type, confidence);
    }

    public void close() {
        if (imageClassifier != null) {
            imageClassifier.close();
        }
    }

    public static class VehicleType {
        public String typeName;
        public float confidence;

        public VehicleType(String typeName, float confidence) {
            this.typeName = typeName;
            this.confidence = confidence;
        }
    }
}
