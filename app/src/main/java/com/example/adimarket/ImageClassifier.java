package com.example.adimarket;

import android.content.Context;
import android.graphics.Bitmap;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import java.io.IOException;
import java.nio.MappedByteBuffer;

/**
 * Utility class for Image Classification using TensorFlow Lite.
 * Handles model loading and inference for general object recognition.
 */
public class ImageClassifier {

    private Interpreter tflite;
    private final String MODEL_PATH = "vehicle_model.tflite";
    private final String[] labels = {"Mobil", "Motor", "Truk"};
    private final int INPUT_SIZE = 224;

    public ImageClassifier(Context context) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);
        options.setUseNNAPI(true);
        MappedByteBuffer modelFile = FileUtil.loadMappedFile(context, MODEL_PATH);
        tflite = new Interpreter(modelFile, options);
    }

    public ClassificationResult classify(Bitmap bitmap) {
        if (tflite == null) return null;

        // Preprocess: Resize image to model input size
        Bitmap resizedImage = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);

        // Convert to TensorImage
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(resizedImage);

        // Prepare output buffer (assuming 3 classes as per existing activity)
        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(
                new int[]{1, 3}, DataType.FLOAT32);

        // Run inference
        tflite.run(tensorImage.getBuffer(), outputBuffer.getBuffer());

        // Get results
        float[] results = outputBuffer.getFloatArray();

        // Find highest probability
        int maxIndex = 0;
        float maxProb = results[0];

        for (int i = 1; i < results.length; i++) {
            if (results[i] > maxProb) {
                maxProb = results[i];
                maxIndex = i;
            }
        }

        return new ClassificationResult(labels[maxIndex], maxProb);
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }

    public static class ClassificationResult {
        public String label;
        public float confidence;

        public ClassificationResult(String label, float confidence) {
            this.label = label;
            this.confidence = confidence;
        }
    }
}
