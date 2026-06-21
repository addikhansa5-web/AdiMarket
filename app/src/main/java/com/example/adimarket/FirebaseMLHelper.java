package com.example.adimarket;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

/**
 * Helper for Firebase & ML Kit Integration (Point 2)
 */
public class FirebaseMLHelper {

    private Context context;
    private TextRecognizer recognizer;

    public FirebaseMLHelper(Context context) {
        this.context = context;
        // Initialize ML Kit Text Recognizer
        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    public void processDocument(Bitmap bitmap, final MLCallback callback) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        
        recognizer.process(image)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text visionText) {
                        callback.onSuccess(visionText.getText());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseML", "Error: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    public interface MLCallback {
        void onSuccess(String result);
        void onError(String error);
    }
}
