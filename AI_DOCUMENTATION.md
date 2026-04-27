# Dokumentasi Fitur AI - AdiMarket

Dokumen ini menjelaskan implementasi fitur AI (Phase 1 - 12) yang terintegrasi dalam aplikasi AdiMarket.

## 1. Persiapan & Setup (Phase 1 - 2)
- **Teknologi**: TensorFlow Lite, Google ML Kit.
- **Konfigurasi**: Penambahan dependensi pada `build.gradle` untuk mendukung pemrosesan tensor dan pengenalan teks.

## 2. Pengenalan Gambar & Klasifikasi Kendaraan (Phase 3 - 4)
- **File**: `ImageRecognitionActivity.java`
- **Fungsi**: Mendeteksi jenis kendaraan (Mobil, Motor, Truk) menggunakan kamera.
- **Model**: TensorFlow Lite (`vehicle_model.tflite`).

## 3. Prediksi Harga (Phase 5)
- **File**: `PricePredictionActivity.java`
- **Fungsi**: Memberikan estimasi harga jual berdasarkan tahun, merek, dan kondisi kendaraan menggunakan logika regresi.

## 4. AI Chatbot (Phase 6)
- **File**: `ChatbotActivity.java`
- **Fungsi**: Asisten virtual untuk menjawab pertanyaan pengguna seputar marketplace.

## 5. OCR Scanner (Phase 7)
- **File**: `OCRActivity.java`
- **Fungsi**: Mengekstraksi teks dari dokumen kendaraan (seperti STNK) secara otomatis menggunakan Google ML Kit.

## 6. Recommendation AI (Phase 8)
- **File**: `RecommendationEngine.java`
- **Fungsi**: Memberikan rekomendasi kendaraan yang dipersonalisasi berdasarkan riwayat klik pengguna.

## 7. Voice Search (Phase 9)
- **File**: `VoiceSearchActivity.java`
- **Fungsi**: Mencari kendaraan di marketplace menggunakan perintah suara.

## 8. Fraud Detection (Phase 10)
- **File**: `FraudDetection.java`
- **Fungsi**: Mendeteksi iklan yang mencurigakan berdasarkan pola harga dan deskripsi yang tidak wajar.

## 9. Sentiment Analysis (Phase 11)
- **File**: `SentimentAnalyzer.java`
- **Fungsi**: Menganalisis ulasan pengguna untuk menentukan apakah sentimennya Positif, Negatif, atau Netral.

## 10. Personalization Engine (Phase 12)
- **File**: `PersonalizationEngine.java`
- **Fungsi**: Menyesuaikan antarmuka dan konten aplikasi berdasarkan preferensi unik setiap pengguna.

---
**Status Proyek**: Phase 1 - 15 Selesai.
**Optimasi**: Menggunakan Multithreading dan NNAPI untuk performa maksimal.
